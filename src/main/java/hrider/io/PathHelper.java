package hrider.io;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathHelper {

    //region Constants
    private final static String  FILE_SEPARATOR = System.getProperty("file.separator");
    private final static Pattern ENV_VAR        = Pattern.compile("((?<=\\$\\{)[a-zA-Z_0-9]*(?=\\}))");
    //endregion

    //region Constructor
    private PathHelper() {
    }
    //endregion

    //region Public Methods
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
            }
        }
        return expandedPath;
    }

    public static String append(String path1, String path2) {
        StringBuilder path = new StringBuilder();

        if (path1 != null) {
            String normalisedPath = expand(normalise(path1));
            path.append(normalisedPath);

            if (!normalisedPath.endsWith(FILE_SEPARATOR)) {
                path.append(FILE_SEPARATOR);
            }
        }

        if (path2 != null) {
            String normalisedPath = expand(normalise(path2));
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
            String normalisedPath = expand(normalise(path));
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
            String normalisedPath = expand(normalise(path));
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
            if ("\\".equals(FILE_SEPARATOR)) {
                return path.replace("/", "\\");
            }
            else {
                return path.replace("\\", "/");
            }
        }
        return null;
    }
    //endregion
}
