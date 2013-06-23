package hrider.converters;

import hrider.config.GlobalConfig;
import hrider.io.Log;
import hrider.io.PathHelper;
import hrider.reflection.JavaPackage;

import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

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
 *          This class is responsible for loading and creation of type converters.
 */
@SuppressWarnings({"CallToPrintStackTrace", "ResultOfMethodCallIgnored"})
public class ConvertersLoader {

    //region Variables
    private static final Log logger = Log.getLogger(ConvertersLoader.class);
    private static final List<ConvertersLoaderHandler> handlers;
    private static       Map<String, TypeConverter>    converters;
    //endregion

    //region Constructor
    static {
        handlers = new ArrayList<ConvertersLoaderHandler>();
        converters = load();
    }

    private ConvertersLoader() {
    }
    //endregion

    //region Public Methods

    /**
     * Adds a handler.
     *
     * @param handler The new handler.
     */
    public static void addHandler(ConvertersLoaderHandler handler) {
        handlers.add(handler);
    }

    /**
     * Removes a handler.
     *
     * @param handler A handler to remove.
     */
    public static void removeHandler(ConvertersLoaderHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Checks whether the specified converter exists.
     *
     * @param name The name of the converter to check.
     * @return True if the specified converter exists or False otherwise.
     */
    public static boolean exists(String name) {
        return converters.containsKey(name);
    }

    /**
     * Gets all loaded converters.
     *
     * @return A list of type converters.
     */
    public static Collection<TypeConverter> getConverters() {
        return converters.values();
    }

    /**
     * Gets all loaded converters that support column name conversions.
     *
     * @return A list of type converters.
     */
    public static Collection<TypeConverter> getNameConverters() {
        Collection<TypeConverter> list = new ArrayList<TypeConverter>();
        for (TypeConverter converter : converters.values()) {
            if (converter.isValidForNameConversion()) {
                list.add(converter);
            }
        }
        return list;
    }

    /**
     * Gets a specific converter according to the provided name.
     *
     * @param name The name of the converter to get.
     * @return A type converter if found or null otherwise.
     */
    public static TypeConverter getConverter(String name) {
        return converters.get(name);
    }

    /**
     * Handles converter editing.
     *
     * @param oldName A new converter name if the name was changed or the old one.
     * @param newName A new converter name if the name was changed or the old one.
     */
    public static void editConverter(String oldName, String newName) {
        if (!oldName.equals(newName)) {
            deleteConverter(oldName);
        }

        reload();

        for (ConvertersLoaderHandler handler : handlers) {
            handler.onEdit(oldName, newName);
        }
    }

    /**
     * Removes converter from the loader's cache and from the file system.
     *
     * @param name The name of the converter to remove.
     */
    public static void removeConverter(String name) {
        if (deleteConverter(name)) {
            reload();

            for (ConvertersLoaderHandler handler : handlers) {
                handler.onRemove(name);
            }
        }
    }

    /**
     * Reloads converters.
     */
    public static void reload() {
        converters = load();

        for (ConvertersLoaderHandler handler : handlers) {
            handler.onLoad();
        }
    }
    //endregion

    //region Private Methods
    private static boolean deleteConverter(String name) {
        TypeConverter converter = converters.get(name);
        if (converter != null) {
            converters.remove(name);

            File classFile = new File(
                PathHelper.append(
                    PathHelper.append(GlobalConfig.instance().getConvertersClassesFolder(), "hrider/converters/custom"),
                    converter.getClass().getSimpleName() + ".class"));

            if (classFile.exists()) {
                classFile.delete();
            }

            File codeFile = new File(PathHelper.append(GlobalConfig.instance().getConvertersCodeFolder(), converter.getClass().getSimpleName() + ".java"));
            if (codeFile.exists()) {
                codeFile.delete();
            }

            return true;
        }
        return false;
    }

    private static Map<String, TypeConverter> load() {
        Map<String, TypeConverter> map = new TreeMap<String, TypeConverter>();

        loadPackage("hrider.converters", map);
        loadFolder(GlobalConfig.instance().getConvertersClassesFolder(), "hrider.converters.custom", map);

        return map;
    }

    private static void loadPackage(String packageName, Map<String, TypeConverter> map) {
        try {
            for (Class<?> clazz : JavaPackage.getClasses(packageName)) {
                if (TypeConverter.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                    try {
                        TypeConverter converter = (TypeConverter)clazz.getConstructor().newInstance();
                        map.put(converter.getName(), converter);
                    }
                    catch (Exception e) {
                        logger.error(e, "Failed to load converter '%s'", clazz.getName());
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error(e, "Failed to load converters from the package '%s'", packageName);
        }
    }

    private static void loadFolder(String folder, String packageName, Map<String, TypeConverter> map) {
        try {
            URLClassLoader loader = new URLClassLoader(
                new URL[]{
                    new File(folder).toURI().toURL()
                }, Thread.currentThread().getContextClassLoader());

            for (Class<?> clazz : JavaPackage.getClassesFromFolder(loader, new File(folder), packageName)) {
                if (TypeConverter.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                    try {
                        TypeConverter converter = (TypeConverter)clazz.getConstructor().newInstance();
                        converter.setCode(
                            loadCode(
                                new File(
                                    PathHelper.append(GlobalConfig.instance().getConvertersCodeFolder(), converter.getClass().getSimpleName() + ".java"))));

                        map.put(converter.getName(), converter);
                    }
                    catch (Exception e) {
                        logger.error(e, "Failed to load converter '%s'", clazz.getName());
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error(e, "Failed to load converters from the folder '%s' for the package '%s'", folder, packageName);
        }
    }

    private static String loadCode(File file) throws IOException, FileNotFoundException {
        if (file.exists()) {
            StringBuilder code = new StringBuilder();

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));

                String line;
                while ((line = reader.readLine()) != null) {
                    code.append(line);
                    code.append(PathHelper.LINE_SEPARATOR);
                }
                return code.toString();
            }
            finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
        return null;
    }
    //endregion
}
