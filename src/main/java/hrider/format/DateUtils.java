package hrider.format;

import hrider.config.GlobalConfig;
import hrider.io.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
public class DateUtils {

    private static final Log logger = Log.getLogger(DateUtils.class);
    private static final DateFormat df;

    static {
        df = new SimpleDateFormat(GlobalConfig.instance().getDateFormat(), Locale.ENGLISH);
        df.setTimeZone(TimeZone.getTimeZone(GlobalConfig.instance().getDateTimeZone()));
    }

    private DateUtils() {
    }

    public static DateFormat getDefaultDateFormat() {
        return df;
    }

    public static String format(Date date) {
        return df.format(date);
    }

    public static Date parse(String date) {
        try {
            return df.parse(date);
        }
        catch (ParseException e) {
            logger.error(e, "Failed to convert Date from string '%s'.", date);
            return null;
        }
    }
}
