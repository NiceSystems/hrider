package hrider.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/23/12
 * Time: 2:20 PM
 */
public class TableFactory {

    private Configuration config;
    private Map<String, HTable> tables;

    public TableFactory(Configuration config) {
        this.config = config;
        this.tables = new HashMap<String, HTable>();
    }

    public synchronized HTable create(String tableName) throws IOException {
        if (this.tables.containsKey(tableName)) {
            return this.tables.get(tableName);
        }

        HTable table = new HTable(this.config, tableName);
        this.tables.put(tableName, table);

        return table;
    }

    public synchronized void dispose(String tableName) throws IOException {
        HTable table = this.tables.remove(tableName);
        if (table != null) {
            table.close();
        }
    }

    public synchronized void clear() {
        this.tables.clear();
    }
}
