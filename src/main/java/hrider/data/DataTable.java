package hrider.data;

import hrider.hbase.HbaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 10/24/12
 * Time: 8:50 AM
 */
public class DataTable {

    private String tableName;
    private HbaseHelper hbaseHelper;
    private List<DataRow> rows;

    public DataTable(String tableName) {
        this(tableName, null);
    }

    public DataTable(String tableName, HbaseHelper hbaseHelper) {
        this.tableName = tableName;
        this.hbaseHelper = hbaseHelper;
        this.rows = new ArrayList<DataRow>();
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public HbaseHelper getHbaseHelper() {
        return this.hbaseHelper;
    }

    public void addRow(DataRow row) {
        this.rows.add(row);
    }

    public void removeRow(DataRow row) {
        this.rows.remove(row);
    }

    public Iterable<DataRow> getRows() {
        return this.rows;
    }

    public int getRowsCount() {
        return this.rows.size();
    }
}
