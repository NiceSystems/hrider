package hrider.converters;

import hrider.io.Log;

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
 *          This class is a base class for type converters.
 */
@SuppressWarnings("CallToSimpleGetterFromWithinClass")
public abstract class TypeConverter implements Comparable<TypeConverter>, Serializable {

    //region Constants
    protected static final Log logger = Log.getLogger(TypeConverter.class);
    protected static final byte[] EMPTY_BYTES_ARRAY = new byte[0];

    private static final long serialVersionUID = 1490434164342371320L;
    //endregion

    //region Variables
    private String name;
    private String code;
    //endregion

    //region Constructor
    protected TypeConverter() {
        this.name = this.getClass().getSimpleName().replace("Converter", "");
    }
    //endregion

    //region Public Methods

    /**
     * Gets the code of the type converter if available.
     *
     * @return The original code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the code for the type converter.
     *
     * @param code The code to set.
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the name of the converter to be presented as a column type.
     *
     * @return The name of the converter.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Checks whether the type converter can be edited.
     *
     * @return True if the type converter can be edited or False otherwise.
     */
    public boolean isEditable() {
        return code != null;
    }

    /**
     * Indicates whether the type converter can be used for column name conversions.
     *
     * @return True if the type converter can be used for column name conversions or False otherwise.
     */
    public boolean isValidForNameConversion() {
        return false;
    }

    /**
     * Converts an {@link String} to an array of bytes.
     *
     * @param value An string to convert.
     * @return A converted byte array.
     */
    public abstract byte[] toBytes(String value);

    /**
     * Converts an {@link byte[]} to a {@link String}.
     *
     * @param value An byte[] to convert.
     * @return A converted string.
     */
    public abstract String toString(byte[] value);

    @Override
    public int compareTo(TypeConverter o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (TypeConverter.class.isAssignableFrom(obj.getClass())) {
            return getName().equals(((TypeConverter)obj).getName());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }
    //endregion
}
