package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import hrider.data.ColumnFamily;
import hrider.data.TableDescriptor;
import hrider.ui.design.JCellEditor;

import javax.swing.*;
import javax.swing.event.*;
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
public class AddTableDialog extends JDialog {

    //region Constants
    private static final long   serialVersionUID = 779315024557912307L;
    private static final String NEW_TABLE_NAME   = "new_table";
    //endregion

    //region Variables
    private JPanel            contentPane;
    private JButton           buttonOK;
    private JButton           buttonCancel;
    private JTextField        tableNameTextField;
    private JButton           removeFamilyButton;
    private JButton           addFamilyButton;
    private JList             familiesList;
    private JButton           editFamilyButton;
    private JTable            metadataTable;
    private DefaultTableModel metadataModel;
    private JButton           addButton;
    private JButton           removeButton;
    private DefaultListModel  familiesListModel;
    private boolean           okPressed;
    private TableDescriptor   tableDescriptor;
    //endregion

    //region Constructor
    public AddTableDialog() {
        this(null);
    }

    public AddTableDialog(TableDescriptor descriptor) {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Create table");
        getRootPane().setDefaultButton(this.buttonOK);

        if (descriptor == null) {
            this.tableDescriptor = new TableDescriptor(NEW_TABLE_NAME);
        }
        else {
            this.tableDescriptor = descriptor.clone();
        }

        this.familiesListModel = new DefaultListModel();
        this.familiesList.setModel(this.familiesListModel);

        TableCellEditor cellEditor = new JCellEditor(null, true);

        this.metadataModel = new DefaultTableModel();
        this.metadataTable.setModel(this.metadataModel);

        this.metadataModel.addColumn("Key");
        this.metadataModel.addColumn("Value");
        this.metadataTable.setRowHeight(this.metadataTable.getFont().getSize() + 8);
        this.metadataTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        this.metadataTable.getColumn("Key").setCellEditor(cellEditor);
        this.metadataTable.getColumn("Value").setCellEditor(cellEditor);

        this.metadataTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    removeButton.setEnabled(e.getFirstIndex() >= 0);
                }
            });

        this.metadataModel.addTableModelListener(
            new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getType() == TableModelEvent.UPDATE) {
                        String key = (String)metadataTable.getValueAt(e.getFirstRow(), 0);
                        String val = (String)metadataTable.getValueAt(e.getFirstRow(), 1);

                        if (key != null && !key.isEmpty() && val != null && !val.isEmpty()) {
                            tableDescriptor.setValue(key, val);
                        }
                    }
                }
            });

        for (Map.Entry<String, String> entry : tableDescriptor.getMetadata().entrySet()) {
            this.metadataModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        this.tableNameTextField.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    tableDescriptor.setName(tableNameTextField.getText());
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    tableDescriptor.setName(tableNameTextField.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    tableDescriptor.setName(tableNameTextField.getText());
                }
            });

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
                        TableCellEditor editor = metadataTable.getCellEditor();
                        if (editor != null) {
                            editor.stopCellEditing();
                        }

                        int[] selectedRows = metadataTable.getSelectedRows();
                        for (int selectedRow : selectedRows) {
                            metadataModel.removeRow(selectedRow);
                        }
                    }
                }
            });

        this.addFamilyButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AddFamilyDialog dialog = new AddFamilyDialog(null);
                    if (dialog.showDialog(contentPane)) {
                        ColumnFamily columnFamily = dialog.getColumnFamily();
                        if (!familiesListModel.contains(columnFamily)) {
                            familiesListModel.addElement(columnFamily);

                            tableDescriptor.addFamily(columnFamily);
                        }
                    }
                }
            });

        editFamilyButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ColumnFamily columnFamily = (ColumnFamily)familiesList.getSelectedValue();
                    if (columnFamily != null) {
                        AddFamilyDialog dialog = new AddFamilyDialog(columnFamily);
                        if (dialog.showDialog(contentPane)) {
                            familiesList.paintImmediately(familiesList.getBounds());
                        }
                    }
                }
            });

        this.removeFamilyButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (Object item : familiesList.getSelectedValues()) {
                        familiesListModel.removeElement(item);
                        tableDescriptor.removeFamily((ColumnFamily)item);
                    }
                }
            });

        this.familiesList.addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    editFamilyButton.setEnabled(familiesList.getSelectedIndices().length > 0);
                    removeFamilyButton.setEnabled(familiesList.getSelectedIndices().length > 0);
                }
            });

        for (ColumnFamily columnFamily : tableDescriptor.families()) {
            familiesListModel.addElement(columnFamily);
        }

        this.tableNameTextField.setText(tableDescriptor.getName());
        this.tableNameTextField.selectAll();
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

    public TableDescriptor getTableDescriptor() {
        if (this.okPressed) {
            return this.tableDescriptor;
        }
        return null;
    }
    //endregion

    //region Private Methods
    private void onOK() {
        if (this.tableNameTextField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "The table name is required.", "Error", JOptionPane.ERROR_MESSAGE);
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
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMaximumSize(new Dimension(-1, -1));
        contentPane.setMinimumSize(new Dimension(-1, -1));
        contentPane.setPreferredSize(new Dimension(350, 350));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Column Families:");
        panel1.add(
            label1, new GridConstraints(
            4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(
            scrollPane1, new GridConstraints(
            5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        familiesList = new JList();
        scrollPane1.setViewportView(familiesList);
        final JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        panel1.add(
            toolBar1, new GridConstraints(
            4, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        addFamilyButton = new JButton();
        addFamilyButton.setEnabled(true);
        addFamilyButton.setIcon(new ImageIcon(getClass().getResource("/images/add.png")));
        addFamilyButton.setMaximumSize(new Dimension(27, 27));
        addFamilyButton.setMinimumSize(new Dimension(27, 27));
        addFamilyButton.setPreferredSize(new Dimension(27, 27));
        addFamilyButton.setText("");
        addFamilyButton.setToolTipText("Add column family");
        toolBar1.add(addFamilyButton);
        editFamilyButton = new JButton();
        editFamilyButton.setEnabled(false);
        editFamilyButton.setIcon(new ImageIcon(getClass().getResource("/images/edit.png")));
        editFamilyButton.setMaximumSize(new Dimension(27, 27));
        editFamilyButton.setMinimumSize(new Dimension(27, 27));
        editFamilyButton.setPreferredSize(new Dimension(27, 27));
        editFamilyButton.setText("");
        editFamilyButton.setToolTipText("Edit column family");
        toolBar1.add(editFamilyButton);
        removeFamilyButton = new JButton();
        removeFamilyButton.setEnabled(false);
        removeFamilyButton.setIcon(new ImageIcon(getClass().getResource("/images/delete.png")));
        removeFamilyButton.setMaximumSize(new Dimension(27, 27));
        removeFamilyButton.setMinimumSize(new Dimension(27, 27));
        removeFamilyButton.setPreferredSize(new Dimension(27, 27));
        removeFamilyButton.setText("");
        removeFamilyButton.setToolTipText("Remove column family");
        toolBar1.add(removeFamilyButton);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(
            panel2, new GridConstraints(
            0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Table Name:");
        panel2.add(
            label2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        tableNameTextField = new JTextField();
        panel2.add(
            tableNameTextField, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(-1, 24), null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel1.add(
            separator1, new GridConstraints(
            1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(
            scrollPane2, new GridConstraints(
            3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        metadataTable = new JTable();
        scrollPane2.setViewportView(metadataTable);
        final JToolBar toolBar2 = new JToolBar();
        toolBar2.setFloatable(false);
        panel1.add(
            toolBar2, new GridConstraints(
            2, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        addButton = new JButton();
        addButton.setIcon(new ImageIcon(getClass().getResource("/images/add.png")));
        addButton.setMaximumSize(new Dimension(27, 27));
        addButton.setMinimumSize(new Dimension(27, 27));
        addButton.setPreferredSize(new Dimension(27, 27));
        addButton.setText("");
        addButton.setToolTipText("Add metadata attribute");
        toolBar2.add(addButton);
        removeButton = new JButton();
        removeButton.setEnabled(false);
        removeButton.setHorizontalAlignment(0);
        removeButton.setIcon(new ImageIcon(getClass().getResource("/images/delete.png")));
        removeButton.setMaximumSize(new Dimension(27, 27));
        removeButton.setMinimumSize(new Dimension(27, 27));
        removeButton.setPreferredSize(new Dimension(27, 27));
        removeButton.setText("");
        removeButton.setToolTipText("Remove metadata attribute");
        toolBar2.add(removeButton);
        final JLabel label3 = new JLabel();
        label3.setText("Table metadata");
        panel1.add(
            label3, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel3.add(
            panel4, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Ok");
        panel4.add(
            buttonOK, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel4.add(
            buttonCancel, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        contentPane.add(
            separator2, new GridConstraints(
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
