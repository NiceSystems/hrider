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

    private static final String KEY_DATE_FORMAT                    = "global.dateFormat";
    private static final String KEY_EXTERNAL_VIEWER_FILE_EXTENSION = "global.externalViewerFileExtension";
    private static final String KEY_EXTERNAL_VIEWER_DELIMETER      = "global.externalViewerDelimiter";
    private static final String KEY_BATCH_READ_SIZE                = "global.batch.readSize";
    private static final String KEY_BATCH_WRITE_SIZE               = "global.batch.writeSize";

    private static final String DEFAULT_DATE_FORMAT                    = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String DEFAULT_EXTERNAL_VIEWER_FILE_EXTENSION = ".csv";
    private static final String DEFAULT_EXTERNAL_VIEWER_DELIMETER      = ",";
    private static final int    DEFAULT_KEY_BATCH_READ_SIZE            = 1000;
    private static final int    DEFAULT_KEY_BATCH_WRITE_SIZE           = 100;

    //region Variables
    /**
     * A list of properties.
     */
    private static Properties properties;
    /**
     * The date time format used to convert strings to {@link Date}.
     */
    private static String     dateFormat;
    /**
     * The extension of the file to be created for the external viewer.
     */
    private static String     externalViewerFileExtension;
    /**
     * The character to be used as a data separator.
     */
    private static String     externalViewerDelimiter;
    /**
     * The number of rows to be read in a batch operation.
     */
    private static int        batchSizeForRead;
    /**
     * The number of rows to be written as a batch.
     */
    private static int        batchSizeForWrite;
    //endregion

    //region Constructor
    static {
        properties = new Properties();
        FileInputStream stream = null;

        try {
            File file = new File("./config.properties");
            if (!file.exists()) {
                file.createNewFile();

                writeDefaults();
            }

            stream = new FileInputStream(file);
            properties.load(stream);

            dateFormat = properties.getProperty(KEY_DATE_FORMAT);
            if (dateFormat == null) {
                dateFormat = DEFAULT_DATE_FORMAT;
            }

            externalViewerFileExtension = properties.getProperty(KEY_EXTERNAL_VIEWER_FILE_EXTENSION);
            if (externalViewerFileExtension == null) {
                externalViewerFileExtension = DEFAULT_EXTERNAL_VIEWER_FILE_EXTENSION;
            }

            externalViewerDelimiter = properties.getProperty(KEY_EXTERNAL_VIEWER_DELIMETER);
            if (externalViewerDelimiter == null) {
                externalViewerDelimiter = DEFAULT_EXTERNAL_VIEWER_DELIMETER;
            }

            batchSizeForRead = parseInt(properties.getProperty(KEY_BATCH_READ_SIZE, "0"));
            if (batchSizeForRead == 0) {
                batchSizeForRead = DEFAULT_KEY_BATCH_READ_SIZE;
            }

            batchSizeForWrite = parseInt(properties.getProperty(KEY_BATCH_WRITE_SIZE, "0"));
            if (batchSizeForWrite == 0) {
                batchSizeForWrite = DEFAULT_KEY_BATCH_WRITE_SIZE;
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
     * Gets an extension to be used for a file to be created for the external viewer.
     *
     * @return A {@link String} representing a file extension.
     */
    public static String getExternalViewerFileExtension() {
        return externalViewerFileExtension;
    }

    /**
     * Gets a delimiter to be used in the file to separate the data.
     *
     * @return A character to be used as a delimiter to separate the data in the file.
     */
    public static char getExternalViewerDelimeter() {
        return externalViewerDelimiter.charAt(0);
    }

    public static int getBatchSizeForRead() {
        return batchSizeForRead;
    }

    public static int getBatchSizeForWrite() {
        return batchSizeForWrite;
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

    //region Private Methods
    private static void writeDefaults() {
        set(KEY_DATE_FORMAT, DEFAULT_DATE_FORMAT);
        set(KEY_EXTERNAL_VIEWER_DELIMETER, DEFAULT_EXTERNAL_VIEWER_DELIMETER);
        set(KEY_EXTERNAL_VIEWER_FILE_EXTENSION, DEFAULT_EXTERNAL_VIEWER_FILE_EXTENSION);
        set(KEY_BATCH_READ_SIZE, Integer.toString(DEFAULT_KEY_BATCH_READ_SIZE));
        set(KEY_BATCH_WRITE_SIZE, Integer.toString(DEFAULT_KEY_BATCH_WRITE_SIZE));
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }
    //endregion
}
