package hrider.system;

import java.util.ArrayList;
import java.util.List;

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
 *          This class represents an in memory clipboard. Or in other words the copy/paste operations performed inside the
 *          application only.
 */
public class InMemoryClipboard {

    //region Variables
    /**
     * The data to be saved in clipboard.
     */
    private static ClipboardData           data;
    /**
     * A list of listeners.
     */
    private static List<ClipboardListener> listeners;
    //endregion

    //region Constructor
    static {
        listeners = new ArrayList<ClipboardListener>();
    }

    private InMemoryClipboard() {
    }
    //endregion

    //region Public Methods

    /**
     * Adds a new listener to the clipboard.
     *
     * @param listener A listener to add.
     */
    public static void addListener(ClipboardListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from clipboard.
     *
     * @param listener A listener to remove.
     */
    public static void removeListener(ClipboardListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets a data into the clipboard.
     *
     * @param clipboardData The data to set.
     * @param <T>           The type of the data.
     */
    public static <T> void setData(ClipboardData<T> clipboardData) {
        data = clipboardData;

        for (ClipboardListener listener : listeners) {
            listener.onChanged(clipboardData);
        }
    }

    /**
     * Gets the data from the clipboard.
     *
     * @param <T> The type of the data to retrieve.
     * @return A requested data if found or null.
     */
    @SuppressWarnings("unchecked")
    public static <T> ClipboardData<T> getData() {
        return data;
    }
    //endregion
}
