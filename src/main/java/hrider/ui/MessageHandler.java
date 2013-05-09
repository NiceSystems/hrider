package hrider.ui;

import hrider.io.Log;

import java.util.ArrayList;
import java.util.Collection;

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
 *          This class represents a message handler. Each class or component that want to show the user
 *          some information should use this class. The information is presented in the status bar of the application.
 */
public class MessageHandler {

    private final static Log logger = Log.getLogger(MessageHandler.class);

    /**
     * A list of registered listeners.
     */
    private static Collection<MessageHandlerListener> listeners;

    private MessageHandler() {
    }

    static {
        listeners = new ArrayList<MessageHandlerListener>();
    }

    /**
     * This method is called by the component that wants to report an error to the user.
     *
     * @param message The error message.
     * @param ex      The exception.
     */
    public static void addError(String message, Exception ex) {
        if (message != null && !message.isEmpty()) {
            logger.error(ex, message);
        }

        for (MessageHandlerListener listener : listeners) {
            listener.onError(message, ex);
        }
    }

    /**
     * This method is called by the component that wants to report additional information about
     * current operation to the user.
     *
     * @param message The message to report.
     */
    public static void addInfo(String message) {
        if (message != null && !message.isEmpty()) {
            logger.info(message);
        }

        for (MessageHandlerListener listener : listeners) {
            listener.onInfo(message);
        }
    }

    /**
     * This method is called by the component that wants to report to the user additional information with the option to perform
     * an action.
     *
     * @param action The action to execute if clicked.
     */
    public static void addAction(UIAction action) {
        for (MessageHandlerListener listener : listeners) {
            listener.onAction(action);
        }
    }

    /**
     * Adds a listener to the list of registered listeners.
     *
     * @param listener A listener to add.
     */
    public static void addListener(MessageHandlerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from the list of registered listeners.
     *
     * @param listener A listener to remove.
     */
    public static void removeListener(MessageHandlerListener listener) {
        listeners.remove(listener);
    }
}
