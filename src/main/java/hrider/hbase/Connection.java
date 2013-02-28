package hrider.hbase;

import hrider.config.ConnectionDetails;
import hrider.config.GlobalConfig;
import hrider.data.DataCell;
import hrider.data.DataRow;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
     * Creates a new table in the hbase cluster.
     *
     * @param tableName The name of the table to create.
     * @throws IOException Error accessing hbase.
     */
    public void createTable(String tableName) throws IOException, TableNotFoundException {
        createTable(tableName, new ArrayList<String>());
    }

    /**
     * Creates a new table in the hbase cluster with the specified column families.
     *
     * @param tableName      The name of the table to create.
     * @param columnFamilies A list of column families to add to the table.
     * @throws IOException Error accessing hbase.
     */
    public void createTable(String tableName, Collection<String> columnFamilies) throws IOException, TableNotFoundException {
        if (!this.hbaseAdmin.tableExists(tableName)) {
            this.hbaseAdmin.createTable(new HTableDescriptor(tableName));

            for (HbaseActionListener listener : this.listeners) {
                listener.tableOperation(tableName, "created");
            }
        }

        if (!columnFamilies.isEmpty()) {
            HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));
            for (HColumnDescriptor column : td.getColumnFamilies()) {
                columnFamilies.remove(column.getNameAsString());
            }

            if (!columnFamilies.isEmpty()) {
                createFamilies(tableName, columnFamilies);
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

        // Delete your table
        if (this.hbaseAdmin.isTableEnabled(tableName)) {
            this.hbaseAdmin.disableTable(tableName);
        }
        this.hbaseAdmin.deleteTable(tableName);

        // Recreate your table
        this.hbaseAdmin.createTable(td);

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
    public void copyTable(String targetTable, String sourceTable, Connection sourceCluster) throws IOException, TableNotFoundException {
        HTable source = sourceCluster.factory.get(sourceTable);
        HTable target = this.factory.get(targetTable);

        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(targetTable));

        Collection<String> families = new HashSet<String>();
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            families.add(column.getNameAsString());
        }

        Scan scan = new Scan();
        scan.setCaching(GlobalConfig.instance().getBatchSizeForRead());

        ResultScanner scanner = source.getScanner(scan);

        boolean isValid;
        do {
            Result result = scanner.next();

            isValid = result != null;
            if (isValid) {
                Collection<String> familiesToCreate = new HashSet<String>();

                Put put = new Put(result.getRow());
                for (KeyValue keyValue : result.list()) {
                    put.add(keyValue);

                    String columnFamily = Bytes.toStringBinary(keyValue.getFamily());
                    if (!families.contains(columnFamily)) {
                        familiesToCreate.add(columnFamily);
                    }
                }

                if (!familiesToCreate.isEmpty()) {
                    createFamilies(targetTable, familiesToCreate);

                    families.addAll(familiesToCreate);
                }

                target.put(put);

                for (HbaseActionListener listener : this.listeners) {
                    listener.copyOperation(sourceCluster.serverName, sourceTable, this.serverName, targetTable, result);
                }
            }
        }
        while (isValid);
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

        Collection<String> families = new ArrayList<String>();
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            families.add(column.getNameAsString());
        }

        Collection<String> familiesToCreate = new HashSet<String>();
        List<Put> puts = new ArrayList<Put>();

        for (DataRow row : rows) {
            Put put = new Put(row.getKey().toByteArray());

            for (DataCell cell : row.getCells()) {
                String[] parts = cell.getColumnName().split(":");
                if (parts.length == 2) {
                    if (!families.contains(parts[0])) {
                        familiesToCreate.add(parts[0]);
                    }

                    byte[] family = Bytes.toBytesBinary(parts[0]);
                    byte[] column = Bytes.toBytesBinary(parts[1]);
                    byte[] value = cell.getTypedValue().toByteArray();

                    put.add(family, column, value);
                }
            }

            puts.add(put);

            for (HbaseActionListener listener : this.listeners) {
                listener.rowOperation(tableName, row, "added");
            }
        }

        if (!familiesToCreate.isEmpty()) {
            createFamilies(tableName, familiesToCreate);
        }

        HTable table = this.factory.get(tableName);
        table.put(puts);
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

        Collection<String> families = new ArrayList<String>();
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            families.add(column.getNameAsString());
        }

        Collection<String> familiesToCreate = new HashSet<String>();

        Put put = new Put(row.getKey().toByteArray());
        for (DataCell cell : row.getCells()) {
            String[] parts = cell.getColumnName().split(":");
            if (parts.length == 2) {
                if (!families.contains(parts[0])) {
                    familiesToCreate.add(parts[0]);
                }

                byte[] family = Bytes.toBytesBinary(parts[0]);
                byte[] column = Bytes.toBytesBinary(parts[1]);
                byte[] value = cell.getTypedValue().toByteArray();

                put.add(family, column, value);
            }
        }

        if (!familiesToCreate.isEmpty()) {
            createFamilies(tableName, familiesToCreate);
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
        table.delete(new Delete(row.getKey().toByteArray()));

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
    public Collection<String> getColumnFamilies(String tableName) throws IOException, TableNotFoundException {
        Collection<String> columnFamilies = new ArrayList<String>();

        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            columnFamilies.add(column.getNameAsString());
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
        return new Scanner(this.factory, tableName);
    }

    /**
     * Gets a query scanner for the specified table.
     *
     * @param tableName The name of the table to be scanned.
     * @param query     A query to be used with the scanner.
     * @return An instance of the query scanner.
     */
    public QueryScanner getScanner(String tableName, Query query) {
        return new QueryScanner(this.factory, tableName, query);
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
    private void createFamilies(String tableName, Iterable<String> families) throws IOException {
        if (this.hbaseAdmin.isTableEnabled(tableName)) {
            this.hbaseAdmin.disableTable(tableName);
        }

        for (String family : families) {
            this.hbaseAdmin.addColumn(tableName, new HColumnDescriptor(family));

            for (HbaseActionListener listener : this.listeners) {
                listener.columnOperation(tableName, family, "added");
            }
        }

        this.hbaseAdmin.enableTable(tableName);
    }
    //endregion
}
