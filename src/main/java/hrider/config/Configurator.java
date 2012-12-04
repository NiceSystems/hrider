package hrider.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 9/12/12
 * Time: 11:19 AM
 * To change this template use File | Settings | File Templates.
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
            file.createNewFile();

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
