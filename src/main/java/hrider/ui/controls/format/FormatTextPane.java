package hrider.ui.controls.format;

import hrider.converters.TypeConverter;
import hrider.io.Log;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.regex.Pattern;

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
 *          This class represents a custom text pane to present the data in a friendly way.
 */
public class FormatTextPane extends JTextPane implements TextFormatter {

    //region Constants
    private static final Log  logger           = Log.getLogger(FormatTextPane.class);
    private static final long serialVersionUID = 6270183148379328084L;
    //endregion

    //region Variables
    private TypeConverter typeConverter;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link FormatTextPane} class.
     */
    public FormatTextPane() {
        this.setEditorKitForContentType("text/custom", new FormatEditorKit(this));
        this.setContentType("text/custom");
    }
    //endregion

    //region Public Methods

    /**
     * Replaces the text pane content with the new text.
     *
     * @param t The text to set.
     */
    @Override
    public void setText(String t) {
        if (t != null) {
            super.setText(format(t));
        }
        else {
            super.setText(t);
        }
    }

    /**
     * Sets the type converter to format the data.
     *
     * @param typeConverter The type converter to set.
     */
    public void setTypeConverter(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    @Override
    public Map<Pattern, Color> getColorMappings() {
        if (typeConverter != null) {
            return typeConverter.getColorMappings();
        }
        return null;
    }

    /**
     * Formats the text.
     *
     * @param value The text to format.
     * @return A formatted text.
     */
    @Override
    public String format(String value) {
        try {
            if (typeConverter != null) {
                return typeConverter.format(value);
            }
            return value;
        }
        catch (Exception e) {
            logger.error(e, "Failed to format XML '%s'.", value);
            return value;
        }
    }
    //endregion
}
