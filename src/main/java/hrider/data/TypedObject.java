package hrider.data;

import hrider.config.GlobalConfig;

import java.text.DateFormat;
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
 *          This class represents an object with type.
 */
public class TypedObject {

    //region Variables
    /**
     * An actual value.
     */
    private Object     value;
    /**
     * The type of the {@link TypedObject#value}.
     */
    private ObjectType type;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link TypedObject} class.
     *
     * @param objType  The type of the object.
     * @param objValue The object.
     */
    public TypedObject(ObjectType objType, Object objValue) {
        this.type = objType;

        if (this.type == null) {
            this.type = ObjectType.String;
        }

        if (objValue instanceof byte[]) {
            this.value = this.type.fromByteArray((byte[])objValue);
        }
        else {
            this.value = objValue;
        }
    }
    //endregion

    //region Public Properties

    /**
     * Gets the value.
     *
     * @return A value.
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * Sets a new value.
     *
     * @param obj A new value.
     */
    public void setValue(Object obj) {
        this.value = obj;
    }

    /**
     * Gets the type of the object.
     *
     * @return The object type.
     */
    public ObjectType getType() {
        return this.type;
    }

    /**
     * Sets a new object type.
     *
     * @param objType A new object type.
     */
    public void setType(ObjectType objType) {
        if (this.type != null) {
            this.value = objType.fromByteArray(this.type.fromObject(this.value));
        }
        this.type = objType;
    }

    /**
     * Tries to understand the type of the value based on value itself.
     *
     * @return A guessed type if successful or null.
     */
    public ObjectType guessType() {
        if (value instanceof String) {
            String str = ((String)value).trim();

            if (str.startsWith("{") && str.endsWith("}")) {
                return ObjectType.Json;
            }

            if (str.startsWith("<") && str.endsWith(">")) {
                return ObjectType.Xml;
            }
        }
        return null;
    }
    //endregion

    //region Public Methods

    /**
     * Converts an object to a byte array according ot its type.
     *
     * @return A byte array.
     */
    public byte[] toByteArray() {
        return this.type.fromObject(this.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypedObject) {
            TypedObject typedObject = (TypedObject)obj;
            return typedObject.value != null ? typedObject.value.equals(this.value) : this.value == null;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.value != null ? this.value.hashCode() : 0;
    }

    @Override
    public String toString() {
        if (this.value != null) {
            if (this.type == ObjectType.DateTime) {
                DateFormat df = new SimpleDateFormat(GlobalConfig.instance().getDateFormat(), Locale.ENGLISH);
                return df.format((Date)this.value);
            }
            return this.value.toString();
        }
        return "";
    }
    //endregion
}
