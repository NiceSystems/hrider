package hrider.data;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/28/12
 * Time: 5:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class TypedColumn {

    private String column;
    private ObjectType type;

    public TypedColumn(String columnName, ObjectType columnType) {
        this.column = columnName;
        this.type = columnType;
    }

    public String getColumn() {
        return this.column;
    }

    public void setColumn(String value) {
        this.column = value;
    }

    public ObjectType getType() {
        return this.type;
    }

    public void setType(ObjectType value) {
        this.type = value;
    }
}
