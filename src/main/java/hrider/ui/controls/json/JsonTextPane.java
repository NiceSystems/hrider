package hrider.ui.controls.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import hrider.io.Log;

import javax.swing.*;

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
 *          This class represents a custom text pane to present the JSON in a friendly way.
 */
public class JsonTextPane extends JTextPane {

    //region Constants
    private static final Log  logger           = Log.getLogger(JsonTextPane.class);
    private static final long serialVersionUID = 6270183148379328084L;
    //endregion

    //region Constructor
    public JsonTextPane() {
    }
    //endregion

    //region Public Methods

    /**
     * Replaces the content of the text pane with the provided text.
     *
     * @param t The text to set.
     */
    @Override
    public void setText(String t) {
        if (t != null) {
            super.setText(formatJson(t));
        }
        else {
            super.setText(t);
        }
    }

    /**
     * Validates if the text pane content is a valid JSON.
     */
    public void validateJson() {
        new JsonParser().parse(getText());
    }
    //endregion

    //region Private Methods

    /**
     * Formats JSON.
     *
     * @param json A JSON to format.
     * @return A formatted JSON.
     */
    private static String formatJson(String json) {
        try {
            Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
            return gson.toJson(new JsonParser().parse(json));
        }
        catch (Exception e) {
            logger.error(e, "Failed to format json '%s'.", json);
            return json;
        }
    }
    //endregion
}
