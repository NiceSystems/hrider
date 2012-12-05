package hrider.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
 *          This class represents a factory for creating tables. Once the table is created it is cached for future use.
 */
public class TableFactory {

    //region Variables
    /**
     * A configuration object containing connection related information.
     */
    private Configuration config;
    /**
     * A collection of created tables.
     */
    private Map<String, HTable> tables;
    //endregion

    //region Constructor

    /**
     * Initializes an instance of the {@link TableFactory} class.
     * @param config A configuration object required to connect to hbase.
     */
    public TableFactory(Configuration config) {
        this.config = config;
        this.tables = new HashMap<String, HTable>();
    }
    //endregion

    //region Public Methods

    /**
     * Creates a new table if it wasn't created yet or returns the already created one.
     * @param tableName The name of the table to get/create.
     * @return A reference to the {@link HTable}.
     * @throws IOException Error accessing hbase.
     */
    public synchronized HTable get(String tableName) throws IOException {
        if (this.tables.containsKey(tableName)) {
            return this.tables.get(tableName);
        }

        HTable table = new HTable(this.config, tableName);
        this.tables.put(tableName, table);

        return table;
    }

    /**
     * Cleans the resources of the specified table.
     * @param tableName The name of the table to dispose.
     * @throws IOException Error accessing hbase.
     */
    public synchronized void dispose(String tableName) throws IOException {
        HTable table = this.tables.remove(tableName);
        if (table != null) {
            table.close();
        }
    }

    /**
     * Clears the cache.
     */
    public synchronized void clear() {
        this.tables.clear();
    }
    //endregion
}
