package hrider.config;

import java.util.ArrayList;
import java.util.Arrays;
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
        final String zookeeperHost = get(String.class, "connection.zookeeper.host", null);
        final String zookeeperPort = get(String.class, "connection.zookeeper.port", null);

        if (zookeeperHost == null || zookeeperPort == null) {
            return null;
        }

        return new ConnectionDetails() {{
            setZookeeper(new ServerDetails(zookeeperHost, zookeeperPort));
        }};
    }

    /**
     * Saves a connection details to the configuration.
     *
     * @param connection A connection details to save.
     */
    public void setConnection(ConnectionDetails connection) {
        set("connection.zookeeper.host", connection.getZookeeper().getHost());
        set("connection.zookeeper.port", connection.getZookeeper().getPort());
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
     * @param clazz The class that represents the type of the data to be returned.
     * @param table The name of the table.
     * @param key   The key to identify the data.
     * @param <T>   The type of the data to be returned.
     * @return The data of the specified type.
     */
    public <T> T getTableConfig(Class<T> clazz, String table, String key) {
        return get(clazz, String.format("table.%s.%s", table, key));
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
    }

    /**
     * Sets a configuration data for the specified table and column.
     *
     * @param table The name of the table.
     * @param key   The identifier of the value.
     * @param value The data to set.
     */
    public void setTableConfig(String table, String key, String value) {
        set(String.format("table.%s.%s", table, key), value);
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
    }

    /**
     * Gets a list of table filters.
     *
     * @return A list of table filters.
     */
    public List<String> getTableFilters() {
        String filters = get(String.class, "table.filters");
        if (filters != null && !filters.isEmpty()) {
            String[] parts = filters.split(";");
            return Arrays.asList(parts);
        }
        return new ArrayList<String>();
    }

    /**
     * Gets a previously used table filter.
     *
     * @return A filter.
     */
    public String getSelectedTableFilter() {
        return get(String.class, "table.filters.selected");
    }

    /**
     * Gets a list of column filters.
     *
     * @param table The name of the table.
     * @return A list of column filters.
     */
    public List<String> getColumnFilters(String table) {
        String filters = get(String.class, String.format("table.%s.column.filters", table));
        if (filters != null && !filters.isEmpty()) {
            String[] parts = filters.split(";");
            return Arrays.asList(parts);
        }
        return new ArrayList<String>();
    }

    /**
     * Gets a previously used column filter.
     *
     * @param table The name of the table.
     * @return A filter.
     */
    public String getSelectedColumnFilter(String table) {
        return get(String.class, String.format("table.%s.column.filters.selected", table));
    }

    /**
     * Saves table filter(s).
     *
     * @param filters Filter(s) to save.
     */
    public void setTablesFilter(Iterable<String> filters) {
        StringBuilder sb = new StringBuilder();
        for (String filter : filters) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(filter);
        }

        set("table.filters", sb.toString());
    }

    /**
     * Saves currently used table filter.
     *
     * @param filter The filter to save.
     */
    public void setSelectedTableFilter(String filter) {
        set("table.filters.selected", filter);
    }

    /**
     * Saves column filter(s).
     *
     * @param table   The name of the table.
     * @param filters Filter(s) to save.
     */
    public void setColumnsFilter(String table, Iterable<String> filters) {
        StringBuilder sb = new StringBuilder();
        for (String filter : filters) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(filter);
        }

        set(String.format("table.%s.column.filters", table), sb.toString());
    }

    /**
     * Saves currently used column filter.
     *
     * @param table  The name of the table.
     * @param filter The filter to save.
     */
    public void setSelectedColumnFilter(String table, String filter) {
        set(String.format("table.%s.column.filters.selected", table), filter);
    }
    //endregion
}
