package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import hrider.data.ConvertibleObject;
import hrider.data.DataRow;
import hrider.ui.design.JCellEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
public class PasteDialog extends JDialog {

    //region Variables
    private static final long serialVersionUID = 1459899796815018087L;

    private JPanel                          contentPane;
    private JButton                         buttonOK;
    private JButton                         buttonCancel;
    private JTable                          table;
    private boolean                         okPressed;
    private Map<ConvertibleObject, DataRow> rows;
    //endregion

    //region Constructor
    public PasteDialog(Iterable<DataRow> rows) {
        this.rows = new HashMap<ConvertibleObject, DataRow>();

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
        this.table.getColumn("Original Key").setCellEditor(new JCellEditor(null, false));
        this.table.getColumn("New Key").setCellEditor(new JCellEditor(null, true));

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
    //endregion

    //region Public Methods
    public boolean showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);

        return this.okPressed;
    }

    public Collection<DataRow> getRows() {
        if (this.okPressed) {
            return this.rows.values();
        }
        return null;
    }
    //endregion

    //region Private Methods
    private void onOK() {
        boolean isValid = true;

        Map<ConvertibleObject, ConvertibleObject> validKeys = new HashMap<ConvertibleObject, ConvertibleObject>();

        for (int i = 0 ; i < this.table.getRowCount() ; i++) {
            String value = (String)this.table.getValueAt(i, 1);
            if (value != null) {
                ConvertibleObject key = (ConvertibleObject)this.table.getValueAt(i, 0);
                try {
                    byte[] bytes = key.getType().toBytes(value);
                    validKeys.put(new ConvertibleObject(key.getType(), bytes), key);
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        this.contentPane, String.format(
                        "The new key for '%s' is in a wrong format; expected %s type.%sError: %s", key.getValueAsString(), key.getType(), "\n", e.getMessage()),
                        "Error", JOptionPane.ERROR_MESSAGE);

                    isValid = false;
                    break;
                }
            }
        }

        if (isValid) {
            for (Map.Entry<ConvertibleObject, ConvertibleObject> entry : validKeys.entrySet()) {
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
        if (table.getRowCount() > 0) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(
            panel2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Paste");
        panel2.add(
            buttonOK, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(
            buttonCancel, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 5, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(10);
        label1.setHorizontalTextPosition(10);
        label1.setText("Define new keys for pasted rows if necessary. If no new key is provided the");
        panel3.add(
            label1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
            null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(
            scrollPane1, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        table = new JTable();
        table.setPreferredScrollableViewportSize(new Dimension(300, 150));
        scrollPane1.setViewportView(table);
        final JLabel label2 = new JLabel();
        label2.setText("original will be used and if there is a row with the same key it will be updated.");
        panel3.add(
            label2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JSeparator separator1 = new JSeparator();
        contentPane.add(
            separator1, new GridConstraints(
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
