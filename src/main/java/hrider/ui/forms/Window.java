package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

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
            JFrame frame = new JFrame("H-Rider - " + getVersion());

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

    private static String getVersion() {
        try {
            String jarPath = Window.class.getProtectionDomain().getCodeSource().getLocation().getPath();

            JarFile file = new JarFile(jarPath);
            return file.getManifest().getMainAttributes().get(new Attributes.Name("version")).toString();
        }
        catch (IOException e) {
            e.printStackTrace();
            return "Unknown Version";
        }
    }
    //endregion

    {
        // GUI initializer generated by IntelliJ IDEA GUI Designer
        // >>> IMPORTANT!! <<<
        // DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        topPanel = new JPanel();
        topPanel.setLayout(new GridLayoutManager(3, 1, new Insets(4, 4, 4, 4), -1, -1));
        topPanel.setMinimumSize(new Dimension(650, 450));
        topPanel.setPreferredSize(new Dimension(1200, 550));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        panel1.setBackground(new Color(-1));
        topPanel.add(
            panel1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 50), null, 0,
            false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JLabel label1 = new JLabel();
        label1.setIcon(new ImageIcon(getClass().getResource("/images/h-rider.png")));
        label1.setText("");
        panel1.add(label1, BorderLayout.WEST);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel2.setBackground(new Color(-1));
        panel1.add(panel2, BorderLayout.CENTER);
        final JLabel label2 = new JLabel();
        label2.setFont(new Font("Curlz MT", Font.BOLD, 28));
        label2.setForeground(new Color(-13408513));
        label2.setText(" H-Rider");
        panel2.add(label2, BorderLayout.WEST);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel3.setBackground(new Color(-1));
        panel2.add(panel3, BorderLayout.CENTER);
        final JLabel label3 = new JLabel();
        label3.setFont(new Font("Curlz MT", label3.getFont().getStyle(), 20));
        label3.setHorizontalAlignment(10);
        label3.setHorizontalTextPosition(11);
        label3.setText("  The ultimate Hbase viewer and editor...");
        panel3.add(label3, BorderLayout.WEST);
        connectToCluster = new JLinkButton();
        connectToCluster.setText("Connect to a cluster...");
        panel3.add(connectToCluster, BorderLayout.EAST);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        topPanel.add(
            panel4, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(566, 377), null, 0, false));
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(1);
        panel4.add(tabbedPane, BorderLayout.CENTER);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(30);
        scrollPane1.setVerticalScrollBarPolicy(20);
        topPanel.add(
            scrollPane1, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, new Dimension(-1, 24), null, 0, false));
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        statusLabel = new JTextArea();
        statusLabel.setBackground(new Color(-986896));
        statusLabel.setEditable(false);
        statusLabel.setLineWrap(true);
        statusLabel.setWrapStyleWord(true);
        scrollPane1.setViewportView(statusLabel);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return topPanel;
    }
}
