package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import hrider.data.ColumnFamily;
import hrider.data.TableDescriptor;
import hrider.ui.design.JCellEditor;
import hrider.ui.design.JTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
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
public class UpdateTableMetadata extends JDialog {

    //region Variables
    private static final long serialVersionUID = -2969175353751989403L;

    private JPanel               contentPane;
    private JButton              buttonUpdate;
    private JButton              buttonCancel;
    private JTable               tableMetadata;
    private DefaultTableModel    tableModel;
    private JComboBox            cmbFamilies;
    private DefaultComboBoxModel cmbFamiliesModel;
    private JTable               columnMetadata;
    private JButton              addTableMetadataButton;
    private JButton              removeTableMetadataButton;
    private JButton              addColumnMetadataButton;
    private JButton              removeColumnMetadataButton;
    private DefaultTableModel    columnModel;
    private boolean              okPressed;
    private boolean              isEditable;
    //endregion

    //region Constructor
    public UpdateTableMetadata(final TableDescriptor descriptor, boolean canEdit) {
        setContentPane(contentPane);
        setModal(true);
        setTitle(descriptor.getName() + " table metadata");
        getRootPane().setDefaultButton(buttonUpdate);

        TableCellEditor cellEditor = new JCellEditor(null, canEdit);

        this.isEditable = canEdit;
        this.buttonUpdate.setEnabled(canEdit);
        this.addTableMetadataButton.setEnabled(canEdit);
        this.addColumnMetadataButton.setEnabled(canEdit);
        this.cmbFamilies.setEditable(canEdit);

        this.tableModel = new DefaultTableModel();
        this.tableMetadata.setModel(this.tableModel);

        this.tableModel.addColumn("Key");
        this.tableModel.addColumn("Value");
        this.tableMetadata.setRowHeight(this.tableMetadata.getFont().getSize() + 8);
        this.tableMetadata.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        this.tableMetadata.getColumn("Key").setCellEditor(cellEditor);
        this.tableMetadata.getColumn("Value").setCellEditor(cellEditor);

        this.tableMetadata.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    removeTableMetadataButton.setEnabled(e.getFirstIndex() >= 0 && isEditable);
                }
            });

        this.tableModel.addTableModelListener(
            new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getType() == TableModelEvent.UPDATE) {
                        String key = (String)tableMetadata.getValueAt(e.getFirstRow(), 0);
                        String val = (String)tableMetadata.getValueAt(e.getFirstRow(), 1);

                        if (key != null && !key.isEmpty() && val != null && !val.isEmpty()) {
                            descriptor.setValue(key, val);
                        }
                    }
                }
            });

        for (Map.Entry<String, String> entry : descriptor.getMetadata().entrySet()) {
            this.tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        this.columnModel = new DefaultTableModel();
        this.columnMetadata.setModel(this.columnModel);

        this.columnModel.addColumn("Key");
        this.columnModel.addColumn("Value");
        this.columnMetadata.setRowHeight(this.columnMetadata.getFont().getSize() + 8);
        this.columnMetadata.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        this.columnMetadata.getColumn("Key").setCellEditor(cellEditor);
        this.columnMetadata.getColumn("Value").setCellEditor(cellEditor);

        this.columnMetadata.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    removeColumnMetadataButton.setEnabled(e.getFirstIndex() >= 0 && isEditable);
                }
            });

        this.columnModel.addTableModelListener(
            new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getType() == TableModelEvent.UPDATE) {
                        ColumnFamily columnFamily = (ColumnFamily)cmbFamilies.getSelectedItem();
                        if (columnFamily != null) {
                            String key = (String)columnMetadata.getValueAt(e.getFirstRow(), 0);
                            String val = (String)columnMetadata.getValueAt(e.getFirstRow(), 1);

                            if (key != null && !key.isEmpty() && val != null && !val.isEmpty()) {
                                columnFamily.setValue(key, val);
                            }
                        }
                    }
                }
            });

        this.cmbFamiliesModel = new DefaultComboBoxModel();
        this.cmbFamilies.setModel(this.cmbFamiliesModel);

        this.cmbFamilies.getEditor().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object item = cmbFamilies.getEditor().getItem();
                    if (item == null || item instanceof String && ((String)item).isEmpty()) {
                        Object selectedItem = cmbFamilies.getSelectedItem();
                        if (selectedItem != null) {
                            cmbFamilies.removeItem(selectedItem);
                        }
                    }
                    else if (item instanceof String) {
                        ColumnFamily columnFamily = new ColumnFamily((String)item);

                        int index = cmbFamiliesModel.getIndexOf(columnFamily);
                        if (index == -1) {
                            cmbFamilies.addItem(columnFamily);
                        }
                        else {
                            columnFamily = (ColumnFamily)cmbFamiliesModel.getElementAt(index);
                        }

                        cmbFamilies.setSelectedItem(columnFamily);
                    }
                }
            });

        this.cmbFamilies.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    columnModel.setRowCount(0);

                    Object item = cmbFamilies.getSelectedItem();
                    if (item instanceof ColumnFamily) {
                        ColumnFamily columnFamily = (ColumnFamily)item;

                        for (Map.Entry<String, String> entry : columnFamily.getMetadata().entrySet()) {
                            columnModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                        }
                    }
                }
            });

        this.cmbFamilies.getEditor().getEditorComponent().addFocusListener(
            new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    getRootPane().setDefaultButton(null);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    getRootPane().setDefaultButton(buttonUpdate);
                }
            });

        for (ColumnFamily columnFamily : descriptor.families()) {
            this.cmbFamilies.addItem(columnFamily);
        }

        if (this.cmbFamilies.getItemCount() > 0) {
            this.cmbFamilies.setSelectedIndex(0);
        }

        buttonUpdate.addActionListener(
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

        addTableMetadataButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tableModel.addRow(new Object[]{"", ""});

                    int row = tableModel.getRowCount() - 1;

                    tableMetadata.setRowSelectionInterval(row, row);
                    tableMetadata.scrollRectToVisible(tableMetadata.getCellRect(row, 0, false));
                }
            });

        removeTableMetadataButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (tableMetadata.getRowCount() > 0) {
                        JTableModel.stopCellEditing(tableMetadata);

                        int[] selectedRows = tableMetadata.getSelectedRows();
                        for (int selectedRow : selectedRows) {
                            tableModel.removeRow(selectedRow);
                        }
                    }
                }
            });

        addColumnMetadataButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    columnModel.addRow(new Object[]{"", ""});

                    int row = columnModel.getRowCount() - 1;

                    columnMetadata.setRowSelectionInterval(row, row);
                    columnMetadata.scrollRectToVisible(columnMetadata.getCellRect(row, 0, false));
                }
            });

        removeColumnMetadataButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (columnMetadata.getRowCount() > 0) {
                        JTableModel.stopCellEditing(columnMetadata);

                        int[] selectedRows = columnMetadata.getSelectedRows();
                        for (int selectedRow : selectedRows) {
                            columnModel.removeRow(selectedRow);
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
    //endregion

    //region Private Methods
    private void onOK() {
        JTableModel.stopCellEditing(tableMetadata);
        JTableModel.stopCellEditing(columnMetadata);

        this.okPressed = true;
        dispose();
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
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMaximumSize(new Dimension(-1, -1));
        contentPane.setMinimumSize(new Dimension(-1, -1));
        contentPane.setPreferredSize(new Dimension(400, 400));
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
        buttonUpdate = new JButton();
        buttonUpdate.setText("Update");
        panel2.add(
            buttonUpdate, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(
            buttonCancel, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Table metadata");
        panel3.add(
            label1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(
            scrollPane1, new GridConstraints(
            1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableMetadata = new JTable();
        scrollPane1.setViewportView(tableMetadata);
        final JLabel label2 = new JLabel();
        label2.setText("Column Family");
        panel3.add(
            label2, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        panel3.add(
            toolBar1, new GridConstraints(
            2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(-1, 20), null, 0, false));
        cmbFamilies = new JComboBox();
        cmbFamilies.setEditable(true);
        toolBar1.add(cmbFamilies);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        toolBar1.add(toolBar$Separator1);
        addColumnMetadataButton = new JButton();
        addColumnMetadataButton.setIcon(new ImageIcon(getClass().getResource("/images/add.png")));
        addColumnMetadataButton.setMaximumSize(new Dimension(27, 27));
        addColumnMetadataButton.setMinimumSize(new Dimension(27, 27));
        addColumnMetadataButton.setPreferredSize(new Dimension(27, 27));
        addColumnMetadataButton.setText("");
        addColumnMetadataButton.setToolTipText("Add metadata attribute");
        toolBar1.add(addColumnMetadataButton);
        removeColumnMetadataButton = new JButton();
        removeColumnMetadataButton.setEnabled(false);
        removeColumnMetadataButton.setHorizontalAlignment(0);
        removeColumnMetadataButton.setIcon(new ImageIcon(getClass().getResource("/images/delete.png")));
        removeColumnMetadataButton.setMaximumSize(new Dimension(27, 27));
        removeColumnMetadataButton.setMinimumSize(new Dimension(27, 27));
        removeColumnMetadataButton.setPreferredSize(new Dimension(27, 27));
        removeColumnMetadataButton.setText("");
        removeColumnMetadataButton.setToolTipText("Remove metadata attribute");
        toolBar1.add(removeColumnMetadataButton);
        final JToolBar toolBar2 = new JToolBar();
        toolBar2.setFloatable(false);
        panel3.add(
            toolBar2, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        addTableMetadataButton = new JButton();
        addTableMetadataButton.setIcon(new ImageIcon(getClass().getResource("/images/add.png")));
        addTableMetadataButton.setMaximumSize(new Dimension(27, 27));
        addTableMetadataButton.setMinimumSize(new Dimension(27, 27));
        addTableMetadataButton.setPreferredSize(new Dimension(27, 27));
        addTableMetadataButton.setText("");
        addTableMetadataButton.setToolTipText("Add metadata attribute");
        toolBar2.add(addTableMetadataButton);
        removeTableMetadataButton = new JButton();
        removeTableMetadataButton.setEnabled(false);
        removeTableMetadataButton.setHorizontalAlignment(0);
        removeTableMetadataButton.setIcon(new ImageIcon(getClass().getResource("/images/delete.png")));
        removeTableMetadataButton.setMaximumSize(new Dimension(27, 27));
        removeTableMetadataButton.setMinimumSize(new Dimension(27, 27));
        removeTableMetadataButton.setPreferredSize(new Dimension(27, 27));
        removeTableMetadataButton.setText("");
        removeTableMetadataButton.setToolTipText("Remove metadata attribute");
        toolBar2.add(removeTableMetadataButton);
        final JScrollPane scrollPane2 = new JScrollPane();
        contentPane.add(
            scrollPane2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        columnMetadata = new JTable();
        scrollPane2.setViewportView(columnMetadata);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
