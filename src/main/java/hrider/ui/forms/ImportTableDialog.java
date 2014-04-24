package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import hrider.config.GlobalConfig;
import hrider.converters.TypeConverter;
import hrider.data.*;
import hrider.hbase.Connection;
import hrider.hbase.HbaseActionListener;
import hrider.ui.controls.WideComboBox;
import hrider.ui.design.JTableModel;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
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
    private static final long serialVersionUID = 2869574256289701614L;

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
    private JComboBox         cmbFileType;
    private JPanel            panelDelimited;
    private JLabel            labelDelimiter;
    private DefaultTableModel tableModel;
    private boolean           canceled;
    private TypeConverter     nameConverter;
    //endregion

    //region Constructor
    public ImportTableDialog(
        final Connection connection, final String tableName, TypeConverter nameConverter, Iterable<TypedColumn> columns,
        final Collection<ColumnFamily> columnFamilies) {

        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Import table from file");
        getRootPane().setDefaultButton(this.btImport);

        this.nameConverter = nameConverter;

        if (tableName != null) {
            this.tfTableName.setText(tableName);
            this.tfFilePath.setText(String.format("%s.hfile", tableName));
        }

        this.tableModel = new JTableModel(1);
        this.rowsTable.setModel(this.tableModel);

        this.tableModel.addColumn("Column Name");
        this.tableModel.addColumn("Column Type");
        this.rowsTable.setRowHeight(this.rowsTable.getFont().getSize() + 8);
        this.rowsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        JComboBox comboBox = new WideComboBox();

        for (ColumnType columnType : ColumnType.getTypes()) {
            comboBox.addItem(columnType);
        }

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
                    JTableModel.stopCellEditing(rowsTable);

                    AddColumnDialog dialog = new AddColumnDialog(columnFamilies);
                    if (dialog.showDialog(ImportTableDialog.this)) {
                        ColumnQualifier column = dialog.getColumn();

                        int rowIndex = getRowIndex(rowsTable, 1, column);
                        if (rowIndex == -1) {
                            tableModel.addRow(new Object[]{column, ColumnType.String, null});
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
                    JTableModel.stopCellEditing(rowsTable);

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

        this.cmbFileType.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean visible = "Delimited".equals(cmbFileType.getSelectedItem());

                    cmbDelimiter.setVisible(visible);
                    labelDelimiter.setVisible(visible);
                    panelDelimited.setVisible(visible);

                    if (tableName != null && tfFilePath.getText().startsWith(tableName)) {
                        if (visible) {
                            tfFilePath.setText(String.format("%s.csv", tableName));
                        }
                        else {
                            tfFilePath.setText(String.format("%s.hfile", tableName));
                        }
                    }

                    pack();
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
        JTableModel.stopCellEditing(rowsTable);

        final File file = new File(this.tfFilePath.getText().trim());
        if (!file.exists()) {
            JOptionPane.showMessageDialog(
                this.contentPane, String.format("The specified file '%s' does not exist.", this.tfFilePath.getText()), "Error", JOptionPane.ERROR_MESSAGE);

            return;
        }

        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    tfTableName.setEnabled(false);
                    tfFilePath.setEnabled(false);
                    cmbFileType.setEnabled(false);
                    cmbDelimiter.setEnabled(false);
                    btCancel.setEnabled(true);
                    btClose.setEnabled(false);
                    btBrowse.setEnabled(false);
                    btImport.setEnabled(false);

                    canceled = false;

                    try {
                        String tableName = tfTableName.getText().trim();
                        if (!connection.tableExists(tableName)) {
                            int option = JOptionPane.showConfirmDialog(
                                contentPane, String.format("The specified table '%s' does not exist.\nDo you want to create it?", tableName), "Create table",
                                JOptionPane.YES_NO_OPTION);

                            if (option != JOptionPane.YES_OPTION) {
                                return;
                            }

                            AddTableDialog dialog = new AddTableDialog(new TableDescriptor(tableName));
                            if (!dialog.showDialog(contentPane)) {
                                return;
                            }

                            connection.createOrModifyTable(dialog.getTableDescriptor());
                        }

                        if ("Delimited".equals(cmbFileType.getSelectedItem())) {
                            importDelimitedFile(connection, tableName, file.getAbsolutePath());
                        }
                        else {
                            importHFile(connection, tableName, file.getAbsolutePath());
                        }
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            contentPane, String.format("Failed to import file %s.\nError: %s", tfFilePath.getText(), e.getMessage()), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    finally {
                        tfTableName.setEnabled(true);
                        tfFilePath.setEnabled(true);
                        cmbFileType.setEnabled(true);
                        cmbDelimiter.setEnabled("Delimited".equals(cmbFileType.getSelectedItem()));
                        btCancel.setEnabled(false);
                        btClose.setEnabled(true);
                        btBrowse.setEnabled(true);
                        btImport.setEnabled(true);
                    }
                }
            }).start();
    }

    private void importDelimitedFile(Connection connection, String table, String filePath) throws IOException, FileNotFoundException, TableNotFoundException {

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String header = reader.readLine();
            if (header != null) {
                Pattern p = Pattern.compile(Pattern.quote(Character.toString(getDelimiter())));

                List<String> columns = Arrays.asList(p.split(header));
                if (columns.isEmpty()) {
                    throw new IllegalArgumentException("No columns information found in the file.");
                }

                if (!columns.contains(ColumnQualifier.KEY.getName())) {
                    throw new IllegalArgumentException("Column 'key' is missing in the file.");
                }

                Map<String, ColumnType> columnTypes = getColumnTypes();
                Map<String, ColumnQualifier> columnQualifiers = getColumnQualifiers();

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

                        ColumnType type = columnTypes.get(column);
                        if (type == null) {
                            type = ColumnType.String;
                        }

                        if (ColumnQualifier.isKey(column)) {
                            row.setKey(new ConvertibleObject(type, type.toBytes(value)));
                        }

                        ColumnQualifier qualifier = columnQualifiers.get(column);
                        if (qualifier == null) {
                            qualifier = new ColumnQualifier(column, nameConverter);
                        }

                        row.addCell(new DataCell(row, qualifier, new ConvertibleObject(type, type.toBytes(value))));
                    }

                    if (row.getKey() != null) {
                        rows.add(row);
                    }

                    if (readCount % batch == 0) {
                        connection.setRows(table, rows);

                        writtenCount += rows.size();
                        rows.clear();

                        writtenRowsCount.setText(Long.toString(writtenCount));
                    }

                    line = reader.readLine();
                }

                // Write last rows.
                if (!rows.isEmpty()) {
                    connection.setRows(table, rows);

                    writtenCount += rows.size();
                    rows.clear();

                    writtenRowsCount.setText(Long.toString(writtenCount));
                }
            }
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException ignore) {
                }
            }
        }
    }

    private void importHFile(Connection connection, String table, String filePath) throws IOException, TableNotFoundException {
        final int[] counter = {1};

        HbaseActionListener listener = new HbaseActionListener() {
            @Override
            public void copyOperation(String source, String sourceTable, String target, String targetTable, Result result) {
            }

            @Override
            public void saveOperation(String tableName, String path, Result result) {
            }

            @Override
            public void loadOperation(String tableName, String path, Put put) {
                readRowsCount.setText(Long.toString(counter[0]));
                readRowsCount.paintImmediately(readRowsCount.getBounds());

                writtenRowsCount.setText(Long.toString(counter[0]++));
                writtenRowsCount.paintImmediately(writtenRowsCount.getBounds());
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
            connection.loadTable(table, filePath);
        }
        finally {
            connection.removeListener(listener);
        }
    }

    private Map<String, ColumnType> getColumnTypes() {
        Map<String, ColumnType> columnTypes = new HashMap<String, ColumnType>();
        for (int i = 0 ; i < this.rowsTable.getRowCount() ; i++) {
            ColumnQualifier columnName = (ColumnQualifier)this.rowsTable.getValueAt(i, 0);
            ColumnType columnType = (ColumnType)this.rowsTable.getValueAt(i, 1);

            columnTypes.put(columnName.getFullName(), columnType);
        }
        return columnTypes;
    }

    private Map<String, ColumnQualifier> getColumnQualifiers() {
        Map<String, ColumnQualifier> columnQualifiers = new HashMap<String, ColumnQualifier>();
        for (int i = 0 ; i < this.rowsTable.getRowCount() ; i++) {
            ColumnQualifier qualifier = (ColumnQualifier)this.rowsTable.getValueAt(i, 0);
            columnQualifiers.put(qualifier.getFullName(), qualifier);
        }
        return columnQualifiers;
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
        contentPane.setLayout(new GridLayoutManager(5, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            4, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(
            panel2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
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
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
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
        labelDelimiter = new JLabel();
        labelDelimiter.setText("Delimiter:");
        labelDelimiter.setVisible(false);
        panel3.add(
            labelDelimiter, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
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
            3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Table name:");
        panel3.add(
            label2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        tfTableName = new JTextField();
        panel3.add(
            tfTableName, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, -1), null, 0, false));
        panelDelimited = new JPanel();
        panelDelimited.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panelDelimited.setVisible(false);
        panel3.add(
            panelDelimited, new GridConstraints(
            4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        panelDelimited.add(
            panel4, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Select types for columns:");
        panel4.add(
            label3, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel4.add(
            spacer1, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        btRemoveColumn = new JButton();
        btRemoveColumn.setEnabled(false);
        btRemoveColumn.setText("Remove");
        panel4.add(
            btRemoveColumn, new GridConstraints(
            1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btAddColumn = new JButton();
        btAddColumn.setEnabled(true);
        btAddColumn.setText("Add...");
        panel4.add(
            btAddColumn, new GridConstraints(
            1, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel4.add(
            separator1, new GridConstraints(
            0, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelDelimited.add(
            scrollPane1, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 200), null, 0, false));
        rowsTable = new JTable();
        rowsTable.setEnabled(true);
        scrollPane1.setViewportView(rowsTable);
        final JLabel label4 = new JLabel();
        label4.setFont(new Font(label4.getFont().getName(), Font.BOLD, label4.getFont().getSize()));
        label4.setText("* Note: data for not listed columns will be treated as of String type.");
        panelDelimited.add(
            label4, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
            null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("File type:");
        panel3.add(
            label5, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        cmbFileType = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("HFile");
        defaultComboBoxModel2.addElement("Delimited");
        cmbFileType.setModel(defaultComboBoxModel2);
        panel3.add(
            cmbFileType, new GridConstraints(
            2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        contentPane.add(
            separator2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel5, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Written rows:");
        panel5.add(
            label6, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(
            spacer2, new GridConstraints(
            1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        writtenRowsCount = new JLabel();
        writtenRowsCount.setText("?");
        panel5.add(
            writtenRowsCount, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Read rows:");
        panel5.add(
            label7, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        readRowsCount = new JLabel();
        readRowsCount.setText("?");
        panel5.add(
            readRowsCount, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JSeparator separator3 = new JSeparator();
        contentPane.add(
            separator3, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
