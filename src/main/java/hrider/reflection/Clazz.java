package hrider.reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 *          This class is a helper class that simplifies use of reflection.
 */
public class Clazz {

    private Clazz() {
    }

    /**
     * Converts a primitive value represented as {@link String} to object.
     *
     * @param type  The type of the primitive.
     * @param value The value to be converted.
     * @return A converted object.
     */
    @SuppressWarnings("unchecked")
    public static Object primitiveToObject(Class type, String value) {
        if (isPrimitive(type)) {
            if (value != null && !value.isEmpty()) {
                String typeName = type.getSimpleName();
                if ("int".equals(typeName) || "Integer".equals(typeName)) {
                    return Integer.parseInt(value);
                }
                if ("long".equalsIgnoreCase(typeName)) {
                    return Long.parseLong(value);
                }
                if ("boolean".equalsIgnoreCase(typeName)) {
                    return Boolean.parseBoolean(value);
                }
                if ("byte".equalsIgnoreCase(typeName)) {
                    return Byte.parseByte(value);
                }
                if ("char".equals(typeName) || "Character".equals(typeName)) {
                    return value.charAt(0);
                }
                if ("short".equalsIgnoreCase(typeName)) {
                    return Short.parseShort(value);
                }
                if ("float".equalsIgnoreCase(typeName)) {
                    return Float.parseFloat(value);
                }
                if ("double".equalsIgnoreCase(typeName)) {
                    return Double.parseDouble(value);
                }
                if ("String".equals(typeName)) {
                    return value;
                }
                if (type.isEnum()) {
                    return Enum.valueOf(type, value);
                }
            }
        }
        return null;
    }

    /**
     * Sets a value to the field.
     *
     * @param obj   The instance which field is to be updated.
     * @param field The field to be updated.
     * @param value The value to be set.
     * @throws IllegalAccessException Error accessing the field.
     */
    public static void setValue(Object obj, Field field, Object value) throws IllegalAccessException {
        field.setAccessible(true);

        String typeName = field.getType().getSimpleName();
        if ("int".equals(typeName) || "Integer".equals(typeName)) {
            field.setInt(obj, (Integer)value);
        }
        else if ("long".equalsIgnoreCase(typeName)) {
            field.setLong(obj, (Long)value);
        }
        else if ("boolean".equalsIgnoreCase(typeName)) {
            field.setBoolean(obj, (Boolean)value);
        }
        else if ("byte".equalsIgnoreCase(typeName)) {
            field.setByte(obj, (Byte)value);
        }
        else if ("char".equals(typeName) || "Character".equals(typeName)) {
            field.setChar(obj, (Character)value);
        }
        else if ("short".equalsIgnoreCase(typeName)) {
            field.setShort(obj, (Short)value);
        }
        else if ("float".equalsIgnoreCase(typeName)) {
            field.setFloat(obj, (Float)value);
        }
        else if ("double".equalsIgnoreCase(typeName)) {
            field.setDouble(obj, (Double)value);
        }
        else {
            field.set(obj, value);
        }
    }

    /**
     * Gets a value of the field.
     *
     * @param obj   The instance which field's value is to be retrieved.
     * @param field The field that holds the value.
     * @return A value extracted from the field.
     * @throws IllegalAccessException Error accessing the field.
     */
    public static Object getValue(Object obj, Field field) throws IllegalAccessException {
        field.setAccessible(true);

        String typeName = field.getType().getSimpleName();
        if ("int".equals(typeName) || "Integer".equals(typeName)) {
            return field.getInt(obj);
        }
        if ("long".equalsIgnoreCase(typeName)) {
            return field.getLong(obj);
        }
        if ("boolean".equalsIgnoreCase(typeName)) {
            return field.getBoolean(obj);
        }
        if ("byte".equalsIgnoreCase(typeName)) {
            return field.getByte(obj);
        }
        if ("char".equals(typeName) || "Character".equals(typeName)) {
            return field.getChar(obj);
        }
        if ("short".equalsIgnoreCase(typeName)) {
            return field.getShort(obj);
        }
        if ("float".equalsIgnoreCase(typeName)) {
            return field.getFloat(obj);
        }
        if ("double".equalsIgnoreCase(typeName)) {
            return field.getDouble(obj);
        }
        return field.get(obj);
    }

    /**
     * Returns all public/protected/private fields including super classes.
     *
     * @param clazz The class which fileds should be retreived.
     * @return A list of fields containing in the class and its super classes.
     */
    public static List<Field> getFields(Class clazz) {
        List<Field> fields = new ArrayList<Field>();
        Collections.addAll(fields, clazz.getDeclaredFields());

        if (clazz.getSuperclass() != null) {
            fields.addAll(getFields(clazz.getSuperclass()));
        }

        return fields;
    }

    /**
     * Checks whether the type is primitive.
     *
     * @param type The type to check.
     * @return True if the type is primitive or false otherwise.
     */
    public static boolean isPrimitive(Class type) {
        if (null == type) {
            return false;
        }

        String typeName = type.getSimpleName();
        return "int".equals(typeName) || "Integer".equals(typeName) ||
               "long".equalsIgnoreCase(typeName) ||
               "boolean".equalsIgnoreCase(typeName) ||
               "byte".equalsIgnoreCase(typeName) ||
               "char".equals(typeName) || "Character".equals(typeName) ||
               "short".equalsIgnoreCase(typeName) ||
               "float".equalsIgnoreCase(typeName) ||
               "double".equalsIgnoreCase(typeName) ||
               "String".equals(typeName) ||
               "Date".equals(typeName) ||
               type.isEnum();
    }

    /**
     * Checks whether the string represents a number.
     *
     * @param value The string to check.
     * @return True if the string represents a number or false otherwise.
     */
    public static boolean isNumber(String value) {
        for (char letter : value.toCharArray()) {
            if (!Character.isDigit(letter)) {
                return false;
            }
        }
        return true;
    }
}
