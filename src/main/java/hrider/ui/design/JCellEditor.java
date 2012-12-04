package hrider.ui.design;

import com.michaelbaranov.microba.calendar.DatePicker;
import hrider.config.Configurator;
import hrider.data.DataCell;
import hrider.data.ObjectType;
import hrider.ui.ChangeTracker;
import hrider.ui.controls.json.JsonEditor;
import hrider.ui.controls.xml.XmlEditor;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.beans.PropertyVetoException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/26/12
 * Time: 2:02 PM
 */
@SuppressWarnings("SerializableHasSerializationMethods")
public class JCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final long serialVersionUID = -2190137522499893284L;

    private JTextField    textEditor;
    private DatePicker    dateEditor;
    private XmlEditor     xmlEditor;
    private JsonEditor    jsonEditor;
    private DataCell      cell;
    private ChangeTracker changeTracker;
    private int           typeColumn;
    private EditorType    editorType;

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
        this.dateEditor.setDateFormat(new SimpleDateFormat(Configurator.getDateFormat(), Locale.ENGLISH));
        this.dateEditor.setFieldEditable(canEdit);
        this.xmlEditor = new XmlEditor();
        this.xmlEditor.setEditable(canEdit);
        this.jsonEditor = new JsonEditor();
        this.jsonEditor.setEditable(canEdit);
    }

    // This method is called when a cell value is edited by the user.
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
        else if (type == ObjectType.Xml) {
            this.editorType = EditorType.Xml;
        }
        else if (type == ObjectType.Json) {
            this.editorType = EditorType.Json;
        }

        initializeEditor(value);

        return getEditor();
    }

    // This method is called when editing is completed.
    // It must return the new value to be stored in the cell.
    @Override
    public Object getCellEditorValue() {
        String text = null;

        switch (this.editorType) {
            case Date:
                Date date = this.dateEditor.getDate();
                if (date != null) {
                    DateFormat df = new SimpleDateFormat(Configurator.getDateFormat(), Locale.ENGLISH);
                    text = df.format(date);
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
                this.changeTracker.addChange(this.cell);
            }
            return this.cell;
        }

        return text;
    }

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

    private Component getEditor() {
        switch (this.editorType) {
            case Date:
                return this.dateEditor;
            case Text:
                return this.textEditor;
            case Xml:
                return this.xmlEditor;
            case Json:
                return this.jsonEditor;
        }
        return this.textEditor;
    }
}
