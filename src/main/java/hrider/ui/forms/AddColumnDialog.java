package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import hrider.data.ColumnFamily;
import hrider.data.ColumnQualifier;
import hrider.data.ColumnType;
import hrider.ui.design.JCellEditor;
import hrider.ui.design.JTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
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
public class AddColumnDialog extends JDialog {

    //region Variables
    private static final long serialVersionUID = -6724785784849116026L;

    private JPanel               contentPane;
    private JButton              buttonOK;
    private JButton              buttonCancel;
    private JComboBox            comboBox;
    private DefaultComboBoxModel comboBoxModel;
    private JTextField           columnNameTextField;
    private JTable               metadataTable;
    private JButton              addButton;
    private JButton              removeButton;
    private DefaultTableModel    metadataModel;
    private boolean              okPressed;
    private JCellEditor          cellEditor;
    //endregion

    //region Constructor
    public AddColumnDialog(Iterable<ColumnFamily> columnFamilies) {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Create new column");
        getRootPane().setDefaultButton(buttonOK);

        this.cellEditor = new JCellEditor(null, false);

        this.comboBoxModel = new DefaultComboBoxModel();
        this.comboBox.setModel(this.comboBoxModel);

        this.metadataModel = new DefaultTableModel();
        this.metadataTable.setModel(this.metadataModel);

        this.metadataModel.addColumn("Key");
        this.metadataModel.addColumn("Value");
        this.metadataTable.setRowHeight(this.metadataTable.getFont().getSize() + 8);
        this.metadataTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        this.metadataTable.getColumn("Key").setCellEditor(this.cellEditor);
        this.metadataTable.getColumn("Value").setCellEditor(this.cellEditor);

        this.metadataTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    ColumnFamily columnFamily = (ColumnFamily)comboBox.getSelectedItem();
                    removeButton.setEnabled(e.getFirstIndex() >= 0 && columnFamily != null && columnFamily.isEditable());
                }
            });

        this.metadataModel.addTableModelListener(
            new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getType() == TableModelEvent.UPDATE) {
                        ColumnFamily columnFamily = (ColumnFamily)comboBox.getSelectedItem();
                        if (columnFamily != null) {
                            String key = (String)metadataTable.getValueAt(e.getFirstRow(), 0);
                            String val = (String)metadataTable.getValueAt(e.getFirstRow(), 1);

                            if (key != null && !key.isEmpty() && val != null && !val.isEmpty()) {
                                columnFamily.setValue(key, val);
                            }
                        }
                    }
                }
            });

        this.comboBox.getEditor().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object item = comboBox.getEditor().getItem();
                    if (item == null || item instanceof String && ((String)item).isEmpty()) {
                        Object selectedItem = comboBox.getSelectedItem();
                        if (selectedItem != null) {
                            comboBox.removeItem(selectedItem);
                        }
                    }
                    else if (item instanceof String) {
                        ColumnFamily columnFamily = new ColumnFamily((String)item);

                        int index = comboBoxModel.getIndexOf(columnFamily);
                        if (index == -1) {
                            comboBox.addItem(columnFamily);
                        }
                        else {
                            columnFamily = (ColumnFamily)comboBoxModel.getElementAt(index);
                        }

                        comboBox.setSelectedItem(columnFamily);
                    }
                }
            });

        this.comboBox.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    metadataModel.setRowCount(0);

                    Object item = comboBox.getSelectedItem();
                    if (item instanceof ColumnFamily) {
                        ColumnFamily columnFamily = (ColumnFamily)item;

                        cellEditor.setEditable(columnFamily.isEditable());
                        addButton.setEnabled(columnFamily.isEditable());

                        for (Map.Entry<String, String> entry : columnFamily.getMetadata().entrySet()) {
                            metadataModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                        }
                    }
                }
            });

        this.comboBox.getEditor().getEditorComponent().addFocusListener(
            new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    getRootPane().setDefaultButton(null);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    getRootPane().setDefaultButton(buttonOK);
                }
            });

        for (ColumnFamily columnFamily : columnFamilies) {
            this.comboBox.addItem(columnFamily);
        }

        if (this.comboBox.getItemCount() > 0) {
            this.comboBox.setSelectedIndex(0);
        }

        this.buttonOK.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
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

        this.addButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    metadataModel.addRow(new Object[]{"", ""});

                    int row = metadataModel.getRowCount() - 1;

                    metadataTable.setRowSelectionInterval(row, row);
                    metadataTable.scrollRectToVisible(metadataTable.getCellRect(row, 0, false));
                }
            });

        this.removeButton.addActionListener(
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

        ColumnFamily columnFamily = (ColumnFamily)comboBox.getSelectedItem();

        this.addButton.setEnabled(columnFamily != null && columnFamily.isEditable());
        this.removeButton.setEnabled(this.addButton.isEnabled());
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

    public ColumnQualifier getColumn() {
        if (this.okPressed) {
            return new ColumnQualifier(
                this.columnNameTextField.getText(), (ColumnFamily)this.comboBox.getSelectedItem(), ColumnType.BinaryString.getConverter());
        }
        return null;
    }
    //endregion

    //region Private Methods
    private void onOK() {
        JTableModel.stopCellEditing(metadataTable);

        if (this.columnNameTextField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "The column name is required.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else {
            this.okPressed = true;
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
        contentPane.setLayout(new GridLayoutManager(6, 2, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMaximumSize(new Dimension(-1, -1));
        contentPane.setMinimumSize(new Dimension(-1, -1));
        contentPane.setPreferredSize(new Dimension(350, 300));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            5, 0, 1, 2, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(
            panel2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Add");
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
        panel3.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Column Family");
        panel3.add(
            label1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Column Name");
        panel3.add(
            label2, new GridConstraints(
            0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        comboBox = new JComboBox();
        comboBox.setEditable(true);
        comboBox.setFocusCycleRoot(false);
        comboBox.setFocusTraversalPolicyProvider(false);
        comboBox.setRequestFocusEnabled(false);
        panel3.add(
            comboBox, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            null, null, 0, false));
        columnNameTextField = new JTextField();
        panel3.add(
            columnNameTextField, new GridConstraints(
            1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText(":");
        panel3.add(
            label3, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(
            scrollPane1, new GridConstraints(
            3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        metadataTable = new JTable();
        scrollPane1.setViewportView(metadataTable);
        final JSeparator separator1 = new JSeparator();
        contentPane.add(
            separator1, new GridConstraints(
            4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
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
        final JLabel label4 = new JLabel();
        label4.setText("Column metadata");
        contentPane.add(
            label4, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JSeparator separator2 = new JSeparator();
        contentPane.add(
            separator2, new GridConstraints(
            1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
