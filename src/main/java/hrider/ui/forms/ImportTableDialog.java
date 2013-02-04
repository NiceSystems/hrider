package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import hrider.config.GlobalConfig;
import hrider.data.*;
import hrider.hbase.Connection;
import hrider.ui.design.JCellEditor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
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
public class ImportTableDialog extends JDialog {

    //region Variables
    private JPanel            contentPane;
    private JButton           btImport;
    private JButton           btClose;
    private JTextField        tfFilePath;
    private JButton           btBrowse;
    private JComboBox         cmbDelimiter;
    private JTable            rowsTable;
    private JTextField        tfTableName;
    private JButton           btAddColumn;
    private JButton           btRemoveColumn;
    private JButton           btCancel;
    private JLabel            writtenRowsCount;
    private JLabel            readRowsCount;
    private DefaultTableModel tableModel;
    private boolean           canceled;
    //endregion

    //region Constructor
    public ImportTableDialog(final Connection connection, String tableName, Iterable<TypedColumn> columns, final Iterable<String> columnFamilies) {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Import table from file");
        getRootPane().setDefaultButton(this.btImport);

        this.tfTableName.setText(tableName);
        this.tfFilePath.setText(String.format("%s.csv", tableName));

        this.tableModel = new DefaultTableModel();
        this.rowsTable.setModel(this.tableModel);

        this.tableModel.addColumn("Column Name");
        this.tableModel.addColumn("Column Type");
        this.rowsTable.setRowHeight(this.rowsTable.getFont().getSize() + 8);
        this.rowsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        JComboBox comboBox = new JComboBox();

        for (ObjectType objectType : ObjectType.values()) {
            comboBox.addItem(objectType);
        }

        this.rowsTable.getColumn("Column Name").setCellEditor(new JCellEditor(null, false));
        this.rowsTable.getColumn("Column Type").setCellEditor(new DefaultCellEditor(comboBox));

        for (TypedColumn typedColumn : columns) {
            this.tableModel.addRow(new Object[]{typedColumn.getColumn(), typedColumn.getType(), null});
        }

        this.rowsTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    btRemoveColumn.setEnabled(e.getFirstIndex() != -1);
                }
            });

        this.btImport.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (validateInput()) {
                        onImport(connection);
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

        this.btAddColumn.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopCellEditing(rowsTable);

                    AddColumnDialog dialog = new AddColumnDialog(columnFamilies);
                    dialog.showDialog(ImportTableDialog.this);

                    String columnName = dialog.getColumnName();
                    if (columnName != null) {
                        int rowIndex = getRowIndex(rowsTable, 1, columnName);
                        if (rowIndex == -1) {
                            tableModel.addRow(new Object[]{columnName, ObjectType.String, null});
                            rowIndex = tableModel.getRowCount() - 1;
                        }

                        rowsTable.setRowSelectionInterval(rowIndex, rowIndex);
                    }
                }
            });

        this.btRemoveColumn.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedRow = rowsTable.getSelectedRow();

                    while (selectedRow != -1) {
                        tableModel.removeRow(selectedRow);
                        selectedRow = rowsTable.getSelectedRow();
                    }

                    btRemoveColumn.setEnabled(false);
                }
            });

        this.btCancel.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canceled = true;
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
    private static void stopCellEditing(JTable table) {
        if (table.getRowCount() > 0) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
    }

    private static int getRowIndex(JTable table, int columnIndex, Object expectedValue) {
        for (int i = 0 ; i < table.getRowCount() ; i++) {
            Object value = table.getValueAt(i, columnIndex);
            if (value != null && value.equals(expectedValue)) {
                return i;
            }
        }
        return -1;
    }

    private void onImport(final Connection connection) {
        new Thread(
            new Runnable() {
                @Override
                public void run() {

                    tfTableName.setEnabled(false);
                    tfFilePath.setEnabled(false);
                    cmbDelimiter.setEnabled(false);
                    btCancel.setEnabled(true);
                    btClose.setEnabled(false);
                    btBrowse.setEnabled(false);
                    btImport.setEnabled(false);

                    canceled = false;

                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new FileReader(tfFilePath.getText()));
                        String header = reader.readLine();
                        if (header != null) {
                            Pattern p = Pattern.compile(Pattern.quote(Character.toString(getDelimiter())));

                            List<String> columns = Arrays.asList(p.split(header));
                            if (columns.isEmpty()) {
                                throw new IllegalArgumentException("No columns information found in the file.");
                            }

                            if (!columns.contains("key")) {
                                throw new IllegalArgumentException("Column 'key' is missing in the file.");
                            }

                            String tableName = tfTableName.getText().trim();
                            connection.createTable(tableName);

                            Map<String, ObjectType> columnTypes = getColumnTypes();

                            long readCount = 1;
                            long writtenCount = 0;
                            int batch = GlobalConfig.instance().getBatchSizeForWrite();

                            Collection<DataRow> rows = new ArrayList<DataRow>();

                            String line = reader.readLine();
                            while (line != null && !canceled) {

                                readRowsCount.setText(Long.toString(readCount++));
                                DataRow row = new DataRow();

                                String[] values = p.split(line);
                                for (int i = 0 ; i < values.length && i < columns.size() ; i++) {
                                    String column = columns.get(i).trim();
                                    String value = values[i].trim();

                                    ObjectType type = columnTypes.get(column);
                                    if (type == null) {
                                        type = ObjectType.String;
                                    }

                                    row.addCell(new DataCell(row, column, new TypedObject(type, type.fromString(value))));

                                    if ("key".equalsIgnoreCase(column)) {
                                        row.setKey(new TypedObject(type, type.fromString(value)));
                                    }
                                }

                                if (row.getKey() != null) {
                                    rows.add(row);
                                }

                                if (readCount % batch == 0) {
                                    connection.setRows(tableName, rows);

                                    writtenCount += rows.size();
                                    rows.clear();

                                    writtenRowsCount.setText(Long.toString(writtenCount));
                                }

                                line = reader.readLine();
                            }

                            // Write last rows.
                            if (!rows.isEmpty()) {
                                connection.setRows(tableName, rows);

                                writtenCount += rows.size();
                                rows.clear();

                                writtenRowsCount.setText(Long.toString(writtenCount));
                            }
                        }
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            contentPane, String.format("Failed to import file %s.\nError: %s", tfFilePath.getText(), e.getMessage()), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            }
                            catch (IOException ignore) {
                            }
                        }

                        tfTableName.setEnabled(true);
                        tfFilePath.setEnabled(true);
                        cmbDelimiter.setEnabled(true);
                        btCancel.setEnabled(false);
                        btClose.setEnabled(true);
                        btBrowse.setEnabled(true);
                        btImport.setEnabled(true);
                    }
                }
            }).start();
    }

    private Map<String, ObjectType> getColumnTypes() {
        Map<String, ObjectType> columnTypes = new HashMap<String, ObjectType>();
        for (int i = 0 ; i < this.rowsTable.getRowCount() ; i++) {
            String columnName = (String)this.rowsTable.getValueAt(i, 0);
            ObjectType columnType = (ObjectType)this.rowsTable.getValueAt(i, 1);

            columnTypes.put(columnName, columnType);
        }
        return columnTypes;
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

        if (!new File(path).exists()) {
            JOptionPane.showMessageDialog(this.contentPane, "The specified file does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
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
        contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(
            panel2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btImport = new JButton();
        btImport.setText("Import");
        panel2.add(
            btImport, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btCancel = new JButton();
        btCancel.setEnabled(false);
        btCancel.setText("Cancel");
        panel2.add(
            btCancel, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btClose = new JButton();
        btClose.setText("Close");
        panel2.add(
            btClose, new GridConstraints(
            0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel1.add(
            separator1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("File path:");
        panel3.add(
            label1, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        tfFilePath = new JTextField();
        panel3.add(
            tfFilePath, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, -1), null, 0, false));
        btBrowse = new JButton();
        btBrowse.setText("Browse");
        panel3.add(
            btBrowse, new GridConstraints(
            1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Delimiter:");
        panel3.add(
            label2, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        cmbDelimiter = new JComboBox();
        cmbDelimiter.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("|");
        defaultComboBoxModel1.addElement(",");
        defaultComboBoxModel1.addElement("-");
        defaultComboBoxModel1.addElement(":");
        cmbDelimiter.setModel(defaultComboBoxModel1);
        panel3.add(
            cmbDelimiter, new GridConstraints(
            2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Table name:");
        panel3.add(
            label3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        tfTableName = new JTextField();
        panel3.add(
            tfTableName, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(
            panel4, new GridConstraints(
            3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(
            panel5, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Select types for columns:");
        panel5.add(
            label4, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel5.add(
            spacer1, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        btRemoveColumn = new JButton();
        btRemoveColumn.setEnabled(false);
        btRemoveColumn.setText("Remove");
        panel5.add(
            btRemoveColumn, new GridConstraints(
            1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btAddColumn = new JButton();
        btAddColumn.setText("Add...");
        panel5.add(
            btAddColumn, new GridConstraints(
            1, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel5.add(
            separator2, new GridConstraints(
            0, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel4.add(
            scrollPane1, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 200), null, 0, false));
        rowsTable = new JTable();
        scrollPane1.setViewportView(rowsTable);
        final JLabel label5 = new JLabel();
        label5.setFont(new Font(label5.getFont().getName(), Font.BOLD, label5.getFont().getSize()));
        label5.setText("* Note: data for not listed columns will be treated as of String type.");
        panel4.add(
            label5, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
            null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        contentPane.add(
            separator3, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel6, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Written rows:");
        panel6.add(
            label6, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(
            spacer2, new GridConstraints(
            1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        writtenRowsCount = new JLabel();
        writtenRowsCount.setText("?");
        panel6.add(
            writtenRowsCount, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Read rows:");
        panel6.add(
            label7, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        readRowsCount = new JLabel();
        readRowsCount.setText("?");
        panel6.add(
            readRowsCount, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
