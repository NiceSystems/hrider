package hrider.data;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 *          This class represents a descriptor of the table.
 */
public class TableDescriptor implements Serializable, Cloneable {

    //region Variables
    private static final long serialVersionUID = 3044847453855547590L;

    private String              name;
    private Map<String, String> metadata;
    private List<ColumnFamily>  families;
    private boolean             editable;
    //endregion

    //region Constructor
    private TableDescriptor() {
    }

    public TableDescriptor(String name) {
        this(new HTableDescriptor(name));

        this.editable = true;
    }

    public TableDescriptor(HTableDescriptor descriptor) {
        this.name = descriptor.getNameAsString();
        this.families = new ArrayList<ColumnFamily>();

        for (HColumnDescriptor column : descriptor.getColumnFamilies()) {
            this.families.add(new ColumnFamily(column));
        }

        loadMetadata(descriptor);
    }
    //endregion

    //region Public Properties
    public boolean isEditable() {
        return editable;
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
        return new TableDescriptor("defaults").metadata;
    }

    public void addFamily(ColumnFamily family) {
        if (!this.families.contains(family)) {
            this.families.add(family);
        }
    }

    public void removeFamily(ColumnFamily family) {
        this.families.remove(family);
    }

    public Iterable<ColumnFamily> families() {
        return this.families;
    }

    public HTableDescriptor toDescriptor() {
        HTableDescriptor descriptor = new HTableDescriptor(this.name);
        for (Map.Entry<String, String> entry : this.metadata.entrySet()) {
            descriptor.setValue(entry.getKey(), entry.getValue());
        }

        for (ColumnFamily columnFamily : this.families) {
            descriptor.addFamily(columnFamily.toDescriptor());
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
    public TableDescriptor clone() {
        TableDescriptor descriptor = new TableDescriptor();
        descriptor.name = this.name;
        descriptor.metadata = new HashMap<String, String>(this.metadata);
        descriptor.editable = this.editable;
        descriptor.families = new ArrayList<ColumnFamily>();

        for (ColumnFamily family : this.families) {
            descriptor.families.add(family.clone());
        }

        return descriptor;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TableDescriptor) {
            return ((TableDescriptor)obj).name.equals(this.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
    //endregion

    //region Private Methods
    private void loadMetadata(HTableDescriptor descriptor) {
        this.metadata = new HashMap<String, String>();

        for (Map.Entry<ImmutableBytesWritable, ImmutableBytesWritable> pair : descriptor.getValues().entrySet()) {
            this.metadata.put(Bytes.toString(pair.getKey().get()), Bytes.toString(pair.getValue().get()));
        }
    }
    //endregion
}
