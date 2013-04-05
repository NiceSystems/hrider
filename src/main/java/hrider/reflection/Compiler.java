package hrider.reflection;

import hrider.config.GlobalConfig;
import hrider.io.PathHelper;

import javax.tools.*;
import java.io.*;
import java.util.Arrays;

public class Compiler {

    private Compiler() {
    }

    public static Class<?> compile(String name, String code) throws IOException, ClassNotFoundException {
        String folder = GlobalConfig.instance().getCompilationFolder();

        String path = name.replace('.', '/');
        File file = saveCode(PathHelper.append(folder, path), code);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(file));
        boolean success = compiler.getTask(null, fileManager, null, null, null, units).call();
        if (success) {
            ClassLoader classLoader = fileManager.getClassLoader(null);
            return classLoader.loadClass(name);
        }

        return null;
    }

    private static File saveCode(String path, String code) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

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
}
