package hrider.ui;

import javax.swing.*;
import java.awt.*;

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
 *          This is a listener interface that allows to register for tab action events.
 */
public interface TabActionListener {

    /**
     * The method is called each time the tab of {@link JTabbedPane} is closed by the user.
     *
     * @param component The component of the closed tab.
     */
    void onTabClosed(Component component);

    /**
     * The method is called each time the tab of {@link JTabbedPane} is being duplicated by the user.
     *
     * @param component The component of the duplicated tab.
     */
    void onTabDuplicated(Component component);
}
