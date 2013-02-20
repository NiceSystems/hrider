package hrider.ui.design;

import com.michaelbaranov.microba.calendar.DatePicker;
import hrider.config.GlobalConfig;
import hrider.data.DataCell;
import hrider.data.ObjectType;
import hrider.ui.ChangeTracker;
import hrider.ui.controls.json.JsonEditor;
import hrider.ui.controls.xml.XmlEditor;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.beans.PropertyVetoException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    private static final long serialVersionUID = -2190137522499893284L;
    //endregion

    //region Variables
    private JTextField    textEditor;
    private DatePicker    dateEditor;
    private DatePicker    jodaDateEditor;
    private XmlEditor     xmlEditor;
    private JsonEditor    jsonEditor;
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
        this.textEditor = new JTextField();
        this.textEditor.setBorder(BorderFactory.createEmptyBorder());
        this.textEditor.setEditable(canEdit);
        this.dateEditor = new DatePicker(null);
        this.dateEditor.setBorder(BorderFactory.createEmptyBorder());
        this.dateEditor.setDateFormat(new SimpleDateFormat(GlobalConfig.instance().getDateFormat(), Locale.ENGLISH));
        this.dateEditor.setFieldEditable(canEdit);
        this.jodaDateEditor = new DatePicker(null);
        this.jodaDateEditor.setBorder(BorderFactory.createEmptyBorder());
        this.jodaDateEditor.setDateFormat(new SimpleDateFormat(GlobalConfig.instance().getJodaDateFormat(), Locale.ENGLISH));
        this.jodaDateEditor.setFieldEditable(canEdit);
        this.xmlEditor = new XmlEditor();
        this.xmlEditor.setEditable(canEdit);
        this.jsonEditor = new JsonEditor();
        this.jsonEditor.setEditable(canEdit);
    }
    //endregion

    //region Public Methods

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

        this.editorType = EditorType.Text;
        ObjectType type = ObjectType.String;

        // Check if the value contains information regarding its type.
        if (value instanceof DataCell) {
            this.cell = (DataCell)value;
            type = this.cell.getTypedValue().getType();
        }
        else {
            this.cell = null;
        }

        if (this.typeColumn != -1) {
            type = (ObjectType)table.getValueAt(row, this.typeColumn);
        }

        if (type == ObjectType.DateTime) {
            this.editorType = EditorType.Date;
        }
        else if (type == ObjectType.JodaDateTime) {
            this.editorType = EditorType.JodaDate;
        }
        else if (type == ObjectType.Xml) {
            this.editorType = EditorType.Xml;
        }
        else if (type == ObjectType.Json) {
            this.editorType = EditorType.Json;
        }

        initializeEditor(value);

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

        switch (this.editorType) {
            case Date:
                Date date = this.dateEditor.getDate();
                if (date != null) {
                    DateFormat df = new SimpleDateFormat(GlobalConfig.instance().getDateFormat(), Locale.ENGLISH);
                    text = df.format(date);
                }
                break;
            case JodaDate:
                // Try parsing it, just to make sure it won't get accidentally set to null
                // if there's text but it has a typo.
                Date jodaDate = this.jodaDateEditor.getDate();
                if (jodaDate != null) {
                    String pattern = GlobalConfig.instance().getJodaDateFormat();
                    DateTimeFormatter jodaDF = DateTimeFormat.forPattern(pattern);
                    jodaDF.withOffsetParsed();
                    text = jodaDF.print(new DateTime(jodaDate));
                }
                break;
            case Text:
                text = this.textEditor.getText().trim();
                break;
            case Xml:
                text = this.xmlEditor.getText().trim();
                break;
            case Json:
                text = this.jsonEditor.getText().trim();
                break;
        }

        if (text != null && text.isEmpty()) {
            text = null;
        }

        if (this.cell != null) {
            Object value = this.cell.toObject(text);
            if (value == null || !this.cell.contains(value)) {
                this.cell.getTypedValue().setValue(value);

                if (this.changeTracker != null) {
                    this.changeTracker.addChange(this.cell);
                }
            }
            return this.cell;
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
    private void initializeEditor(Object value) {
        switch (this.editorType) {
            case Date:
                try {
                    if (this.cell != null) {
                        this.dateEditor.setDate((Date)this.cell.getTypedValue().getValue());
                    }
                }
                catch (PropertyVetoException ignore) {
                }
                break;
            case JodaDate:
                try {
                    if (this.cell != null) {
                        this.jodaDateEditor.setDate(((DateTime)this.cell.getTypedValue().getValue()).toDate());
                    }
                }
                catch (PropertyVetoException ignore) {
                }
                break;
            case Text:
                if (value != null) {
                    this.textEditor.setText(value.toString());
                }
                else {
                    this.textEditor.setText(null);
                }
                break;
            case Xml:
                if (value != null) {
                    this.xmlEditor.setText(value.toString());
                }
                else {
                    this.xmlEditor.setText(null);
                }
                break;
            case Json:
                if (value != null) {
                    this.jsonEditor.setText(value.toString());
                }
                else {
                    this.jsonEditor.setText(null);
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
        switch (this.editorType) {
            case Date:
                return this.dateEditor;
            case JodaDate:
                return this.jodaDateEditor;
            case Text:
                return this.textEditor;
            case Xml:
                return this.xmlEditor;
            case Json:
                return this.jsonEditor;
        }
        return this.textEditor;
    }
    //endregion
}
