package hrider.ui.controls.format;

import javax.swing.text.StyledEditorKit;
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
 *          <p/>
 *          This class is used to customize style of the view.
 */
public class FormatEditorKit extends StyledEditorKit {

    //region Constants
    private static final long serialVersionUID = 2969169649596107757L;
    //endregion

    //region Variables
    private FormatViewFactory viewFactory;
    //endregion

    //region Constructor
    public FormatEditorKit(TextFormatter formatter) {
        this.viewFactory = new FormatViewFactory(formatter);
    }
    //endregion

    //region Public Methods
    @Override
    public ViewFactory getViewFactory() {
        return this.viewFactory;
    }

    @Override
    public String getContentType() {
        return "text/custom";
    }
    //endregion
}
