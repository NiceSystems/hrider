package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import hrider.config.ClusterConfig;
import hrider.config.ConnectionDetails;
import hrider.config.PropertiesConfig;
import hrider.config.ViewConfig;
import hrider.data.DataTable;
import hrider.hbase.Connection;
import hrider.hbase.ConnectionManager;
import hrider.system.Clipboard;
import hrider.system.ClipboardData;
import hrider.system.InMemoryClipboard;
import hrider.ui.MessageHandler;
import hrider.ui.MessageHandlerListener;
import hrider.ui.TabClosedListener;
import hrider.ui.UIAction;
import hrider.ui.controls.JCloseButton;
import hrider.ui.controls.JLinkButton;
import hrider.ui.views.DesignerView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
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
    private JTabbedPane                  tabbedPane;
    private JLinkButton                  connectToCluster;
    private JPanel                       actionPanel;
    private JLabel                       actionLabel1;
    private JLinkButton                  actionLabel2;
    private JLabel                       actionLabel3;
    private JButton                      copyToClipboard;
    private boolean                      canceled;
    private UIAction                     uiAction;
    private String                       lastError;
    private Map<Component, DesignerView> viewMap;
    //endregion

    //region Constructor
    public Window() {
        this.viewMap = new HashMap<Component, DesignerView>();

        Font font = this.actionLabel1.getFont();
        this.actionLabel1.setFont(font.deriveFont(font.getStyle() | ~Font.BOLD));
        this.actionLabel2.setFont(font.deriveFont(font.getStyle() | ~Font.BOLD));
        this.actionLabel3.setFont(font.deriveFont(font.getStyle() | ~Font.BOLD));

        MessageHandler.addListener(
            new MessageHandlerListener() {
                @Override
                public void onInfo(String message) {
                    lastError = null;
                    uiAction = null;

                    copyToClipboard.setVisible(false);

                    actionLabel1.setText(message);

                    // this trick allows to keep all labels on the same level. The empty text on the link button moves all labels up for a couple of pixels.
                    actionLabel2.setText(" ");
                    actionLabel2.setLinkColor(new Color(240, 240, 240)); // the control color which is not visible.

                    actionLabel3.setText("");
                }

                @Override
                public void onError(String message, Exception ex) {
                    uiAction = null;

                    copyToClipboard.setVisible(true);

                    String error = ' ' + message;
                    lastError = error;

                    if (ex != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);

                        ex.printStackTrace(pw);

                        lastError += ": " + sw.toString();
                        error += ": " + ex.getMessage();

                        pw.close();
                    }

                    actionLabel1.setText(error);
                    actionLabel2.setText(" ");
                    actionLabel2.setLinkColor(new Color(240, 240, 240));
                    actionLabel3.setText("");
                }

                @Override
                public void onAction(UIAction action) {
                    lastError = null;
                    uiAction = action;

                    copyToClipboard.setVisible(false);

                    String[] messageParts = action.getFormattedMessage();
                    if (messageParts.length > 0) {
                        actionLabel1.setText(messageParts[0]);
                    }
                    else {
                        actionLabel1.setText("");
                    }

                    if (messageParts.length > 1) {
                        actionLabel2.setText(messageParts[1]);
                        actionLabel2.setLinkColor(new Color(0, 0, 255)); // the blue link color.
                    }
                    else {
                        actionLabel2.setText(" ");
                        actionLabel2.setLinkColor(new Color(240, 240, 240));
                    }

                    if (messageParts.length > 2) {
                        actionLabel3.setText(messageParts[2]);
                    }
                    else {
                        actionLabel3.setText("");
                    }
                }
            });

        this.connectToCluster.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ConnectionDetails connectionDetails = showDialog();
                    if (connectionDetails != null) {
                        try {
                            Connection connection = ConnectionManager.create(connectionDetails);
                            loadView(connection);
                        }
                        catch (IOException ex) {
                            MessageHandler.addError(String.format("Failed to connect to %s.", connectionDetails.getZookeeper().getHost()), ex);
                        }
                    }
                }
            });

        this.actionLabel2.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (uiAction != null) {
                        uiAction.execute();
                    }
                }
            });

        this.copyToClipboard.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (lastError != null) {
                        Clipboard.setText(lastError);
                    }
                }
            });
    }
    //endregion

    //region Public Methods
    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
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
        window.loadViews(new Splash());

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

    private static String getVersion() {
        try {
            String jarPath = Window.class.getProtectionDomain().getCodeSource().getLocation().getPath();

            JarFile file = new JarFile(jarPath);
            return file.getManifest().getMainAttributes().get(new Attributes.Name("version")).toString();
        }
        catch (IOException ignore) {
            return "Unknown Version";
        }
    }

    /**
     * Load all cluster views.
     */
    private void loadViews(final Splash splash) {

        final Collection<ConnectionDetails> loaded = new ArrayList<ConnectionDetails>();

        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    Collection<ConnectionDetails> connections = new ArrayList<ConnectionDetails>();

                    // See if there are clusters previously connected.
                    Collection<String> clusters = ViewConfig.instance().getClusters();
                    for (String clusterName : clusters) {
                        if (PropertiesConfig.fileExists(clusterName)) {
                            boolean canConnect = false;

                            ClusterConfig clusterConfig = new ClusterConfig(clusterName);
                            ConnectionDetails connectionDetails = clusterConfig.getConnection();

                            if (connectionDetails != null) {
                                splash.update(String.format("Connecting to %s...", connectionDetails.getZookeeper().getHost()));

                                if (connectionDetails.canConnect()) {
                                    connections.add(connectionDetails);

                                    canConnect = true;
                                }
                            }

                            if (!canConnect) {
                                ViewConfig.instance().removeCluster(clusterName);

                                PropertiesConfig.fileRemove(clusterName);
                            }
                        }
                        else {
                            ViewConfig.instance().removeCluster(clusterName);
                        }
                    }

                    for (ConnectionDetails connectionDetails : connections) {
                        try {
                            splash.update(String.format("Loading %s view...", connectionDetails.getZookeeper().getHost()));

                            Connection connection = ConnectionManager.create(connectionDetails);
                            loadView(connection);

                            loaded.add(connectionDetails);
                        }
                        catch (IOException e) {
                            MessageHandler.addError(String.format("Failed to connect to %s.", connectionDetails.getZookeeper().getHost()), e);
                        }
                    }

                    splash.dispose();
                }
            }).start();

        splash.showDialog();

        if (loaded.isEmpty()) {
            ConnectionDetails connectionDetails = showDialog();
            if (connectionDetails != null) {
                try {
                    Connection connection = ConnectionManager.create(connectionDetails);
                    loadView(connection);
                }
                catch (IOException e) {
                    MessageHandler.addError(String.format("Failed to connect to %s.", connectionDetails.getZookeeper().getHost()), e);
                }
            }
            else {
                this.canceled = true;
            }
        }
    }

    /**
     * Loads a single cluster view.
     *
     * @param connection A connection to be used to connect to the cluster.
     */
    private void loadView(Connection connection) {
        DesignerView view = new DesignerView(this.topPanel, connection);

        int index = this.tabbedPane.indexOfTab(connection.getServerName());
        if (index == -1) {
            index = this.tabbedPane.getTabCount();

            JCloseButton closeButton = new JCloseButton(connection.getServerName(), this.tabbedPane);
            this.viewMap.put(closeButton, view);

            closeButton.addTabClosedListener(
                new TabClosedListener() {
                    @Override
                    public void onTabClosed(Component component) {
                        DesignerView designerView = viewMap.get(component);

                        ClipboardData<DataTable> data = InMemoryClipboard.getData();
                        if (data != null) {
                            Connection helper = data.getData().getConnection();
                            if (helper != null) {
                                if (helper.equals(designerView.getConnection())) {
                                    InMemoryClipboard.setData(null);
                                }
                            }
                        }

                        ViewConfig.instance().removeCluster(designerView.getConnection().getServerName());

                        PropertiesConfig.fileRemove(designerView.getConnection().getServerName());

                        ConnectionManager.release(designerView.getConnection().getConnectionDetails());
                        viewMap.remove(component);
                    }
                });

            this.tabbedPane.addTab(connection.getServerName(), view.getView());
            this.tabbedPane.setSelectedIndex(index);
            this.tabbedPane.setTabComponentAt(index, closeButton);

            ViewConfig.instance().addCluster(connection.getServerName());

            view.populate();
        }
        else {
            this.tabbedPane.setSelectedIndex(index);
        }
    }

    /**
     * Shows a connection dialog.
     *
     * @return A reference to {@link hrider.config.ConnectionDetails} class that contains all required information to connect to the cluster.
     */
    private ConnectionDetails showDialog() {
        ConnectionDetailsDialog dialog = new ConnectionDetailsDialog();
        dialog.showDialog(this.topPanel);

        return dialog.getConnectionDetails();
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
        actionPanel = new JPanel();
        actionPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        topPanel.add(
            actionPanel, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 34),
            new Dimension(-1, 34), null, 0, false));
        actionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        copyToClipboard = new JButton();
        copyToClipboard.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
        copyToClipboard.setLabel("");
        copyToClipboard.setMargin(new Insets(0, 0, 0, 0));
        copyToClipboard.setText("");
        actionPanel.add(copyToClipboard);
        actionLabel1 = new JLabel();
        actionLabel1.setFocusable(false);
        actionLabel1.setText("");
        actionPanel.add(actionLabel1);
        actionLabel2 = new JLinkButton();
        actionLabel2.setFocusPainted(false);
        actionLabel2.setFocusable(false);
        actionLabel2.setMargin(new Insets(0, 0, 0, 0));
        actionPanel.add(actionLabel2);
        actionLabel3 = new JLabel();
        actionLabel3.setFocusable(false);
        actionLabel3.setText("");
        actionPanel.add(actionLabel3);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return topPanel;
    }
}
