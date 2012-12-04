package hrider.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/29/12
 * Time: 9:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class DataRow {

    private TypedObject           key;
    private Map<String, DataCell> cells;

    public DataRow() {
        this.cells = new HashMap<String, DataCell>();
    }

    public DataRow(TypedObject key) {
        this();
        this.key = key;
    }

    public TypedObject getKey() {
        return this.key;
    }

    public void setKey(TypedObject key) {
        this.key = key;
    }

    public DataCell getCell(String columnName) {
        return this.cells.get(columnName);
    }

    public void addCell(DataCell cell) {
        this.cells.put(cell.getColumnName(), cell);
    }

    public void removeCell(DataCell cell) {
        this.cells.remove(cell.getColumnName());
    }

    public void clearCells() {
        this.cells.clear();
    }

    public boolean hasCells() {
        return !this.cells.isEmpty();
    }

    public Iterable<DataCell> getCells() {
        return this.cells.values();
    }

    public void updateColumnType(String columnName, ObjectType columnType) {
        DataCell cell = getCell(columnName);
        if (cell != null && cell.getTypedValue() != null) {
            cell.getTypedValue().setType(columnType);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataRow) {
            DataRow row = (DataRow)obj;
            return row.key.equals(this.key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("key: ");
        sb.append(this.key);
        sb.append("; values: ");

        int i = 0;
        for (Map.Entry<String, DataCell> entry : this.cells.entrySet()) {
            sb.append(entry.getValue());

            if (i < this.cells.size() - 1) {
                sb.append(", ");
            }
            i++;
        }

        return sb.toString();
    }
}
