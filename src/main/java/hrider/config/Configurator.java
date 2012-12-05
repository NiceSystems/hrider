package hrider.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
 *          This class is responsible for saving/retrieving configuration properties.
 */
public class Configurator {

    //region Variables
    /**
     * A list of properties.
     */
    private static Properties properties;
    /**
     * The date time format used to convert strings to {@link Date}.
     */
    private static String     dateFormat;
    //endregion

    //region Constructor
    static {
        properties = new Properties();
        FileInputStream stream = null;

        try {
            File file = new File("./config.properties");
            if (!file.exists()) {
                file.createNewFile();
            }

            stream = new FileInputStream(file);
            properties.load(stream);

            dateFormat = properties.getProperty("global.dateFormat");
            if (dateFormat == null) {
                dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
            }
        }
        catch (Exception ignore) {
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException ignore) {
                }
            }
        }
    }

    private Configurator() {
    }
    //endregion

    //region Public Methods

    /**
     * Gets the date time format to be used to parse/convert date time strings.
     *
     * @return A {@link String} representing date time format.
     */
    public static String getDateFormat() {
        return dateFormat;
    }

    /**
     * Gets a specific configuration property according to the provided key.
     *
     * @param key The key to look for.
     * @return A property value if found or null otherwise.
     */
    public static String get(String key) {
        return properties.getProperty(key);
    }


    /**
     * Sets a property associated with the provided key.
     *
     * @param key   The key to associate with the value.
     * @param value The value to save in the configuration.
     */
    public static void set(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Saves all configuration properties to the file.
     */
    public static void save() {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(new File("./config.properties"));
            properties.store(stream, "");
        }
        catch (Exception ignore) {
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * Gets all properties in the key/value format which starts with the specific prefix.
     *
     * @param startsWith The prefix to look for in the property names.
     * @return A key/value pairs of properties if found or an empty map.
     */
    public static Map<String, String> getAll(String startsWith) {
        Map<String, String> values = new HashMap<String, String>();
        for (String property : properties.stringPropertyNames()) {
            if (property.startsWith(startsWith)) {
                values.put(property, properties.getProperty(property));
            }
        }
        return values;
    }
    //endregion
}
