package hrider.ui.design;

import hrider.data.CheckedRow;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;

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
 *          This class represents a custom renderer for {@link JCheckBox} component to be used in the {@link JTable}.
 *          The main purpose of this class is to provide a cell editor for cells that hold boolean values.
 *          In addition this class provide an option to mark some of the cells to always remain checked or
 *          in other words to ensure that some of the cells cannot be unchecked.
 */
public class JCheckBoxRenderer extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

    //region Variables
    private Collection<CheckedRow> checkedRows;
    private JCheckBox              component;
    private boolean                isRepresentedAsString;
    //endregion

    //region Constructor
    public JCheckBoxRenderer(CheckedRow... checkedRows) {
        this.isRepresentedAsString = false;
        this.checkedRows = Arrays.asList(checkedRows);
        this.component = new JCheckBox();
        this.component.setHorizontalAlignment(SwingConstants.CENTER);
        this.component.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                stopCellEditing();
            }
        });
    }
    //endregion

    //region Public Methods
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
    //endregion

    //region Private Methods
    private boolean isEditable(JTable table, int row) {
        for (CheckedRow checkedRow : this.checkedRows) {
            Object obj = table.getValueAt(row, checkedRow.getColumnIndex());
            if (checkedRow.getExpectedValue().equals(obj)) {
                return false;
            }
        }
        return true;
    }

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
    //endregion
}
