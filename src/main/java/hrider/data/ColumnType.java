package hrider.data;

import hrider.converters.ConvertersLoader;
import hrider.converters.TypeConverter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

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
 *          This class represents a type of the data supported by the tool.
 */
public class ColumnType implements Serializable {

    //region Constants
    public final static ColumnType String       = new ColumnType("String");
    public final static ColumnType BinaryString = new ColumnType("BinaryString");
    public final static ColumnType Boolean      = new ColumnType("Boolean");
    public final static ColumnType Integer      = new ColumnType("Integer");
    public final static ColumnType DateAsLong   = new ColumnType("DateAsLong");
    public final static ColumnType DateAsString = new ColumnType("DateAsString");
    public final static ColumnType Double       = new ColumnType("Double");
    public final static ColumnType Float        = new ColumnType("Float");
    public final static ColumnType Long         = new ColumnType("Long");
    public final static ColumnType Short        = new ColumnType("Short");
    public final static ColumnType Json         = new ColumnType("Json");
    public final static ColumnType Xml          = new ColumnType("Xml");
    public final static ColumnType RegionInfo   = new ColumnType("RegionInfo");

    private static final long serialVersionUID = -5311385557088499851L;
    //endregion

    //region Variables
    private TypeConverter converter;
    //endregion

    //region Constructor
    public ColumnType(String name) {
        this(ConvertersLoader.getConverter(name));
    }

    public ColumnType(TypeConverter converter) {
        if (converter == null) {
            throw new IllegalArgumentException("converter cannot be null");
        }
        this.converter = converter;
    }
    //endregion

    //region Public Methods

    /**
     * Creates an instance of {@link ColumnType} from the name.
     *
     * @param name The name of the type to be created.
     * @return A newly created {@link ColumnType}.
     */
    public static ColumnType fromName(String name) {
        if (ConvertersLoader.exists(name)) {
            return new ColumnType(name);
        }
        return null;
    }

    /**
     * Creates an instance of {@link ColumnType} from the name. If there is no column type matching the name the default type is returned.
     *
     * @param name        The name of the type to be created.
     * @param defaultType The default type to be returned if the requested one does not exist.
     * @return A newly created {@link ColumnType} if found or user provided default type otherwise.
     */
    public static ColumnType fromNameOrDefault(String name, ColumnType defaultType) {
        if (ConvertersLoader.exists(name)) {
            return new ColumnType(name);
        }
        return defaultType;
    }

    /**
     * Gets all supported column types.
     *
     * @return A list of supported column types.
     */
    public static Collection<ColumnType> getTypes() {
        Collection<ColumnType> types = new ArrayList<ColumnType>();
        for (TypeConverter converter : ConvertersLoader.getConverters()) {
            types.add(new ColumnType(converter));
        }
        return types;
    }

    /**
     * Gets column types that support column name conversions.
     *
     * @return A list of column types.
     */
    public static Collection<ColumnType> getNameTypes() {
        Collection<ColumnType> types = new ArrayList<ColumnType>();
        for (TypeConverter converter : ConvertersLoader.getNameConverters()) {
            types.add(new ColumnType(converter));
        }
        return types;
    }

    /**
     * Gets the type of the column. The default value is {@link ColumnType#String} in case the column is not known.
     *
     * @param column The name of the column.
     * @return The type of the column if found or a {@link ColumnType#String} as a default.
     */
    public static ColumnType fromColumn(String column) {
        if (column.contains("timestamp")) {
            return DateAsLong;
        }
        if ("key".equals(column)) {
            return BinaryString;
        }
        if ("info:regioninfo".equals(column)) {
            return RegionInfo;
        }
        return String;
    }

    /**
     * Gets the name of the column type.
     *
     * @return The type's name.
     */
    public String getName() {
        return converter.getName();
    }

    /**
     * Gets a converter used to convert the values of the type.
     *
     * @return An instance of {@link TypeConverter}.
     */
    public TypeConverter getConverter() {
        return converter;
    }

    /**
     * Checks whether the column type can be edited.
     *
     * @return True if the column type can be edited or False otherwise.
     */
    public boolean isEditable() {
        return converter.isEditable();
    }

    /**
     * Converts an {@link Object} to a {@link String}.
     *
     * @param value An object to convert.
     * @return A converted string.
     */
    public String toString(byte[] value) {
        return converter.toString(value);
    }

    /**
     * Converts a value represented as a {@link String} to an array of bytes according to the type.
     *
     * @param value The value to converters.
     * @return A byte array representing the value or a null if something went wrong during conversion.
     */
    public byte[] toBytes(String value) {
        return converter.toBytes(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ColumnType) {
            return this.converter.equals(((ColumnType)obj).converter);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.converter.hashCode();
    }

    @Override
    public String toString() {
        return this.converter.toString();
    }
    //endregion
}
