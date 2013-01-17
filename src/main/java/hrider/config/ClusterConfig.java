package hrider.config;

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
 *          This class is responsible for saving/retrieving configuration properties related to the specific cluster.
 */
public class ClusterConfig extends PropertiesConfig {

    //region Constructor

    /**
     * Initializes a new instance of the {@link ClusterConfig} class.
     *
     * @param name The name of the configuration file.
     */
    public ClusterConfig(String name) {
        super(name);
    }
    //endregion

    //region Public Methods

    /**
     * Gets a connection details used to connect to the hbase cluster.
     *
     * @return A reference to the {@link ConnectionDetails} class.
     */
    public ConnectionDetails getConnection() {
        final String hbaseHost = get(String.class, "connection.hbase.host", null);
        final String hbasePort = get(String.class, "connection.hbase.port", null);
        final String zookeeperHost = get(String.class, "connection.zookeeper.host", null);
        final String zookeeperPort = get(String.class, "connection.zookeeper.port", null);

        if (hbaseHost == null || zookeeperHost == null || hbasePort == null || zookeeperPort == null) {
            return null;
        }

        return new ConnectionDetails() {{
            setHbaseServer(new ServerDetails(hbaseHost, hbasePort));
            setZookeeperServer(new ServerDetails(zookeeperHost, zookeeperPort));
        }};
    }

    /**
     * Saves a connection details to the configuration.
     *
     * @param connection A connection details to save.
     */
    public void setConnection(ConnectionDetails connection) {
        set("connection.hbase.host", connection.getHbaseServer().getHost());
        set("connection.hbase.port", connection.getHbaseServer().getPort());
        set("connection.zookeeper.host", connection.getZookeeperServer().getHost());
        set("connection.zookeeper.port", connection.getZookeeperServer().getPort());

        save();
    }

    /**
     * Gets a configuration data related to the specified table.
     *
     * @param clazz The class that represents the type of the data to be returned.
     * @param table The name of the table.
     * @param <T>   The type of the data to be returned.
     * @return The data of the specified type.
     */
    public <T> T getTableConfig(Class<T> clazz, String table) {
        return get(clazz, String.format("table.%s", table));
    }

    /**
     * Gets a configuration data related to the specified table and a corresponding column.
     *
     * @param clazz  The class that represents the type of the data to be returned.
     * @param table  The name of the table.
     * @param column The name of the column.
     * @param <T>    The type of the data to be returned.
     * @return The data of the specified type.
     */
    public <T> T getTableConfig(Class<T> clazz, String table, String column) {
        return get(clazz, String.format("table.%s.%s", table, column));
    }

    /**
     * Gets a configuration data related to the specified table of the provided column and a corresponding key.
     *
     * @param clazz  The class that represents the type of the data to be returned.
     * @param table  The name of the table.
     * @param column The name of the column.
     * @param key    The key to identify the data.
     * @param <T>    The type of the data to be returned.
     * @return The data of the specified type.
     */
    public <T> T getTableConfig(Class<T> clazz, String table, String column, String key) {
        return get(clazz, String.format("table.%s.%s.%s", table, column, key));
    }

    /**
     * Sets a configuration data for the specified table.
     *
     * @param table The name of the table.
     * @param value The data to set.
     */
    public void setTableConfig(String table, String value) {
        set(String.format("table.%s", table), value);
        save();
    }

    /**
     * Sets a configuration data for the specified table and column.
     *
     * @param table  The name of the table.
     * @param column The name of the column.
     * @param value  The data to set.
     */
    public void setTableConfig(String table, String column, String value) {
        set(String.format("table.%s.%s", table, column), value);
        save();
    }

    /**
     * Sets a configuration data for the specified table, column and key.
     *
     * @param table  The name of the table.
     * @param column The name of the column.
     * @param key    The additional identifier.
     * @param value  The data to set.
     */
    public void setTableConfig(String table, String column, String key, String value) {
        set(String.format("table.%s.%s.%s", table, column, key), value);
        save();
    }

    /**
     * Gets a filter to filter tables.
     *
     * @return A string representing a filter.
     */
    public String getTablesFilter() {
        return get(String.class, "filter.tables");
    }

    /**
     * Gets a type of the tables filter: simple or regex.
     *
     * @return A string representing a filter type.
     */
    public String getTablesFilterType() {
        return get(String.class, "filter.tables.type");
    }

    /**
     * Gets a filter to filter columns.
     *
     * @param table The name of the table.
     * @return A string representing a filter.
     */
    public String getColumnsFilter(String table) {
        return get(String.class, String.format("filter.%s.columns", table));
    }

    /**
     * Gets a type of the columns filter: simple or regex.
     *
     * @param table The name of the table.
     * @return A string representing a filter type.
     */
    public String getColumnsFilterType(String table) {
        return get(String.class, String.format("filter.%s.columns.type", table));
    }

    /**
     * Saves a tables filter.
     *
     * @param filter A filter to save.
     * @param type A type of the filter: simple or regex.
     */
    public void setTablesFilter(String filter, String type) {
        set("filter.tables", filter);
        set("filter.tables.type", type);
        save();
    }

    /**
     * Saves a columns filter.
     *
     * @param table The name of the table.
     * @param filter A filter to save.
     * @param type A type of the filter: simple or regex.
     */
    public void setColumnsFilter(String table, String filter, String type) {
        set(String.format("filter.%s.columns", table), filter);
        set(String.format("filter.%s.columns.type", table), type);
        save();
    }
    //endregion
}
