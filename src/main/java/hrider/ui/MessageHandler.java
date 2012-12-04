package hrider.ui;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 10/17/12
 * Time: 12:28 PM
 */
public class MessageHandler {

    private static Collection<MessageHandlerListener> listeners;

    private MessageHandler() {
    }

    static {
        listeners = new ArrayList<MessageHandlerListener>();
    }

    public static void addError(String message, Exception ex) {
        for (MessageHandlerListener listener : listeners) {
            listener.onError(message, ex);
        }
    }

    public static void addInfo(String message) {
        for (MessageHandlerListener listener : listeners) {
            listener.onInfo(message);
        }
    }

    public static void addListener(MessageHandlerListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(MessageHandlerListener listener) {
        listeners.remove(listener);
    }
}
