package hrider.data;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/28/12
 * Time: 6:42 PM
 */
public class DataCell {

    private DataRow     row;
    private String      columnName;
    private TypedObject typedValue;

    public DataCell(DataRow row, String columnName, TypedObject typedValue) {
        this.row = row;
        this.columnName = columnName;
        this.typedValue = typedValue;
    }

    public DataRow getRow() {
        return this.row;
    }

    public void setRow(DataRow row) {
        this.row = row;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public TypedObject getTypedValue() {
        return this.typedValue;
    }

    public void setTypedValue(TypedObject typedValue) {
        this.typedValue = typedValue;
    }

    public boolean contains(Object value) {
        if (this.typedValue.getValue() != null) {
            return this.typedValue.getValue().equals(value);
        }
        return false;
    }

    public Object toObject(String data) {
        return this.typedValue.getType().toObject(data);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataCell) {
            DataCell cell = (DataCell)obj;
            return
                cell.typedValue.equals(this.typedValue) &&
                cell.row.equals(this.row) &&
                cell.columnName.equals(this.columnName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.typedValue.hashCode();
    }

    @Override
    public String toString() {
        return this.typedValue.toString();
    }
}
