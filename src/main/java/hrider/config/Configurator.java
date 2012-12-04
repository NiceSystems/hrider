package hrider.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * This class is responsible for
 */
public class Configurator {

    private static Properties properties;
    private static String dateFormat;

    private Configurator() {
    }

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

    public static String getDateFormat() {
        return dateFormat;
    }

    public static void set(String key, String value) {
        properties.setProperty(key, value);
    }

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

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static Map<String, String> getAll(String startsWith) {
        Map<String, String> values = new HashMap<String, String>();
        for (String property : properties.stringPropertyNames()) {
            if (property.startsWith(startsWith)) {
                values.put(property, properties.getProperty(property));
            }
        }
        return values;
    }
}
