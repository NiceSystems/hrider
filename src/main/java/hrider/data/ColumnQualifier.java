package hrider.data;

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
 *          This class represents a column qualifier.
 */
public class ColumnQualifier implements Serializable {

    //region Constants
    public final static ColumnQualifier KEY = new ColumnQualifier("key");
    //endregion

    //region Variables
    private static final long serialVersionUID = 7851349292786398645L;
    private String       name;
    private ColumnFamily columnFamily;
    //endregion

    //region Constructor
    public ColumnQualifier(String name) {
        this(name, null);
    }

    public ColumnQualifier(String name, ColumnFamily columnFamily) {
        if (columnFamily == null) {
            if (name.contains(":")) {
                String[] parts = name.split(":");

                this.name = parts[1];
                this.columnFamily = new ColumnFamily(parts[0]);
            }
            else {
                this.name = name;
            }
        }
        else {
            this.name = name;
            this.columnFamily = columnFamily;
        }
    }
    //endregion

    //region Public Properties
    public String getName() {
        return this.name;
    }

    public String getFullName() {
        if (this.columnFamily != null) {
            return String.format("%s:%s", this.columnFamily, this.name);
        }

        return this.name;
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
    //endregion

    //region Public Methods
    public static boolean isKey(String name) {
        return KEY.name.equalsIgnoreCase(name);
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
