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

public class CustomConverterDialog extends JDialog {

    //region Constants
    private final static String IMPORTS_PLACE_HOLDER                      = "$$IMPORTS_PLACE_HOLDER$$";
    private final static String VARIABLES_PLACE_HOLDER                    = "$$VARIABLES_PLACE_HOLDER$$";
    private final static String CONVERTER_NAME_PLACE_HOLDER               = "$$CONVERTER_NAME_PLACE_HOLDER$$";
    private final static String BYTES_TO_STRING_CODE_PLACE_HOLDER         = "$$BYTES_TO_STRING_CODE_PLACE_HOLDER$$";
    private final static String STRING_TO_BYTES_CODE_PLACE_HOLDER         = "$$STRING_TO_BYTES_CODE_PLACE_HOLDER$$";
    private final static String CAN_CONVERT_CODE_PLACE_HOLDER             = "$$CAN_CONVERT_CODE_PLACE_HOLDER$$";
    private final static String CAN_FORMAT_CODE_PLACE_HOLDER              = "$$CAN_FORMAT_CODE_PLACE_HOLDER$$";
    private final static String IS_VALID_FOR_NAME_CONVERSION_PLACE_HOLDER = "$$IS_VALID_FOR_NAME_CONVERSION_PLACE_HOLDER$$";
    private final static String COLOR_MAPPINGS_PLACE_HOLDER               = "$$COLOR_MAPPINGS_PLACE_HOLDER$$";
    private final static String FORMAT_PLACE_HOLDER                       = "$$FORMAT_PLACE_HOLDER$$";
    private final static String START_MARK                                = "//----$$ start %s $$----";
    private final static String END_MARK                                  = "//----$$ end %s $$----";
    private final static String IMPORTS_MARK                              = "imports";
    private final static String VARIABLES_MARK                            = "variables";
    private final static String TO_STRING_METHOD_MARK                     = "toString";
    private final static String TO_BYTES_METHOD_MARK                      = "toBytes";
    private final static String CAN_CONVERT_MARK                          = "canConvert";
    private final static String IS_VALID_FOR_NAME_CONVERSION_MARK         = "isValidForNameConversion";
    private final static String COLOR_MAPPINGS_MARK                       = "colorMappings";
    private final static String FORMAT_MARK                               = "format";

    private final static String TEMPLATE = "package hrider.converters.custom;\n" +
                                           '\n' +
                                           String.format(START_MARK, IMPORTS_MARK) + '\n' +
                                           IMPORTS_PLACE_HOLDER + '\n' +
                                           String.format(END_MARK, IMPORTS_MARK) + '\n' +
                                           '\n' +
                                           "public class $$CONVERTER_NAME_PLACE_HOLDER$$ extends hrider.converters.TypeConverter {\n" +
                                           '\n' +
                                           String.format(START_MARK, VARIABLES_MARK) + '\n' +
                                           VARIABLES_PLACE_HOLDER + '\n' +
                                           String.format(END_MARK, VARIABLES_MARK) + '\n' +
                                           '\n' +
                                           "    /**\n" +
                                           "     * Converts an array of bytes to String.\n" +
                                           "     */\n" +
                                           "    @Override\n" +
                                           "    public String toString(byte[] value) {\n" +
                                           "        if (value == null) {\n" +
                                           "            return null;\n" +
                                           "        }\n" +

                                           String.format(START_MARK, TO_STRING_METHOD_MARK) + '\n' +
                                           BYTES_TO_STRING_CODE_PLACE_HOLDER + '\n' +
                                           String.format(END_MARK, TO_STRING_METHOD_MARK) + '\n' +

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

                                           String.format(START_MARK, TO_BYTES_METHOD_MARK) + '\n' +
                                           STRING_TO_BYTES_CODE_PLACE_HOLDER + '\n' +
                                           String.format(END_MARK, TO_BYTES_METHOD_MARK) + '\n' +

