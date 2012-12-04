package hrider.hbase;

import hrider.data.DataCell;
import hrider.data.DataRow;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/23/12
 * Time: 2:27 PM
 */
public class HbaseHelper {

    //region Variables
    private String                    serverName;
    private HBaseAdmin                hbaseAdmin;
    private TableFactory              factory;
    private List<HbaseActionListener> listeners;
    //endregion

    //region Constructor
    public HbaseHelper(Configuration config) throws IOException {
        this.serverName = config.get("hbase.zookeeper.quorum");
        this.factory = new TableFactory(config);
        this.listeners = new ArrayList<HbaseActionListener>();

        try {
            this.hbaseAdmin = new HBaseAdmin(config);
        }
        catch (Exception e) {
            throw new IOException("Failed to access hbase administration.", e);
        }
    }
    //endregion

    //region Public Methods
    public String getServerName() {
        return this.serverName;
    }
    //endregion

    //region Public Methods
    public void addListener(HbaseActionListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(HbaseActionListener listener) {
        this.listeners.remove(listener);
    }

    public void reset() {
        this.factory.clear();
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean tableExists(String tableName) throws IOException {
        return this.hbaseAdmin.tableExists(tableName);
    }

    public void createTable(String tableName) throws IOException {
        createTable(tableName, new ArrayList<String>());
    }

    public void createTable(String tableName, Collection<String> columnFamilies) throws IOException {
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

    public void truncateTable(String tableName) throws IOException {
        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));

        // Delete your table
        if (this.hbaseAdmin.isTableEnabled(tableName)) {
            this.hbaseAdmin.disableTable(tableName);
        }
        this.hbaseAdmin.deleteTable(tableName);

        // Recreate your talbe
        this.hbaseAdmin.createTable(td);

        for (HbaseActionListener listener : this.listeners) {
            listener.tableOperation(tableName, "truncated");
        }
    }

    public void copyTable(String targetTable, String sourceTable, HbaseHelper sourceCluster) throws IOException {
        HTable source = sourceCluster.factory.create(sourceTable);
        HTable target = this.factory.create(targetTable);

        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(targetTable));

        Collection<String> families = new HashSet<String>();
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            families.add(column.getNameAsString());
        }

        ResultScanner scanner = source.getScanner(new Scan());

        boolean isValid;
        do {
            Result result = scanner.next();

            isValid = result != null;
            if (isValid) {
                Collection<String> familiesToCreate = new ArrayList<String>();

                Put put = new Put(result.getRow());
                for (KeyValue keyValue : result.list()) {
                    put.add(keyValue);

                    String columnFamily = Bytes.toString(keyValue.getFamily());
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
                    listener.copyOperation(this.serverName, sourceCluster.serverName, targetTable, result);
                }
            }
        }
        while (isValid);
    }

    public void setRow(String tableName, DataRow row) throws IOException {
        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));

        Collection<String> families = new ArrayList<String>();
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            families.add(column.getNameAsString());
        }

        Collection<String> familiesToCreate = new ArrayList<String>();

        Put put = new Put(row.getKey().toByteArray());
        for (DataCell cell : row.getCells()) {
            String[] parts = cell.getColumnName().split(":");
            if (parts.length == 2) {
                if (!families.contains(parts[0])) {
                    familiesToCreate.add(parts[0]);
                }

                byte[] family = Bytes.toBytes(parts[0]);
                byte[] column = Bytes.toBytes(parts[1]);
                byte[] value = cell.getTypedValue().toByteArray();

                put.add(family, column, value);
            }
        }

        if (!familiesToCreate.isEmpty()) {
            createFamilies(tableName, familiesToCreate);
        }

        HTable table = this.factory.create(tableName);
        table.put(put);

        for (HbaseActionListener listener : this.listeners) {
            listener.rowOperation(tableName, row, "added");
        }
    }

    public void deleteRow(String tableName, DataRow row) throws IOException {
        HTable table = this.factory.create(tableName);
        table.delete(new Delete(row.getKey().toByteArray()));

        for (HbaseActionListener listener : this.listeners) {
            listener.rowOperation(tableName, row, "removed");
        }
    }

    public Collection<String> getTables() throws IOException {
        Collection<String> tables = new ArrayList<String>();
        for (HTableDescriptor tableDescriptor : this.hbaseAdmin.listTables()) {
            tables.add(tableDescriptor.getNameAsString());
        }
        return tables;
    }

    public Collection<String> getColumnFamilies(String tableName) throws IOException {
        Collection<String> columnFamilies = new ArrayList<String>();

        HTableDescriptor td = this.hbaseAdmin.getTableDescriptor(Bytes.toBytes(tableName));
        for (HColumnDescriptor column : td.getColumnFamilies()) {
            columnFamilies.add(column.getNameAsString());
        }

        return columnFamilies;
    }

    public Scanner getScanner(String tableName) {
        return new Scanner(this.factory, tableName);
    }

    public QueryScanner getScanner(String tableName, Query query) {
        return new QueryScanner(this.factory, tableName, query);
    }
    //endregion

    //region Private Methods
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
