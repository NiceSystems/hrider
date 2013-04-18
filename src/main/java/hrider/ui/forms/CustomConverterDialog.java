package hrider.ui.forms;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import hrider.config.GlobalConfig;
import hrider.converters.TypeConverter;
import hrider.io.Log;
import hrider.io.PathHelper;
import hrider.reflection.JavaCompiler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.regex.Pattern;

public class CustomConverterDialog extends JDialog {

    //region Constants
    private final static String IMPORTS_PLACE_HOLDER                      = "$$IMPORTS_PLACE_HOLDER$$";
    private final static String CONVERTER_NAME_PLACE_HOLDER               = "$$CONVERTER_NAME_PLACE_HOLDER$$";
    private final static String BYTES_TO_STRING_CODE_PLACE_HOLDER         = "$$BYTES_TO_STRING_CODE_PLACE_HOLDER$$";
    private final static String STRING_TO_BYTES_CODE_PLACE_HOLDER         = "$$STRING_TO_BYTES_CODE_PLACE_HOLDER$$";
    private final static String IS_VALID_FOR_NAME_CONVERSION_PLACE_HOLDER = "$$IS_VALID_FOR_NAME_CONVERSION_PLACE_HOLDER$$";

    private final static String TEMPLATE = "package hrider.converters.custom;\n" +
                                           '\n' +
                                           "$$IMPORTS_PLACE_HOLDER$$\n" +
                                           '\n' +
                                           "public class $$CONVERTER_NAME_PLACE_HOLDER$$ extends hrider.converters.TypeConverter {\n" +
                                           '\n' +
                                           "    /**\n" +
                                           "     * Converts an array of bytes to String.\n" +
                                           "     */\n" +
                                           "    @Override\n" +
                                           "    public String toString(byte[] value) {\n" +
                                           "        if (value == null) {\n" +
                                           "            return null;\n" +
                                           "        }\n" +
                                           "$$BYTES_TO_STRING_CODE_PLACE_HOLDER$$\n" +
                                           "    }\n" +
                                           '\n' +
                                           "    /**\n" +
                                           "     * Converts a String to an array of bytes.\n" +
                                           "     */\n" +
                                           "    @Override\n" +
                                           "    public byte[] toBytes(String value) {\n" +
                                           "        if (value == null) {\n" +
                                           "            return EMPTY_BYTES_ARRAY;\n" +
                                           "        }\n" +
                                           "$$STRING_TO_BYTES_CODE_PLACE_HOLDER$$\n" +
                                           "    }\n" +
                                           '\n' +
                                           "    /**\n" +
                                           "     * Indicates whether the type converter can be used for column name conversions.\n" +
                                           "     */\n" +
                                           "    @Override\n" +
                                           "    public boolean isValidForNameConversion() {\n" +
                                           "        return $$IS_VALID_FOR_NAME_CONVERSION_PLACE_HOLDER$$;\n" +
                                           "    }" +
                                           "}\n";

    private static final long serialVersionUID = -8239690036592242474L;
    private final static Log  logger           = Log.getLogger(PathHelper.class);
    //endregion

    //region Variables
    private JPanel     contentPane;
    private JButton    buttonOK;
    private JButton    buttonCancel;
    private JTextField tfConverterName;
    private JTextArea  taBytesToObjectMethod;
    private JTextArea  taImports;
    private JTextArea  taObjectToBytesMethod;
    private JCheckBox  chbNameConverter;
    private boolean    okPressed;
    //endregion

