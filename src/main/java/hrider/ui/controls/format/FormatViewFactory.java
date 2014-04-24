package hrider.ui.controls.format;

import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

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
public class FormatViewFactory implements ViewFactory {

    private TextFormatter formatter;

    public FormatViewFactory(TextFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @see ViewFactory#create(Element)
     */
    @Override
    public View create(Element elem) {
        return new FormatView(formatter, elem);
    }
}