                                           "    }\n" +
                                           '\n' +
                                           "    /**\n" +
                                           "     * Checks whether the provided byte array can be converted by the converter.\n" +
                                           "     */\n" +
                                           "    @Override\n" +
                                           "    public boolean canConvert(byte[] value) {\n" +
                                           "        if (value == null) {\n" +
                                           "            return false;\n" +
                                           "        }\n" +

                                           String.format(START_MARK, CAN_CONVERT_MARK) + '\n' +
                                           CAN_CONVERT_CODE_PLACE_HOLDER + '\n' +
                                           String.format(END_MARK, CAN_CONVERT_MARK) + '\n' +

                                           "    }\n" +
                                           '\n' +
                                           "    /**\n" +
                                           "     * Indicates whether the type converter supports formatting of the data.\n" +
                                           "     *\n" +
                                           "     * @return True if the type converter can be used to format the data or False otherwise.\n" +
                                           "     */\n" +
                                           "    @Override\n" +
                                           "    public boolean supportsFormatting() {" +
                                           "        return " + CAN_FORMAT_CODE_PLACE_HOLDER + ';' +
                                           "    }\n" +
                                           '\n' +
                                           "    /**\n" +
                                           "     * Indicates whether the type converter can be used for column name conversions.\n" +
                                           "     */\n" +
                                           "    @Override\n" +
                                           "    public boolean isValidForNameConversion() {\n" +
                                           "        return \n" +
                                           String.format(START_MARK, IS_VALID_FOR_NAME_CONVERSION_MARK) + '\n' +
                                           IS_VALID_FOR_NAME_CONVERSION_PLACE_HOLDER + '\n' +
                                           String.format(END_MARK, IS_VALID_FOR_NAME_CONVERSION_MARK) + '\n' +
                                           "        ;" +
                                           "    }" +
                                           '\n' +
                                           "    /**\n" +
                                           "     * Gets the mapping between the regular expression and the color to be used for drawing the text.\n" +
                                           "     *\n" +
                                           "     * @return The color to regular expression mappings.\n" +
                                           "     */\n" +
                                           "    public Map<Pattern, Color> getColorMappings() {\n" +

                                           String.format(START_MARK, COLOR_MAPPINGS_MARK) + '\n' +
                                           COLOR_MAPPINGS_PLACE_HOLDER + '\n' +
                                           String.format(END_MARK, COLOR_MAPPINGS_MARK) + '\n' +

                                           "    }\n" +
                                           '\n' +
                                           "    /**\n" +
                                           "     * Apply the custom formatting on the data.\n" +
                                           "     *\n" +
                                           "     * @param value The data to format.\n" +
                                           "     * @return The formatted data.\n" +
                                           "     */\n" +
                                           "    public String format(String value) {\n" +
                                           "        if (value == null) {\n" +
                                           "            return null;\n" +
                                           "        }\n" +

                                           String.format(START_MARK, FORMAT_MARK) + '\n' +
                                           FORMAT_PLACE_HOLDER + '\n' +
                                           String.format(END_MARK, FORMAT_MARK) + '\n' +

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
    private JTextArea  taCanConvertMethod;
    private JTextArea  taVariables;
    private JTextArea  taColorMappings;
    private JTextArea  taFormat;
    private boolean    okPressed;
    //endregion

