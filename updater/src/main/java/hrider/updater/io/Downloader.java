package hrider.updater.io;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
public class Downloader {

    //region Constants
    private static final int    DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final double TOTAL_PERCENTS      = 100.0;
    //endregion

    //region Variables
    private static ProgressListener listener;
    //endregion

    //region Constructor
    private Downloader() {
    }
    //endregion

    //region Public Properties
    public static ProgressListener getListener() {
        return listener;
    }

    public static void setListener(ProgressListener listener) {
        Downloader.listener = listener;
    }
    //endregion

    //region Public Methods
    public static File download(URL url) throws IOException, FileNotFoundException, MalformedURLException {
        StringBuilder fileName = new StringBuilder();
        StringBuilder extension = new StringBuilder();

        extractFileNameAndExtension(url.getPath(), fileName, extension);

        File temp = File.createTempFile(fileName.toString() + '-', extension.length() == 0 ? null : extension.toString());
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        int code = connection.getResponseCode();
        while (code / 100 == 3) {
            URL redirectedUrl = new URL(connection.getHeaderField("Location"));
            connection.disconnect();

            connection = (HttpURLConnection)redirectedUrl.openConnection();
            code = connection.getResponseCode();
        }

        InputStream in = connection.getInputStream();
        OutputStream out = new FileOutputStream(temp);

        try {
            copy(in, connection.getContentLength(), out);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ignore) {
            }

            try {
                out.close();
            }
            catch (IOException ignore) {
            }
        }

        return temp;
    }
    //endregion

    //region Private Methods
    private static void copy(InputStream in, double inSize, OutputStream out) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        double totalBytesRead = 0;
        int bytesRead;

        while (-1 != (bytesRead = in.read(buffer))) {
            out.write(buffer, 0, bytesRead);

            totalBytesRead += bytesRead;

            if (listener != null) {
                listener.onProgress((int)(totalBytesRead / inSize * TOTAL_PERCENTS));
            }
        }
    }

    private static void extractFileNameAndExtension(String path, StringBuilder fileName, StringBuilder extension) {
        String leaf = path.replace("\\", "/");

        int index = leaf.lastIndexOf('/');
        if (index != -1) {
            leaf = leaf.substring(index + 1);
        }

        index = leaf.lastIndexOf('.');
        if (index == -1) {
            fileName.append(leaf);
        }
        else {
            fileName.append(leaf.substring(0, index));
            extension.append(leaf.substring(index));
        }
    }
    //endregion
}
