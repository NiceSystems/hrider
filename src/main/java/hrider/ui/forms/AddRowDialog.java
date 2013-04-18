package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import hrider.data.*;
import hrider.ui.controls.WideComboBox;
import hrider.ui.design.JCellEditor;
import hrider.ui.design.JCheckBoxRenderer;
import hrider.ui.design.JTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

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
public class AddRowDialog extends JDialog {

    //region Variables
    private static final long serialVersionUID = 3548012990232068349L;

    private JPanel            contentPane;
    private JButton           buttonAdd;
    private JButton           buttonCancel;
    private JTable            rowsTable;
    private JButton           buttonAddColumn;
    private DefaultTableModel tableModel;
    private boolean           okPressed;
    //endregion

    //region Constructor
    public AddRowDialog(Iterable<TypedColumn> columns, final Iterable<ColumnFamily> columnFamilies) {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Add new row");
        getRootPane().setDefaultButton(this.buttonAdd);

        this.tableModel = new JTableModel(0, 2, 3);
        this.rowsTable.setModel(this.tableModel);

        this.tableModel.addColumn("Use");
        this.tableModel.addColumn("Column Name");
        this.tableModel.addColumn("Column Type");
        this.tableModel.addColumn("Value");
        this.rowsTable.setRowHeight(this.rowsTable.getFont().getSize() + 8);
        this.rowsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.rowsTable.getColumn("Use").setCellRenderer(new JCheckBoxRenderer(new CheckedRow(1, ColumnQualifier.KEY)));
        this.rowsTable.getColumn("Use").setCellEditor(new JCheckBoxRenderer(new CheckedRow(1, ColumnQualifier.KEY)));
        this.rowsTable.getColumn("Use").setPreferredWidth(20);

        JComboBox comboBox = new WideComboBox();

        for (ColumnType columnType : ColumnType.getTypes()) {
            comboBox.addItem(columnType);
        }

        this.rowsTable.getColumn("Column Type").setCellEditor(new DefaultCellEditor(comboBox));
        this.rowsTable.getColumn("Value").setCellEditor(new JCellEditor(null, 2, true));

        for (TypedColumn typedColumn : columns) {
            this.tableModel.addRow(new Object[]{Boolean.TRUE, typedColumn.getColumn(), typedColumn.getType(), null});
        }

        this.buttonAdd.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JTableModel.stopCellEditing(rowsTable);

                    onOK();
                }
            });

        this.buttonCancel.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    onCancel();
                }
            });

        // call onCancel() on ESCAPE
        this.contentPane.registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        this.buttonAddColumn.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    JTableModel.stopCellEditing(rowsTable);

                    AddColumnDialog dialog = new AddColumnDialog(columnFamilies);
                    if (dialog.showDialog(AddRowDialog.this)) {
                        ColumnQualifier column = dialog.getColumn();

                        int rowIndex = getRowIndex(rowsTable, 1, column);
                        if (rowIndex == -1) {
                            tableModel.addRow(new Object[]{Boolean.TRUE, column, ColumnType.String, null});
                            rowIndex = tableModel.getRowCount() - 1;
                        }

                        rowsTable.setRowSelectionInterval(rowIndex, rowIndex);
                    }
                }
            });
    }
    //endregion

    //region Public Methods
    public boolean showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);

        return this.okPressed;
    }

    public DataRow getRow() {
        if (this.okPressed) {
            DataRow row = new DataRow();
            for (int i = 0 ; i < this.rowsTable.getRowCount() ; i++) {
                boolean use = (Boolean)this.rowsTable.getValueAt(i, 0);
                if (use) {
                    ColumnQualifier columnQualifier = (ColumnQualifier)this.rowsTable.getValueAt(i, 1);
                    ColumnType columnType = (ColumnType)this.rowsTable.getValueAt(i, 2);
                    byte[] value = columnType.toBytes((String)this.rowsTable.getValueAt(i, 3));

                    if (columnQualifier.isKey()) {
                        row.setKey(new ConvertibleObject(columnType, value));
                    }

                    row.addCell(new DataCell(row, columnQualifier, new ConvertibleObject(columnType, value)));
                }
            }
            return row;
        }
        return null;
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

    private void onOK() {

        JTableModel.stopCellEditing(rowsTable);

        String value = null;

        for (int i = 0 ; i < this.rowsTable.getRowCount() ; i++) {
            ColumnQualifier qualifier = (ColumnQualifier)this.rowsTable.getValueAt(i, 1);
            if (qualifier.isKey()) {
                value = (String)this.rowsTable.getValueAt(i, 3);
                break;
            }
        }

        if (value == null || value.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this.contentPane, "The key is required field.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else {
            ColumnQualifier qualifier = null;

            try {
                for (int i = 0 ; i < this.rowsTable.getRowCount() ; i++) {
                    boolean use = (Boolean)this.rowsTable.getValueAt(i, 0);
                    if (use) {
                        value = (String)this.rowsTable.getValueAt(i, 3);
                        qualifier = (ColumnQualifier)this.rowsTable.getValueAt(i, 1);
                        ColumnType valueType = (ColumnType)this.rowsTable.getValueAt(i, 2);

                        valueType.toBytes(value);
                    }
                }

                this.okPressed = true;
                dispose();
            }
            catch (Exception e) {
                JOptionPane.showMessageDialog(
                    this.contentPane, String.format("The value of the column '%s' is in a wrong format.%sError: %s", qualifier, "\n", e.getMessage()), "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
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
        buttonAddColumn = new JButton();
        buttonAddColumn.setText("Add Column...");
        contentPane.add(
            buttonAddColumn, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        contentPane.add(
            separator1, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(
            scrollPane1, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 200), null, 0, false));
        rowsTable = new JTable();
        scrollPane1.setViewportView(rowsTable);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        contentPane.add(
            panel1, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonAdd = new JButton();
        buttonAdd.setText("Add");
        panel1.add(
            buttonAdd, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel1.add(
            buttonCancel, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
