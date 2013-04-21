package hrider.system;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * Copyright (C) 2012 NICE Systems ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Igor Cher
 * @version %I%, %G%
 *          <p/>
 *          This class provides an access ot the system clipboard.
 */
public class Clipboard {

    //region Constructor
    private Clipboard() {
    }
    //endregion

    //region Public Methods

    /**
     * Checks if the clipboard has text.
     *
     * @return True if there is any text in the system clipboard or False otherwise.
     */
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

    /**
     * Sets a provided text to the system clipboard.
     *
     * @param text The text to set.
     */
    public static void setText(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    /**
     * Gets a text from the system clipboard.
     *
     * @return A text if there is any or a null.
     */
    public static String getText() {
        Transferable data = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            return (String)data.getTransferData(DataFlavor.stringFlavor);
        }
        catch (Exception ignore) {
            return null;
        }
    }
    //endregion
}
