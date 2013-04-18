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
 *          The class represents a column with type. The type information is used to converters the value represented by an array of bytes to the actual object.
 */
public class TypedColumn {

    //region Variables
    /**
     * The name of the column.
     */
    private ColumnQualifier column;
    /**
     * The type of the column.
     */
    private ColumnType type;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link TypedColumn} class.
     *
     * @param column     The qualifier of the column.
     * @param columnType The type of the column.
     */
    public TypedColumn(ColumnQualifier column, ColumnType columnType) {
        this.column = column;
        this.type = columnType;
    }
    //endregion

    //region Public Properties

    /**
     * Gets the qualifier of the column.
     *
     * @return The name of the column.
     */
    public ColumnQualifier getColumn() {
        return this.column;
    }

    /**
     * Sets a new column qualifier.
     *
     * @param value A new column qualifier.
     */
    public void setColumn(ColumnQualifier value) {
        this.column = value;
    }

    /**
     * Gets the type of the column.
     *
     * @return The type of the column.
     */
    public ColumnType getType() {
        return this.type;
    }

    /**
     * Sets a new column type.
     *
     * @param value A new column type.
     */
    public void setType(ColumnType value) {
        this.type = value;
    }
    //endregion
}
