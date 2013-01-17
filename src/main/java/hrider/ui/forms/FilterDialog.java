package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import hrider.filters.EquationFilter;
import hrider.filters.EquationOperator;
import hrider.filters.Filter;
import hrider.ui.controls.ErrorProvider;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
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
 *          <p/>
 *          This class represents a dialog to create filters.
 */
public class FilterDialog extends JDialog {

    //region Variables
    private JPanel           contentPane;
    private JButton          btSave;
    private JButton          btCancel;
    private JTextField       tfRegex;
    private JButton          btTestRegex;
    private JList            originalList;
    private JList            filteredList;
    private DefaultListModel filteredListModel;
    private JTextArea        textArea;
    private JButton          btAdd;
    private JButton          btClear;
    private JComboBox        cmbOperator;
    private JButton          btTestPattern;
    private ErrorProvider    errorProvider;
    private JCheckBox        excludeCheckBox;
    private List<String>     values;
    private boolean          savePressed;
    //endregion

    //region Constructor
    public FilterDialog(String regex, List<String> values) {
        setContentPane(contentPane);
        setModal(true);
        setTitle("Define filters");
        getRootPane().setDefaultButton(btSave);

        this.textArea.setText(regex);
        this.values = values;

        DefaultListModel originalListModel = new DefaultListModel();
        originalList.setModel(originalListModel);

        filteredListModel = new DefaultListModel();
        filteredList.setModel(filteredListModel);

        for (String value : values) {
            originalListModel.addElement(value);
            filteredListModel.addElement(value);
        }

        for (EquationOperator operator : EquationOperator.values()) {
            cmbOperator.addItem(operator);
        }

        tfRegex.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    errorProvider.clearError();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    errorProvider.clearError();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });

        btSave.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onSave();
                }
            });

        btCancel.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    onCancel();
                }
            });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        btTestRegex.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        String regex = tfRegex.getText();
                        if (excludeCheckBox.isSelected()) {
                            regex = String.format("~(%s)", regex);
                        }

                        applyFilter(EquationFilter.parse(regex));
                    }
                    catch (Exception ex) {
                        errorProvider.setError(ex.getMessage());
                    }
                }
            });

        btAdd.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        EquationFilter.parse(tfRegex.getText());

                        String prefix = "";
                        if (excludeCheckBox.isSelected()) {
                            prefix = "~";
                        }

                        String regex = textArea.getText().trim();
                        if (!regex.isEmpty()) {
                            regex = String.format("%s %s ", regex, cmbOperator.getSelectedItem());
                            textArea.setText(String.format("(%s%s(%s))", regex, prefix, tfRegex.getText()));
                        }
                        else {
                            textArea.setText(String.format("%s(%s)", prefix, tfRegex.getText()));
                        }
                    }
                    catch (Exception ex) {
                        errorProvider.setError(ex.getMessage());
                    }
                }
            });

        btClear.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    textArea.setText("");
                }
            });

        btTestPattern.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    applyFilter(EquationFilter.parse(textArea.getText()));
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

    public String getRegex() {
        if (savePressed) {
            return textArea.getText();
        }
        return null;
    }
    //endregion

    //region Private Methods
    private void applyFilter(Filter filter) {
        this.filteredListModel.clear();

        for (String value : this.values) {
            if (filter.match(value)) {
                this.filteredListModel.addElement(value);
            }
        }
    }

    private void onSave() {
        savePressed = true;
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
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setPreferredSize(new Dimension(400, 300));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(
            panel2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btSave = new JButton();
        btSave.setText("Save");
        panel2.add(
            btSave, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btCancel = new JButton();
        btCancel.setText("Cancel");
        panel2.add(
            btCancel, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel1.add(
            separator1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 5, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btTestRegex = new JButton();
        btTestRegex.setText("Test");
        panel3.add(
            btTestRegex, new GridConstraints(
            0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(
            panel4, new GridConstraints(
            4, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Original Data");
        panel4.add(
            label1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Filtered Data");
        panel4.add(
            label2, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel4.add(
            scrollPane1, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        originalList = new JList();
        scrollPane1.setViewportView(originalList);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel4.add(
            scrollPane2, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        filteredList = new JList();
        scrollPane2.setViewportView(filteredList);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(
            panel5, new GridConstraints(
            2, 4, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btAdd = new JButton();
        btAdd.setText("Add");
        panel5.add(
            btAdd, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btClear = new JButton();
        btClear.setText("Clear");
        panel5.add(
            btClear, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btTestPattern = new JButton();
        btTestPattern.setText("Test");
        panel5.add(
            btTestPattern, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel3.add(
            scrollPane3, new GridConstraints(
            3, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane3.setViewportView(textArea);
        final JLabel label3 = new JLabel();
        label3.setText("Operator");
        panel3.add(
            label3, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        cmbOperator = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        cmbOperator.setModel(defaultComboBoxModel1);
        panel3.add(
            cmbOperator, new GridConstraints(
            2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(
            spacer1, new GridConstraints(
            2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(
            panel6, new GridConstraints(
            0, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tfRegex = new JTextField();
        panel6.add(
            tfRegex, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, 24), null, 0, false));
        errorProvider = new ErrorProvider();
        panel6.add(
            errorProvider, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel3.add(
            separator2, new GridConstraints(
            1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        excludeCheckBox = new JCheckBox();
        excludeCheckBox.setText("Exclude");
        excludeCheckBox.setToolTipText("Check if the regular expression should be used to exclude the data");
        panel3.add(
            excludeCheckBox, new GridConstraints(
            2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
