package hrider.io;

import java.io.File;
import java.io.IOException;
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
    public final static String FILE_SEPARATOR = System.getProperty("file.separator");
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
        return normalise(".");
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
            path.append(path1);
            path.append(FILE_SEPARATOR);
        }

        if (path2 != null) {
            path.append(path2);
        }

        return expand(path.toString());
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
        return path;
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
        return path;
    }

    public static String normalise(String path) {
        try {
            return new File(path).getCanonicalPath();
        }
        catch (IOException e) {
            logger.warn(e, "Failed to canonicalize the path: '%s'", path);
            return path;
        }
    }
    //endregion
}
