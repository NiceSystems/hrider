package hrider.updater.io;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

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
public class ZipHelper {

    //region Constants
    private static final int BUFFER_SIZE = 4096;
    //endregion

    //region Constructor
    private ZipHelper() {
    }
    //endregion

    //region Public Methods
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void unzip(File zippedFile, File targetDir) throws ZipException, IOException, FileNotFoundException {
        targetDir.mkdirs();

        ZipFile zipFile = new ZipFile(zippedFile);

        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File targetFile = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                }
                else {
                    InputStream input = zipFile.getInputStream(entry);
                    try {
                        OutputStream output = new FileOutputStream(targetFile);
                        try {
                            copy(input, output);
                        }
                        finally {
                            output.close();
                        }
                    }
                    finally {
                        input.close();
                    }
                }
            }
        }
        finally {
            zipFile.close();
        }
    }

    public static List<String> getRootEntries(File zippedFile) throws IOException, ZipException {
        List<String> fileNames = new ArrayList<String>();

        ZipFile zipFile = new ZipFile(zippedFile);

        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                int index = entry.getName().indexOf('/');
                if (index == -1 || index == entry.getName().length() - 1) {
                    fileNames.add(entry.getName());
                }
            }
        }
        finally {
            zipFile.close();
        }

        return fileNames;
    }
    //endregion

    //region Private Methods
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }
    //endregion
}
