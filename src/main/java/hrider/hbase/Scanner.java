package hrider.hbase;

import hrider.config.GlobalConfig;
import hrider.converters.TypeConverter;
import hrider.data.*;
import hrider.ui.MessageHandler;
import org.apache.commons.lang.time.StopWatch;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;

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
@SuppressWarnings({"OverlyNestedMethod", "ClassWithTooManyMethods"})
public class Scanner {

    //region Variables
    /**
     * The connection which owns the scanner.
     */
    private Connection              connection;
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
     * Holds a rows number in the table if the calculation has not been completed because of the timeout.
     */
    private long                    partialRowsCount;
    /**
     * The number of the last loaded row.
     */
    private long                    lastRow;
    /**
     * The map of column types. The key is the name of the column and the value is the type of the objects within the column.
     */
    private Map<String, ColumnType> columnTypes;
    /**
     * A list of markers. The marker is used for pagination to mark where the previous scan has stopped.
     */
    private Stack<Marker>           markers;
    /**
     * A list of loaded rows.
     */
    private Collection<DataRow>     current;
    /**
     * Indicates if this scanner should support only forward navigation.
     */
    private boolean                 forwardNavigateOnly;
    /**
     * Represents a converter for column names.
     */
    private TypeConverter           columnNameConverter;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link Scanner} class.
     *
     * @param connection The reference to the connection.
     * @param tableName  The name of the table to be scanned.
     */
    public Scanner(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
        this.rowsCount = 0;
        this.lastRow = 0;
        this.markers = new Stack<Marker>();
    }
    //endregion

    //region Public Properties

    /**
     * Gets a reference to the {@link Connection}.
     *
     * @return A reference to the {@link Connection}.
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * Gets a reference to the {@link Configuration} instance used by connection.
     *
     * @return A reference to the {@link Configuration} instance.
     */
    public Configuration getConfiguration() {
        return this.connection.getConfiguration();
    }

    /**
     * Gets the value indicating if this scanner supports only forward navigation.
     *
     * @return True if this instance of the scanner supports only forward navigation or False if both forward and backward navigation are supported.
     */
    public boolean getForwardNavigateOnly() {
        return this.forwardNavigateOnly;
    }

    /**
     * Sets the value indicating if this scanner should support forward navigate only.
     *
     * @param forwardNavigateOnly True if this instance of the scanner should support only forward navigation or False to support both forward and backward navigations.
     */
    public void setForwardNavigateOnly(boolean forwardNavigateOnly) {
        this.forwardNavigateOnly = forwardNavigateOnly;
    }

    /**
     * Gets the name of the table.
     *
     * @return The table name.
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * Indicates that the rows count has been partially calculated.
     *
     * @return True if the rows count has been stopped before reaching end of the table or False otherwise.
     */
    public boolean isRowsCountPartiallyCalculated() {
        return this.partialRowsCount > 0;
    }

    /**
     * Gets a list of columns. If there is no columns at this moment they will be loaded according to the provided rows number. In other words only columns
     * of loaded rows will be returned.
     *
     * @param rowsNumber The number of rows to look for the columns in case there is no columns loaded at this moment.
     * @return A list of columns.
     */
    public Collection<ColumnQualifier> getColumns(int rowsNumber) {
        if (this.markers.isEmpty() || peekMarker().columns.size() != rowsNumber) {
            try {
                return loadColumns(rowsNumber);
            }
            catch (IOException e) {
                MessageHandler.addError(String.format("Failed to load columns for table %s.", this.tableName), e);
                return new ArrayList<ColumnQualifier>();
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
    public Map<String, ColumnType> getColumnTypes() {
        return this.columnTypes;
    }

    /**
     * Sets a mapping of column names to column types.
     *
     * @param columnTypes A new map of column types.
     */
    public void setColumnTypes(Map<String, ColumnType> columnTypes) {
        this.columnTypes = columnTypes;
    }

    /**
     * Gets the last loaded row number.
     *
     * @return The number of the last loaded row.
     */
    public long getLastRow() {
        return this.lastRow;
    }

    /**
     * Indicates if there is more rows to show.
     *
     * @return True if there is more rows to show or False otherwise.
     */
    public boolean hasNext() {
        return this.lastRow < this.rowsCount;
    }

    /**
     * Indicates if the backward navigation is possible.
     *
     * @return True if there are previously loaded rows or False otherwise.
     */
    public boolean hasPrev() {
        return this.markers.size() > 1;
    }
    //endregion

    //region Public Methods

    /**
     * Validates whether the values held by the column can be converted to the specified type.
     *
     * @param columnType The type to check.
     * @return True if the value can be converted to the specified type or False otherwise.
     */
    public boolean isColumnOfType(String columnName, ColumnType columnType) {
        Collection<DataRow> rows = this.current;
        if (rows != null) {
            int counter = 0;
            Iterator<DataRow> iterator = rows.iterator();

            while (counter < 10 && iterator.hasNext()) {
                DataRow row = iterator.next();
                if (!row.isCellOfType(columnName, columnType)) {
                    return false;
                }
                counter++;
            }

            return true;
        }
        return true;
    }

    /**
     * Updates a column type of all cells in the specified column.
     *
     * @param columnName The name of the column.
     * @param columnType The new column type.
     */
    public void updateColumnType(String columnName, ColumnType columnType) {
        Collection<DataRow> rows = this.current;
        if (rows != null) {
            for (DataRow row : rows) {
                row.updateColumnType(columnName, columnType);
            }
        }
    }

    /**
     * Updates converter for the column name.
     *
     * @param converter The new column name converter.
     */
    public void updateColumnNameConverter(TypeConverter converter) {
        this.columnNameConverter = converter;

        Collection<DataRow> rows = this.current;
        if (rows != null) {
            for (DataRow row : rows) {
                row.updateColumnNameConverter(converter);
            }
        }
    }

    /**
     * Resets the cache.
     *
     * @param startKey The key the scan should start from. This parameter can be null.
     */
    public void resetCurrent(ConvertibleObject startKey) {
        this.current = null;
        this.rowsCount = 0;
        this.lastRow = 0;
        this.markers.clear();

        if (startKey != null) {
            this.markers.push(new Marker(startKey, new ArrayList<DataRow>(), new ArrayList<ColumnQualifier>()));
        }
    }

    /**
     * Gets a first row in the table.
     *
     * @return A first row in the table.
     * @throws IOException Error accessing hbase.
     */
    public DataRow getFirstRow() throws IOException {
        Scan scan = getScanner();

        HTable table = this.connection.getTableFactory().get(this.tableName);
        ResultScanner scanner = table.getScanner(scan);

        try {
            Collection<DataRow> rows = new LinkedList<DataRow>();
            Collection<ColumnQualifier> columns = new LinkedList<ColumnQualifier>();

            columns.add(ColumnQualifier.KEY);

            loadRows(scanner, 0, 1, rows, columns);
            if (rows.isEmpty()) {
                return null;
            }

            return rows.iterator().next();
        }
        finally {
            scanner.close();
        }
    }

    /**
     * Gets a list of already loaded rows.
     *
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
        return current(0, rowsNumber);
    }

    /**
     * Gets a list of rows loaded starting from the offset.
     *
     * @param offset     The first row to start loading from.
     * @param rowsNumber The number of rows to load.
     * @return A list of rows.
     * @throws IOException Error accessing hbase.
     */
    public Collection<DataRow> current(long offset, int rowsNumber) throws IOException {
        if (this.current == null ||
            this.current.size() != rowsNumber ||
            this.current.size() + offset != this.lastRow) {

            // offset should start from 1.
            if (offset == 0) {
                offset++;
            }

            this.markers.clear();
            this.current = next(offset - 1, rowsNumber);
            this.lastRow = offset + this.current.size() - 1;
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
        this.lastRow += this.current.size();

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

            this.lastRow -= this.current.size();
            this.current = peekMarker().rows;

            updateColumnNameConverter(columnNameConverter);

            for (Map.Entry<String, ColumnType> entry : this.columnTypes.entrySet()) {
                updateColumnType(entry.getKey(), entry.getValue());
            }
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
    public long getRowsCount(long timeout) throws IOException {
        if (this.rowsCount == 0) {
            this.partialRowsCount = 0;

            Scan scan = getScanner();

            FilterList filters = new FilterList();
            if (scan.getFilter() != null) {
                filters.addFilter(scan.getFilter());
            }

            filters.addFilter(new FirstKeyOnlyFilter());

            scan.setFilter(filters);
            scan.setCaching(GlobalConfig.instance().getBatchSizeForRead());

            HTable table = this.connection.getTableFactory().get(this.tableName);
            ResultScanner scanner = table.getScanner(scan);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {
                int count = 0;
                for (Result rr = scanner.next() ; rr != null ; rr = scanner.next(), count++) {
                    if (stopWatch.getTime() > timeout) {
                        this.partialRowsCount = count;

                        break;
                    }
                }

                this.rowsCount = count;
            }
            finally {
                stopWatch.stop();
                scanner.close();
            }
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
    protected ConvertibleObject loadRows(
        ResultScanner scanner, long offset, int rowsNumber, Collection<DataRow> rows, Collection<ColumnQualifier> columns) throws IOException {

        ColumnType keyType = this.columnTypes.get(ColumnQualifier.KEY.getName());
        Map<ColumnQualifier, ColumnQualifier> loadedColumns = new HashMap<ColumnQualifier, ColumnQualifier>();

        int index = 0;
        boolean isValid;
        ConvertibleObject key = null;

        TypeConverter nameConverter = getColumnNameConverterInternal();

        HTable table = this.connection.getTableFactory().get(this.tableName);
        HTableDescriptor tableDescriptor = table.getTableDescriptor();

        do {
            Result result = scanner.next();

            isValid = result != null && rows.size() < rowsNumber;
            if (isValid && isValidRow(result)) {
                if (index >= offset) {
                    key = new ConvertibleObject(keyType, result.getRow());

                    DataRow row = new DataRow(key);
                    row.addCell(new DataCell(row, ColumnQualifier.KEY, key));

                    NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyMap = result.getMap();
                    for (NavigableMap.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyEntry : familyMap.entrySet()) {
                        HColumnDescriptor columnDescriptor = tableDescriptor.getFamily(familyEntry.getKey());

                        for (NavigableMap.Entry<byte[], NavigableMap<Long, byte[]>> qualifierEntry : familyEntry.getValue().entrySet()) {
                            ColumnQualifier qualifier = new ColumnQualifier(qualifierEntry.getKey(), new ColumnFamily(columnDescriptor), nameConverter);

                            String columnName = qualifier.getFullName();
                            ColumnType columnType = this.columnTypes.get(columnName);

                            if (columnType == null) {
                                columnType = ColumnType.String;
                            }

                            for (NavigableMap.Entry<Long, byte[]> cell : qualifierEntry.getValue().entrySet()) {
                                row.addCell(new DataCell(row, qualifier, new ConvertibleObject(columnType, cell.getValue())));
                            }

                            if (!loadedColumns.containsKey(qualifier)) {
                                columns.add(qualifier);
                                loadedColumns.put(qualifier, null);
                            }
                        }
                    }

                    rows.add(row);
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
    private Collection<ColumnQualifier> loadColumns(int rowsNumber) throws IOException {
        Collection<ColumnQualifier> columns = new ArrayList<ColumnQualifier>();
        columns.add(ColumnQualifier.KEY);

        Map<ColumnQualifier, ColumnQualifier> loadedColumns = new HashMap<ColumnQualifier, ColumnQualifier>();

        int itemsNumber = rowsNumber <= GlobalConfig.instance().getBatchSizeForRead() ? rowsNumber : GlobalConfig.instance().getBatchSizeForRead();

        Scan scan = getScanner();
        scan.setCaching(itemsNumber);

        HTable table = this.connection.getTableFactory().get(this.tableName);
        HTableDescriptor tableDescriptor = table.getTableDescriptor();

        ResultScanner scanner = table.getScanner(scan);
        try {
            TypeConverter nameConverter = getColumnNameConverterInternal();

            Result row;
            int counter = 0;

            do {
                row = scanner.next();
                if (row != null) {
                    NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyMap = row.getMap();
                    for (NavigableMap.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyEntry : familyMap.entrySet()) {
                        HColumnDescriptor columnDescriptor = tableDescriptor.getFamily(familyEntry.getKey());

                        for (byte[] quantifier : familyEntry.getValue().keySet()) {
                            ColumnQualifier columnQualifier = new ColumnQualifier(quantifier, new ColumnFamily(columnDescriptor), nameConverter);
                            if (!loadedColumns.containsKey(columnQualifier)) {
                                columns.add(columnQualifier);
                                loadedColumns.put(columnQualifier, null);
                            }
                        }
                    }
                }

                counter++;
            }
            while (row != null && counter < rowsNumber);

            return columns;
        }
        finally {
            scanner.close();
        }
    }

    /**
     * Loads the following rows.
     *
     * @param offset     The offset to start from.
     * @param rowsNumber The number of rows to load.
     * @return A list of loaded rows.
     * @throws IOException Error accessing hbase.
     */
    private Collection<DataRow> next(long offset, int rowsNumber) throws IOException {
        int itemsNumber = rowsNumber <= GlobalConfig.instance().getBatchSizeForRead() ? rowsNumber : GlobalConfig.instance().getBatchSizeForRead();

        Scan scan = getScanner();
        scan.setCaching(itemsNumber);

        if (!this.markers.isEmpty()) {
            scan.setStartRow(peekMarker().key.getValue());
        }

        if (this.forwardNavigateOnly) {
            // Remove the current marker to reduce the memory load in case backward navigation should not be supported.
            popMarker();
        }

        HTable table = this.connection.getTableFactory().get(this.tableName);
        ResultScanner scanner = table.getScanner(scan);

        try {
            Collection<DataRow> rows = new LinkedList<DataRow>();
            Collection<ColumnQualifier> columns = new LinkedList<ColumnQualifier>();

            columns.add(ColumnQualifier.KEY);

            ConvertibleObject lastKey = loadRows(scanner, offset, rowsNumber, rows, columns);
            if (lastKey != null) {
                this.markers.push(new Marker(lastKey, rows, columns));
            }

            return rows;
        }
        finally {
            scanner.close();
        }
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

    /**
     * Gets configured column name converter or a default one.
     *
     * @return A type converter.
     */
    private TypeConverter getColumnNameConverterInternal() {
        if (this.columnNameConverter != null) {
            return this.columnNameConverter;
        }
        return ColumnType.BinaryString.getConverter();
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
        private ConvertibleObject           key;
        /**
         * A list of previously loaded rows.
         */
        private Collection<DataRow>         rows;
        /**
         * A list of columns loaded from the rows.
         */
        private Collection<ColumnQualifier> columns;
        //endregion

        //region Constructor

        /**
         * Initializes a new instance of the {@link Marker} class.
         *
         * @param key     The last loaded key.
         * @param rows    A list of loaded rows.
         * @param columns A list of columns loaded from the rows.
         */
        private Marker(ConvertibleObject key, Collection<DataRow> rows, Collection<ColumnQualifier> columns) {
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
