package hrider.updater.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import hrider.updater.io.Downloader;
import hrider.updater.io.FileHelper;
import hrider.updater.io.ProgressListener;
import hrider.updater.io.ZipHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
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
 */
public class Window {

    //region Constants
    private static final Pattern JAR_PATTERN     = Pattern.compile("h-rider-?[0-9]{0,4}\\.?[0-9]{0,4}\\.?[0-9]{0,4}\\.?[0-9]{0,4}\\.jar");
    private static final Pattern UPDATER_PATTERN = Pattern.compile("h-rider-updater-?[0-9]{0,4}\\.?[0-9]{0,4}\\.?[0-9]{0,4}\\.?[0-9]{0,4}\\.jar");
    //endregion

    //region Variables
    private JPanel       topPanel;
    private JFrame       frame;
    private JProgressBar progressBar;
    private JButton      cancelButton;
    private JLabel       actionLabel;
    //endregion

    //region Constructor
    public Window(final URL url, final File targetFolder) {

        cancelButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                }
            });

        Downloader.setListener(
            new ProgressListener() {
                @Override
                public void onProgress(int percentage) {
                    progressBar.setValue(percentage);
                }
            });

        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        File file = Downloader.download(url);
                        if (file != null) {
                            actionLabel.setText("Deleting old files...");

                            List<String> filesToRemove = ZipHelper.getRootEntries(file);
                            for (String fileName : filesToRemove) {
                                if (JAR_PATTERN.matcher(fileName).find()) {
                                    File jar = FileHelper.findFile(targetFolder, JAR_PATTERN);
                                    FileHelper.delete(jar);
                                }
                                else if (UPDATER_PATTERN.matcher(fileName).find()) {
                                    File jar = FileHelper.findFile(targetFolder, UPDATER_PATTERN);
                                    FileHelper.delete(jar);
                                }
                                else {
                                    FileHelper.delete(new File(targetFolder + "/" + fileName));
                                }
                            }

                            actionLabel.setText("Extracting archive...");
                            ZipHelper.unzip(file, targetFolder);

                            File jar = FileHelper.findFile(
                                targetFolder, Pattern.compile("h-rider-?[0-9]{0,4}\\.?[0-9]{0,4}\\.?[0-9]{0,4}\\.?[0-9]{0,4}\\.jar"));

                            if (jar != null) {
                                Runtime.getRuntime().exec(String.format("java -jar %s", jar.getName()), null, jar.getParentFile());
                            }
                        }
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(topPanel, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    finally {
                        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                    }
                }
            }).start();
    }
    //endregion

    //region Public Methods
    public static void main(String[] args) throws MalformedURLException {
        if (args.length != 2) {
            throw new IllegalArgumentException("args missing");
        }

        final URL url = new URL(args[0]);
        final File targetFolder = new File(args[1]);

        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(
            new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI(url, targetFolder);
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
    private static void createAndShowGUI(URL url, File targetFolder) {
        Window window = new Window(url, targetFolder);

        JFrame frame = new JFrame("h-rider-updater - " + getVersion());
        window.frame = frame;

        frame.setContentPane(window.topPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationByPlatform(true);
        frame.setIconImage(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("images/h-rider.png")).getImage());
        frame.setResizable(false);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
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
        topPanel.setLayout(new GridLayoutManager(2, 2, new Insets(10, 10, 10, 10), -1, -1));
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        topPanel.add(
            progressBar, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(300, 20), null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        topPanel.add(
            cancelButton, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        actionLabel = new JLabel();
        actionLabel.setText("Downloading...");
        topPanel.add(
            actionLabel, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return topPanel;
    }
}
