package hrider.data;

import hrider.config.Configurator;
import org.apache.hadoop.hbase.util.Bytes;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
 *          This enum represents a type of the data supported by the tool.
 */
@SuppressWarnings("UnnecessaryDefault")
public enum ObjectType {

    String,
    Integer,
    Long,
    Float,
    Double,
    Boolean,
    Short,
    DateTime,
    Xml,
    Json;

    //region Public Methods

    /**
     * Gets the type of the column. The default value is {@link ObjectType#String} in case the column is not known.
     *
     * @param column The name of the column.
     * @return The type of the column if found or a {@link ObjectType#String} as a default.
     */
    public static ObjectType fromColumn(String column) {
        if (column.toLowerCase().endsWith("timestamp")) {
            return Long;
        }
        return String;
    }

    /**
     * Converts a value represented as a {@link String} to an object according to the type.
     *
     * @param value The value to convert.
     * @return An object representing the value or a null if something went wrong during conversion.
     */
    public Object toObject(String value) {
        if (value == null) {
            return null;
        }

        switch (this) {
            case String:
                return value;
            case Integer:
                return java.lang.Integer.parseInt(value);
            case Long:
                return java.lang.Long.parseLong(value);
            case Float:
                return java.lang.Float.parseFloat(value);
            case Double:
                return java.lang.Double.parseDouble(value);
            case Boolean:
                return java.lang.Boolean.parseBoolean(value);
            case Short:
                return java.lang.Short.parseShort(value);
            case DateTime:
                DateFormat df = new SimpleDateFormat(Configurator.getDateFormat(), Locale.ENGLISH);
                try {
                    return df.parse(value);
                }
                catch (ParseException ignored) {
                    return null;
                }
            case Xml:
            case Json:
                return value;
            default:
                return value;
        }
    }

    /**
     * Converts a value represented as a {@link String} to an array of bytes according to the type.
     *
     * @param value The value to convert.
     * @return A byte array representing the value or a null if something went wrong during conversion.
     */
    public byte[] fromString(String value) {
        if (value == null) {
            return null;
        }

        switch (this) {
            case String:
                return Bytes.toBytesBinary(value);
            case Integer:
                return Bytes.toBytes(java.lang.Integer.parseInt(value));
            case Long:
                return Bytes.toBytes(java.lang.Long.parseLong(value));
            case Float:
                return Bytes.toBytes(java.lang.Float.parseFloat(value));
            case Double:
                return Bytes.toBytes(java.lang.Double.parseDouble(value));
            case Boolean:
                return Bytes.toBytes(java.lang.Boolean.parseBoolean(value));
            case Short:
                return Bytes.toBytes(java.lang.Short.parseShort(value));
            case DateTime:
                return Bytes.toBytesBinary(value);
            case Xml:
            case Json:
                return Bytes.toBytesBinary(value);
            default:
                return Bytes.toBytesBinary(value);
        }
    }

    /**
     * Converts a value represented as a {@link byte[]} to an object according to the type.
     *
     * @param value The value to convert.
     * @return An object representing the value or a null if something went wrong during conversion.
     */
    public Object fromByteArray(byte[] value) {
        if (value == null) {
            return null;
        }

        switch (this) {
            case String:
                return Bytes.toStringBinary(value);
            case Integer:
                return Bytes.toInt(value);
            case Long:
                return Bytes.toLong(value);
            case Float:
                return Bytes.toFloat(value);
            case Double:
                return Bytes.toDouble(value);
            case Boolean:
                return Bytes.toBoolean(value);
            case Short:
                return Bytes.toShort(value);
            case DateTime:
                DateFormat df = new SimpleDateFormat(Configurator.getDateFormat(), Locale.ENGLISH);
                try {
                    return df.parse(Bytes.toStringBinary(value));
                }
                catch (ParseException ignored) {
                    return null;
                }
            case Xml:
            case Json:
                return Bytes.toStringBinary(value);
            default:
                return Bytes.toStringBinary(value);
        }
    }

    /**
     * Converts a value represented as an {@link Object} to an array of bytes according to the type.
     *
     * @param value The value to convert.
     * @return A byte array representing the value or a null if something went wrong during conversion.
     */
    public byte[] fromObject(Object value) {
        if (value == null) {
            return null;
        }

        switch (this) {
            case String:
                return Bytes.toBytesBinary((String)value);
            case Integer:
                return Bytes.toBytes((java.lang.Integer)value);
            case Long:
                return Bytes.toBytes((java.lang.Long)value);
            case Float:
                return Bytes.toBytes((java.lang.Float)value);
            case Double:
                return Bytes.toBytes((java.lang.Double)value);
            case Boolean:
                return Bytes.toBytes((java.lang.Boolean)value);
            case Short:
                return Bytes.toBytes((java.lang.Short)value);
            case DateTime:
                if (value instanceof Date) {
                    DateFormat df = new SimpleDateFormat(Configurator.getDateFormat(), Locale.ENGLISH);
                    return Bytes.toBytesBinary(df.format((Date)value));
                }
                else {
                    return Bytes.toBytesBinary((String)value);
                }
            case Xml:
            case Json:
                return Bytes.toBytesBinary((String)value);
            default:
                return Bytes.toBytesBinary((String)value);
        }
    }
    //endregion
}
