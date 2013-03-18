package hrider.hbase;

import hrider.data.DataRow;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;

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
 *          This interface represents a listener for hbase operations.
 */
public interface HbaseActionListener {

    /**
     * An event that is raised when a row is copied from one table to another.
     *
     * @param source      The name of the source machine.
     * @param sourceTable The name of the table to be copied from.
     * @param target      The name of the target machine.
     * @param targetTable The name of the table to be copied to.
     * @param result      The row to be copied.
     */
    void copyOperation(String source, String sourceTable, String target, String targetTable, Result result);

    /**
     * An event that is raised when a row is saved to the file.
     *
     * @param tableName The name of the table.
     * @param path      The path to the file.
     * @param result    The row to be saved.
     */
    void saveOperation(String tableName, String path, Result result);

    /**
     * An event that is raised when a row is loaded to the table from the file.
     *
     * @param tableName The name of the table.
     * @param path      The path to the file.
     * @param put       The row to be loaded.
     */
    void loadOperation(String tableName, String path, Put put);

    /**
     * An event that is raised when a table is created/deleted/truncated.
     *
     * @param tableName The name of the table.
     * @param operation The current operation to be performed.
     */
    void tableOperation(String tableName, String operation);

    /**
     * An event that is raised when a row is created/deleted/updated.
     *
     * @param tableName The name of the table that contains the row.
     * @param row       The row to be created/deleted/updated.
     * @param operation The current operation to be performed.
     */
    void rowOperation(String tableName, DataRow row, String operation);

    /**
     * An event that is raised when a column is created.
     *
     * @param tableName The name of the table that contains the column.
     * @param column    The name of the column.
     * @param operation The current operation to be performed.
     */
    void columnOperation(String tableName, String column, String operation);
}
