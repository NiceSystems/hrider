package hrider.io;

import org.apache.log4j.Logger;

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
 *          This class is a log wrapper.
 */
public class Log {

    //region Variables
    private Logger logger;
    //endregion

    //region Constructor
    private Log(Class<?> clazz) {
        logger = Logger.getLogger(clazz);
    }
    //endregion

    //region Public Methods
    public static Log getLogger(Class<?> clazz) {
        return new Log(clazz);
    }

    public void info(String format, Object... args) {
        logger.info(String.format(format, args));
    }

    public void debug(String format, Object... args) {
        logger.debug(String.format(format, args));
    }

    public void warn(Throwable t, String format, Object... args) {
        logger.warn(String.format(format, args), t);
    }

    public void error(Throwable t, String format, Object... args) {
        logger.error(String.format(format, args), t);
    }
    //endregion
}
