package hrider.hbase;

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
public class TableUtil {

    public static final String ROOT_TABLE = "-ROOT-";
    public static final String META_TABLE = ".META.";

    private TableUtil() {
    }

    public static boolean isMetaTable(String tableName) {
        return META_TABLE.equals(tableName) || ROOT_TABLE.equals(tableName);
    }
}
