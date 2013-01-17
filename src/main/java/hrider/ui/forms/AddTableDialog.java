package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

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

    //region Variables
    private JPanel           contentPane;
    private JButton          buttonOK;
    private JButton          buttonCancel;
    private JTextField       tableNameTextField;
    private JButton          removeFamilyButton;
    private JButton          addFamilyButton;
    private JList            familiesList;
    private DefaultListModel familiesListModel;
    private boolean          okPressed;
    //endregion

    //region Constructor
    public AddTableDialog() {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Add new table");
        getRootPane().setDefaultButton(this.buttonOK);

        this.familiesListModel = new DefaultListModel();
        this.familiesList.setModel(this.familiesListModel);

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

        this.addFamilyButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String columnFamily = JOptionPane.showInputDialog(
                        AddTableDialog.this, "Column family:", "Add new column family", JOptionPane.PLAIN_MESSAGE);

                    if (columnFamily != null && !AddTableDialog.this.familiesListModel.contains(columnFamily)) {
                        AddTableDialog.this.familiesListModel.addElement(columnFamily);
                    }
                }
            });

        this.removeFamilyButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (Object value : AddTableDialog.this.familiesList.getSelectedValues()) {
                        AddTableDialog.this.familiesListModel.removeElement(value);
                    }
                }
            });

        this.tableNameTextField.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    AddTableDialog.this.addFamilyButton.setEnabled(!AddTableDialog.this.tableNameTextField.getText().trim().isEmpty());
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    AddTableDialog.this.addFamilyButton.setEnabled(!AddTableDialog.this.tableNameTextField.getText().trim().isEmpty());
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    AddTableDialog.this.addFamilyButton.setEnabled(!AddTableDialog.this.tableNameTextField.getText().trim().isEmpty());
                }
            });

        this.familiesList.addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    AddTableDialog.this.removeFamilyButton.setEnabled(AddTableDialog.this.familiesList.getSelectedIndices().length > 0);
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

    public String getTableName() {
        if (this.okPressed) {
            return this.tableNameTextField.getText().trim();
        }
        return null;
    }

    public List<String> getColumnFamilies() {
        List<String> columnFamilies = new ArrayList<String>();
        if (this.okPressed) {
            for (int i = 0 ; i < AddTableDialog.this.familiesListModel.getSize() ; i++) {
                columnFamilies.add((String)AddTableDialog.this.familiesListModel.getElementAt(i));
            }
        }
        return columnFamilies;
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
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Table Name:");
        panel1.add(
            label1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        tableNameTextField = new JTextField();
        panel1.add(
            tableNameTextField, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Column Families:");
        panel1.add(
            label2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(
            panel2, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        removeFamilyButton = new JButton();
        removeFamilyButton.setEnabled(false);
        removeFamilyButton.setText("Remove Family");
        panel2.add(
            removeFamilyButton, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addFamilyButton = new JButton();
        addFamilyButton.setEnabled(false);
        addFamilyButton.setText("Add Family");
        panel2.add(
            addFamilyButton, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(
            scrollPane1, new GridConstraints(
            2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        familiesList = new JList();
        scrollPane1.setViewportView(familiesList);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel3.add(
            panel4, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Add");
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
        final JSeparator separator1 = new JSeparator();
        panel3.add(
            separator1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
