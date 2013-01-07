package hrider.data;

import hrider.hbase.Connection;

import java.util.ArrayList;
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
 *          This class represents a grid or an hbase table. The class' primary usage is a copy/paste operations.
 */
public class DataTable {

    //region Variables
    /**
     * The name of the table.
     */
    private String        tableName;
    /**
     * This class is used to access the hbase.
     */
    private Connection connection;
    /**
     * The list of rows that belong to the table.
     */
    private List<DataRow> rows;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link DataTable} class.
     *
     * @param tableName The name of the table.
     */
    public DataTable(String tableName) {
        this(tableName, null);
    }

    /**
     * Initializes a new instance of the {@link DataTable} class.
     *
     * @param tableName   The name of the table.
     * @param connection The reference to an hbase helper.
     */
    public DataTable(String tableName, Connection connection) {
        this.tableName = tableName;
        this.connection = connection;
        this.rows = new ArrayList<DataRow>();
    }
    //endregion

    //region Public Properties

    /**
     * Gets the name of the table.
     *
     * @return The name of the table.
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * Sets a new table name.
     *
     * @param tableName A new table name.
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Gets a reference to the {@link hrider.hbase.Connection} class that was used to load the data from hbase.
     *
     * @return The reference to the hbase helper.
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * Adds a row to the table.
     *
     * @param row The row to add.
     */
    public void addRow(DataRow row) {
        this.rows.add(row);
    }

    /**
     * Removes the row from the table.
     *
     * @param row The row to remove.
     */
    public void removeRow(DataRow row) {
        this.rows.remove(row);
    }

    /**
     * Gets a collection of all table rows.
     *
     * @return A list of row if there are any or an empty collection.
     */
    public Iterable<DataRow> getRows() {
        return this.rows;
    }

    /**
     * Gets the number of rows in the table.
     *
     * @return A number of rows in the table.
     */
    public int getRowsCount() {
        return this.rows.size();
    }
    //endregion
}
