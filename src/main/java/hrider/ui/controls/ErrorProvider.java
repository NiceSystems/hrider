package hrider.ui.controls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
 *          This class represents a user control that is used to provide error handling to the user.
 */
public class ErrorProvider extends JLabel {

    //region Variables
    private transient Icon icon;
    private Timer timer;
    private int   counter;
    //endregion

    //region Constructor
    public ErrorProvider() {
        super("", new ImageIcon(ErrorProvider.class.getResource("/images/error.png")), SwingConstants.CENTER);

        setVisible(false);
        setPreferredSize(new Dimension(16, 16));

        this.icon = getIcon();
        this.timer = new Timer(
            500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getIcon() == null) {
                    setIcon(icon);
                }
                else {
                    setIcon(null);
                }

                counter++;

                if (counter == 6) {
                    timer.stop();
                }
            }
        });
    }
    //endregion

    //region Public Methods
    public void clearError() {
        setVisible(false);
    }

    public void setError(String error) {
        if (!this.timer.isRunning()) {
            setToolTipText(error == null || error.isEmpty() ? "Error message was not provided" : error);
            setVisible(true);

            this.counter = 0;
            this.timer.start();
        }
    }
    //endregion
}
