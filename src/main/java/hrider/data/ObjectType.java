package hrider.data;

import hrider.config.Configurator;
import org.apache.hadoop.hbase.util.Bytes;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/23/12
 * Time: 5:42 PM
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

    public static ObjectType fromColumn(String column) {
        if (column.toLowerCase().endsWith("timestamp")) {
            return Long;
        }
        return String;
    }

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

    public byte[] fromString(String value) {
        if (value == null) {
            return null;
        }

        switch (this) {
            case String:
                return Bytes.toBytes(value);
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
                return Bytes.toBytes(value);
            case Xml:
            case Json:
                return Bytes.toBytes(value);
            default:
                return Bytes.toBytes(value);
        }
    }

    public Object fromByteArray(byte[] value) {
        if (value == null) {
            return null;
        }

        switch (this) {
            case String:
                return Bytes.toString(value);
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
                    return df.parse(Bytes.toString(value));
                }
                catch (ParseException ignored) {
                    return null;
                }
            case Xml:
            case Json:
                return Bytes.toString(value);
            default:
                return Bytes.toString(value);
        }
    }

    public byte[] fromObject(Object value) {
        if (value == null) {
            return null;
        }

        switch (this) {
            case String:
                return Bytes.toBytes((String)value);
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
                    return Bytes.toBytes(df.format((Date)value));
                }
                else {
                    return Bytes.toBytes((String)value);
                }
            case Xml:
            case Json:
                return Bytes.toBytes((String)value);
            default:
                return Bytes.toBytes((String)value);
        }
    }
}
