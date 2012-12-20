package hrider.ui.forms;

import hrider.config.Configurator;
import hrider.data.*;
import hrider.hbase.HbaseHelper;
import hrider.ui.design.JCellEditor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
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
    private JPanel            contentPane;
    private JButton btImport;
    private JButton btClose;
    private JTextField        tfFilePath;
    private JButton           btBrowse;
    private JComboBox         cmbDelimiter;
    private JTable            rowsTable;
    private JTextField        tfTableName;
    private JButton btAddColumn;
    private JButton btRemoveColumn;
    private JButton btCancel;
    private JLabel writtenRowsCount;
    private JLabel readRowsCount;
    private DefaultTableModel tableModel;
    private boolean           canceled;
    //endregion

    //region Constructor
    public ImportTableDialog(final HbaseHelper hbaseHelper, String tableName, Iterable<TypedColumn> columns, final Iterable<String> columnFamilies) {
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
                    ImportTableDialog.this.btRemoveColumn.setEnabled(e.getFirstIndex() != -1);
                }
            });

        this.btImport.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (validateInput()) {
                        onImport(hbaseHelper);
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
                    dialog.setSelectedFile(new File(ImportTableDialog.this.tfFilePath.getText()));

                    int returnVal = dialog.showSaveDialog(ImportTableDialog.this.contentPane);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        ImportTableDialog.this.tfFilePath.setText(dialog.getSelectedFile().getAbsolutePath());
                    }
                }
            });

        this.btAddColumn.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopCellEditing(ImportTableDialog.this.rowsTable);

                    AddColumnDialog dialog = new AddColumnDialog(columnFamilies);
                    dialog.showDialog(ImportTableDialog.this);

                    String columnName = dialog.getColumnName();
                    if (columnName != null) {
                        int rowIndex = getRowIndex(ImportTableDialog.this.rowsTable, 1, columnName);
                        if (rowIndex == -1) {
                            ImportTableDialog.this.tableModel.addRow(new Object[]{columnName, ObjectType.String, null});
                            rowIndex = ImportTableDialog.this.tableModel.getRowCount() - 1;
                        }

                        ImportTableDialog.this.rowsTable.setRowSelectionInterval(rowIndex, rowIndex);
                    }
                }
            });

        this.btRemoveColumn.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedRow = ImportTableDialog.this.rowsTable.getSelectedRow();

                    while (selectedRow != -1) {
                        ImportTableDialog.this.tableModel.removeRow(selectedRow);
                        selectedRow = ImportTableDialog.this.rowsTable.getSelectedRow();
                    }

                    ImportTableDialog.this.btRemoveColumn.setEnabled(false);
                }
            });

        this.btCancel.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ImportTableDialog.this.canceled = true;
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

    private void onImport(final HbaseHelper hbaseHelper) {
        new Thread(
            new Runnable() {
                @Override
                public void run() {

                    ImportTableDialog.this.tfTableName.setEnabled(false);
                    ImportTableDialog.this.tfFilePath.setEnabled(false);
                    ImportTableDialog.this.cmbDelimiter.setEnabled(false);
                    ImportTableDialog.this.btCancel.setEnabled(true);
                    ImportTableDialog.this.btClose.setEnabled(false);
                    ImportTableDialog.this.btBrowse.setEnabled(false);
                    ImportTableDialog.this.btImport.setEnabled(false);

                    ImportTableDialog.this.canceled = false;

                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new FileReader(ImportTableDialog.this.tfFilePath.getText()));
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

                            String tableName = ImportTableDialog.this.tfTableName.getText().trim();
                            hbaseHelper.createTable(tableName);

                            Map<String, ObjectType> columnTypes = getColumnTypes();

                            long readCount = 1;
                            long writtenCount = 1;
                            int batch = Configurator.getBatchSizeForWrite();

                            Collection<DataRow> rows = new ArrayList<DataRow>();

                            String line = reader.readLine();
                            while (line != null && !ImportTableDialog.this.canceled) {

                                ImportTableDialog.this.readRowsCount.setText(Long.toString(readCount++));
                                DataRow row = new DataRow();

                                String[] values = p.split(line);
                                for (int i = 0; i < values.length && i < columns.size(); i++) {
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
                                    hbaseHelper.setRows(tableName, rows);

                                    writtenCount += rows.size();
                                    rows.clear();

                                    ImportTableDialog.this.writtenRowsCount.setText(Long.toString(writtenCount));
                                }

                                line = reader.readLine();
                            }

                            // Write last rows.
                            if (!rows.isEmpty()) {
                                hbaseHelper.setRows(tableName, rows);

                                writtenCount += rows.size();
                                rows.clear();

                                ImportTableDialog.this.writtenRowsCount.setText(Long.toString(writtenCount));
                            }
                        }
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            ImportTableDialog.this.contentPane,
                            String.format("Failed to import file %s.\nError: %s", ImportTableDialog.this.tfFilePath.getText(), e.getMessage()), "Error",
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

                        ImportTableDialog.this.tfTableName.setEnabled(true);
                        ImportTableDialog.this.tfFilePath.setEnabled(true);
                        ImportTableDialog.this.cmbDelimiter.setEnabled(true);
                        ImportTableDialog.this.btCancel.setEnabled(false);
                        ImportTableDialog.this.btClose.setEnabled(true);
                        ImportTableDialog.this.btBrowse.setEnabled(true);
                        ImportTableDialog.this.btImport.setEnabled(true);
                    }
                }
            }).start();
    }

    private Map<String, ObjectType> getColumnTypes() {
        Map<String, ObjectType> columnTypes = new HashMap<String, ObjectType>();
        for (int i = 0; i < this.rowsTable.getRowCount(); i++) {
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
}