    //region Constructor
    public CustomConverterDialog(TypeConverter converter) {
        setContentPane(contentPane);
        setModal(true);
        setTitle(converter == null ? "Create custom type converter" : "Edit custom type converter");
        getRootPane().setDefaultButton(buttonOK);

        taImports.setTabSize(4);
        taVariables.setTabSize(4);
        taBytesToObjectMethod.setTabSize(4);
        taObjectToBytesMethod.setTabSize(4);
        taCanConvertMethod.setTabSize(4);
        taColorMappings.setTabSize(4);
        taFormat.setTabSize(4);

        if (converter != null) {
            tfConverterName.setText(converter.getClass().getSimpleName());
            chbNameConverter.setSelected(converter.isValidForNameConversion());

            taImports.setText(extractCode(converter.getCode(), IMPORTS_MARK));
            taVariables.setText(extractCode(converter.getCode(), VARIABLES_MARK));
            taBytesToObjectMethod.setText(extractCode(converter.getCode(), TO_STRING_METHOD_MARK));
            taObjectToBytesMethod.setText(extractCode(converter.getCode(), TO_BYTES_METHOD_MARK));
            taCanConvertMethod.setText(extractCode(converter.getCode(), CAN_CONVERT_MARK));
            taColorMappings.setText(extractCode(converter.getCode(), COLOR_MAPPINGS_MARK));
            taFormat.setText(extractCode(converter.getCode(), FORMAT_MARK));
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
    private static String extractCode(String code, String mark) {
        String startMark = String.format(START_MARK, mark);
        String endMark = String.format(END_MARK, mark);

        int startIndex = code.indexOf(startMark);
        int endIndex = code.indexOf(endMark);

        return code.substring(startIndex + startMark.length(), endIndex).trim();
    }

    private void onOK() {
        // add your code here

        String code = TEMPLATE;

        code = code.replace(IMPORTS_PLACE_HOLDER, taImports.getText());
        code = code.replace(VARIABLES_PLACE_HOLDER, taVariables.getText());
        code = code.replace(CONVERTER_NAME_PLACE_HOLDER, tfConverterName.getText());
        code = code.replace(BYTES_TO_STRING_CODE_PLACE_HOLDER, taBytesToObjectMethod.getText());
        code = code.replace(STRING_TO_BYTES_CODE_PLACE_HOLDER, taObjectToBytesMethod.getText());
        code = code.replace(CAN_CONVERT_CODE_PLACE_HOLDER, taCanConvertMethod.getText());
        code = code.replace(IS_VALID_FOR_NAME_CONVERSION_PLACE_HOLDER, chbNameConverter.isSelected() ? "true" : "false");
        code = code.replace(FORMAT_PLACE_HOLDER, taFormat.getText());
        code = code.replace(COLOR_MAPPINGS_PLACE_HOLDER, taColorMappings.getText());

        if ("return value; // replace with your code".equals(taFormat.getText().trim())) {
            code = code.replace(CAN_FORMAT_CODE_PLACE_HOLDER, "false");
        }
        else {
            code = code.replace(CAN_FORMAT_CODE_PLACE_HOLDER, "true");
        }

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
        contentPane.setLayout(new GridLayoutManager(5, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel1, new GridConstraints(
            4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
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
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(
            panel3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Converter Name:");
        panel3.add(
            label1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        tfConverterName = new JTextField();
        tfConverterName.setText("CustomConverter");
        panel3.add(
            tfConverterName, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(150, 24), null, 0, false));
        final JSeparator separator1 = new JSeparator();
        contentPane.add(
            separator1, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW,
            null, null, null, 0, false));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        contentPane.add(
            tabbedPane1, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Imports", panel4);
        final JLabel label2 = new JLabel();
        label2.setText("Define all the imports used by the formatter here");
        panel4.add(
            label2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel4.add(
            scrollPane1, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 250), null, 0, false));
        taImports = new JTextArea();
        taImports.setText("import org.apache.hadoop.hbase.util.Bytes;\nimport java.util.regex.Pattern;\nimport java.util.Map;\nimport java.awt.Color;");
        scrollPane1.setViewportView(taImports);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Variables", panel5);
        final JLabel label3 = new JLabel();
        label3.setText("Define any instance variables here if you need to save the state of the converter between the method calls");
        panel5.add(
            label3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel5.add(
            scrollPane2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 250), null, 0, false));
        taVariables = new JTextArea();
        taVariables.setText("");
        scrollPane2.setViewportView(taVariables);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("toString", panel6);
        final JLabel label4 = new JLabel();
        label4.setText("public String toString(byte[] value) { // bytes to string conversion");
        panel6.add(
            label4, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel6.add(
            scrollPane3, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 250), null, 0, false));
        taBytesToObjectMethod = new JTextArea();
        taBytesToObjectMethod.setText("   return Bytes.toString(value); // replace with your code");
        scrollPane3.setViewportView(taBytesToObjectMethod);
        final JLabel label5 = new JLabel();
        label5.setText("}");
        panel6.add(
            label5, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("toBytes", panel7);
        final JLabel label6 = new JLabel();
        label6.setText("public byte[] toBytes(String value) { // string to bytes conversion");
        panel7.add(
            label6, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane4 = new JScrollPane();
        panel7.add(
            scrollPane4, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 250), null, 0, false));
        taObjectToBytesMethod = new JTextArea();
        taObjectToBytesMethod.setText("   return Bytes.toBytes(value); // replace with your code");
        scrollPane4.setViewportView(taObjectToBytesMethod);
        final JLabel label7 = new JLabel();
        label7.setText("}");
        panel7.add(
            label7, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("canConvert", panel8);
        final JLabel label8 = new JLabel();
        label8.setText("public boolean canConvert(byte[] value) { // checks correctness of the value");
        panel8.add(
            label8, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane5 = new JScrollPane();
        panel8.add(
            scrollPane5, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 250), null, 0, false));
        taCanConvertMethod = new JTextArea();
        taCanConvertMethod.setText("   return true; // replace with your code");
        scrollPane5.setViewportView(taCanConvertMethod);
        final JLabel label9 = new JLabel();
        label9.setText("}");
        panel8.add(
            label9, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Highlight", panel9);
        final JLabel label10 = new JLabel();
        label10.setText("public Map<Pattern, Color> getColorMappings() { // regex to color mapping");
        panel9.add(
            label10, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane6 = new JScrollPane();
        panel9.add(
            scrollPane6, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 250), null, 0, false));
        taColorMappings = new JTextArea();
        taColorMappings.setText(
            "// Example of XML highlighting. The regular expression must be inside the brackets () to form the group.\n//\n// Map<Pattern, Color> map = new HashMap<Pattern, Color>();\n// map.put(Pattern.compile(\"(</?[a-zA-Z]*)\\\\s?>?\"), new Color(63, 127, 127)); -- start tag\n// map.put(Pattern.compile(\"\\\\s(\\\\w*)=\"), new Color(127, 0, 127)); -- attr tag\n// map.put(Pattern.compile(\"(/>)\"), new Color(63, 127, 127)); -- end tag\n// map.put(Pattern.compile(\"[a-zA-Z-]*=(\\\"[^\\\"]*\\\")\"), new Color(42, 0, 255)); -- attr value\n// map.put(Pattern.compile(\"(<!--.*-->)\"), new Color(63, 95, 191)); -- comment\n//        \n// return map;\n\n   return null; // replace with your code");
        taColorMappings.setToolTipText("");
        scrollPane6.setViewportView(taColorMappings);
        final JLabel label11 = new JLabel();
        label11.setText("}");
        panel9.add(
            label11, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Format", panel10);
        final JLabel label12 = new JLabel();
        label12.setText("public String format(String value) { // apply custom formatting of the data");
        panel10.add(
            label12, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JScrollPane scrollPane7 = new JScrollPane();
        panel10.add(
            scrollPane7, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 250), null, 0, false));
        taFormat = new JTextArea();
        taFormat.setText("   return value; // replace with your code");
        scrollPane7.setViewportView(taFormat);
        final JLabel label13 = new JLabel();
        label13.setText("}");
        panel10.add(
            label13, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        chbNameConverter = new JCheckBox();
        chbNameConverter.setSelected(false);
        chbNameConverter.setText("The converter can be used for column name conversions as well.");
        contentPane.add(
            chbNameConverter, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label1.setLabelFor(tfConverterName);
        label2.setLabelFor(taImports);
        label3.setLabelFor(taImports);
        label4.setLabelFor(taBytesToObjectMethod);
        label6.setLabelFor(taObjectToBytesMethod);
        label8.setLabelFor(taObjectToBytesMethod);
        label10.setLabelFor(taBytesToObjectMethod);
        label12.setLabelFor(taBytesToObjectMethod);
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
        StringToBytes,
        CanConvert
    }
}
