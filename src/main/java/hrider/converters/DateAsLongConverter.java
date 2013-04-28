package hrider.converters;

import hrider.config.GlobalConfig;
import hrider.io.Log;
import org.apache.hadoop.hbase.util.Bytes;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
 *          This class is responsible for converting data from byte[] to Date represented as Long and vice versa.
 */
public class DateAsLongConverter extends TypeConverter {

    private static final Log  logger           = Log.getLogger(DateAsLongConverter.class);
    private static final long serialVersionUID = 7524770450006316409L;

    @Override
    public String toString(byte[] value) {
        if (value == null) {
            return null;
        }

        DateFormat df = new SimpleDateFormat(GlobalConfig.instance().getDateFormat(), Locale.ENGLISH);
        return df.format(new Date(Bytes.toLong(value)));
    }

    @Override
    public byte[] toBytes(String value) {
        if (value == null) {
            return EMPTY_BYTES_ARRAY;
        }

        DateFormat df = new SimpleDateFormat(GlobalConfig.instance().getDateFormat(), Locale.ENGLISH);
        try {
            Date date = df.parse(value);
            return Bytes.toBytes(date.getTime());
        }
        catch (ParseException e) {
            logger.error(e, "Failed to convert DateTime '%s' to byte array.", value);
            return null;
        }
    }
}
