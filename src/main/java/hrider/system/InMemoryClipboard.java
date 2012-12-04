package hrider.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 10/24/12
 * Time: 8:44 AM
 */
public class InMemoryClipboard {

    private static ClipboardData data;
    private static List<ClipboardListener> listeners;

    static {
        listeners = new ArrayList<ClipboardListener>();
    }

    private InMemoryClipboard() {
    }

    public static void addListener(ClipboardListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(ClipboardListener listener) {
        listeners.remove(listener);
    }

    public static <T> void setData(ClipboardData<T> clipboardData) {
        data = clipboardData;

        for (ClipboardListener listener : listeners) {
            listener.onChanged(clipboardData);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ClipboardData<T> getData() {
        return data;
    }

}
