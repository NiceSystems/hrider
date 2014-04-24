package hrider.converters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.awt.*;
import java.util.Map;
import java.util.regex.Pattern;

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
 *          This class is a place holder for JSON editors.
 */
public class JsonConverter extends StringConverter {

    private static final long serialVersionUID = 6603609684230707505L;

    @Override
    public boolean isValidForNameConversion() {
        return false;
    }

    @Override
    public boolean supportsFormatting() {
        return true;
    }

    @Override
    public String format(String value) {
        return formatJson(value);
    }

    @Override
    public Map<Pattern, Color> getColorMappings() {
        return null;
    }

    public static String toJson(Object obj) {
        try {
            Gson gson = new GsonBuilder().create();
            return gson.toJson(obj);
        }
        catch (Exception e) {
            logger.error(e, "Failed to convert object to json '%s'.", obj);
            return null;
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(json, type);
        }
        catch (Exception e) {
            logger.error(e, "Failed to convert json '%s' to object.", json);
            return null;
        }
    }

    /**
     * Formats JSON.
     *
     * @param json A JSON to format.
     * @return A formatted JSON.
     */
    public static String formatJson(String json) {
        try {
            Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
            return gson.toJson(new JsonParser().parse(json));
        }
        catch (Exception e) {
            logger.error(e, "Failed to format json '%s'.", json);
            return json;
        }
    }
}
