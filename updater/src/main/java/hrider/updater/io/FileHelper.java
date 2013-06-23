package hrider.updater.io;

import java.io.File;
import java.util.Arrays;
import java.util.List;
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
 */
public class FileHelper {

    //region Constructor
    private FileHelper() {
    }
    //endregion

    //region Public Methods
    public static void delete(File path, String... exclude) {
        if (path != null && path.exists()) {
            List<String> excludedPaths = Arrays.asList(exclude);

            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!excludedPaths.contains(file.getName())) {
                        delete(file);
                    }
                }
            }

            path.delete();
        }
    }

    public static File findFile(File folder, Pattern regex) {
        if (folder != null && folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (regex.matcher(file.getName()).find()) {
                        return file;
                    }
                }
            }
        }
        return null;
    }
    //endregion
}
