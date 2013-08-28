package hrider.ui.controls.xml;

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
 *          This class represents an editor for a data saved as a XML in the hbase. The editor is used primarily with JTable.
 */
public class XmlEditor extends JPanel {

    //region Constants
    private static final long serialVersionUID = -6427009457813292664L;
    //endregion

    //region Variables
    /**
     * A text field that is shown within the cell.
     */
    private JTextField  textField;
    /**
     * A text pane that is shown in a popup menu and presents the XML as a formatted string.
     */
    private XmlTextPane textPane;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link XmlEditor} class.
     */
    public XmlEditor(final CellEditor cellEditor) {

        this.textPane = new XmlTextPane();
        this.textPane.setLayout(new BorderLayout());

        this.textField = new JTextField();
        this.textField.setLayout(new BorderLayout());
        this.textField.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.textField.setEditable(false);

        this.setLayout(new BorderLayout());

        final JPopupMenu popup = new JPopupMenu();

        final JScrollPane pane = new JScrollPane(this.textPane);
        pane.setBorder(BorderFactory.createEtchedBorder());

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JButton saveButton = new JButton("Mark for save");
        saveButton.setPreferredSize(new Dimension(115, 24));
        saveButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        popup.setVisible(false);
                        XmlEditor.this.textPane.validateXml();

                        XmlEditor.this.textField.setText(XmlEditor.this.textPane.getText());
                        cellEditor.stopCellEditing();
                    }
                    catch (Exception ex) {
                        JOptionPane.showMessageDialog(XmlEditor.this, ex.getMessage(), "Invalid XML", JOptionPane.ERROR_MESSAGE);
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
                    Rectangle screenSize = XmlEditor.this.getGraphicsConfiguration().getBounds();

                    int width = (int)Math.min(screenSize.getWidth(), size.getWidth() + 35);
                    int height = (int)Math.min(screenSize.getHeight(), size.getHeight() + 45);

                    width = Math.max(200, width);
                    height = Math.max(150, height);

                    popup.setPopupSize(width, height);
                    popup.show(XmlEditor.this, 0, XmlEditor.this.getHeight());
                }
            });

        this.add(this.textField, BorderLayout.CENTER);
        this.add(button, BorderLayout.EAST);

        this.invalidate();
        this.repaint();
    }
    //endregion

    //region Public Methods

    /**
     * Gets the original or modified text.
     *
     * @return An original or modified text.
     */
    public String getText() {
        return this.textField.getText();
    }

    /**
     * Sets the new text into the editor.
     *
     * @param text The new text to set.
     */
    public void setText(String text) {
        this.textField.setText(text);
        this.textPane.setText(text);
    }

    /**
     * Sets the value indicating if the text can be edited.
     *
     * @param editable The value to set.
     */
    public void setEditable(Boolean editable) {
        this.textPane.setEditable(editable);
    }
    //endregion
}
