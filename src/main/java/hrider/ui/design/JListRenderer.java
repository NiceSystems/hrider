package hrider.ui.design;

import hrider.hbase.Connection;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class JListRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 8219559461829225540L;

    private Connection connection;

    public JListRenderer(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof String) {
            try {
                boolean enabled = connection.tableEnabled((String)value);
                if (!enabled) {
                    setForeground(Color.gray);
                }
            }
            catch (IOException ignore) {
            }
        }
        return component;
    }
}
