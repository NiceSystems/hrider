package hrider.ui.forms;

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
}
