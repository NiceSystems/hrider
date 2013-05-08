package hrider.config;

import hrider.io.Log;
import hrider.reflection.Clazz;

import java.io.*;
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
 *          The base class for all configuration classes that are based on files represented as properties.
 *          This class also supports dynamic update of the configuration data meaning that a file is tracked for changes.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public abstract class PropertiesConfig {

    //region Variables
    private final static Log logger = Log.getLogger(PropertiesConfig.class);

    private Properties properties;
    private File       file;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link PropertiesConfig} class with a file name.
     *
     * @param name The name of the configuration file.
     */
    protected PropertiesConfig(String name) {
        this.properties = new Properties();

        try {
            this.file = loadFile(name);
            if (!this.file.exists()) {
                File folder = new File("config/");
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                this.file.createNewFile();

                onFileCreated();
            }

            loadProperties(this.file);
            loadSystemProperties();
        }
        catch (IOException e) {
            logger.error(e, "Failed to load properties from the file '%s'", name);
        }
    }
    //endregion

    //region Public Methods

    /**
     * Checks if the specified properties file exists.
     *
     * @param name The name of the file to check. The name should not contain extension.
     * @return True if the file exists or False otherwise.
     */
    public static boolean fileExists(String name) {
        return loadFile(name).exists();
    }

    /**
     * Removes a file that corresponds to the provided name.
     *
     * @param name The name of the properties file without extension to remove.
     * @return True if the file has been successfully removed or False otherwise.
     */
    public static boolean fileRemove(String name) {
        return loadFile(name).delete();
    }

    /**
     * Gets a value for the property specified by name.
     *
     * @param clazz The class that represents the type of the value.
     * @param name  The name of the property.
     * @param <T>   The type of the value.
     * @return A value of the requested property.
     */
    public <T> T get(Class<T> clazz, String name) {
        return get(clazz, name, null);
    }

    /**
     * Gets a value for the property specified by name.
     *
     * @param clazz        The class that represents the type of the value.
     * @param name         The name of the property.
     * @param defaultValue The default value to be returned in case property is not found.
     * @param <T>          The type of the value.
     * @return A value of the requested property.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz, String name, String defaultValue) {
        String value = this.properties.getProperty(name, defaultValue);
        if (value != null && !value.isEmpty()) {
            return (T)Clazz.fromPrimitive(clazz, value);
        }
        return null;
    }

    /**
     * Sets the property value.
     *
     * @param name  The name of the property.
     * @param value The value to set.
     */
    public void set(String name, String value) {
        this.properties.setProperty(name, value);
    }

    /**
     * Removes a property from the file.
     *
     * @param name The name of the property to remove.
     */
    public void remove(String name) {
        this.properties.remove(name);
    }

    /**
     * Gets all properties in the key/value format which starts with the specific prefix.
     *
     * @param startsWith The prefix to look for in the property names.
     * @return A key/value pairs of properties if found or an empty map.
     */
    public Map<String, String> getAll(String startsWith) {
        Map<String, String> values = new HashMap<String, String>();
        for (String property : this.properties.stringPropertyNames()) {
            if (property.startsWith(startsWith)) {
                values.put(property, this.properties.getProperty(property));
            }
        }
        return values;
    }

    /**
     * Saves all configuration properties to the file.
     */
    public void save() {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(this.file);
            this.properties.store(stream, null);
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
    //endregion

    //region Protected Methods
    protected void onFileCreated() {

    }
    //endregion

    //region Private Methods
    private static File loadFile(String name) {
        return new File("config/" + name + ".properties");
    }

    private void loadProperties(File file) throws IOException, FileNotFoundException {
        if (file != null) {
            InputStream stream = null;
            try {
                stream = new FileInputStream(file);
                loadProperties(stream);
            }
            finally {
                if (null != stream) {
                    stream.close();
                }
            }
        }
    }

    private void loadProperties(InputStream stream) throws IOException {
        if (stream != null) {
            this.properties.load(stream);
        }
    }

    private void loadSystemProperties() {
        for (String name : this.properties.stringPropertyNames()) {
            String property = System.getProperty(name);
            if (null != property) {
                this.properties.setProperty(name, property);
            }
        }
    }
    //endregion
}
