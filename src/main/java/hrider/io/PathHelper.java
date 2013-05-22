package hrider.io;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
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
 *          This class is a helper class to work with path's.
 */
public class PathHelper {

    //region Constants
    public final static String FILE_SEPARATOR = "/";
    public final static String LINE_SEPARATOR = System.getProperty("line.separator");

    private final static Log     logger  = Log.getLogger(PathHelper.class);
    private final static Pattern ENV_VAR = Pattern.compile("((?<=\\$\\{)[a-zA-Z_0-9]*(?=\\}))");
    //endregion

    //region Constructor
    private PathHelper() {
    }
    //endregion

    //region Public Methods
    /**
     * Gets current folder of the executing process.
     *
     * @return A path to the current folder.
     */
    public static String getCurrentFolder() {
        return getParent(new File(".").getAbsolutePath());
    }

    /**
     * Removes extension from the file path.
     *
     * @param path The path to remove extension.
     * @return A new path if the provided path contained extension or an original path otherwise.
     */
    public static String getPathWithoutExtension(String path) {
        String normalisedPath = expand(path);

        int index = normalisedPath.lastIndexOf('.');
        if (index != -1) {
            normalisedPath = normalisedPath.substring(0, index);
        }

        return normalisedPath;
    }

    public static String getFileNameWithoutExtension(String path) {
        String leaf = getLeaf(path);

        int index = leaf.lastIndexOf('.');
        if (index != -1) {
            leaf = leaf.substring(0, index);
        }

        return leaf;
    }

    public static String getFileExtension(String path) {
        int index = path.lastIndexOf('.');
        if (index != -1) {
            return path.substring(index);
        }
        return null;
    }

    /**
     * Removes media from the provided path. For example D://some_path/some_folder -> some_path/some_folder
     *
     * @param path The path to remove the media.
     * @return A new path if the provided path contained media or an original path otherwise.
     */
    public static String getPathWithoutMedia(String path) {
        String normalisedPath = expand(path);

        int index = normalisedPath.indexOf(':' + FILE_SEPARATOR);
        if (index != -1) {
            normalisedPath = normalisedPath.substring(index + 1 + FILE_SEPARATOR.length());
        }

        return normalisedPath;
    }

    public static String expand(String path) {
        String expandedPath = path;
        if (expandedPath != null) {
            Matcher matcher = ENV_VAR.matcher(expandedPath);
            while (matcher.find()) {
                String envVar = matcher.group();

                String expVar = System.getenv(envVar);
                if (expVar != null) {
                    expandedPath = expandedPath.replace(String.format("${%s}", envVar), expVar);
                }

                expVar = System.getProperty(envVar);
                if (expVar != null) {
                    expandedPath = expandedPath.replace(String.format("${%s}", envVar), expVar);
                }
            }
        }
        return normalise(expandedPath);
    }

    public static String append(String path1, String path2) {
        StringBuilder path = new StringBuilder();

        if (path1 != null) {
            String normalisedPath = expand(path1);
            path.append(normalisedPath);

            if (!normalisedPath.endsWith(FILE_SEPARATOR)) {
                path.append(FILE_SEPARATOR);
            }
        }

        if (path2 != null) {
            String normalisedPath = expand(path2);
            if (path.length() > 0) {
                if (normalisedPath.startsWith(FILE_SEPARATOR)) {
                    path.append(normalisedPath.substring(FILE_SEPARATOR.length()));
                }
                else {
                    path.append(normalisedPath);
                }
            }
            else {
                path.append(normalisedPath);
            }
        }

        return path.toString();
    }

    public static String getParent(String path) {
        if (path != null) {
            String normalisedPath = expand(path);
            if (normalisedPath.endsWith(FILE_SEPARATOR)) {
                normalisedPath = normalisedPath.substring(0, normalisedPath.length() - FILE_SEPARATOR.length());
            }

            int index = normalisedPath.lastIndexOf(FILE_SEPARATOR);
            if (index != -1) {
                return normalisedPath.substring(0, index);
            }
        }

        return null;
    }

    public static String getLeaf(String path) {
        if (path != null) {
            String normalisedPath = expand(path);
            if (normalisedPath.endsWith(FILE_SEPARATOR)) {
                normalisedPath = normalisedPath.substring(0, normalisedPath.length() - FILE_SEPARATOR.length());
            }

            int index = normalisedPath.lastIndexOf(FILE_SEPARATOR);
            if (index != -1) {
                return normalisedPath.substring(index + 1);
            }
        }

        return null;
    }

    public static String normalise(String path) {
        if (path != null) {
            String normalizedPath = path;
            boolean removeSlash = false;

            try {
                normalizedPath = normalizedPath.replace("\\", FILE_SEPARATOR);
                removeSlash = !normalizedPath.startsWith(FILE_SEPARATOR);

                if (!normalizedPath.startsWith("file:")) {
                    if (normalizedPath.startsWith(FILE_SEPARATOR)) {
                        normalizedPath = "file:" + normalizedPath;
                    }
                    else {
                        normalizedPath = "file:/" + normalizedPath;
                    }
                }

                URI uri = new URI(normalizedPath);
                normalizedPath = uri.getPath();
            }
            catch (URISyntaxException e) {
                logger.warn(e, "Path is not valid URI: '%s'", normalizedPath);
            }

            // Remove slash in the beginning of the path if it was added by the URI class.
            if (removeSlash && normalizedPath.startsWith(FILE_SEPARATOR)) {
                normalizedPath = normalizedPath.substring(FILE_SEPARATOR.length());
            }

            return normalizedPath;
        }
        return null;
    }
    //endregion
}
