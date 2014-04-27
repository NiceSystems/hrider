package hrider.ui.controls.format;

import hrider.converters.TypeConverter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
 *          This class represents an editor for a custom data formatter. The editor is used primarily with JTable.
 */
public class FormatEditor extends JPanel {

    //region Constants
    private static final long serialVersionUID = -6427009457813292664L;
    //endregion

    //region Variables
    /**
     * A text field that is shown within the cell.
     */
    private JTextField     textField;
    /**
     * A text pane that is shown in a popup menu and presents the data as a formatted string.
     */
    private FormatTextPane textPane;
    /**
     * This button is clicked when he changes need to be saved.
     */
    private JButton        saveButton;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link FormatEditor} class.
     */
    public FormatEditor(final CellEditor cellEditor, boolean canEdit) {
        textPane = new FormatTextPane();
        textPane.setEditable(canEdit);
        textPane.setLayout(new BorderLayout());

        textField = new JTextField();
        textField.setLayout(new BorderLayout());
        textField.setBorder(new EmptyBorder(0, 0, 0, 0));
        textField.setEditable(false);

        setLayout(new BorderLayout());

        final JPopupMenu popup = new JPopupMenu();

        final JScrollPane pane = new JScrollPane(textPane);
        pane.setBorder(BorderFactory.createEtchedBorder());

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        saveButton = new JButton("Mark for save");
        saveButton.setEnabled(canEdit);
        saveButton.setPreferredSize(new Dimension(115, 24));
        saveButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        popup.setVisible(false);

                        textField.setText(textPane.getText());
                        cellEditor.stopCellEditing();
                    }
                    catch (Exception ex) {
                        JOptionPane.showMessageDialog(FormatEditor.this, ex.getMessage(), "Invalid data", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(75, 24));
        cancelButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    popup.setVisible(false);
                }
            });

        buttonsPanel.add(new JSeparator());
        buttonsPanel.add(saveButton);
        buttonsPanel.add(cancelButton);

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.setBorder(new EmptyBorder(1, 1, 1, 1));
        content.add(pane, BorderLayout.CENTER);
        content.add(buttonsPanel, BorderLayout.SOUTH);

        popup.setLayout(new BorderLayout());
        popup.setBorder(BorderFactory.createEtchedBorder());
        popup.add(content, BorderLayout.CENTER);

        JButton button = new JButton("...");
        button.setFocusable(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dimension size = pane.getPreferredSize();
                    Rectangle screenSize = getGraphicsConfiguration().getBounds();

                    int width = (int)Math.min(screenSize.getWidth(), size.getWidth() + 35);
                    int height = (int)Math.min(screenSize.getHeight(), size.getHeight() + 45);

                    width = Math.max(250, width);
                    height = Math.max(150, height);

                    popup.setPopupSize(width, height);
                    popup.show(FormatEditor.this, 0, getHeight());
                }
            });

        add(textField, BorderLayout.CENTER);
        add(button, BorderLayout.EAST);

        invalidate();
        repaint();
    }
    //endregion

    //region Public Methods

    /**
     * Gets the original or modified text.
     *
     * @return An original or modified text.
     */
    public String getText() {
        return textPane.getText();
    }

    /**
     * Sets the new text into the editor.
     *
     * @param text The new text to set.
     */
    public void setText(String text) {
        textField.setText(text);
        textPane.setText(text);
    }

    /**
     * Sets the value indicating if the text can be edited.
     *
     * @param editable The value to set.
     */
    public void setEditable(Boolean editable) {
        textPane.setEditable(editable);
        saveButton.setEnabled(editable);
    }

    /**
     * Sets the type converter to format the data.
     *
     * @param typeConverter The type converter to set.
     */
    public void setTypeConverter(TypeConverter typeConverter) {
        textPane.setTypeConverter(typeConverter);
    }
    //endregion
}