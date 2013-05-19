package hrider.system;

import java.util.Arrays;
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
public class Version implements Comparable<Version> {

    //region Constants
    private static final Pattern DELIMITER = Pattern.compile("\\.");
    //endregion

    //region Variables
    private int[] version;
    //endregion

    //region Public Methods
    public static int compare(String ver1, String ver2) {
        Version version1 = new Version(ver1);
        Version version2 = new Version(ver2);

        return version1.compareTo(version2);
    }

    public Version(String version) {
        String[] parts = DELIMITER.split(version);
        if (parts.length < 2 && parts.length > 4) {
            throw new IllegalArgumentException(String.format("incorrect version format: %s", version));
        }

        this.version = new int[] {0, 0, 0, 0};
        for (int i = 0; i < parts.length; i++) {
            this.version[i] = Integer.parseInt(parts[i]);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Version) {
            return Arrays.equals(version, ((Version)obj).version);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(version);
    }

    @Override
    public int compareTo(Version o) {
        for (int i = 0; i < version.length && i < o.version.length; i++) {
            if (version[i] != o.version[i]) {
                return version[i] - o.version[i];
            }
        }
        return 0;
    }
    //endregion
}
