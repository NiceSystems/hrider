package hrider.export;

import hrider.data.ColumnQualifier;
import hrider.data.DataCell;
import hrider.data.DataRow;
import hrider.format.CharacterDelimitedFormatter;
import hrider.format.Formatter;

import java.io.IOException;
import java.io.OutputStream;

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
 *          This class represents an exporter that saves the data into the file.
 */
public class FileExporter implements Exporter {

    //region Constants
    private static final byte[] LINE_SEPARATOR = System.getProperty("line.separator").getBytes();
    //endregion

    //region Variables
    private Character    delimiter;
    private OutputStream stream;
    private boolean      headerWritten;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link FileExporter} class.
     *
     * @param stream    An output stream to write to.
     * @param delimiter A character to be used as a data delimiter.
     */
    public FileExporter(OutputStream stream, char delimiter) {
        this.stream = stream;
        this.delimiter = delimiter;
        this.headerWritten = false;
    }
    //endregion

    //region Public Methods

    /**
     * Writes a list of rows to the output stream.
     *
     * @param row A row to write.
     * @throws IOException
     */
    @Override
    public void write(DataRow row, Iterable<ColumnQualifier> columns) throws IOException {
        if (!this.headerWritten) {

            Formatter formatter = new CharacterDelimitedFormatter(this.delimiter);
            for (ColumnQualifier column : columns) {
                formatter.append(column);
            }

            this.stream.write(formatter.getFormattedString().getBytes());
            this.stream.write(LINE_SEPARATOR);

            this.headerWritten = true;
        }

        Formatter formatter = new CharacterDelimitedFormatter(this.delimiter);

        for (ColumnQualifier column : columns) {
            DataCell cell = row.getCell(column);
            if (cell != null) {
                formatter.append(cell.getValue());
            }
            else {
                formatter.append("");
            }
        }

        this.stream.write(formatter.getFormattedString().getBytes());
        this.stream.write(LINE_SEPARATOR);
    }
    //endregion
}
