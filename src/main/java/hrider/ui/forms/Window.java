package hrider.ui.forms;

import hrider.data.DataTable;
import hrider.hbase.HbaseHelper;
import hrider.system.ClipboardData;
import hrider.system.InMemoryClipboard;
import hrider.ui.MessageHandler;
import hrider.ui.MessageHandlerListener;
import hrider.ui.TabClosedListener;
import hrider.ui.controls.JCloseButton;
import hrider.ui.controls.JLinkButton;
import hrider.ui.views.DesignerView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

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
public class Window {

    //region Variables
    private JPanel                       topPanel;
    private JTextArea                    statusLabel;
    private JTabbedPane                  tabbedPane;
    private JLinkButton                  connectToCluster;
    private boolean                      canceled;
    private Map<Component, DesignerView> viewMap;
    //endregion

    //region Constructor
    public Window() {
        this.viewMap = new HashMap<Component, DesignerView>();

        MessageHandler.addListener(
            new MessageHandlerListener() {
                @Override
                public void onInfo(String message) {
                    Window.this.statusLabel.setText(message);
                    Window.this.statusLabel.paintImmediately(Window.this.statusLabel.getBounds());
                }

                @Override
                public void onError(String message, Exception ex) {
                    String error = message;
                    if (ex != null) {
                        error += ": " + ex.toString();
                    }

                    Window.this.statusLabel.setText(error);
                    Window.this.statusLabel.paintImmediately(Window.this.statusLabel.getBounds());
                }
            });

        this.connectToCluster.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    loadTables();
                }
            });

        loadTables();
    }
    //endregion

    //region Public Methods
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(
            new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI();
                }
            });
    }
    //endregion

    //region Private Methods

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        Window window = new Window();
        if (!window.canceled) {
            JFrame frame = new JFrame("H-Rider");

            frame.setContentPane(window.topPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationByPlatform(true);
            frame.setIconImage(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("images/h-rider.png")).getImage());

            // Display the window.
            frame.pack();
            frame.setVisible(true);
        }
    }

    private void loadTables() {

        ConnectionDetailsDialog dialog = new ConnectionDetailsDialog();
        dialog.showDialog(this.topPanel);

        HbaseHelper hbaseHelper = dialog.getHBaseHelper();
        if (hbaseHelper != null) {
            DesignerView view = new DesignerView(this.topPanel, hbaseHelper);

            int index = this.tabbedPane.indexOfTab(dialog.getHBaseServer().getHost());
            if (index == -1) {
                index = this.tabbedPane.getTabCount();

                JCloseButton closeButton = new JCloseButton(dialog.getHBaseServer().getHost(), this.tabbedPane);
                this.viewMap.put(closeButton, view);

                closeButton.addTabClosedListener(
                    new TabClosedListener() {
                        @Override
                        public void onTabClosed(Component component) {
                            ClipboardData<DataTable> data = InMemoryClipboard.getData();
                            if (data != null) {
                                HbaseHelper helper = data.getData().getHbaseHelper();
                                if (helper != null) {
                                    DesignerView designerView = Window.this.viewMap.get(component);
                                    if (helper.equals(designerView.getHbaseHelper())) {
                                        InMemoryClipboard.setData(null);
                                    }
                                }
                            }
                        }
                    });

                this.tabbedPane.addTab(dialog.getHBaseServer().getHost(), view.getView());
                this.tabbedPane.setSelectedIndex(index);
                this.tabbedPane.setTabComponentAt(index, closeButton);

                view.populate();
            }
            else {
                this.tabbedPane.setSelectedIndex(index);
            }
        }
        else {
            this.canceled = true;
        }
    }
    //endregion
}
