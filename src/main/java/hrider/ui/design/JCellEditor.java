package hrider.ui.design;

import com.michaelbaranov.microba.calendar.DatePicker;
import hrider.data.ColumnType;
import hrider.data.DataCell;
import hrider.format.DateUtils;
import hrider.io.Log;
import hrider.ui.ChangeTracker;
import hrider.ui.controls.format.FormatEditor;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.Date;

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
 *          This class represents a cell editor which supports different data types to be edited in the {@link JTable}.
 *          The list of supported data types can be found here {@link EditorType}.
 */
public class JCellEditor extends AbstractCellEditor implements TableCellEditor {

    //region Constants
    private static final Log  logger           = Log.getLogger(JCellEditor.class);
    private static final long serialVersionUID = -2190137522499893284L;
    //endregion

    //region Variables
    private JTextField    textEditor;
    private DatePicker    dateEditor;
    private FormatEditor  formatEditor;
    private DataCell      cell;
    private ChangeTracker changeTracker;
    private int           typeColumn;
    private EditorType    editorType;
    //endregion

    //region Constructor
    public JCellEditor(ChangeTracker changeTracker, boolean canEdit) {
        this(changeTracker, -1, canEdit);
    }

    public JCellEditor(ChangeTracker changeTracker, int typeColumn, boolean canEdit) {
        this.changeTracker = changeTracker;
        this.typeColumn = typeColumn;

        textEditor = new JTextField();
        textEditor.setBorder(BorderFactory.createEmptyBorder());
        textEditor.setEditable(canEdit);
        dateEditor = new DatePicker(null);
        dateEditor.setBorder(BorderFactory.createEmptyBorder());
        dateEditor.setDateFormat(DateUtils.getDefaultDateFormat());
        dateEditor.setFieldEditable(canEdit);
        formatEditor = new FormatEditor(this, canEdit);
    }
    //endregion

    //region Public Methods

    /**
     * Sets the editor to be editable or not.
     *
     * @param editable True if the editor should be editable or False otherwise.
     */
    public void setEditable(boolean editable) {
        textEditor.setEditable(editable);
        dateEditor.setFieldEditable(editable);
        formatEditor.setEditable(editable);
    }

    /**
     * This method is called when a cell value is edited by the user.
     *
     * @param table      The table that owns the cell.
     * @param value      The value to be edited.
     * @param isSelected Indicates if the cell is selected.
     * @param row        The row number.
     * @param column     The column number.
     * @return The component used to edit the value.
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

        editorType = EditorType.Text;
        ColumnType type = ColumnType.String;

        // Check if the value contains information regarding its type.
        if (value instanceof DataCell) {
            cell = (DataCell)value;
            type = cell.getType();
        }
        else {
            cell = null;
        }

        if (typeColumn != -1) {
            type = (ColumnType)table.getValueAt(row, typeColumn);
        }

        if (type.equals(ColumnType.DateAsString) || type.equals(ColumnType.DateAsLong)) {
            editorType = EditorType.Date;
        }
        else if (type.getConverter().supportsFormatting()) {
            editorType = EditorType.Format;
        }

        initializeEditor(type, value);

        return getEditor();
    }

    /**
     * This method is called when editing is completed.
     * It must return the new value to be stored in the cell.
     *
     * @return A new value to be stored in the cell
     */
    @Override
    public Object getCellEditorValue() {
        String text = null;

        switch (editorType) {
            case Date:
                Date date = dateEditor.getDate();
                if (date != null) {
                    text = DateUtils.format(date);
                }
                break;
            case Text:
                text = textEditor.getText();
                break;
            case Format:
                text = formatEditor.getText();
                break;
        }

        if (text != null && text.isEmpty()) {
            text = null;
        }

        if (cell != null) {
            if (!cell.hasValue(text)) {
                cell.setValue(text);

                if (changeTracker != null) {
                    changeTracker.addChange(cell);
                }
            }
            return cell;
        }

        return text;
    }
    //endregion

    //region Private Methods

    /**
     * Initializes an editor with the provided value.
     *
     * @param value The value to pass to the editor.
     */
    private void initializeEditor(ColumnType type, Object value) {
        switch (editorType) {
            case Date:
                if (cell != null) {
                    try {
                        Date date = DateUtils.parse(cell.getValue());
                        dateEditor.setDate(date);
                    }
                    catch (Exception e) {
                        logger.error(e, "Failed to convert value to Date.", value);
                    }
                }
                break;
            case Text:
                if (value != null) {
                    textEditor.setText(value.toString());
                }
                else {
                    textEditor.setText(null);
                }
                break;
            case Format:
                if (value != null) {
                    formatEditor.setTypeConverter(type.getConverter());
                    formatEditor.setText(value.toString());
                }
                else {
                    formatEditor.setText(null);
                }
                break;
        }
    }

    /**
     * Gets a correct editor according to the editor type.
     *
     * @return The component used to edit the value.
     */
    private Component getEditor() {
        switch (editorType) {
            case Date:
                return dateEditor;
            case Text:
                return textEditor;
            case Format:
                return formatEditor;
        }
        return textEditor;
    }
    //endregion
}
