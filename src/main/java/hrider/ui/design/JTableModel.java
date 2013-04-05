package hrider.ui.design;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.util.Arrays;
import java.util.List;

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
 */
public class JTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 7602909046413522975L;
    private List<Integer> editableColumns;

    public JTableModel(Integer... editableColumns) {
        this.editableColumns = Arrays.asList(editableColumns);
    }

    /**
     * Stops editing of the cell if there is any.
     *
     * @param table The table that contains the cell.
     */
    public static void stopCellEditing(JTable table) {
        if (table.getRowCount() > 0) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return editableColumns.contains(column);
    }
}
