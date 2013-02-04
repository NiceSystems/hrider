package hrider.format;

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
 *          This class represents a formatter that uses character to delimit data.
 */
public class CharacterDelimitedFormatter implements Formatter {

    private char          separator;
    private StringBuilder formatted;

    public CharacterDelimitedFormatter(char separator) {
        this.separator = separator;
        this.formatted = new StringBuilder();
    }

    /**
     * Appends an object to a formatted string.
     *
     * @param value A value to append to a formatted string.
     */
    @Override
    public void append(Object value) {

        if (this.formatted.length() > 0) {
            this.formatted.append(this.separator);
        }

        if (value != null) {
            this.formatted.append(value.toString());
        }
    }

    /**
     * Gets a formatted string.
     *
     * @return A formatted string.
     */
    @Override
    public String getFormattedString() {
        return this.formatted.toString();
    }
}
