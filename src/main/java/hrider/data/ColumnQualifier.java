package hrider.data;

import hrider.converters.TypeConverter;
import hrider.io.Log;

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
 *          This class represents a column qualifier.
 */
public class ColumnQualifier implements Serializable {

    //region Constants
    public final static ColumnQualifier KEY = new ColumnQualifier("key", ColumnType.BinaryString.getConverter());
    //endregion

    //region Variables
    private static final Log  logger           = Log.getLogger(ColumnQualifier.class);
    private static final long serialVersionUID = 7851349292786398645L;

    private byte[]        name;
    private ColumnFamily  columnFamily;
    private TypeConverter nameConverter;
    //endregion

    //region Constructor
    public ColumnQualifier(String name, TypeConverter nameConverter) {
        this(nameConverter.toBytes(name), null, nameConverter);
    }

    public ColumnQualifier(String name, ColumnFamily columnFamily, TypeConverter nameConverter) {
        this(nameConverter.toBytes(name), columnFamily, nameConverter);
    }

    public ColumnQualifier(byte[] name, ColumnFamily columnFamily, TypeConverter nameConverter) {
        this.name = name;
        this.columnFamily = columnFamily;
        this.nameConverter = nameConverter;
    }
    //endregion

    //region Public Properties
    public String getName() {
        try {
            return this.nameConverter.toString(this.name);
        }
        catch (Exception e) {
            logger.error(e, "Failed to convert byte array '%s' to string using '%s' converter.", Arrays.toString(this.name), this.nameConverter.getName());
            return ColumnType.BinaryString.toString(this.name);
        }
    }

    public byte[] getNameAsByteArray() {
        return this.name;
    }

    public String getFullName() {
        if (this.columnFamily != null) {
            return String.format("%s:%s", this.columnFamily, getName());
        }
        return getName();
    }

    public String getFamily() {
        if (this.columnFamily != null) {
            return this.columnFamily.getName();
        }
        return null;
    }

    public ColumnFamily getColumnFamily() {
        return this.columnFamily;
    }

    public TypeConverter getNameConverter() {
        return nameConverter;
    }

    public void setNameConverter(TypeConverter nameConverter) {
        this.nameConverter = nameConverter;
    }
    //endregion

    //region Public Methods
    public static boolean isKey(String name) {
        return KEY.getName().equalsIgnoreCase(name);
    }

    public boolean isKey() {
        return KEY.equals(this);
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ColumnQualifier) {
            return this.toString().equals(obj.toString());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
    //endregion
}
