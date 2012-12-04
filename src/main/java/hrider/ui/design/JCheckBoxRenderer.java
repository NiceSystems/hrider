package hrider.ui.design;

import hrider.data.RequiredRow;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/23/12
 * Time: 5:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class JCheckBoxRenderer extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

    private Collection<RequiredRow> requiredRows;
    private JCheckBox component;
    private boolean isRepresentedAsString;

    public JCheckBoxRenderer(RequiredRow... requiredRows) {
        this.isRepresentedAsString = false;
        this.requiredRows = Arrays.asList(requiredRows);
        this.component = new JCheckBox();
        this.component.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if (isSelected) {
            this.component.setForeground(table.getSelectionForeground());
            this.component.setBackground(table.getSelectionBackground());
        }
        else {
            this.component.setForeground(table.getForeground());
            this.component.setBackground(table.getBackground());
        }

        this.component.setSelected(toBoolean(value));
        return this.component;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

        this.component.setSelected(toBoolean(value));
        return this.component;
    }

    @Override
    public Object getCellEditorValue() {
        if (this.isRepresentedAsString) {
            return this.component.isSelected() ? "true" : "false";
        }
        return this.component.isSelected();
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent)e;
            JTable table = (JTable)e.getSource();

            return isEditable(table, table.rowAtPoint(mouseEvent.getPoint()));
        }
        return false;
    }

    private boolean isEditable(JTable table, int row) {
        for (RequiredRow requiredRow : this.requiredRows) {
            Object obj = table.getValueAt(row, requiredRow.getColumnIndex());
            if (requiredRow.getExpectedValue().equals(obj)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            this.isRepresentedAsString = false;
            return (Boolean)value;
        }
        if (value instanceof String) {
            this.isRepresentedAsString = true;
            return Boolean.parseBoolean((String)value);
        }
        return false;
    }
}
