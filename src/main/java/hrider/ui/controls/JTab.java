package hrider.ui.controls;

import hrider.ui.Images;
import hrider.ui.TabActionListener;
import hrider.ui.views.DesignerView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

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
 *          This class represents a custom button used on tabs in tab component. The button allows to user to close unnecessary tabs.
 */
public class JTab extends JPanel {

    //region Constants
    private static final long serialVersionUID = 3457653319908745576L;
    //endregion

    //region Variables
    private List<TabActionListener> listeners;
    //endregion

    //region Constructor
    public JTab(String title, final DesignerView view, final JTabbedPane pane) {

        setOpaque(false);

        this.listeners = new ArrayList<TabActionListener>();

        JLabel label = new JLabel();
        label.setText(title);
        label.setHorizontalTextPosition(SwingConstants.TRAILING);
        label.setIcon(Images.get("server"));

        JButton closeButton = new JButton(Images.get("close"));
        closeButton.setPreferredSize(new Dimension(16, 16));
        closeButton.setToolTipText("Close the tab");
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        closeButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (pane.getTabCount() > 1) {
                        pane.removeTabAt(pane.indexOfComponent(view.getView()));

                        for (TabActionListener listener : listeners) {
                            listener.onTabClosed(view);
                        }
                    }
                }
            });

        JButton duplicateButton = new JButton(Images.get("duplicate"));
        duplicateButton.setPreferredSize(new Dimension(16, 16));
        duplicateButton.setToolTipText("Duplicate the tab");
        duplicateButton.setFocusPainted(false);
        duplicateButton.setOpaque(false);
        duplicateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        duplicateButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (TabActionListener listener : listeners) {
                        listener.onTabDuplicated(view);
                    }
                }
            });

        add(label);
        add(closeButton);
        add(duplicateButton);
    }
    //endregion

    //region Public Methods
    public void addTabActionListener(TabActionListener listener) {
        this.listeners.add(listener);
    }

    public void removeTabActionListener(TabActionListener listener) {
        this.listeners.remove(listener);
    }
    //endregion
}
