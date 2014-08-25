package hrider.hbase;

import hrider.config.ConnectionDetails;
import hrider.config.GlobalConfig;
import hrider.data.ColumnFamily;
import hrider.data.DataCell;
import hrider.data.DataRow;
import hrider.data.TableDescriptor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.encoding.DataBlockEncoding;
import org.apache.hadoop.hbase.io.hfile.CacheConfig;
import org.apache.hadoop.hbase.io.hfile.HFile;
import org.apache.hadoop.hbase.regionserver.StoreFile;
import org.apache.hadoop.hbase.regionserver.StoreFileScanner;
import org.apache.hadoop.hbase.regionserver.metrics.SchemaMetrics;
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
 *          This class represents a data access to the hbase tables.
 */
public class Connection {

    //region Variables
    /**
     * The name of the muster server where the hbase is located.
     */
    private String                    serverName;
    /**
     * A reference to the hbase administration class.
     */
    private HBaseAdmin                hbaseAdmin;
    /**
     * A reference to the tables factory class.
     */
    private TableFactory              factory;
    /**
     * A configuration used to connect to hbase.
     */
    private ConnectionDetails         connectionDetails;
    /**
     * A list of listeners.
     */
    private List<HbaseActionListener> listeners;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link Connection} class.
     *
     * @param connectionDetails A configuration to be used to connect to the hbase administration.
     * @throws IOException Error connecting to hbase.
     */
    public Connection(ConnectionDetails connectionDetails) throws IOException {
        this.connectionDetails = connectionDetails;
        this.serverName = connectionDetails.getZookeeper().getHost();
        this.listeners = new ArrayList<HbaseActionListener>();

        try {
            Configuration config = connectionDetails.createConfig();

            this.factory = new TableFactory(config);
            this.hbaseAdmin = new HBaseAdmin(config);
        }
        catch (Exception e) {
            throw new IOException("Failed to access hbase administration.", e);
        }
    }
    //endregion

    //region Public Properties

    /**
     * Gets a reference to the {@link TableFactory} instance used by connection.
     *
     * @return A reference to the {@link TableFactory} instance.
     */
    public TableFactory getTableFactory() {
        return this.factory;
    }

    /**
     * Gets a reference to the {@link Configuration} instance used by connection.
     *
     * @return A reference to the {@link Configuration} instance.
     */
    public Configuration getConfiguration() {
        return this.hbaseAdmin.getConfiguration();
    }

    /**
     * Gets a configuration used to connect to hbase.
     *
     * @return A reference to the {@link ConnectionDetails} class.
     */
    public ConnectionDetails getConnectionDetails() {
        return this.connectionDetails;
    }

    /**
     * Gets the name of the hbase muster server.
     *
     * @return The server name.
     */
    public String getServerName() {
        return this.serverName;
    }
    //endregion

    //region Public Methods

    /**
     * Gets a descriptor for the specified table.
     *
     * @param tableName The name of the table.
     * @return A new instance of the {@link TableDescriptor}.
     * @throws IOException            Error accessing hbase.
     * @throws TableNotFoundException The specified table does not exist.
     */
    public TableDescriptor getTableDescriptor(String tableName) throws IOException, TableNotFoundException {
        return new TableDescriptor(this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName)));
    }

    /**
     * Adds a listener for hbase related operations.
     *
     * @param listener A listener to add.
     */
    public void addListener(HbaseActionListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes the listener.
     *
     * @param listener A listener to remove.
     */
    public void removeListener(HbaseActionListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Resets the caching.
     */
    public void reset() {
        this.factory.clear();
    }

    /**
     * Checks whether the table with the specified name exists on the hbase cluster.
     *
     * @param tableName The name of the table to check.
     * @return True if the table exists on the hbase cluster or False otherwise.
     * @throws IOException Error accessing hbase.
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean tableExists(String tableName) throws IOException {
        return tableName != null && this.hbaseAdmin.tableExists(tableName);
    }

    /**
     * Checks whether the specified table is enabled.
     *
     * @param tableName The name of the table to check.
     * @return True if the table is enabled or False otherwise.
     * @throws IOException Error accessing hbase.
     */
    public boolean tableEnabled(String tableName) throws IOException {
        return TableUtil.isMetaTable(tableName) || tableName != null && this.hbaseAdmin.isTableEnabled(tableName);
    }

    /**
     * Creates a new table or modifies an existing one in the hbase cluster.
     *
     * @param tableName The name of the table to create.
     * @throws IOException Error accessing hbase.
     */
    public void createOrModifyTable(String tableName) throws IOException, TableNotFoundException {
        createOrModifyTable(new TableDescriptor(tableName));
    }

    /**
     * Creates a new table or modifies an existing one in the hbase cluster.
     *
     * @param tableDescriptor The descriptor of the table to create.
     * @throws IOException Error accessing hbase.
     */
    public void createOrModifyTable(TableDescriptor tableDescriptor) throws IOException, TableNotFoundException {
        if (this.hbaseAdmin.tableExists(tableDescriptor.getName())) {
            if (this.hbaseAdmin.isTableEnabled(tableDescriptor.getName())) {
                this.hbaseAdmin.disableTable(tableDescriptor.getName());
            }

            this.hbaseAdmin.modifyTable(Bytes.toBytes(tableDescriptor.getName()), tableDescriptor.toDescriptor());
            this.hbaseAdmin.enableTable(tableDescriptor.getName());

            for (HbaseActionListener listener : this.listeners) {
                listener.tableOperation(tableDescriptor.getName(), "modified");
            }
        }
        else {
            this.hbaseAdmin.createTable(tableDescriptor.toDescriptor());

            for (HbaseActionListener listener : this.listeners) {
                listener.tableOperation(tableDescriptor.getName(), "created");
            }
        }
    }

    /**
     * Deletes a table from the hbase cluster.
     *
     * @param tableName The name of the table to delete.
     * @throws IOException Error accessing hbase.
     */
    public void deleteTable(String tableName) throws IOException {
        if (this.hbaseAdmin.tableExists(tableName)) {
            if (this.hbaseAdmin.isTableEnabled(tableName)) {
                this.hbaseAdmin.disableTable(tableName);
            }
            this.hbaseAdmin.deleteTable(tableName);

            for (HbaseActionListener listener : this.listeners) {
                listener.tableOperation(tableName, "deleted");
            }
        }
    }

    /**
     * Truncates a table. All the data will be removed.
     *
     * @param tableName The name of the table to be truncated.
     * @throws IOException Error accessing hbase.
     */
    public void truncateTable(String tableName) throws IOException, TableNotFoundException {
        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));

        HTable table = this.factory.get(tableName);

        byte[][] startKeys = table.getStartKeys();
        byte[][] splitKeys = new byte[startKeys.length - 1][];

        System.arraycopy(startKeys, 1, splitKeys, 0, startKeys.length - 1);

        // Delete your table
        if (this.hbaseAdmin.isTableEnabled(tableName)) {
            this.hbaseAdmin.disableTable(tableName);
        }
        this.hbaseAdmin.deleteTable(tableName);

        // Recreate your table
        this.hbaseAdmin.createTable(td, splitKeys);

        for (HbaseActionListener listener : this.listeners) {
            listener.tableOperation(tableName, "truncated");
        }
    }

    /**
     * Copies all the data from one table to another. The tables can be on different clusters.
     *
     * @param targetTable   The name of the target table.
     * @param sourceTable   The name of the source table.
     * @param sourceCluster The source cluster where the source table is located.
     * @throws IOException Error accessing hbase on one of the clusters or on both clusters.
     */
    public void copyTable(TableDescriptor targetTable, TableDescriptor sourceTable, Connection sourceCluster) throws IOException, TableNotFoundException {
        createOrModifyTable(targetTable);

        HTable source = sourceCluster.factory.get(sourceTable.getName());
        HTable target = this.factory.get(targetTable.getName());

        Scan scan = new Scan();
        scan.setCaching(GlobalConfig.instance().getBatchSizeForRead());

        ResultScanner scanner = source.getScanner(scan);
        try {
            List<Put> puts = new ArrayList<Put>();

            int batchSize = GlobalConfig.instance().getBatchSizeForWrite();

            boolean isValid;

            do {
                Result result = scanner.next();

                isValid = result != null;
                if (isValid) {
                    Put put = new Put(result.getRow());
                    for (KeyValue kv : result.list()) {
                        put.add(kv);
                    }

                    puts.add(put);

                    if (puts.size() == batchSize) {
                        target.put(puts);
                        puts.clear();
                    }

                    for (HbaseActionListener listener : this.listeners) {
                        listener.copyOperation(sourceCluster.serverName, sourceTable.getName(), this.serverName, targetTable.getName(), result);
                    }
                }
            }
            while (isValid);

            // add the last puts to the table.
            if (!puts.isEmpty()) {
                target.put(puts);
            }
        }
        finally {
            scanner.close();
        }
    }

    /**
     * Saves a table locally to an HFile.
     *
     * @param tableName The name of the table.
     * @param path      The path tot he file.
     * @throws IOException Error accessing hbase.
     */
    public void saveTable(String tableName, String path) throws IOException {
        FileSystem fs = FileSystem.getLocal(this.getConfiguration());
        HTable table = this.factory.get(tableName);

        Configuration cacheConfig = new Configuration(this.getConfiguration());
        cacheConfig.setFloat(HConstants.HFILE_BLOCK_CACHE_SIZE_KEY, 0.0f);

        StoreFile.Writer writer = new StoreFile.WriterBuilder(
            this.getConfiguration(), new CacheConfig(cacheConfig), fs, HFile.DEFAULT_BLOCKSIZE).withFilePath(new Path(path)).build();

        ResultScanner scanner = null;

        try {
            Scan scan = new Scan();
            scan.setCaching(GlobalConfig.instance().getBatchSizeForRead());

            scanner = table.getScanner(scan);

            boolean isValid;
            do {
                Result result = scanner.next();

                isValid = result != null;
                if (isValid) {
                    for (KeyValue keyValue : result.list()) {
                        writer.append(keyValue);
                    }

                    for (HbaseActionListener listener : this.listeners) {
                        listener.saveOperation(tableName, path, result);
                    }
                }
            }
            while (isValid);
        }
        finally {
            if (scanner != null) {
                scanner.close();
            }

            writer.close();
        }
    }

    /**
     * Flushes a in memory portion of the table into the HFile.
     *
     * @param tableName The name of the table to flush.
     * @throws IOException          Error accessing hbase.
     * @throws InterruptedException
     */
    public void flushTable(String tableName) throws IOException, InterruptedException {
        this.hbaseAdmin.flush(tableName);
    }

    /**
     * Enables the table.
     *
     * @param tableName The name of the table to enable.
     * @throws IOException Error accessing hbase.
     */
    public void enableTable(String tableName) throws IOException {
        this.hbaseAdmin.enableTable(tableName);
    }

    /**
     * Loads a locally saved HFile to an existing table.
     *
     * @param tableName The name of the table to load to.
     * @param path      The path to the HFile.
     * @throws IOException Error accessing hbase.
     */
    public void loadTable(String tableName, String path) throws IOException, TableNotFoundException {
        FileSystem fs = FileSystem.getLocal(this.getConfiguration());
        HTable table = this.factory.get(tableName);

        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));

        Collection<ColumnFamily> families = new HashSet<ColumnFamily>();
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            families.add(new ColumnFamily(column));
        }

        StoreFile.Reader reader = new StoreFile.Reader(fs, new Path(path), new CacheConfig(this.getConfiguration()), DataBlockEncoding.NONE);

        try {
            StoreFileScanner scanner = reader.getStoreFileScanner(false, false);
            SchemaMetrics.configureGlobally(this.getConfiguration());

            // move to the first row.
            scanner.seek(new KeyValue(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

            Collection<ColumnFamily> familiesToCreate = new HashSet<ColumnFamily>();

            Put put = null;
            List<Put> puts = new ArrayList<Put>();

            boolean isValid;
            int batchSize = GlobalConfig.instance().getBatchSizeForWrite();

            do {
                KeyValue kv = scanner.next();

                isValid = kv != null;
                if (isValid) {
                    ColumnFamily columnFamily = new ColumnFamily(Bytes.toStringBinary(kv.getFamily()));
                    if (!families.contains(columnFamily)) {
                        familiesToCreate.add(columnFamily);
                    }

                    if (put == null) {
                        put = new Put(kv.getRow());
                        puts.add(put);
                    }

                    if (!Arrays.equals(put.getRow(), kv.getRow())) {
                        for (HbaseActionListener listener : this.listeners) {
                            listener.loadOperation(tableName, path, put);
                        }

                        if (puts.size() == batchSize) {
                            if (!familiesToCreate.isEmpty()) {
                                createFamilies(tableName, toDescriptors(familiesToCreate));

                                families.addAll(familiesToCreate);
                                familiesToCreate.clear();
                            }

                            HTableUtil.bucketRsPut(table, puts);
                            puts.clear();
                        }

                        put = new Put(kv.getRow());
                        puts.add(put);
                    }

                    put.add(kv);
                }
            }
            while (isValid);

            // add the last put to the table.
            if (!puts.isEmpty()) {
                for (HbaseActionListener listener : this.listeners) {
                    listener.loadOperation(tableName, path, put);
                }

                if (!familiesToCreate.isEmpty()) {
                    createFamilies(tableName, toDescriptors(familiesToCreate));
                }

                HTableUtil.bucketRsPut(table, puts);
            }
        }
        finally {
            reader.close(false);
        }
    }

    /**
     * Sets or adds a rows to the table.
     *
     * @param tableName The name of the table to update.
     * @param rows      A list of rows to set/add.
     * @throws IOException Error accessing hbase.
     */
    public void setRows(String tableName, Iterable<DataRow> rows) throws IOException, TableNotFoundException {
        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));

        Collection<ColumnFamily> families = new ArrayList<ColumnFamily>();
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            families.add(new ColumnFamily(column));
        }

        Collection<ColumnFamily> familiesToCreate = new HashSet<ColumnFamily>();
        List<Put> puts = new ArrayList<Put>();

        for (DataRow row : rows) {
            Put put = new Put(row.getKey().getValue());

            for (DataCell cell : row.getCells()) {
                if (!cell.isKey()) {
                    if (!families.contains(cell.getColumn().getColumnFamily())) {
                        familiesToCreate.add(cell.getColumn().getColumnFamily());
                    }

                    byte[] family = Bytes.toBytesBinary(cell.getColumn().getFamily());
                    byte[] column = Bytes.toBytesBinary(cell.getColumn().getName());
                    byte[] value = cell.getValueAsByteArray();

                    put.add(family, column, value);
                }
            }

            puts.add(put);

            for (HbaseActionListener listener : this.listeners) {
                listener.rowOperation(tableName, row, "added");
            }
        }

        if (!familiesToCreate.isEmpty()) {
            createFamilies(tableName, toDescriptors(familiesToCreate));
        }

        HTable table = this.factory.get(tableName);
        HTableUtil.bucketRsPut(table, puts);
    }

    /**
     * Sets or adds a row to the table.
     *
     * @param tableName The name of the table to update.
     * @param row       The row to set/add.
     * @throws IOException Error accessing hbase.
     */
    public void setRow(String tableName, DataRow row) throws IOException, TableNotFoundException {
        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));

        Collection<ColumnFamily> families = new ArrayList<ColumnFamily>();
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            families.add(new ColumnFamily(column));
        }

        Collection<ColumnFamily> familiesToCreate = new HashSet<ColumnFamily>();

        Put put = new Put(row.getKey().getValue());
        for (DataCell cell : row.getCells()) {
            if (!cell.isKey()) {
                if (!families.contains(cell.getColumn().getColumnFamily())) {
                    familiesToCreate.add(cell.getColumn().getColumnFamily());
                }

                byte[] family = Bytes.toBytesBinary(cell.getColumn().getFamily());
                byte[] column = Bytes.toBytesBinary(cell.getColumn().getName());
                byte[] value = cell.getValueAsByteArray();

                put.add(family, column, value);
            }
        }

        if (!familiesToCreate.isEmpty()) {
            createFamilies(tableName, toDescriptors(familiesToCreate));
        }

        HTable table = this.factory.get(tableName);
        table.put(put);

        for (HbaseActionListener listener : this.listeners) {
            listener.rowOperation(tableName, row, "added");
        }
    }

    /**
     * Deletes a row from the table.
     *
     * @param tableName The name of the table.
     * @param row       The row to be deleted.
     * @throws IOException Error accessing hbase.
     */
    public void deleteRow(String tableName, DataRow row) throws IOException {
        HTable table = this.factory.get(tableName);
        table.delete(new Delete(row.getKey().getValue()));

        for (HbaseActionListener listener : this.listeners) {
            listener.rowOperation(tableName, row, "removed");
        }
    }

    /**
     * Gets a list of all table names in the hbase cluster.
     *
     * @return A list of table names.
     * @throws IOException Error accessing hbase.
     */
    public Collection<String> getTables() throws IOException {
        Collection<String> tables = new ArrayList<String>();
        for (HTableDescriptor tableDescriptor : this.hbaseAdmin.listTables()) {
            tables.add(tableDescriptor.getNameAsString());
        }
        return tables;
    }

    /**
     * Gets all column families of the specified table.
     *
     * @param tableName The name of the table which column families should be retrieved.
     * @return A list of column family names.
     * @throws IOException Error accessing hbase.
     */
    public Collection<ColumnFamily> getColumnFamilies(String tableName) throws IOException, TableNotFoundException {
        Collection<ColumnFamily> columnFamilies = new ArrayList<ColumnFamily>();

        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            columnFamilies.add(new ColumnFamily(column));
        }

        return columnFamilies;
    }

    /**
     * Gets a scanner for the specified table.
     *
     * @param tableName The name of the table to be scanned.
     * @return An instance of the scanner.
     */
    public Scanner getScanner(String tableName) {
        return new Scanner(this, tableName);
    }

    /**
     * Gets a query scanner for the specified table.
     *
     * @param tableName The name of the table to be scanned.
     * @param query     A query to be used with the scanner.
     * @return An instance of the query scanner.
     */
    public QueryScanner getScanner(String tableName, Query query) {
        return new QueryScanner(this, tableName, query);
    }
    //endregion

    //region Private Methods

    /**
     * Adds column families to the specified table.
     *
     * @param tableName The name of the table to add column families.
     * @param families  A list of column families to add.
     * @throws IOException Error accessing hbase.
     */
    private void createFamilies(String tableName, Iterable<HColumnDescriptor> families) throws IOException {
        if (this.hbaseAdmin.isTableEnabled(tableName)) {
            this.hbaseAdmin.disableTable(tableName);
        }

        for (HColumnDescriptor family : families) {
            this.hbaseAdmin.addColumn(tableName, family);

            for (HbaseActionListener listener : this.listeners) {
                listener.columnOperation(tableName, family.getNameAsString(), "added");
            }
        }

        this.hbaseAdmin.enableTable(tableName);
    }

    /**
     * Converts column family to column descriptor.
     *
     * @param families A list of column families to converters.
     * @return A list of column descriptors.
     */
    private static Iterable<HColumnDescriptor> toDescriptors(Iterable<ColumnFamily> families) {
        Collection<HColumnDescriptor> descriptors = new ArrayList<HColumnDescriptor>();
        for (ColumnFamily family : families) {
            descriptors.add(family.toDescriptor());
        }
        return descriptors;
    }
    //endregion
}
