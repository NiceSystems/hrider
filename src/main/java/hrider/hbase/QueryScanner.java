package hrider.hbase;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Copyright (C) 2012 NICE Systems ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Igor Cher
 * @version %I%, %G%
 *          <p/>
 *          This class represents a scanner that uses query to filter the rows returned from the hbase.
 */
public class QueryScanner extends Scanner {

    //region Constants
    /**
     * A constant representing a size of the unicode character.
     */
    private static final int UNICODE_CHAR_SIZE = 4;
    //endregion

    //region Variables
    /**
     * A query to be used with he scanner.
     */
    private Query query;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link QueryScanner} class.
     *
     * @param factory   The reference to the table factory class.
     * @param tableName The name of the table the scan is going to be done on.
     * @param query     The query to use.
     */
    public QueryScanner(TableFactory factory, String tableName, Query query) {
        super(factory, tableName);

        this.query = query;
    }
    //endregion

    //region Public Methods

    /**
     * Gets a query which is used during the scan operation.
     *
     * @return A query.
     */
    public Query getQuery() {
        return this.query;
    }

    /**
     * Sets a new query to use.
     *
     * @param query A new query to set.
     */
    @SuppressWarnings("ObjectEquality")
    public void setQuery(Query query) {
        if (this.query != query) {
            resetCurrent(null);
        }
        this.query = query;
    }
    //endregion

    //region Protected Methods

    /**
     * Gets an hbase scanner.
     *
     * @return An hbase scanner.
     * @throws IOException Error accessing hbase.
     */
    @Override
    protected Scan getScanner() throws IOException {
        Scan scan = super.getScanner();

        if (this.query != null) {
            if (this.query.getStartKey() != null) {
                scan.setStartRow(this.query.getStartKey());
            }

            if (this.query.getEndKey() != null) {
                scan.setStopRow(padWithMaxUnicode(this.query.getEndKey()));
            }

            if (this.query.getStartDate() != null && this.query.getEndDate() != null) {
                scan.setTimeRange(this.query.getStartDate().getTime(), this.query.getEndDate().getTime());
            }

            if (this.query.getWord() != null) {
                WritableByteArrayComparable comparator;

                switch (this.query.getOperator()) {
                    case Contains:
                        comparator = new SubstringComparator(this.query.getWord());
                        break;
                    case StartsWith:
                        comparator = new RegexStringComparator(String.format("^%s.*", this.query.getWord()));
                        break;
                    case EndsWith:
                        comparator = new RegexStringComparator(String.format(".*%s$", this.query.getWord()));
                        break;
                    case Less:
                    case LessOrEqual:
                    case Equal:
                    case NotEqual:
                    case GreaterOrEqual:
                    case Greater:
                        comparator = new BinaryComparator(this.query.getWordAsByteArray());
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("The specified operator type '%s' is not supported.", this.query.getOperator()));
                }

                scan.setFilter(
                    new SingleColumnValueFilter(
                        Bytes.toBytesBinary(this.query.getFamily()),
                        Bytes.toBytesBinary(this.query.getColumn()),
                        this.query.getOperator().toFilter(),
                        comparator));
            }
        }

        return scan;
    }

    /**
     * Checks if the row is valid according to the query. If a query is done on a specific column and the row does not contain this column the row
     * is considered invalid.
     *
     * @param row The row to check.
     * @return True if the query doesn't have a specific column or a row contains the column used with the query or False otherwise.
     */
    @Override
    protected boolean isValidRow(Result row) {
        Query localQuery = this.query;
        if (localQuery != null && localQuery.getFamily() != null && localQuery.getColumn() != null) {
            return row.containsColumn(Bytes.toBytesBinary(localQuery.getFamily()), Bytes.toBytesBinary(localQuery.getColumn()));
        }
        return true;
    }
    //endregion

    //region Private Methods

    /**
     * Adds additional 4 bytes to the end key byte array. This prevents the scan go further than the end key.
     *
     * @param endKey The end key to update.
     * @return A new byte array containing extra bytes.
     */
    private static byte[] padWithMaxUnicode(byte[] endKey) {
        ByteBuffer buffer = ByteBuffer.allocate(endKey.length + UNICODE_CHAR_SIZE);
        buffer.put(endKey);
        buffer.put(Byte.MAX_VALUE);
        buffer.put(Byte.MAX_VALUE);
        buffer.put(Byte.MAX_VALUE);
        buffer.put(Byte.MAX_VALUE);
        return buffer.array();
    }
    //endregion
}