    //region Constructor
    public CustomConverterDialog(TypeConverter converter) {
        setContentPane(contentPane);
        setModal(true);
        setTitle(converter == null ? "Create custom type converter" : "Edit custom type converter");
        getRootPane().setDefaultButton(buttonOK);

        if (converter != null) {
            tfConverterName.setText(converter.getClass().getSimpleName());
            chbNameConverter.setSelected(converter.isValidForNameConversion());

            loadMethods(converter.getCode());
        }
        else {
            tfConverterName.select(0, 6);
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

    public String getConverterName() {
        return tfConverterName.getText().replace("Converter", "");
    }
    //endregion

    //region Private Methods
    private void loadMethods(String code) {
        Methods method = Methods.None;

        int skipCount = 0;
        int bracketsCount = 0;

        StringBuilder body = new StringBuilder();

        for (String line : code.replace("  ", " ").split(Pattern.quote(PathHelper.LINE_SEPARATOR))) {
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("public String toString(byte[] value) {")) {
                bracketsCount++;
                method = Methods.BytesToString;

                continue;
            }

            if (trimmedLine.startsWith("public byte[] toBytes(String value) {")) {
                bracketsCount++;
                method = Methods.StringToBytes;

                continue;
            }

            if (method != Methods.None) {
                if (trimmedLine.endsWith("{")) {
                    bracketsCount++;
                }

                if (trimmedLine.endsWith("}")) {
                    bracketsCount--;
                }

                if (trimmedLine.startsWith("if (value == null) {")) {
                    skipCount++;
                }

                if (bracketsCount == 0) {
                    setMethodBody(method, body);

                    method = Methods.None;
                    skipCount = 0;
                }
                else {
                    if (skipCount == 0 || skipCount == 4) {
                        if (body.length() > 0) {
                            body.append(PathHelper.LINE_SEPARATOR);
                        }

                        body.append(indent(bracketsCount));
                        body.append(trimmedLine);
                    }
                    else {
                        skipCount++;
                    }
                }
            }
        }
    }

    private void setMethodBody(Methods method, StringBuilder body) {
        if (method != Methods.None) {
            switch (method) {
                case None:
                    break;
                case BytesToString:
                    taBytesToObjectMethod.setText(body.toString());
                    break;
                case StringToBytes:
                    taObjectToBytesMethod.setText(body.toString());
                    break;
            }

            body.setLength(0);
        }
    }

    private static String indent(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < length ; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    private void onOK() {
        // add your code here

        String code = TEMPLATE;

        code = code.replace(IMPORTS_PLACE_HOLDER, taImports.getText());
        code = code.replace(CONVERTER_NAME_PLACE_HOLDER, tfConverterName.getText());
        code = code.replace(BYTES_TO_STRING_CODE_PLACE_HOLDER, taBytesToObjectMethod.getText());
        code = code.replace(STRING_TO_BYTES_CODE_PLACE_HOLDER, taObjectToBytesMethod.getText());
        code = code.replace(IS_VALID_FOR_NAME_CONVERSION_PLACE_HOLDER, chbNameConverter.isSelected() ? "true" : "false");

        contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            File sourceCode = JavaCompiler.saveCode(tfConverterName.getText(), code, GlobalConfig.instance().getConvertersCodeFolder());
            if (JavaCompiler.compile(sourceCode, GlobalConfig.instance().getConvertersClassesFolder())) {
                this.okPressed = true;

                dispose();
            }
            else {
                StringBuilder message = new StringBuilder();
                for (String error : JavaCompiler.getErrors()) {
                    message.append(error);
                }

                JOptionPane.showMessageDialog(contentPane, message, "Compilation Errors", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception e) {
            logger.error(e, "Failed to compile code.");
            JOptionPane.showMessageDialog(contentPane, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        finally {
            contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

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
        contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(
            spacer1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(
            panel2, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Save");
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
        panel3.setLayout(new GridLayoutManager(9, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(
            scrollPane1, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 70), null, 0, false));
        taBytesToObjectMethod = new JTextArea();
        taBytesToObjectMethod.setText("   return Bytes.toString(value); // replace with your code");
        scrollPane1.setViewportView(taBytesToObjectMethod);
        final JLabel label1 = new JLabel();
        label1.setText("Imports:");
        panel3.add(
            label1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(
            scrollPane2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 40), null, 0, false));
        taImports = new JTextArea();
        taImports.setText("import org.apache.hadoop.hbase.util.Bytes;");
        scrollPane2.setViewportView(taImports);
        final JLabel label2 = new JLabel();
        label2.setText("public String toString(byte[] value) { // bytes to object conversion");
        panel3.add(
            label2, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("}");
        panel3.add(
            label3, new GridConstraints(
            4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("public byte[] toBytes(String value) { // object to bytes conversion");
        panel3.add(
            label4, new GridConstraints(
            5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel3.add(
            scrollPane3, new GridConstraints(
            6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 70), null, 0, false));
        taObjectToBytesMethod = new JTextArea();
        taObjectToBytesMethod.setText("   return Bytes.toBytes(value); // replace with your code");
        scrollPane3.setViewportView(taObjectToBytesMethod);
        final JLabel label5 = new JLabel();
        label5.setText("}");
        panel3.add(
            label5, new GridConstraints(
            7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        chbNameConverter = new JCheckBox();
        chbNameConverter.setSelected(true);
        chbNameConverter.setText("The converter can be used for column name conversions as well.");
        panel3.add(
            chbNameConverter, new GridConstraints(
            8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel4, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Converter Name:");
        panel4.add(
            label6, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        tfConverterName = new JTextField();
        tfConverterName.setText("CustomConverter");
        panel4.add(
            tfConverterName, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, 24), null, 0, false));
        final JSeparator separator1 = new JSeparator();
        contentPane.add(
            separator1, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        label1.setLabelFor(taImports);
        label2.setLabelFor(taBytesToObjectMethod);
        label4.setLabelFor(taObjectToBytesMethod);
        label6.setLabelFor(tfConverterName);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    //endregion

    private enum Methods {
        None,
        BytesToString,
        StringToBytes
    }
}
