package hrider.ui.forms;

import hrider.data.*;
import hrider.ui.ChangeTracker;
import hrider.ui.design.JCellEditor;
import hrider.ui.design.JCheckBoxRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;

public class AddRowDialog extends JDialog {

    private static final long serialVersionUID = 3548012990232068349L;
    private JPanel            contentPane;
    private JButton           buttonAdd;
    private JButton           buttonCancel;
    private JTable            rowsTable;
    private JButton           buttonAddColumn;
    private DefaultTableModel tableModel;
    private boolean           okPressed;

    public AddRowDialog(ChangeTracker changeTracker, Iterable<TypedColumn> columns, final Iterable<String> columnFamilies) {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Add new row");
        getRootPane().setDefaultButton(this.buttonAdd);

        this.tableModel = new DefaultTableModel();
        this.rowsTable.setModel(this.tableModel);

        this.tableModel.addColumn("Use");
        this.tableModel.addColumn("Column Name");
        this.tableModel.addColumn("Column Type");
        this.tableModel.addColumn("Value");
        this.rowsTable.setRowHeight(this.rowsTable.getFont().getSize() + 8);
        this.rowsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.rowsTable.getColumn("Use").setCellRenderer(new JCheckBoxRenderer(new RequiredRow(1, "key")));
        this.rowsTable.getColumn("Use").setCellEditor(new JCheckBoxRenderer(new RequiredRow(1, "key")));
        this.rowsTable.getColumn("Use").setPreferredWidth(20);

        JComboBox comboBox = new JComboBox();

        for (ObjectType objectType : ObjectType.values()) {
            comboBox.addItem(objectType);
        }

        this.rowsTable.getColumn("Column Name").setCellEditor(new JCellEditor(changeTracker, false));
        this.rowsTable.getColumn("Column Type").setCellEditor(new DefaultCellEditor(comboBox));
        this.rowsTable.getColumn("Value").setCellEditor(new JCellEditor(changeTracker, 2, true));

        for (TypedColumn typedColumn : columns) {
            this.tableModel.addRow(new Object[]{Boolean.TRUE, typedColumn.getColumn(), typedColumn.getType(), null});
        }

        this.buttonAdd.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopCellEditing(AddRowDialog.this.rowsTable);

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

                    stopCellEditing(AddRowDialog.this.rowsTable);

                    AddColumnDialog dialog = new AddColumnDialog(columnFamilies);
                    dialog.showDialog(AddRowDialog.this);

                    String columnName = dialog.getColumnName();
                    if (columnName != null) {
                        int rowIndex = getRowIndex(AddRowDialog.this.rowsTable, 1, columnName);
                        if (rowIndex == -1) {
                            AddRowDialog.this.tableModel.addRow(new Object[]{Boolean.TRUE, columnName, ObjectType.String, null});
                            rowIndex = AddRowDialog.this.tableModel.getRowCount() - 1;
                        }

                        AddRowDialog.this.rowsTable.setRowSelectionInterval(rowIndex, rowIndex);
                    }
                }
            });
    }

    public void showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    public DataRow getRow() {
        if (this.okPressed) {
            DataRow row = new DataRow();
            for (int i = 0 ; i < this.rowsTable.getRowCount() ; i++) {
                boolean use = (Boolean)this.rowsTable.getValueAt(i, 0);
                if (use) {
                    String columnName = (String)this.rowsTable.getValueAt(i, 1);
                    ObjectType columnType = (ObjectType)this.rowsTable.getValueAt(i, 2);
                    Object value = columnType.fromString((String)this.rowsTable.getValueAt(i, 3));

                    if ("key".equals(columnName)) {
                        row.setKey(new TypedObject(columnType, value));
                    }

                    row.addCell(new DataCell(row, columnName, new TypedObject(columnType, value)));
                }
            }
            return row;
        }
        return null;
    }

    private static void stopCellEditing(JTable table) {
        TableCellEditor editor = table.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
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

    private void onOK() {

        String value = null;

        for (int i = 0 ; i < this.rowsTable.getRowCount() ; i++) {
            String columnName = (String)this.rowsTable.getValueAt(i, 1);
            if ("key".equals(columnName)) {
                value = (String)this.rowsTable.getValueAt(i, 3);
                break;
            }
        }

        if (value == null || value.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this.contentPane, "The key is required field.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else {
            String columnName = null;

            try {
                for (int i = 0 ; i < this.rowsTable.getRowCount() ; i++) {
                    boolean use = (Boolean)this.rowsTable.getValueAt(i, 0);
                    if (use) {
                        value = (String)this.rowsTable.getValueAt(i, 3);
                        columnName = (String)this.rowsTable.getValueAt(i, 1);
                        ObjectType valueType = (ObjectType)this.rowsTable.getValueAt(i, 2);

                        valueType.toObject(value);
                    }
                }

                this.okPressed = true;
                dispose();
            }
            catch (Exception e) {
                JOptionPane.showMessageDialog(
                    this.contentPane, String.format("The value of the column '%s' is in a wrong format.%sError: %s", columnName, "\n", e.getMessage()), "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
