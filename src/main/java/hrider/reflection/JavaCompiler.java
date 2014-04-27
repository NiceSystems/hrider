package hrider.reflection;

import hrider.io.PathHelper;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

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
 *          This class is responsible for compiling java code.
 */
public class JavaCompiler {

    //region Variables
    private static List<String> errors;
    //endregion

    //region Constructor
    static {
        errors = new ArrayList<String>();
    }

    private JavaCompiler() {
    }
    //endregion

    //region Public Properties
    public static List<String> getErrors() {
        return errors;
    }
    //endregion

    //region Public Methods
    public static boolean compile(File sourceCode, String outputFolder) throws Exception {
        errors.clear();

        // make sure all directories are created.
        new File(outputFolder).mkdirs();

        StandardJavaFileManager fileManager = null;

        try {
            javax.tools.JavaCompiler compiler = (javax.tools.JavaCompiler)Class.forName("com.sun.tools.javac.api.JavacTool").getConstructor().newInstance();
            fileManager = compiler.getStandardFileManager(null, null, null);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceCode));

            Collection<String> options = new ArrayList<String>();
            options.addAll(Arrays.asList("-classpath", System.getProperty("java.class.path"), "-d", outputFolder));

            boolean success = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
            if (!success) {
                for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                    errors.add(diagnostic.getMessage(Locale.ENGLISH));
                }
            }

            return success;
        }
        finally {
            if (fileManager != null) {
                fileManager.close();
            }
        }
    }

    public static File saveCode(String className, String code, String outputFolder) throws IOException, FileNotFoundException {
        // make sure all directories are created.
        new File(outputFolder).mkdirs();

        String path = PathHelper.append(outputFolder, className + ".java");
        File file = new File(path);

        FileOutputStream stream = null;

        try {
            stream = new FileOutputStream(file);
            stream.write(code.getBytes());

            return file;
        }
        finally {
            if (stream != null) {
                stream.close();
            }
        }
    }
    //endregion
}
