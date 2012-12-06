package hrider.hbase;

import hrider.data.DataCell;
import hrider.data.DataRow;
import hrider.data.ObjectType;
import hrider.data.TypedObject;
import hrider.ui.MessageHandler;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

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
 *          This class represents a scanner over the hbase tables.
 */
public class Scanner {

    //region Variables
    /**
     * The table factory that is used to access hbase tables.
     */
    private TableFactory            factory;
    /**
     * The name of the hbase table the scanner is going to be executed on.
     */
    private String                  tableName;
    /**
     * Holds a total number of rows in the table. This parameter is calculated by scanning over the whole table.
     * This operation is time consuming and its result should be cached.
     */
    private long                    rowsCount;
    /**
     * The map of column types. The key is the name of the column and the value is the type of the objects within the column.
     */
    private Map<String, ObjectType> columnTypes;
    /**
     * A list of markers. The marker is used for pagination to mark where the previous scan has stopped.
     */
    private Stack<Marker>           markers;
    /**
     * A list of loaded rows.
     */
    private Collection<DataRow>     current;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link Scanner} class.
     *
     * @param factory   The reference to the table factory.
     * @param tableName The name of the table to be scanned.
     */
    public Scanner(TableFactory factory, String tableName) {
        this.factory = factory;
        this.tableName = tableName;
        this.rowsCount = 0;
        this.markers = new Stack<Marker>();
    }
    //endregion

    //region Public Properties

    /**
     * Gets the name of the table.
     *
     * @return The table name.
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * Gets a list of columns. If there is no columns at this moment they will be loaded according to the provided rows number. In other words only columns
     * of loaded rows will be returned.
     *
     * @param rowsNumber The number of rows to look for the columns in case there is no columns loaded at this moment.
     * @return A list of columns.
     */
    public Collection<String> getColumns(int rowsNumber) {
        if (this.markers.isEmpty()) {
            try {
                return loadColumns(rowsNumber);
            }
            catch (IOException e) {
                MessageHandler.addError(String.format("Failed to load columns for table %s.", this.tableName), e);
                return new ArrayList<String>();
            }
        }
        else {
            return peekMarker().columns;
        }
    }

    /**
     * Gets a mapping of column names to column types.
     *
     * @return A map of column types.
     */
    public Map<String, ObjectType> getColumnTypes() {
        return this.columnTypes;
    }

    /**
     * Sets a mapping of column names to column types.
     *
     * @param columnTypes A new map of column types.
     */
    public void setColumnTypes(Map<String, ObjectType> columnTypes) {
        this.columnTypes = columnTypes;
    }
    //endregion

    //region Public Methods

    /**
     * Updates a column type of all cells in the specified column.
     *
     * @param columnName The name of the column.
     * @param columnType The new column type.
     */
    public void updateColumnType(String columnName, ObjectType columnType) {
        Collection<DataRow> rows = this.current;
        if (rows != null) {
            for (DataRow row : rows) {
                row.updateColumnType(columnName, columnType);
            }
        }
    }

    /**
     * Resets the cache.
     *
     * @param startKey The key the scan should start from. This parameter can be null.
     */
    public void resetCurrent(TypedObject startKey) {
        this.current = null;
        this.rowsCount = 0;
        this.markers.clear();

        if (startKey != null) {
            this.markers.push(new Marker(startKey, new ArrayList<DataRow>(), new ArrayList<String>()));
        }
    }

    /**
     * Gets a list of already loaded rows.
     * @return A list of rows.
     */
    public Collection<DataRow> current() {
        return this.current;
    }

    /**
     * Gets a list of rows loaded starting from the beginning.
     *
     * @param rowsNumber The number of rows to load.
     * @return A list of rows.
     * @throws IOException Error accessing hbase.
     */
    public Collection<DataRow> current(int rowsNumber) throws IOException {
        if (this.current == null || this.current.size() != rowsNumber) {
            this.markers.clear();
            this.current = next(0, rowsNumber);
        }
        return this.current;
    }

    /**
     * Gets a list of rows loaded starting from the previous position.
     *
     * @param rowsNumber The number of rows to load.
     * @return A list of rows.
     * @throws IOException Error accessing hbase.
     */
    public Collection<DataRow> next(int rowsNumber) throws IOException {
        this.current = next(this.markers.isEmpty() ? 0 : 1, rowsNumber);
        return this.current;
    }

    /**
     * Gets a list of previously loaded rows.
     *
     * @return A list of rows.
     * @throws IOException Error accessing hbase.
     */
    public Collection<DataRow> prev() throws IOException {
        if (!this.markers.isEmpty()) {
            if (this.markers.size() > 1) {
                popMarker();
            }
            this.current = peekMarker().rows;
        }
        return this.current;
    }

    /**
     * Gets the total number of rows in the table. This value is calculated by scanning throughout the whole table to count the rows. This value is then cached
     * for future uses.
     *
     * @return A total number of rows in the table.
     * @throws IOException Error accessing hbase.
     */
    public synchronized long getRowsCount() throws IOException {
        if (this.rowsCount == 0) {
            Scan scan = getScanner();
            scan.setCaching(1000);
            scan.setBatch(1000);

            HTable table = this.factory.get(this.tableName);
            ResultScanner scanner = table.getScanner(scan);

            int count = 0;
            for (Result rr = scanner.next() ; rr != null ; rr = scanner.next()) {
                if (isValidRow(rr)) {
                    ++count;
                }
            }

            this.rowsCount = count;
        }
        return this.rowsCount;
    }
    //endregion

    //region Protected Methods

    /**
     * Gets the hbase scanner.
     *
     * @return A hbase scanner
     * @throws IOException Error accessing hbase.
     */
    protected Scan getScanner() throws IOException {
        return new Scan();
    }

    /**
     * Checks if the row is valid. The default implementation is return 'True'. This method should be overridden by the derived classes.
     *
     * @param row A row to check.
     * @return True if the row is valid and False otherwise.
     */
    protected boolean isValidRow(Result row) {
        return true;
    }
    //endregion

    //region Private Methods

    /**
     * Loads a specified number of rows from the hbase.
     *
     * @param scanner    The hbase scanner to retrieve the data.
     * @param offset     The offset to start from.
     * @param rowsNumber The number of rows to load.
     * @param rows       The loaded rows. This is the output parameter.
     * @param columns    The columns loaded from rows. This is the output parameter.
     * @return A key of the last loaded row. Used to mark the current position for the next scan.
     * @throws IOException Error accessing hbase.
     */
    protected TypedObject loadRows(ResultScanner scanner, int offset, int rowsNumber, Map<TypedObject, DataRow> rows, Collection<String> columns) throws
        IOException {
        ObjectType keyType = this.columnTypes.get("key");

        int index = 0;
        boolean isValid;
        TypedObject key = null;

        do {
            Result result = scanner.next();

            isValid = result != null && rows.size() < rowsNumber;
            if (isValid && isValidRow(result)) {
                if (index >= offset) {
                    key = new TypedObject(keyType, result.getRow());

                    DataRow row = rows.get(key);
                    if (row == null) {
                        row = new DataRow(key);
                        row.addCell(new DataCell(row, "key", key));

                        rows.put(key, row);
                    }

                    NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyMap = result.getMap();
                    for (NavigableMap.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> family : familyMap.entrySet()) {
                        for (NavigableMap.Entry<byte[], NavigableMap<Long, byte[]>> quantifier : family.getValue().entrySet()) {
                            String columnName = String.format("%s:%s", Bytes.toString(family.getKey()), Bytes.toString(quantifier.getKey()));

                            ObjectType columnType = ObjectType.String;
                            if (this.columnTypes.containsKey(columnName)) {
                                columnType = this.columnTypes.get(columnName);
                            }

                            for (NavigableMap.Entry<Long, byte[]> cell : quantifier.getValue().entrySet()) {
                                row.addCell(new DataCell(row, columnName, new TypedObject(columnType, cell.getValue())));
                            }

                            if (!columns.contains(columnName)) {
                                columns.add(columnName);
                            }
                        }
                    }
                }

                index++;
            }
        }
        while (isValid);

        return key;
    }

    /**
     * Loads column names.
     *
     * @param rowsNumber The number of rows to look for the column names. The column name is a key from key/value pairs in hbase row.
     * @throws IOException Error accessing hbase.
     */
    private Collection<String> loadColumns(int rowsNumber) throws IOException {
        Collection<String> columnNames = new ArrayList<String>();
        HTable table = this.factory.get(this.tableName);

        ResultScanner scanner = table.getScanner(new Scan());
        Result row;

        int counter = 0;

        do {
            row = scanner.next();
            if (row != null) {
                NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyMap = row.getMap();
                for (NavigableMap.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> entry : familyMap.entrySet()) {
                    for (byte[] quantifier : entry.getValue().keySet()) {
                        String columnName = String.format("%s:%s", Bytes.toString(entry.getKey()), Bytes.toString(quantifier));
                        if (!columnNames.contains(columnName)) {
                            columnNames.add(columnName);
                        }
                    }
                }
            }

            counter++;
        }
        while (row != null && counter < rowsNumber);

        return columnNames;
    }

    /**
     * Loads the following rows.
     *
     * @param offset     The offset to start from.
     * @param rowsNumber The number of rows to load.
     * @return A list of loaded rows.
     * @throws IOException Error accessing hbase.
     */
    private Collection<DataRow> next(int offset, int rowsNumber) throws IOException {
        Map<TypedObject, DataRow> rows = new HashMap<TypedObject, DataRow>();

        int itemsNumber = rowsNumber <= 1000 ? rowsNumber : 1000;

        Scan scan = getScanner();
        scan.setCaching(itemsNumber);
        scan.setBatch(itemsNumber);

        if (!this.markers.isEmpty()) {
            scan.setStartRow(peekMarker().key.toByteArray());
        }

        HTable table = this.factory.get(this.tableName);
        ResultScanner scanner = table.getScanner(scan);

        Collection<String> columns = new ArrayList<String>();

        TypedObject lastKey = loadRows(scanner, offset, rowsNumber, rows, columns);
        if (lastKey != null) {
            this.markers.push(new Marker(lastKey, rows.values(), columns));
        }

        return rows.values();
    }

    /**
     * Removes a marker from the stack.
     *
     * @return A marker.
     */
    private Marker popMarker() {
        if (!this.markers.isEmpty()) {
            return this.markers.pop();
        }
        return null;
    }

    /**
     * Peeks the marker from the stack.
     *
     * @return A marker.
     */
    private Marker peekMarker() {
        if (!this.markers.isEmpty()) {
            return this.markers.peek();
        }
        return null;
    }
    //endregion

    /**
     * Represents a marking point between the batches of loaded rows. Each time a new batch of rows is loaded this class holds the last key to
     * start loading the next batch.
     */
    private static class Marker {

        //region Variables
        /**
         * The last key loaded from the previous batch of rows.
         */
        private TypedObject         key;
        /**
         * A list of previously loaded rows.
         */
        private Collection<DataRow> rows;
        /**
         * A list of columns loaded from the rows.
         */
        private Collection<String>  columns;
        //endregion

        //region Constructor

        /**
         * Initializes a new instance of the {@link Marker} class.
         *
         * @param key     The last loaded key.
         * @param rows    A list of loaded rows.
         * @param columns A list of columns loaded from the rows.
         */
        private Marker(TypedObject key, Collection<DataRow> rows, Collection<String> columns) {
            this.key = key;
            this.rows = rows;
            this.columns = columns;
        }
        //endregion

        //region Public Methods
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Marker && ((Marker)obj).key.equals(this.key);
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }
        //endregion
    }
}
