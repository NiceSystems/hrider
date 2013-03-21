package hrider.data;

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
public class DataCell {

    //region Variables
    /**
     * The row the current cell belongs to.
     */
    private DataRow         row;
    /**
     * The column (an hbase qualifier) where the cell is located.
     */
    private ColumnQualifier column;
    /**
     * The value of the cell.
     */
    private TypedObject     typedValue;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link DataCell} with parameters.
     *
     * @param row        The owner of the cell.
     * @param column     The name of the column/qualifier.
     * @param typedValue The value.
     */
    public DataCell(DataRow row, ColumnQualifier column, TypedObject typedValue) {
        this.row = row;
        this.column = column;
        this.typedValue = typedValue;
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
     * Gets a cell value.
     *
     * @return A reference to the {@link TypedObject} that holds the value.
     */
    public TypedObject getTypedValue() {
        return this.typedValue;
    }

    /**
     * Sets a new cell value.
     *
     * @param typedValue A new cell value.
     */
    public void setTypedValue(TypedObject typedValue) {
        this.typedValue = typedValue;
    }

    /**
     * Checks if the cell contains the value.
     *
     * @param value The value to check.
     * @return True if the cell contains the value or False otherwise.
     */
    public boolean contains(Object value) {
        if (this.typedValue.getValue() != null) {
            return this.typedValue.getValue().equals(value);
        }
        return false;
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
     * Converts a data represented as {@link String} to an actual type.
     *
     * @param data The data to convert.
     * @return A converted data.
     */
    public Object toObject(String data) {
        return this.typedValue.getType().toObject(data);
    }

    /**
     * Tries to understand the type of the value based on value itself.
     *
     * @return A guessed type if successful or null.
     */
    public ObjectType guessType() {
        return this.typedValue.guessType();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataCell) {
            DataCell cell = (DataCell)obj;
            return cell.typedValue.equals(this.typedValue) &&
                   cell.row.equals(this.row) &&
                   cell.column.equals(this.column);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.typedValue.hashCode();
    }

    @Override
    public String toString() {
        return this.typedValue.toString();
    }
    //endregion
}
