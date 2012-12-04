package hrider.system;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 10/24/12
 * Time: 8:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClipboardData<T> {
    private T data;

    public ClipboardData(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}
