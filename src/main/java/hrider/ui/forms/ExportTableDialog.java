package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import hrider.config.Configurator;
import hrider.data.DataRow;
import hrider.data.ObjectType;
import hrider.data.TypedColumn;
import hrider.export.FileExporter;
import hrider.hbase.Query;
import hrider.hbase.QueryScanner;
import hrider.hbase.Scanner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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
public class ExportTableDialog extends JDialog {

    //region Variables
    private JPanel     contentPane;
    private JButton    btExport;
    private JButton    btCancel;
    private JTextField tfFilePath;
    private JButton    btBrowse;
    private JLabel     writtenRowsCount;
    private JLabel     totalRowsCount;
    private JComboBox  cmbDelimiter;
    private JButton    btOpen;
    private JButton    btClose;
    private JButton    btExportWithQueryButton;
    private String     filePath;
    private boolean    canceled;
    //endregion

    //region Constructor
    public ExportTableDialog(final QueryScanner scanner) {

        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Export table to file");
        getRootPane().setDefaultButton(this.btExport);

        this.tfFilePath.setText(String.format("%s.csv", scanner.getTableName()));

        this.btExport.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (validateInput()) {
                        onExport(scanner);
                    }
                }
            });

        this.btExportWithQueryButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (validateInput()) {
                        ExportTableDialog.this.contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            Collection<TypedColumn> columns = new ArrayList<TypedColumn>();
                            for (String columnName : scanner.getColumns(100)) {
                                columns.add(new TypedColumn(columnName, ObjectType.String));
                            }

                            ScanDialog dialog = new ScanDialog(null, columns);
                            dialog.showDialog(ExportTableDialog.this.contentPane);

                            Query query = dialog.getQuery();
                            if (query != null) {
                                scanner.setQuery(query);

                                onExport(scanner);
                            }
                        }
                        catch (Exception ex) {
                            JOptionPane.showMessageDialog(
                                ExportTableDialog.this.contentPane,
                                String.format("Failed to load columns for '%s' table.\nError: %s", scanner.getTableName(), ex.getMessage()), "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                        finally {
                            ExportTableDialog.this.contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.btCancel.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ExportTableDialog.this.canceled = true;
                }
            });

        this.btOpen.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (ExportTableDialog.this.filePath != null) {
                        try {
                            Desktop.getDesktop().open(new File(ExportTableDialog.this.filePath));
                        }
                        catch (IOException ex) {
                            JOptionPane.showMessageDialog(
                                ExportTableDialog.this.contentPane,
                                String.format("Failed to open file %s.\nError: %s", ExportTableDialog.this.filePath, ex.getMessage()), "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

        this.btBrowse.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser dialog = new JFileChooser();
                    dialog.setCurrentDirectory(new File("."));
                    dialog.setSelectedFile(new File(ExportTableDialog.this.tfFilePath.getText()));

                    int returnVal = dialog.showSaveDialog(ExportTableDialog.this.contentPane);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        ExportTableDialog.this.tfFilePath.setText(dialog.getSelectedFile().getAbsolutePath());
                    }
                }
            });

        this.btClose.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });

        // call onCancel() on ESCAPE
        this.contentPane.registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        ExportTableDialog.this.totalRowsCount.setText(String.valueOf(scanner.getRowsCount()));
                    }
                    catch (Exception ignore) {
                    }
                }
            }).start();
    }
    //endregion

    //region Public Methods
    public void showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }
    //endregion

    //region Private Methods
    private void onExport(final Scanner scanner) {
        final File file = new File(this.tfFilePath.getText());
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (IOException e) {
                JOptionPane.showMessageDialog(
                    this.contentPane, String.format("Failed to create file %s.\nError: %s", this.tfFilePath.getText(), e.getMessage()), "Error",
                    JOptionPane.ERROR_MESSAGE);

                return;
            }
        }

        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    ExportTableDialog.this.tfFilePath.setEnabled(false);
                    ExportTableDialog.this.cmbDelimiter.setEnabled(false);
                    ExportTableDialog.this.btExport.setEnabled(false);
                    ExportTableDialog.this.btExportWithQueryButton.setEnabled(false);
                    ExportTableDialog.this.btCancel.setEnabled(true);
                    ExportTableDialog.this.btOpen.setEnabled(false);
                    ExportTableDialog.this.btClose.setEnabled(false);
                    ExportTableDialog.this.btBrowse.setEnabled(false);

                    ExportTableDialog.this.canceled = false;

                    FileOutputStream stream = null;
                    try {
                        stream = new FileOutputStream(file);
                        FileExporter exporter = new FileExporter(stream, getDelimiter());

                        scanner.resetCurrent(null);

                        Collection<DataRow> rows = scanner.next(Configurator.getBatchSizeForRead());
                        Collection<String> columnNames = scanner.getColumns(0);

                        int counter = 1;

                        while (!rows.isEmpty() && !ExportTableDialog.this.canceled) {
                            for (DataRow row : rows) {
                                exporter.write(row, columnNames);

                                ExportTableDialog.this.writtenRowsCount.setText(Long.toString(counter++));
                                ExportTableDialog.this.writtenRowsCount.paintImmediately(ExportTableDialog.this.writtenRowsCount.getBounds());
                            }

                            rows = scanner.next(Configurator.getBatchSizeForRead());
                        }

                        ExportTableDialog.this.filePath = file.getAbsolutePath();
                        ExportTableDialog.this.btOpen.setEnabled(true);
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            ExportTableDialog.this.contentPane,
                            String.format("Failed to export to file %s.\nError: %s", ExportTableDialog.this.tfFilePath.getText(), e.getMessage()), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            }
                            catch (IOException ignore) {
                            }
                        }

                        ExportTableDialog.this.tfFilePath.setEnabled(true);
                        ExportTableDialog.this.cmbDelimiter.setEnabled(true);
                        ExportTableDialog.this.btExport.setEnabled(true);
                        ExportTableDialog.this.btExportWithQueryButton.setEnabled(true);
                        ExportTableDialog.this.btCancel.setEnabled(false);
                        ExportTableDialog.this.btClose.setEnabled(true);
                        ExportTableDialog.this.btBrowse.setEnabled(true);
                    }
                }
            }).start();
    }

    private Character getDelimiter() {
        String delimiter = this.cmbDelimiter.getSelectedItem().toString().trim();
        if (delimiter != null && delimiter.length() == 1) {
            return delimiter.charAt(0);
        }
        return null;
    }

    private boolean validateInput() {
        Character delimiter = getDelimiter();
        if (delimiter == null) {
            JOptionPane.showMessageDialog(this.contentPane, "The delimiter field must contain exactly one character.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String path = this.tfFilePath.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this.contentPane, "The file path must be provided.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(
            panel2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btExport = new JButton();
        btExport.setText("Export");
        panel2.add(
            btExport, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btCancel = new JButton();
        btCancel.setEnabled(false);
        btCancel.setText("Cancel");
        panel2.add(
            btCancel, new GridConstraints(
            0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btOpen = new JButton();
        btOpen.setEnabled(false);
        btOpen.setText("Open");
        btOpen.setVisible(true);
        panel2.add(
            btOpen, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btClose = new JButton();
        btClose.setText("Close");
        btClose.setVisible(true);
        panel2.add(
            btClose, new GridConstraints(
            0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btExportWithQueryButton = new JButton();
        btExportWithQueryButton.setText("Export with Query");
        panel2.add(
            btExportWithQueryButton, new GridConstraints(
            0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("File path:");
        panel3.add(
            label1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(
            spacer1, new GridConstraints(
            5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tfFilePath = new JTextField();
        panel3.add(
            tfFilePath, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, -1), null, 0, false));
        btBrowse = new JButton();
        btBrowse.setText("Browse");
        panel3.add(
            btBrowse, new GridConstraints(
            0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Written rows:");
        panel3.add(
            label2, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        writtenRowsCount = new JLabel();
        writtenRowsCount.setText("?");
        panel3.add(
            writtenRowsCount, new GridConstraints(
            3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Total rows:");
        panel3.add(
            label3, new GridConstraints(
            4, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        totalRowsCount = new JLabel();
        totalRowsCount.setText("?");
        panel3.add(
            totalRowsCount, new GridConstraints(
            4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Delimiter:");
        panel3.add(
            label4, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel3.add(
            separator1, new GridConstraints(
            2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        cmbDelimiter = new JComboBox();
        cmbDelimiter.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement(",");
        defaultComboBoxModel1.addElement("|");
        defaultComboBoxModel1.addElement("-");
        defaultComboBoxModel1.addElement(":");
        cmbDelimiter.setModel(defaultComboBoxModel1);
        panel3.add(
            cmbDelimiter, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        contentPane.add(
            separator2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
