package hrider.data;

import java.io.Serializable;
import java.util.Arrays;

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
 *          This class represents an object with type.
 */
public class ConvertibleObject implements Serializable {

    //region Constants
    private static final long serialVersionUID = 1701395986295913520L;
    //endregion

    //region Variables
    /**
     * The type of the {@link ConvertibleObject#value}.
     */
    private ColumnType type;
    /**
     * The original value.
     */
    private byte[]     value;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link ConvertibleObject} class.
     *
     * @param type  The type of the object.
     * @param value The object.
     */
    public ConvertibleObject(ColumnType type, byte[] value) {
        this.type = type;
        this.value = value;
    }
    //endregion

    //region Public Properties

    /**
     * Gets the value.
     *
     * @return A value.
     */
    public byte[] getValue() {
        return this.value;
    }

    /**
     * Sets a new value.
     *
     * @param value A new value.
     */
    public void setValue(byte[] value) {
        this.value = value;
    }

    /**
     * Sets a new value.
     *
     * @param value A new value.
     */
    public void setValueAsString(String value) {
        this.value = this.type.toBytes(value);
    }

    /**
     * Gets a byte[] value as a {@link String}.
     *
     * @return A string representing the original byte[] value.
     */
    public String getValueAsString() {
        if (this.value != null) {
            return this.type.toString(this.value);
        }
        return "";
    }

    /**
     * Validates whether the value held by this object can be converted to the specified type.
     *
     * @param type The type to check.
     * @return True if the value can be converted to the specified type or False otherwise.
     */
    public boolean isOfType(ColumnType type) {
        return type.getConverter().canConvert(value);
    }

    /**
     * Gets the type of the object.
     *
     * @return The object type.
     */
    public ColumnType getType() {
        return this.type;
    }

    /**
     * Sets a new object type.
     *
     * @param type A new object type.
     */
    public void setType(ColumnType type) {
        this.type = type;
    }

    /**
     * Tries to understand the type of the value based on value itself.
     *
     * @return A guessed type if successful or null.
     */
    public ColumnType guessType() {
        if (type.equals(ColumnType.String)) {
            String str = type.toString(this.value);

            if (str.startsWith("{") && str.endsWith("}")) {
                return ColumnType.Json;
            }

            if (str.startsWith("[") && str.endsWith("]")) {
                return ColumnType.Json;
            }

            if (str.startsWith("<") && str.endsWith(">")) {
                return ColumnType.Xml;
            }
        }
        return null;
    }
    //endregion

    //region Public Methods

    /**
     * Checks if the object has exactly the same value.
     *
     * @param value The value to check.
     * @return True if the provided value equals to the object's value or False otherwise.
     */
    public boolean isEqual(String value) {
        if (this.value != null) {
            return this.type.toString(this.value).equals(value);
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConvertibleObject) {
            return Arrays.equals(((ConvertibleObject)obj).value, this.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.value != null ? Arrays.hashCode(this.value) : 0;
    }

    @Override
    public String toString() {
        return getValueAsString();
    }
    //endregion
}
