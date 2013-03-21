package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import hrider.data.ColumnFamily;
import hrider.ui.design.JCellEditor;
import hrider.ui.design.JTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
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
public class AddFamilyDialog extends JDialog {

    //region Variables
    private static final long serialVersionUID = 5050481736896721779L;

    private JPanel            contentPane;
    private JButton           buttonOK;
    private JButton           buttonCancel;
    private JTextField        columnFamilyTextField;
    private JButton           addButton;
    private JButton           removeButton;
    private DefaultTableModel metadataModel;
    private JTable            metadataTable;
    private boolean           okPressed;
    private ColumnFamily      columnFamily;
    //endregion

    //region Constructor
    public AddFamilyDialog(ColumnFamily family) {
        setContentPane(contentPane);
        setModal(true);
        setTitle(String.format("%s column family", family == null ? "Create new" : "Update"));
        getRootPane().setDefaultButton(buttonOK);

        this.columnFamily = family;
        if (this.columnFamily == null) {
            this.columnFamily = new ColumnFamily("CF");
        }

        columnFamilyTextField.setText(this.columnFamily.getName());

        this.metadataModel = new DefaultTableModel();
        this.metadataTable.setModel(this.metadataModel);

        this.metadataModel.addColumn("Key");
        this.metadataModel.addColumn("Value");
        this.metadataTable.setRowHeight(this.metadataTable.getFont().getSize() + 8);
        this.metadataTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        this.metadataTable.getColumn("Key").setCellEditor(new JCellEditor(null, true));
        this.metadataTable.getColumn("Value").setCellEditor(new JCellEditor(null, true));

        this.metadataTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    removeButton.setEnabled(true);
                }
            });

        for (Map.Entry<String, String> entry : this.columnFamily.getMetadata().entrySet()) {
            metadataModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        buttonOK.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onOK();
                }
            });

        buttonCancel.addActionListener(
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
        contentPane.registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        addButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    metadataModel.addRow(new Object[]{"", ""});

                    int row = metadataModel.getRowCount() - 1;

                    metadataTable.setRowSelectionInterval(row, row);
                    metadataTable.scrollRectToVisible(metadataTable.getCellRect(row, 0, false));
                }
            });

        removeButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (metadataTable.getRowCount() > 0) {
                        JTableModel.stopCellEditing(metadataTable);

                        int[] selectedRows = metadataTable.getSelectedRows();
                        for (int selectedRow : selectedRows) {
                            metadataModel.removeRow(selectedRow);
                        }
                    }
                }
            });
    }
    //endregion

    //region Public Methods
    public boolean showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(owner);
        this.setVisible(true);

        return this.okPressed;
    }

    public ColumnFamily getColumnFamily() {
        if (this.okPressed) {
            return columnFamily;
        }
        return null;
    }
    //endregion

    //region Private Methods
    private void onOK() {
        JTableModel.stopCellEditing(metadataTable);

        if (this.columnFamilyTextField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "The column family is required.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else {
            this.okPressed = true;

            for (int i = 0 ; i < metadataTable.getRowCount() ; i++) {
                String key = (String)metadataTable.getValueAt(i, 0);
                String val = (String)metadataTable.getValueAt(i, 1);

                if (key != null && !key.isEmpty() && val != null && !val.isEmpty()) {
                    columnFamily.setValue(key, val);
                }
            }

            columnFamily.setName(columnFamilyTextField.getText().trim());

            dispose();
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
        contentPane.setLayout(new GridLayoutManager(5, 2, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMaximumSize(new Dimension(-1, -1));
        contentPane.setMinimumSize(new Dimension(-1, -1));
        contentPane.setPreferredSize(new Dimension(350, 300));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(
            panel2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
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
        final JSeparator separator1 = new JSeparator();
        panel1.add(
            separator1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Column metadata");
        contentPane.add(
            label1, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(76, 14), null, 0, false));
        final JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        contentPane.add(
            toolBar1, new GridConstraints(
            2, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        addButton = new JButton();
        addButton.setIcon(new ImageIcon(getClass().getResource("/images/add.png")));
        addButton.setMaximumSize(new Dimension(27, 27));
        addButton.setMinimumSize(new Dimension(27, 27));
        addButton.setPreferredSize(new Dimension(27, 27));
        addButton.setText("");
        addButton.setToolTipText("Add metadata attribute");
        toolBar1.add(addButton);
        removeButton = new JButton();
        removeButton.setEnabled(false);
        removeButton.setHorizontalAlignment(0);
        removeButton.setIcon(new ImageIcon(getClass().getResource("/images/delete.png")));
        removeButton.setMaximumSize(new Dimension(27, 27));
        removeButton.setMinimumSize(new Dimension(27, 27));
        removeButton.setPreferredSize(new Dimension(27, 27));
        removeButton.setText("");
        removeButton.setToolTipText("Remove metadata attribute");
        toolBar1.add(removeButton);
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(
            scrollPane1, new GridConstraints(
            3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        metadataTable = new JTable();
        scrollPane1.setViewportView(metadataTable);
        final JSeparator separator2 = new JSeparator();
        contentPane.add(
            separator2, new GridConstraints(
            1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Column Family:");
        panel3.add(
            label2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        columnFamilyTextField = new JTextField();
        panel3.add(
            columnFamilyTextField, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(150, 24), null, 0, false));
        label2.setLabelFor(columnFamilyTextField);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
