package hrider.ui.forms;

import javax.swing.*;
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
public class AddColumnDialog extends JDialog {

    //region Variables
    private JPanel     contentPane;
    private JButton    buttonOK;
    private JButton    buttonCancel;
    private JComboBox  comboBoxColumnFamilies;
    private JTextField columnNameTextField;
    private boolean    okPressed;
    //endregion

    //region Constructor
    public AddColumnDialog(Iterable<String> columnFamilies) {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Create new column");
        getRootPane().setDefaultButton(this.buttonOK);

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

        for (String columnFamily : columnFamilies) {
            this.comboBoxColumnFamilies.addItem(columnFamily);
        }

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
    public void showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    public String getColumnName() {
        if (this.okPressed) {
            return String.format("%s:%s", this.comboBoxColumnFamilies.getSelectedItem(), this.columnNameTextField.getText().trim());
        }
        return null;
    }
    //endregion

    //region Private Methods
    private void onOK() {
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
}
