package hrider.data;

import hrider.converters.TypeConverter;

import java.io.Serializable;
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
 *          This class represents a row in the grid or an hbase row.
 */
public class DataRow implements Serializable {

    //region Constants
    private static final long serialVersionUID = 5158560383309065143L;
    //endregion

    //region Variables
    /**
     * The row key.
     */
    private ConvertibleObject     key;
    /**
     * The list of cells that belong to the row.
     */
    private Map<String, DataCell> cells;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link DataRow} class with default parameters.
     */
    public DataRow() {
        this.cells = new HashMap<String, DataCell>();
    }

    /**
     * Initializes a new instance of the {@link DataRow} class with a specified key.
     *
     * @param key The row key.
     */
    public DataRow(ConvertibleObject key) {
        this();
        this.key = key;
    }
    //endregion

    //region Public Properties

    /**
     * Gets the row key.
     *
     * @return The key of the row.
     */
    public ConvertibleObject getKey() {
        return this.key;
    }

    /**
     * Sets the new key for the row.
     *
     * @param key The new key to set.
     */
    public void setKey(ConvertibleObject key) {
        this.key = key;
    }

    /**
     * Gets the cell according to the specified column name.
     *
     * @param columnName The name of the column to look for the cell.
     * @return The instance of the {@link DataCell} if found or null otherwise.
     */
    public DataCell getCell(String columnName) {
        return this.cells.get(columnName);
    }

    /**
     * Gets the cell according to the specified column qualifier.
     *
     * @param columnQualifier The column qualifier to look for the cell.
     * @return The instance of the {@link DataCell} if found or null otherwise.
     */
    public DataCell getCell(ColumnQualifier columnQualifier) {
        return this.cells.get(columnQualifier.getFullName());
    }

    /**
     * Adds cell to the collection of cells in the row.
     *
     * @param cell The cell to add.
     */
    public void addCell(DataCell cell) {
        this.cells.put(cell.getColumn().getFullName(), cell);
    }

    /**
     * Removes cell from the collection of cells in the row.
     *
     * @param cell The cell to remove.
     */
    public void removeCell(DataCell cell) {
        this.cells.remove(cell.getColumn().getFullName());
    }

    /**
     * Clears all cells of the row.
     */
    public void clearCells() {
        this.cells.clear();
    }

    /**
     * Indicates if the row has cells.
     *
     * @return True if the row has cells or False otherwise.
     */
    public boolean hasCells() {
        return !this.cells.isEmpty();
    }

    /**
     * Gets a collection of all cells of the row.
     *
     * @return A list of cells if there are any or an empty list.
     */
    public Iterable<DataCell> getCells() {
        return this.cells.values();
    }
    //endregion

    //region Public Methods

    /**
     * Validates whether the value held by the cell can be converted to the specified type.
     *
     * @param columnType The type to check.
     * @return True if the value can be converted to the specified type or False otherwise.
     */
    public boolean isCellOfType(String columnName, ColumnType columnType) {
        DataCell cell = getCell(columnName);
        if (cell != null) {
            return cell.isOfType(columnType);
        }
        return true;
    }

    /**
     * Updates a column type for all cells that belong to the specified column.
     *
     * @param columnName The name of the column which type should be updated.
     * @param columnType The new column type.
     */
    public void updateColumnType(String columnName, ColumnType columnType) {
        DataCell cell = getCell(columnName);
        if (cell != null) {
            cell.setType(columnType);
        }
    }

    /**
     * Updates converter for the column name.
     *
     * @param converter The new column name converter.
     */
    public void updateColumnNameConverter(TypeConverter converter) {
        Map<String, DataCell> map = new HashMap<String, DataCell>();
        for (DataCell cell : this.cells.values()) {
            cell.setColumnNameConverter(converter);

            map.put(cell.getColumn().getFullName(), cell);
        }
        this.cells = map;
    }

    /**
     * Gets the key/value representation of the row.
     *
     * @return A map containing a key/value mapping.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new HashMap<String, Object>();
        for (Map.Entry<String, DataCell> entry : this.cells.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getValue());
        }
        return values;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataRow) {
            DataRow row = (DataRow)obj;
            return row.key.equals(this.key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("key: ");
        sb.append(this.key);
        sb.append("; values: ");

        int i = 0;
        for (Map.Entry<String, DataCell> entry : this.cells.entrySet()) {
            sb.append(entry.getValue());

            if (i < this.cells.size() - 1) {
                sb.append(", ");
            }
            i++;
        }

        return sb.toString();
    }
    //endregion
}
