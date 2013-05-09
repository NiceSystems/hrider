package hrider.ui;

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
 *          This is a listener interface that allows to register for messages.
 */
public interface MessageHandlerListener {

    /**
     * The method is called when a component wants to provide an additional information to the user.
     *
     * @param message The message to show to the user.
     */
    void onInfo(String message);

    /**
     * The method is called when a component wants to show the user an error.
     *
     * @param message The error message.
     * @param ex      An exception.
     */
    void onError(String message, Exception ex);

    /**
     * The method is called when a component wants to show the user message with the action.
     *
     * @param action The action to execute if clicked.
     */
    void onAction(UIAction action);
}
