package hrider.ui.forms;

import hrider.data.DataRow;
import hrider.data.TypedObject;
import hrider.ui.ChangeTracker;
import hrider.ui.design.JCellEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PasteDialog extends JDialog {

    private static final long serialVersionUID = 1459899796815018087L;

    private JPanel                    contentPane;
    private JButton                   buttonOK;
    private JButton                   buttonCancel;
    private JTable                    table;
    private boolean                   okPressed;
    private Map<TypedObject, DataRow> rows;

    public PasteDialog(ChangeTracker changeTracker, Iterable<DataRow> rows) {
        this.rows = new HashMap<TypedObject, DataRow>();

        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Define new keys");
        getRootPane().setDefaultButton(this.buttonOK);

        DefaultTableModel tableModel = new DefaultTableModel();
        this.table.setModel(tableModel);

        tableModel.addColumn("Original Key");
        tableModel.addColumn("New Key");
        this.table.setRowHeight(this.table.getFont().getSize() + 8);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.table.getColumn("Original Key").setCellEditor(new JCellEditor(changeTracker, false));
        this.table.getColumn("New Key").setCellEditor(new JCellEditor(changeTracker, true));

        for (DataRow row : rows) {
            this.rows.put(row.getKey(), row);
            tableModel.addRow(new Object[]{row.getKey(), null});
        }

        this.buttonOK.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopCellEditing(PasteDialog.this.table);

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
    }

    public void showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    public Collection<DataRow> getRows() {
        if (this.okPressed) {
            return this.rows.values();
        }
        return null;
    }

    private void onOK() {
        boolean isValid = true;

        Map<TypedObject, TypedObject> validKeys = new HashMap<TypedObject, TypedObject>();

        for (int i = 0 ; i < this.table.getRowCount() ; i++) {
            String value = (String)this.table.getValueAt(i, 1);
            if (value != null) {
                TypedObject key = (TypedObject)this.table.getValueAt(i, 0);
                try {
                    Object obj = key.getType().toObject(value);
                    validKeys.put(new TypedObject(key.getType(), obj), key);
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        this.contentPane, String.format(
                        "The new key for '%s' is in a wrong format; expected %s type.%sError: %s", key.getValue(), key.getType(), "\n", e.getMessage()), "Error",
                        JOptionPane.ERROR_MESSAGE);

                    isValid = false;
                    break;
                }
            }
        }

        if (isValid) {
            for (Map.Entry<TypedObject, TypedObject> entry : validKeys.entrySet()) {
                DataRow row = this.rows.get(entry.getValue());
                row.setKey(entry.getKey());
            }

            this.okPressed = true;
            dispose();
        }
    }

    private void onCancel() {
        dispose();
    }

    private static void stopCellEditing(JTable table) {
        TableCellEditor editor = table.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }
}
