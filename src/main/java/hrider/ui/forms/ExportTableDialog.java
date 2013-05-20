package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import hrider.actions.*;
import hrider.actions.Action;
import hrider.config.GlobalConfig;
import hrider.data.ColumnQualifier;
import hrider.data.ColumnType;
import hrider.data.DataRow;
import hrider.data.TypedColumn;
import hrider.export.FileExporter;
import hrider.hbase.Connection;
import hrider.hbase.HbaseActionListener;
import hrider.hbase.QueryScanner;
import hrider.hbase.Scanner;
import hrider.io.Log;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
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

    //region Constants
    private static final Log logger = Log.getLogger(ExportTableDialog.class);
    //endregion

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
    private JComboBox  cmbFileType;
    private JLabel     labelDelimiter;
    private String     filePath;
    private boolean    canceled;
    //endregion

    //region Constructor
    public ExportTableDialog(final QueryScanner scanner) {

        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Export table to file");
        getRootPane().setDefaultButton(this.btExport);

        this.tfFilePath.setText(String.format("%s.hfile", scanner.getTableName()));

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
                        contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            Collection<TypedColumn> columns = new ArrayList<TypedColumn>();
                            for (ColumnQualifier column : scanner.getColumns(100)) {
                                columns.add(new TypedColumn(column, ColumnType.String));
                            }

                            ScanDialog dialog = new ScanDialog(null, columns);
                            if (dialog.showDialog(contentPane)) {
                                scanner.setQuery(dialog.getQuery());

                                onExport(scanner);
                            }
                        }
                        catch (Exception ex) {
                            JOptionPane.showMessageDialog(
                                contentPane, String.format("Failed to load columns for '%s' table.\nError: %s", scanner.getTableName(), ex.getMessage()),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                        finally {
                            contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.btCancel.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canceled = true;
                }
            });

        this.btOpen.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (filePath != null) {
                        try {
                            Desktop.getDesktop().open(new File(filePath));
                        }
                        catch (IOException ex) {
                            JOptionPane.showMessageDialog(
                                contentPane, String.format("Failed to open file %s.\nError: %s", filePath, ex.getMessage()), "Error",
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
                    dialog.setSelectedFile(new File(tfFilePath.getText()));

                    int returnVal = dialog.showSaveDialog(contentPane);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        tfFilePath.setText(dialog.getSelectedFile().getAbsolutePath());
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

        this.cmbFileType.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean visible = "Delimited".equals(cmbFileType.getSelectedItem());

                    cmbDelimiter.setVisible(visible);
                    labelDelimiter.setVisible(visible);

                    if (tfFilePath.getText().startsWith(scanner.getTableName())) {
                        if (visible) {
                            tfFilePath.setText(String.format("%s.csv", scanner.getTableName()));
                        }
                        else {
                            tfFilePath.setText(String.format("%s.hfile", scanner.getTableName()));
                        }
                    }

                    pack();
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

        RunnableAction.run(
            "export table rows count", new Action<Object>() {
            @Override
            public Object run() throws IOException {
                long totalNumberOfRows = scanner.getRowsCount(GlobalConfig.instance().getRowCountTimeout());
                if (scanner.isRowsCountPartiallyCalculated()) {
                    totalRowsCount.setText("more than " + totalNumberOfRows);
                }
                else {
                    totalRowsCount.setText(String.valueOf(totalNumberOfRows));
                }
                return null;
            }

            @Override
            public void onError(Exception ex) {
                logger.error(ex, "Failed to count rows number.");
            }
        });
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
                    tfFilePath.setEnabled(false);
                    cmbFileType.setEnabled(false);
                    cmbDelimiter.setEnabled(false);
                    btExport.setEnabled(false);
                    btExportWithQueryButton.setEnabled(false);
                    btCancel.setEnabled(true);
                    btOpen.setEnabled(false);
                    btClose.setEnabled(false);
                    btBrowse.setEnabled(false);

                    canceled = false;

                    try {
                        if ("Delimited".equals(cmbFileType.getSelectedItem())) {
                            exportDelimitedFile(file, scanner);
                        }
                        else {
                            exportHFile(file, scanner);
                        }

                        filePath = file.getAbsolutePath();
                        btOpen.setEnabled(true);
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            contentPane, String.format("Failed to export to file %s.\nError: %s", tfFilePath.getText(), e.getMessage()), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    finally {
                        tfFilePath.setEnabled(true);
                        cmbFileType.setEnabled(true);
                        cmbDelimiter.setEnabled("Delimited".equals(cmbFileType.getSelectedItem()));
                        btExport.setEnabled(true);
                        btExportWithQueryButton.setEnabled(true);
                        btCancel.setEnabled(false);
                        btClose.setEnabled(true);
                        btBrowse.setEnabled(true);
                    }
                }
            }).start();
    }

    private void exportDelimitedFile(File file, Scanner scanner) throws IOException, FileNotFoundException {
        FileOutputStream stream = new FileOutputStream(file);
        FileExporter exporter = new FileExporter(stream, getDelimiter());

        try {
            scanner.resetCurrent(null);

            Collection<DataRow> rows = scanner.next(GlobalConfig.instance().getBatchSizeForRead());
            Collection<ColumnQualifier> columns = scanner.getColumns(0);

            int counter = 1;

            while (!rows.isEmpty() && !canceled) {
                for (DataRow row : rows) {
                    exporter.write(row, columns);

                    writtenRowsCount.setText(Long.toString(counter++));
                    writtenRowsCount.paintImmediately(writtenRowsCount.getBounds());
                }

                rows = scanner.next(GlobalConfig.instance().getBatchSizeForRead());
            }
        }
        finally {
            stream.close();
        }
    }

    private void exportHFile(File file, Scanner scanner) throws IOException {
        Connection connection = scanner.getConnection();

        final int[] counter = {1};

        HbaseActionListener listener = new HbaseActionListener() {
            @Override
            public void copyOperation(String source, String sourceTable, String target, String targetTable, Result result) {
            }

            @Override
            public void saveOperation(String tableName, String path, Result result) {
                writtenRowsCount.setText(Long.toString(counter[0]++));
                writtenRowsCount.paintImmediately(writtenRowsCount.getBounds());
            }

            @Override
            public void loadOperation(String tableName, String path, Put put) {
            }

            @Override
            public void tableOperation(String tableName, String operation) {
            }

            @Override
            public void rowOperation(String tableName, DataRow row, String operation) {
            }

            @Override
            public void columnOperation(String tableName, String column, String operation) {
            }
        };

        connection.addListener(listener);

        try {
            connection.saveTable(scanner.getTableName(), file.getAbsolutePath());
        }
        finally {
            connection.removeListener(listener);
        }
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
        panel3.setLayout(new GridLayoutManager(7, 3, new Insets(0, 0, 0, 0), -1, -1));
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
            6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
            4, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        writtenRowsCount = new JLabel();
        writtenRowsCount.setText("?");
        panel3.add(
            writtenRowsCount, new GridConstraints(
            4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Total rows:");
        panel3.add(
            label3, new GridConstraints(
            5, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        totalRowsCount = new JLabel();
        totalRowsCount.setText("?");
        panel3.add(
            totalRowsCount, new GridConstraints(
            5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        labelDelimiter = new JLabel();
        labelDelimiter.setText("Delimiter:");
        labelDelimiter.setVisible(false);
        panel3.add(
            labelDelimiter, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel3.add(
            separator1, new GridConstraints(
            3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        cmbDelimiter = new JComboBox();
        cmbDelimiter.setEditable(true);
        cmbDelimiter.setEnabled(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement(",");
        defaultComboBoxModel1.addElement("|");
        defaultComboBoxModel1.addElement("-");
        defaultComboBoxModel1.addElement(":");
        cmbDelimiter.setModel(defaultComboBoxModel1);
        cmbDelimiter.setVisible(false);
        panel3.add(
            cmbDelimiter, new GridConstraints(
            2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("File type:");
        panel3.add(
            label4, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        cmbFileType = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("HFile");
        defaultComboBoxModel2.addElement("Delimited");
        cmbFileType.setModel(defaultComboBoxModel2);
        panel3.add(
            cmbFileType, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        contentPane.add(
            separator2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
