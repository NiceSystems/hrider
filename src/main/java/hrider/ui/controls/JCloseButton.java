package hrider.ui.controls;

import hrider.ui.TabClosedListener;

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
public class JCloseButton extends JPanel {

    //region Constants
    private static final long serialVersionUID = 3457653319908745576L;
    //endregion

    //region Variables
    private List<TabClosedListener> listeners;
    //endregion

    //region Constructor
    public JCloseButton(final String title, final JTabbedPane pane) {

        this.listeners = new ArrayList<TabClosedListener>();

        JLabel label = new JLabel();
        label.setText(title);
        label.setHorizontalTextPosition(SwingConstants.TRAILING);
        label.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("images/server.png")));

        JButton button = new JButton();

        setOpaque(false);

        button.setPreferredSize(new Dimension(16, 16));
        button.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (pane.getTabCount() > 1) {
                        pane.removeTabAt(pane.indexOfTab(title));

                        for (TabClosedListener listener : JCloseButton.this.listeners) {
                            listener.onTabClosed(JCloseButton.this);
                        }
                    }
                }
            });

        button.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("images/close.png")));
        button.setFocusPainted(false);
        button.setOpaque(false);

        add(label);
        add(button);
    }
    //endregion

    //region Public Methods
    public void addTabClosedListener(TabClosedListener listener) {
        this.listeners.add(listener);
    }

    public void removeTabClosedListener(TabClosedListener listener) {
        this.listeners.remove(listener);
    }
    //endregion
}
