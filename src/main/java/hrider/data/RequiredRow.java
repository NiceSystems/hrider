package hrider.data;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/29/12
 * Time: 1:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequiredRow {

    private int columnIndex;
    private Object expectedValue;

    public RequiredRow(int columnIndex, Object expectedValue) {
        this.columnIndex = columnIndex;
        this.expectedValue = expectedValue;
    }

    public int getColumnIndex() {
        return this.columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public Object getExpectedValue() {
        return this.expectedValue;
    }

    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }
}
