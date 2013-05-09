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
 */
public interface UIAction {

    /**
     * Executes the UI action.
     */
    void execute();

    /**
     * Gets a formatted message. The array might contain 1-3 elements where:
     *   1. The first part of the message if exist.
     *   2. The word/expression that describes the action.
     *   3. The last part of the message if exist.
     *
     *   For example:
     *   new String[]{
     *      "The selected table is disabled, do you want to",
     *      "enable",
     *      "it?"
     *   };
     *
     * @return A three part message.
     */
    String[] getFormattedMessage();
}
