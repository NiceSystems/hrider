package hrider.data;

import hrider.config.Configurator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/26/12
 * Time: 1:27 PM
 */
public class TypedObject {

    private Object value;
    private ObjectType type;

    public TypedObject(ObjectType objType, Object objValue) {
        this.type = objType;

        if (objValue instanceof byte[]) {
            this.value = objType.fromByteArray((byte[])objValue);
        }
        else {
            this.value = objValue;
        }
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object obj) {
        this.value = obj;
    }

    public ObjectType getType() {
        return this.type;
    }

    public void setType(ObjectType objType) {
        if (this.type != null) {
            this.value = objType.fromByteArray(this.type.fromObject(this.value));
        }
        this.type = objType;
    }

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
                DateFormat df = new SimpleDateFormat(Configurator.getDateFormat(), Locale.ENGLISH);
                return df.format((Date)value);
            }
            return this.value.toString();
        }
        return "";
    }
}
