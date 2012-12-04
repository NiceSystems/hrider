package hrider.system;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 10/3/12
 * Time: 3:37 PM
 */
public class Clipboard {

    private Clipboard() {
    }

    public static boolean hasText() {
        Transferable data = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            String value = (String)data.getTransferData(DataFlavor.stringFlavor);
            return value != null;
        }
        catch (Exception ignore) {
            return false;
        }
    }

    public static void setText(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    public static String getText() {
        Transferable data = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            return  (String)data.getTransferData(DataFlavor.stringFlavor);
        }
        catch (Exception ignore) {
            return null;
        }
    }
}
