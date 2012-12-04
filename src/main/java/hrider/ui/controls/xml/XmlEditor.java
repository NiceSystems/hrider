package hrider.ui.controls.xml;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 04/11/12
 * Time: 10:32
 */
public class XmlEditor extends JPanel {

    private static final long serialVersionUID = -6427009457813292664L;

    private JTextField  textField;
    private XmlTextPane textPane;

    public XmlEditor() {

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

        JButton saveButton = new JButton("Save");
        saveButton.setPreferredSize(new Dimension(75, 24));
        saveButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        popup.setVisible(false);
                        XmlEditor.this.textPane.validateXml();

                        XmlEditor.this.textField.setText(XmlEditor.this.textPane.getText());
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

    public String getText() {
        return this.textField.getText();
    }

    public void setText(String text) {
        this.textField.setText(text);
        this.textPane.setText(text);
    }

    public void setEditable(Boolean editable) {
        this.textPane.setEditable(editable);
    }
}
