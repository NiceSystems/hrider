package hrider.io;

import org.apache.log4j.Logger;

public class Log {

    private Logger logger;

    private Log(Class<?> clazz) {
        logger = Logger.getLogger(clazz);
    }

    public static Log getLogger(Class<?> clazz) {
        return new Log(clazz);
    }

    public void info(String format, String... args) {
        logger.info(String.format(format, args));
    }

    public void debug(String format, String... args) {
        logger.debug(String.format(format, args));
    }

    public void warn(Throwable t, String format, String... args) {
        logger.warn(String.format(format, args), t);
    }

    public void error(Throwable t, String format, String... args) {
        logger.error(String.format(format, args), t);
    }
}
