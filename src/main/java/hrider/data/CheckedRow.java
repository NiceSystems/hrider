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
 *          The class represents a row which is required and cannot be unchecked. For example the 'key' row in the 'columns' table should never be unchecked.
 */
public class CheckedRow {

    //region Variables
    /**
     * The index of the column to look for the value.
     */
    private int    columnIndex;
    /**
     * The expected value that should be found in the column specified by the {@link CheckedRow#columnIndex}.
     */
    private Object expectedValue;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link CheckedRow} class.
     *
     * @param columnIndex   An index of the column which value should be checked against the value containing in 'expectedValue' parameter.
     * @param expectedValue An expected value to be compared with the value located in the column defined by the 'columnIndex' parameter.
     */
    public CheckedRow(int columnIndex, Object expectedValue) {
        this.columnIndex = columnIndex;
        this.expectedValue = expectedValue;
    }
    //endregion

    //region Public Properties

    /**
     * Gets the column index.
     *
     * @return An {@link Integer} representing a column index.
     */
    public int getColumnIndex() {
        return this.columnIndex;
    }

    /**
     * Sets a new column index.
     *
     * @param columnIndex A new column index to set.
     */
    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    /**
     * Gets an expected value.
     *
     * @return A {@link String} representing the expected value.
     */
    public Object getExpectedValue() {
        return this.expectedValue;
    }

    /**
     * Sets a new value to be expected.
     *
     * @param expectedValue A new expected value to set.
     */
    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }
    //endregion
}
