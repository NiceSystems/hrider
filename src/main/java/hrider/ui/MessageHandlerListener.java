package hrider.ui;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 10/17/12
 * Time: 12:28 PM
 */
public interface MessageHandlerListener {
    void onInfo(String message);
    void onError(String message, Exception ex);
}
