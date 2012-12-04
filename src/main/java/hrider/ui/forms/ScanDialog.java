package hrider.ui.forms;

import com.michaelbaranov.microba.calendar.DatePicker;
import hrider.config.Configurator;
import hrider.data.ObjectType;
import hrider.data.TypedColumn;
import hrider.hbase.Operator;
import hrider.hbase.Query;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyVetoException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ScanDialog extends JDialog {

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

    public ScanDialog(Query query, Iterable<TypedColumn> columns) {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Search");
        getRootPane().setDefaultButton(this.buttonRun);

        this.startTimeDatePicker.setDateFormat(new SimpleDateFormat(Configurator.getDateFormat(), Locale.ENGLISH));
        this.endTimeDatePicker.setDateFormat(new SimpleDateFormat(Configurator.getDateFormat(), Locale.ENGLISH));

        for (TypedColumn column : columns) {
            if (!"key".equalsIgnoreCase(column.getColumn())) {
                this.comboBoxColumns.addItem(column.getColumn());
            }
        }

        for (Operator operator : Operator.values()) {
            this.comboBoxOperator.addItem(operator);
        }

        for (ObjectType objectType : ObjectType.values()) {
            this.comboBoxStartKeyType.addItem(objectType);
            this.comboBoxEndKeyType.addItem(objectType);
            this.comboBoxWordType.addItem(objectType);
        }

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

    public void showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    public Query getQuery() {
        if (this.okPressed) {
            Query query = new Query();

            if (!this.textFieldStartKey.getText().trim().isEmpty()) {
                query.setStartKey((ObjectType)this.comboBoxStartKeyType.getSelectedItem(), this.textFieldStartKey.getText().trim());
            }

            if (!this.textFieldEndKey.getText().trim().isEmpty()) {
                query.setEndKey((ObjectType)this.comboBoxEndKeyType.getSelectedItem(), this.textFieldEndKey.getText().trim());
            }

            if (this.checkBoxUseDates.isSelected()) {
                query.setStartDate(this.startTimeDatePicker.getDate());
                query.setEndDate(this.endTimeDatePicker.getDate());
            }

            if (!this.textFieldWord.getText().trim().isEmpty()) {
                String column = (String)this.comboBoxColumns.getSelectedItem();

                String[] parts = column.split(":");
                query.setFamily(parts[0]);
                query.setColumn(parts[1]);

                query.setOperator((Operator)this.comboBoxOperator.getSelectedItem());
                query.setWord(this.textFieldWord.getText().trim());
                query.setWordType((ObjectType)this.comboBoxWordType.getSelectedItem());
            }

            return query;
        }
        return null;
    }

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
}
