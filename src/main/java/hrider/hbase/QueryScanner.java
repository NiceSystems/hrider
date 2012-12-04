package hrider.hbase;

import hrider.data.DataRow;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 9/5/12
 * Time: 4:56 PM
 */
public class QueryScanner extends Scanner {

    //region Variables
    private static final int UNICODE_CHAR_SIZE = 4;

    private Query query;
    //endregion

    //region Constructor
    public QueryScanner(TableFactory factory, String tableName, Query query) {
        super(factory, tableName);

        this.query = query;
    }
    //endregion

    //region Public Methods
    public Query getQuery() {
        return this.query;
    }

    @SuppressWarnings("ObjectEquality")
    public void setQuery(Query query) {
        if (this.query != query) {
            resetCurrent(null);
        }
        this.query = query;
    }
    //endregion

    //region Protected Methods
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
                        Bytes.toBytes(this.query.getFamily()), Bytes.toBytes(this.query.getColumn()), this.query.getOperator().toFilter(), comparator));
            }
        }

        return scan;
    }

    @Override
    protected boolean isValidRow(Result result) {
        Query localQuery = this.query;
        if (localQuery != null && localQuery.getFamily() != null && localQuery.getColumn() != null) {
            return result.containsColumn(Bytes.toBytes(localQuery.getFamily()), Bytes.toBytes(localQuery.getColumn()));
        }
        return true;
    }
    //endregion

    //region Private Methods
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
