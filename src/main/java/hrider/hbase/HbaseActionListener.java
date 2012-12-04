package hrider.hbase;

import hrider.data.DataRow;
import org.apache.hadoop.hbase.client.Result;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 10/24/12
 * Time: 9:22 AM
 */
public interface HbaseActionListener {
    void copyOperation(String source, String target, String table, Result result);
    void tableOperation(String tableName, String operation);
    void rowOperation(String tableName, DataRow row, String operation);
    void columnOperation(String tableName, String column, String operation);
}
