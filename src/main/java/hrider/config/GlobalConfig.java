package hrider.config;

import hrider.io.PathHelper;

import java.io.File;

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
 *          This class is responsible for saving/retrieving global configuration properties.
 */
public class GlobalConfig extends PropertiesConfig {

    //region Constants
    private static final String KEY_DATE_FORMAT                    = "global.dateFormat";
    private static final String KEY_EXTERNAL_VIEWER_FILE_EXTENSION = "global.externalViewerFileExtension";
    private static final String KEY_EXTERNAL_VIEWER_DELIMETER      = "global.externalViewerDelimiter";
    private static final String KEY_BATCH_READ_SIZE                = "global.batch.readSize";
    private static final String KEY_BATCH_WRITE_SIZE               = "global.batch.writeSize";
    private static final String KEY_COMPILATION_FOLDER             = "global.compilation.folder";
    private static final String KEY_CONVERTERS_CLASSES_FOLDER      = "global.converters.classes.folder";
    private static final String KEY_CONVERTERS_CODE_FOLDER         = "global.converters.code.folder";

    private static final String DEFAULT_DATE_FORMAT                    = "yyyy-MM-dd HH:mm:ss.SSS ZZ";
    private static final String DEFAULT_EXTERNAL_VIEWER_FILE_EXTENSION = ".csv";
    private static final String DEFAULT_EXTERNAL_VIEWER_DELIMETER      = ",";
    private static final String DEFAULT_BATCH_READ_SIZE                = "1000";
    private static final String DEFAULT_BATCH_WRITE_SIZE               = "100";
    private static final String DEFAULT_COMPILATION_FOLDER             = "dynamic";
    private static final String DEFAULT_CONVERTERS_CLASSES_FOLDER      = "converters/classes";
    private static final String DEFAULT_CONVERTERS_CODE_FOLDER         = "converters/code";
    //endregion

    //region Variables
    private static GlobalConfig instance;
    //endregion

    //region Constructor

    static {
        instance = new GlobalConfig();
    }

    /**
     * Initializes a new instance of the {@link PropertiesConfig} class with a file name.
     */
    private GlobalConfig() {
        super("global");
    }
    //endregion

    //region Public Methods
    public static GlobalConfig instance() {
        return instance;
    }

    /**
     * Gets the date time format to be used to parse/converters date time strings.
     *
     * @return A {@link String} representing date time format.
     */
    public String getDateFormat() {
        return get(String.class, KEY_DATE_FORMAT, DEFAULT_DATE_FORMAT);
    }

    /**
     * Gets an extension to be used for a file to be created for the external viewer.
     *
     * @return A {@link String} representing a file extension.
     */
    public String getExternalViewerFileExtension() {
        return get(String.class, KEY_EXTERNAL_VIEWER_FILE_EXTENSION, DEFAULT_EXTERNAL_VIEWER_FILE_EXTENSION);
    }

    /**
     * Gets a delimiter to be used in the file to separate the data.
     *
     * @return A character to be used as a delimiter to separate the data in the file.
     */
    public char getExternalViewerDelimeter() {
        return get(Character.class, KEY_EXTERNAL_VIEWER_DELIMETER, DEFAULT_EXTERNAL_VIEWER_DELIMETER);
    }

    /**
     * Gets a size of the batch to be used for read operations.
     *
     * @return A size of the batch.
     */
    public int getBatchSizeForRead() {
        return get(Integer.class, KEY_BATCH_READ_SIZE, DEFAULT_BATCH_READ_SIZE);
    }

    /**
     * Gets a size of the batch to be used for write operations.
     *
     * @return A size of the batch.
     */
    public int getBatchSizeForWrite() {
        return get(Integer.class, KEY_BATCH_WRITE_SIZE, DEFAULT_BATCH_WRITE_SIZE);
    }

    /**
     * Gets a folder where the compiled files should be saved.
     *
     * @return A path to a folder.
     */
    public String getCompilationFolder() {
        return PathHelper.append(PathHelper.getCurrentFolder(), get(String.class, KEY_COMPILATION_FOLDER, DEFAULT_COMPILATION_FOLDER));
    }

    /**
     * Gets a folder where the compiled classes of the custom converters should be located.
     *
     * @return A path to the folder.
     */
    public String getConvertersClassesFolder() {
        return PathHelper.append(PathHelper.getCurrentFolder(), get(String.class, KEY_CONVERTERS_CLASSES_FOLDER, DEFAULT_CONVERTERS_CLASSES_FOLDER));
    }

    /**
     * Gets a folder where the java code of the custom converters should be located.
     *
     * @return A path to the folder.
     */
    public String getConvertersCodeFolder() {
        return PathHelper.append(PathHelper.getCurrentFolder(), get(String.class, KEY_CONVERTERS_CODE_FOLDER, DEFAULT_CONVERTERS_CODE_FOLDER));
    }
    //endregion
}
