package hrider.reflection;

import hrider.io.Log;
import hrider.io.PathHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
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
 *          This class is a helper class to work with java packages.
 */
public class JavaPackage {

    //region Constants
    private static       Log     logger               = Log.getLogger(JavaPackage.class);
    private static final Pattern NUMBER_IN_CLASS_NAME = Pattern.compile("[$].*");
    //endregion

    //region Constructor
    private JavaPackage() {
    }
    //endregion

    //region Public Methods

    /**
     * Collects all classes located in the provided package.
     *
     * @param packageName The name of the package.
     * @return A list of classes if found or an empty list otherwise.
     * @throws IOException            Error accessing classes on a file system.
     * @throws ClassNotFoundException Class cannot be found.
     * @throws FileNotFoundException  File cannot be found.
     */
    public static Collection<Class<?>> getClasses(String packageName) throws IOException, ClassNotFoundException, FileNotFoundException {
        return getClasses(Thread.currentThread().getContextClassLoader(), packageName);
    }

    /**
     * Collects all classes located in the provided package.
     *
     * @param loader      The class loader to use for classes.
     * @param packageName The name of the package.
     * @return A list of classes if found or an empty list otherwise.
     * @throws IOException            Error accessing classes on a file system.
     * @throws ClassNotFoundException Class cannot be found.
     * @throws FileNotFoundException  File cannot be found.
     */
    public static Collection<Class<?>> getClasses(ClassLoader loader, String packageName) throws IOException, ClassNotFoundException, FileNotFoundException {
        logger.info("Loading classes from package '%s'", packageName);

        Collection<Class<?>> classes = new HashSet<Class<?>>();
        String path = packageName.replace(".", "/");

        Enumeration<URL> resources = loader.getResources(path + '/');
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String filePath = getPath(url);

            logger.debug("Resource URL: %s", url.toString());
            logger.debug("Normalized path: %s", filePath);

            if (filePath.endsWith(".jar")) {
                classes.addAll(getClassesFromJar(loader, new File(filePath), packageName));
            }
            else {
                classes.addAll(getClassesFromFolder(loader, new File(filePath), packageName));
            }
        }
        return classes;
    }

    /**
     * Collects all classes located in the provided directory under the specified package.
     *
     * @param folder      The directory to search for the classes.
     * @param packageName The name of the package.
     * @return A list of classes if found or an empty list otherwise.
     * @throws ClassNotFoundException Class cannot be found.
     */
    public static Collection<Class<?>> getClassesFromFolder(File folder, String packageName) throws ClassNotFoundException {
        return getClassesFromFolder(Thread.currentThread().getContextClassLoader(), folder, packageName);
    }

    /**
     * Collects all classes located in the provided directory under the specified package.
     *
     * @param loader      The class loader to use for classes.
     * @param folder      The directory to search for the classes.
     * @param packageName The name of the package.
     * @return A list of classes if found or an empty list otherwise.
     * @throws ClassNotFoundException Class cannot be found.
     */
    public static Collection<Class<?>> getClassesFromFolder(ClassLoader loader, File folder, String packageName) throws ClassNotFoundException {
        logger.info("Loading classes from folder '%s'", folder.getAbsolutePath());

        Collection<Class<?>> classes = new HashSet<Class<?>>();
        Collection<String> loadedClasses = new HashSet<String>();

        if (folder.exists()) {
            for (String fileName : folder.list()) {
                File file = new File(PathHelper.append(folder.getAbsolutePath(), fileName));
                if (file.isDirectory()) {
                    classes.addAll(getClassesFromFolder(loader, file, packageName));
                }
                else {
                    if (fileName.endsWith(".class")) {
                        String className = packageName + '.' + PathHelper.getFileNameWithoutExtension(NUMBER_IN_CLASS_NAME.matcher(fileName).replaceAll(""));

                        if (!loadedClasses.contains(className)) {
                            logger.info("Loading class '%s'", className);

                            loadedClasses.add(className);
                            classes.add(loader.loadClass(className));
                        }
                    }
                }
            }
        }

        return classes;
    }

    /**
     * Collects all classes located in the provided jar file under the specified package.
     *
     * @param jarFile     The path to the jar file.
     * @param packageName The name of the package to look into.
     * @return A list of classes if found or an empty list otherwise.
     * @throws IOException            Error accessing classes on a file system.
     * @throws ClassNotFoundException Class cannot be found.
     * @throws FileNotFoundException  File cannot be found.
     */
    public static Collection<Class<?>> getClassesFromJar(File jarFile, String packageName) throws IOException, ClassNotFoundException, FileNotFoundException {
        return getClassesFromJar(Thread.currentThread().getContextClassLoader(), jarFile, packageName);
    }

    /**
     * Collects all classes located in the provided jar file under the specified package.
     *
     * @param loader      The class loader to use for classes.
     * @param jarFile     The path to the jar file.
     * @param packageName The name of the package to look into.
     * @return A list of classes if found or an empty list otherwise.
     * @throws IOException            Error accessing classes on a file system.
     * @throws ClassNotFoundException Class cannot be found.
     * @throws FileNotFoundException  File cannot be found.
     */
    public static Collection<Class<?>> getClassesFromJar(ClassLoader loader, File jarFile, String packageName) throws IOException, ClassNotFoundException,
        FileNotFoundException {

        logger.info("Loading classes from jar '%s'", jarFile.getAbsolutePath());

        Collection<Class<?>> classes = new HashSet<Class<?>>();
        Collection<String> loadedClasses = new HashSet<String>();

        if (jarFile.exists()) {
            JarInputStream stream = null;
            try {
                stream = new JarInputStream(new FileInputStream(jarFile));

                String packagePath = packageName.replace('.', '/');

                JarEntry entry;
                while ((entry = stream.getNextJarEntry()) != null) {
                    if (entry.getName().endsWith(".class") && entry.getName().startsWith(packagePath)) {
                        logger.debug("Found class entry '%s'", entry.getName());

                        String className =
                            packageName + '.' + PathHelper.getFileNameWithoutExtension(NUMBER_IN_CLASS_NAME.matcher(entry.getName()).replaceAll(""));

                        if (!loadedClasses.contains(className)) {
                            logger.info("Loading class '%s'", className);

                            loadedClasses.add(className);
                            classes.add(loader.loadClass(className));
                        }
                    }
                }
            }
            finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }

        return classes;
    }
    //endregion

    //region Private Methods
    private static String getPath(URL url) {
        String path = url.getFile().replace("file:", "");

        int index = path.indexOf('!');
        if (index != -1) {
            path = path.substring(0, index);
        }

        return PathHelper.normalise(path);
    }
    //endregion
}
