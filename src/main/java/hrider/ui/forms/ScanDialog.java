package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.michaelbaranov.microba.calendar.DatePicker;
import hrider.data.ColumnQualifier;
import hrider.data.ColumnType;
import hrider.data.TypedColumn;
import hrider.format.DateUtils;
import hrider.hbase.Operator;
import hrider.hbase.Query;
import hrider.ui.controls.BoundsPopupMenuListener;

import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyVetoException;

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
public class ScanDialog extends JDialog {

    //region Constants
    private static final long serialVersionUID = 694275430107585525L;
    //endregion

    //region Variables
    private JPanel     contentPane;
    private JButton    buttonRun;
    private JButton    buttonCancel;
    private JComboBox  comboBoxColumns;
    private JComboBox  comboBoxOperator;
    private JTextField textFieldWord;
    private JTextField textFieldStartKey;
    private JTextField textFieldEndKey;
    private JComboBox  comboBoxStartKeyType;
    private JComboBox  comboBoxEndKeyType;
    private DatePicker startTimeDatePicker;
    private DatePicker endTimeDatePicker;
    private JCheckBox  checkBoxUseDates;
    private JComboBox  comboBoxWordType;
    private boolean    okPressed;
    //endregion

    //region Constructor
    public ScanDialog(Query query, Iterable<TypedColumn> columns) {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Scan");
        getRootPane().setDefaultButton(this.buttonRun);

        this.startTimeDatePicker.setDateFormat(DateUtils.getDefaultDateFormat());
        this.endTimeDatePicker.setDateFormat(DateUtils.getDefaultDateFormat());

        for (TypedColumn column : columns) {
            if (!column.getColumn().isKey()) {
                this.comboBoxColumns.addItem(column.getColumn());
            }
        }

        PopupMenuListener listener = new BoundsPopupMenuListener(true, false);
        this.comboBoxColumns.addPopupMenuListener(listener);
        this.comboBoxColumns.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXX");

        for (Operator operator : Operator.values()) {
            this.comboBoxOperator.addItem(operator);
        }

        for (ColumnType columnType : ColumnType.getTypes()) {
            this.comboBoxStartKeyType.addItem(columnType);
            this.comboBoxEndKeyType.addItem(columnType);
            this.comboBoxWordType.addItem(columnType);
        }

        this.comboBoxStartKeyType.setSelectedItem(ColumnType.BinaryString);
        this.comboBoxEndKeyType.setSelectedItem(ColumnType.BinaryString);
        this.comboBoxWordType.setSelectedItem(ColumnType.String);

        fillForm(query);

        this.buttonRun.addActionListener(
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

    public Query getQuery() {
        if (this.okPressed) {
            Query query = new Query();

            if (!this.textFieldStartKey.getText().trim().isEmpty()) {
                query.setStartKey((ColumnType)this.comboBoxStartKeyType.getSelectedItem(), this.textFieldStartKey.getText().trim());
            }

            if (!this.textFieldEndKey.getText().trim().isEmpty()) {
                query.setEndKey((ColumnType)this.comboBoxEndKeyType.getSelectedItem(), this.textFieldEndKey.getText().trim());
            }

            if (this.checkBoxUseDates.isSelected()) {
                query.setStartDate(this.startTimeDatePicker.getDate());
                query.setEndDate(this.endTimeDatePicker.getDate());
            }

            ColumnQualifier column = (ColumnQualifier)this.comboBoxColumns.getSelectedItem();

            query.setFamily(column.getFamily());
            query.setColumn(column.getName());
            query.setOperator((Operator)this.comboBoxOperator.getSelectedItem());

            if (!this.textFieldWord.getText().trim().isEmpty()) {
                query.setWord(this.textFieldWord.getText().trim());
                query.setWordType((ColumnType)this.comboBoxWordType.getSelectedItem());
            }

            return query;
        }
        return null;
    }
    //endregion

    //region Private Methods
    private void fillForm(Query query) {
        if (query != null) {
            if (query.getStartKey() != null) {
                this.comboBoxStartKeyType.setSelectedItem(query.getStartKeyType());
                this.textFieldStartKey.setText(query.getStartKeyAsString());
            }

            if (query.getEndKey() != null) {
                this.comboBoxEndKeyType.setSelectedItem(query.getEndKeyType());
                this.textFieldEndKey.setText(query.getEndKeyAsString());
            }

            if (query.getStartDate() != null) {
                this.checkBoxUseDates.setSelected(true);
                try {
                    this.startTimeDatePicker.setDate(query.getStartDate());
                    this.endTimeDatePicker.setDate(query.getEndDate());
                }
                catch (PropertyVetoException ignore) {
                }
            }

            if (query.getWord() != null) {

                if (query.getColumn() != null) {
                    this.comboBoxColumns.setSelectedItem(String.format("%s:%s", query.getFamily(), query.getColumn()));
                }

                this.comboBoxOperator.setSelectedItem(query.getOperator());
                this.textFieldWord.setText(query.getWord());
            }
        }
    }

    private void onOK() {
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
        contentPane.setMaximumSize(new Dimension(400, 350));
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
        buttonRun = new JButton();
        buttonRun.setText("Run");
        panel2.add(
            buttonRun, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(
            buttonCancel, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(6, 3, new Insets(5, 5, 5, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("where");
        panel3.add(
            label1, new GridConstraints(
            4, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        comboBoxColumns = new JComboBox();
        panel3.add(
            comboBoxColumns, new GridConstraints(
            4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        comboBoxOperator = new JComboBox();
        panel3.add(
            comboBoxOperator, new GridConstraints(
            4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        textFieldWord = new JTextField();
        panel3.add(
            textFieldWord, new GridConstraints(
            5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, 26), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("word");
        panel3.add(
            label2, new GridConstraints(
            5, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("start key");
        panel3.add(
            label3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("end key");
        panel3.add(
            label4, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("end time");
        panel3.add(
            label5, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("start time");
        panel3.add(
            label6, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        textFieldStartKey = new JTextField();
        panel3.add(
            textFieldStartKey, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, 26), null, 0, false));
        textFieldEndKey = new JTextField();
        panel3.add(
            textFieldEndKey, new GridConstraints(
            1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, 26), null, 0, false));
        comboBoxStartKeyType = new JComboBox();
        panel3.add(
            comboBoxStartKeyType, new GridConstraints(
            0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        comboBoxEndKeyType = new JComboBox();
        panel3.add(
            comboBoxEndKeyType, new GridConstraints(
            1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        startTimeDatePicker = new DatePicker();
        panel3.add(
            startTimeDatePicker, new GridConstraints(
            2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, 26), null, 0, false));
        endTimeDatePicker = new DatePicker();
        panel3.add(
            endTimeDatePicker, new GridConstraints(
            3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, 26), null, 0, false));
        checkBoxUseDates = new JCheckBox();
        checkBoxUseDates.setText("Use Dates");
        panel3.add(
            checkBoxUseDates, new GridConstraints(
            2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxWordType = new JComboBox();
        panel3.add(
            comboBoxWordType, new GridConstraints(
            5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        contentPane.add(
            separator1, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        label2.setLabelFor(textFieldWord);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
