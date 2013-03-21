package hrider.export;

import hrider.data.ColumnQualifier;
import hrider.data.DataRow;

import java.io.IOException;

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
 *          An exporter interface to be used to export data from the table.
 */
public interface Exporter {

    /**
     * Writes a list of rows into the output stream.
     *
     * @param row A row to write.
     * @param columns A list of columns in the specified order which values to be written.
     */
    void write(DataRow row, Iterable<ColumnQualifier> columns) throws IOException;
}
