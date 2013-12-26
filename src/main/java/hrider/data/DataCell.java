package hrider.data;

import hrider.converters.TypeConverter;

import java.io.Serializable;

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
 *          This class represents a cell in the grid or a key/value pair of the hbase row.
 */
public class DataCell implements Serializable {

    //region Constants
    private static final long serialVersionUID = 387452287094391735L;
    //endregion

    //region Variables
    /**
     * The row the current cell belongs to.
     */
    private DataRow           row;
    /**
     * The column (an hbase qualifier) where the cell is located.
     */
    private ColumnQualifier   column;
    /**
     * The value of the cell.
     */
    private ConvertibleObject convertibleValue;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link DataCell} with parameters.
     *
     * @param row              The owner of the cell.
     * @param column           The name of the column/qualifier.
     * @param convertibleValue The value.
     */
    public DataCell(DataRow row, ColumnQualifier column, ConvertibleObject convertibleValue) {
        this.row = row;
        this.column = column;
        this.convertibleValue = convertibleValue;
    }
    //endregion

    //region Public Methods

    /**
     * Gets a row.
     *
     * @return A reference to the {@link DataRow} which is the owner of the cell.
     */
    public DataRow getRow() {
        return this.row;
    }

    /**
     * Sets a row.
     *
     * @param row The row to set.
     */
    public void setRow(DataRow row) {
        this.row = row;
    }

    /**
     * Gets the column qualifier.
     *
     * @return The column name.
     */
    public ColumnQualifier getColumn() {
        return this.column;
    }

    /**
     * Sets the new column qualifier.
     *
     * @param column A new column name.
     */
    public void setColumn(ColumnQualifier column) {
        this.column = column;
    }

    /**
     * Gets cell's value.
     *
     * @return A cell's value represented as a string.
     */
    public String getValue() {
        return this.convertibleValue.getValueAsString();
    }

    /**
     * Sets a new value for the cell.
     *
     * @param value A new value to set.
     */
    public void setValue(String value) {
        this.convertibleValue.setValueAsString(value);
    }

    /**
     * Gets cell's value as byte array.
     *
     * @return A cell's value represented as a byte array.
     */
    public byte[] getValueAsByteArray() {
        return this.convertibleValue.getValue();
    }

    /**
     * Validates whether the value held by the cell can be converted to the specified type.
     *
     * @param type The type to check.
     * @return True if the value can be converted to the specified type or False otherwise.
     */
    public boolean isOfType(ColumnType type) {
        return this.convertibleValue.isOfType(type);
    }

    /**
     * Gets cell's type.
     *
     * @return The cell's type.
     */
    public ColumnType getType() {
        return this.convertibleValue.getType();
    }

    /**
     * Sets a new object type for the cell.
     *
     * @param type A new object type.
     */
    public void setType(ColumnType type) {
        this.convertibleValue.setType(type);
    }

    /**
     * Sets new converter for column name.
     *
     * @param converter A new converter to set.
     */
    public void setColumnNameConverter(TypeConverter converter) {
        this.column.setNameConverter(converter);
    }

    /**
     * Checks if the cell contains the value.
     *
     * @param value The value to check.
     * @return True if the cell contains the value or False otherwise.
     */
    public boolean hasValue(String value) {
        return convertibleValue.isEqual(value);
    }

    /**
     * Checks whether this cell represents a key.
     *
     * @return True if the cell represents a key or False otherwise.
     */
    public boolean isKey() {
        return this.column.isKey();
    }

    /**
     * Tries to understand the type of the value based on value itself.
     *
     * @return A guessed type if successful or null.
     */
    public ColumnType guessType() {
        return this.convertibleValue.guessType();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataCell) {
            DataCell cell = (DataCell)obj;
            return cell.convertibleValue.equals(this.convertibleValue) &&
                   cell.row.equals(this.row) &&
                   cell.column.equals(this.column);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.convertibleValue.hashCode();
    }

    @Override
    public String toString() {
        return this.convertibleValue.toString();
    }
    //endregion
}
