package hrider.data;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
 *          This class represents a column family.
 */
public class ColumnFamily implements Serializable, Cloneable {

    //region Variables
    private static final long serialVersionUID = 4450785088108421206L;

    private String              name;
    private Map<String, String> metadata;
    private boolean             editable;
    //endregion

    //region Constructor
    private ColumnFamily() {
    }

    public ColumnFamily(String name) {
        this(new HColumnDescriptor(name));

        this.editable = true;
    }

    public ColumnFamily(HColumnDescriptor columnDescriptor) {
        this.name = columnDescriptor.getNameAsString();

        loadMetadata(columnDescriptor);
    }
    //endregion

    //region Public Properties
    public boolean isEditable() {
        return this.editable;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getMetadata() {
        return this.metadata;
    }
    //endregion

    //region Public Methods
    public static Map<String, String> defaults() {
        return new ColumnFamily("defaults").metadata;
    }

    public HColumnDescriptor toDescriptor() {
        HColumnDescriptor descriptor = new HColumnDescriptor(this.name);
        for (Map.Entry<String, String> entry : this.metadata.entrySet()) {
            descriptor.setValue(entry.getKey(), entry.getValue());
        }
        return descriptor;
    }

    public String getValue(String key) {
        return this.metadata.get(key);
    }

    public void setValue(String key, String value) {
        this.metadata.put(key, value);
    }

    @Override
    public ColumnFamily clone() {
        ColumnFamily family = new ColumnFamily();
        family.name = this.name;
        family.metadata = new HashMap<String, String>(this.metadata);
        family.editable = this.editable;

        return family;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ColumnFamily) {
            return ((ColumnFamily)obj).name.equals(this.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
    //endregion

    //region Private Methods
    private void loadMetadata(HColumnDescriptor columnDescriptor) {
        this.metadata = new HashMap<String, String>();

        for (Map.Entry<ImmutableBytesWritable, ImmutableBytesWritable> pair : columnDescriptor.getValues().entrySet()) {
            this.metadata.put(Bytes.toString(pair.getKey().get()), Bytes.toString(pair.getValue().get()));
        }
    }
    //endregion
}
