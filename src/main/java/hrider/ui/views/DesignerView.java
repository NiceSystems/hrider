package hrider.ui.views;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import hrider.actions.Action;
import hrider.actions.RunnableAction;
import hrider.config.ClusterConfig;
import hrider.config.GlobalConfig;
import hrider.converters.ConvertersLoader;
import hrider.converters.TypeConverter;
import hrider.data.*;
import hrider.export.FileExporter;
import hrider.filters.EmptyFilter;
import hrider.filters.Filter;
import hrider.filters.PatternFilter;
import hrider.hbase.Connection;
import hrider.hbase.HbaseActionListener;
import hrider.hbase.Query;
import hrider.hbase.QueryScanner;
import hrider.io.PathHelper;
import hrider.system.ClipboardData;
import hrider.system.ClipboardListener;
import hrider.system.InMemoryClipboard;
import hrider.ui.ChangeTracker;
import hrider.ui.ChangeTrackerListener;
import hrider.ui.MessageHandler;
import hrider.ui.UIAction;
import hrider.ui.controls.WideComboBox;
import hrider.ui.design.*;
import hrider.ui.forms.*;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
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
 *          This class is a main view.
 */
@SuppressWarnings({"MagicNumber", "AnonymousInnerClassWithTooManyMethods"})
public class DesignerView {

    //region Variables
    private JPanel                            topPanel;
    private JList                             tablesList;
    private DefaultListModel                  tablesListModel;
    private JTable                            rowsTable;
    private JButton                           columnPopulate;
    private JTable                            columnsTable;
    private JButton                           rowSave;
    private JButton                           rowDelete;
    private JButton                           rowAdd;
    private JButton                           tableTruncate;
    private JButton                           columnScan;
    private JButton                           tableAdd;
    private JButton                           tableDelete;
    private JSpinner                          rowsNumber;
    private JButton                           rowsPrev;
    private JButton                           rowsNext;
    private JLabel                            rowsTotal;
    private JLabel                            rowsVisible;
    private JButton                           columnCheck;
    private JButton                           rowCopy;
    private JButton                           rowPaste;
    private JButton                           tableRefresh;
    private JButton                           tableCopy;
    private JButton                           tablePaste;
    private JButton                           columnUncheck;
    private JSplitPane                        topSplitPane;
    private JSplitPane                        innerSplitPane;
    private JLabel                            columnsNumber;
    private JLabel                            tablesNumber;
    private JComboBox                         tableFilters;
    private DefaultComboBoxModel              tablesFilterModel;
    private JComboBox                         columnFilters;
    private DefaultComboBoxModel              columnsFilterModel;
    private ItemListener                      columnsFilterListener;
    private JButton                           columnJump;
    private JButton                           rowOpen;
    private JButton                           tableImport;
    private JButton                           tableExport;
    private JButton                           columnRefresh;
    private JButton                           tableFlush;
    private JButton                           tableMetadata;
    private JButton                           columnAddConverter;
    private JButton                           columnEditConverter;
    private JButton                           columnDeleteConverter;
    private WideComboBox                      columnConverters;
    private JLabel                            rowsNumberIcon;
    private DefaultTableModel                 columnsTableModel;
    private DefaultTableModel                 rowsTableModel;
    private Query                             lastQuery;
    private QueryScanner                      scanner;
    private JPanel                            owner;
    private Connection                        connection;
    private ChangeTracker                     changeTracker;
    private Map<ColumnQualifier, TableColumn> rowsTableRemovedColumns;
    private ClusterConfig                     clusterConfig;
    private JComboBox                         cmbColumnTypes;
    private RunnableAction                    rowsCountAction;
    //endregion

    //region Constructor
    public DesignerView(final JPanel owner, final Connection connection) {

        this.owner = owner;
        this.connection = connection;
        this.changeTracker = new ChangeTracker();
        this.rowsTableRemovedColumns = new HashMap<ColumnQualifier, TableColumn>();
        this.clusterConfig = new ClusterConfig(this.connection.getServerName());
        this.clusterConfig.setConnection(connection.getConnectionDetails());
        this.tablesFilterModel = new DefaultComboBoxModel();
        this.tableFilters.setModel(this.tablesFilterModel);
        this.columnsFilterModel = new DefaultComboBoxModel();
        this.columnFilters.setModel(this.columnsFilterModel);

        fillComboBox(tableFilters, null, this.clusterConfig.getTableFilters());

        String tableFilter = this.clusterConfig.getSelectedTableFilter();
        if (tableFilter != null) {
            this.tableFilters.setSelectedItem(tableFilter);
        }

        InMemoryClipboard.addListener(
            new ClipboardListener() {
                @Override
                public void onChanged(ClipboardData data) {
                    tablePaste.setEnabled(hasTableInClipboard());
                    rowPaste.setEnabled(hasRowsInClipboard());
                }
            });

        initializeTablesList();
        initializeColumnsTable();
        initializeRowsTable();

        // Customize look of the dividers that they will look of the same size and without buttons.
        BasicSplitPaneDivider dividerContainer = (BasicSplitPaneDivider)this.topSplitPane.getComponent(0);
        dividerContainer.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        dividerContainer.setLayout(new BorderLayout());

        dividerContainer = (BasicSplitPaneDivider)this.innerSplitPane.getComponent(0);
        dividerContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        dividerContainer.setLayout(new BorderLayout());

        this.rowsNumber.setModel(new SpinnerNumberModel(100, 1, 10000, 100));

        this.tableFilters.addItemListener(
            new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        clusterConfig.setSelectedTableFilter((String)e.getItem());

                        loadTables();
                    }
                }
            });

        this.tableFilters.getEditor().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String item = (String)tableFilters.getEditor().getItem();
                    if (item != null && !item.isEmpty()) {
                        if (tablesFilterModel.getIndexOf(item) == -1) {
                            tableFilters.addItem(item);
                        }

                        tableFilters.setSelectedItem(item);
                        clusterConfig.setTablesFilter(getFilters(tableFilters));
                    }
                    else {
                        Object selectedItem = tableFilters.getSelectedItem();
                        if (selectedItem != null) {
                            tableFilters.removeItem(selectedItem);
                            clusterConfig.setTablesFilter(getFilters(tableFilters));
                        }
                    }
                }
            });

        this.columnsFilterListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    clusterConfig.setSelectedColumnFilter(getSelectedTableName(), (String)e.getItem());

                    populateColumnsTable(false);
                }
            }
        };

        this.columnFilters.addItemListener(this.columnsFilterListener);

        this.columnFilters.getEditor().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String item = (String)columnFilters.getEditor().getItem();
                    if (item != null && !item.isEmpty()) {
                        if (columnsFilterModel.getIndexOf(item) == -1) {
                            columnFilters.addItem(item);
                        }

                        columnFilters.setSelectedItem(item);
                        clusterConfig.setColumnsFilter(getSelectedTableName(), getFilters(columnFilters));
                    }
                    else {
                        Object selectedItem = columnFilters.getSelectedItem();
                        if (selectedItem != null) {
                            columnFilters.removeItem(selectedItem);
                            clusterConfig.setColumnsFilter(getSelectedTableName(), getFilters(columnFilters));
                        }
                    }
                }
            });

        this.columnConverters.addItemListener(
            new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        TypeConverter nameConverter = getColumnNameConverter();
                        updateColumnNameConverter(nameConverter);

                        boolean isEditable = nameConverter.isEditable();

                        int row = columnsTable.getSelectedRow();
                        if (row != -1) {
                            ColumnType type = (ColumnType)columnsTable.getValueAt(row, 2);
                            isEditable |= type.isEditable();
                        }

                        clusterConfig.setTableConfig(getSelectedTableName(), "nameConverter", nameConverter.getName());

                        columnEditConverter.setEnabled(isEditable);
                        columnDeleteConverter.setEnabled(isEditable);
                    }
                }
            });

        this.connection.addListener(
            new HbaseActionListener() {
                @Override
                public void copyOperation(String source, String sourceTable, String target, String targetTable, Result result) {
                    setInfo(
                        String.format(
                            "Copying row '%s' from '%s.%s' to '%s.%s'", Bytes.toStringBinary(result.getRow()), source, sourceTable, target, targetTable));
                }

                @Override
                public void saveOperation(String tableName, String path, Result result) {
                    setInfo(String.format("Saving row '%s' from table '%s' to file '%s'", Bytes.toStringBinary(result.getRow()), tableName, path));
                }

                @Override
                public void loadOperation(String tableName, String path, Put put) {
                    setInfo(String.format("Loading row '%s' to table '%s' from file '%s'", Bytes.toStringBinary(put.getRow()), tableName, path));
                }

                @Override
                public void tableOperation(String tableName, String operation) {
                    setInfo(String.format("The %s table has been %s", tableName, operation));
                }

                @Override
                public void rowOperation(String tableName, DataRow row, String operation) {
                    setInfo(
                        String.format(
                            "The %s row has been %s %s the %s table", row.getKey(), operation, "added".equals(operation) ? "to" : "from", tableName));
                }

                @Override
                public void columnOperation(String tableName, String column, String operation) {
                    setInfo(
                        String.format(
                            "The %s column has been %s %s the %s table", column, operation, "added".equals(operation) ? "to" : "from", tableName));
                }
            });

        this.columnJump.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    String rowNumber = JOptionPane.showInputDialog(
                        topPanel, "Row number:", "Jump to specific row", JOptionPane.PLAIN_MESSAGE);

                    if (rowNumber != null) {
                        try {
                            long offset = Long.parseLong(rowNumber);

                            lastQuery = null;

                            populateRowsTable(offset, Direction.Current);
                        }
                        catch (NumberFormatException ignore) {
                            JOptionPane.showMessageDialog(topPanel, "Row number must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

        this.columnPopulate.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    lastQuery = null;

                    populateRowsTable(Direction.Current);
                }
            });

        this.columnScan.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    ScanDialog dialog = new ScanDialog(lastQuery, getShownTypedColumns());
                    dialog.showDialog(topPanel);

                    lastQuery = dialog.getQuery();
                    if (lastQuery != null) {
                        populateRowsTable(Direction.Current);
                    }
                }
            });

        this.rowOpen.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        File tempFile = File.createTempFile("h-rider", GlobalConfig.instance().getExternalViewerFileExtension());
                        FileOutputStream stream = null;

                        try {
                            stream = new FileOutputStream(tempFile);
                            FileExporter exporter = new FileExporter(stream, GlobalConfig.instance().getExternalViewerDelimeter());

                            List<ColumnQualifier> columns = getShownColumns();

                            for (int i = 0 ; i < rowsTable.getRowCount() ; i++) {
                                exporter.write(((DataCell)rowsTable.getValueAt(i, 0)).getRow(), columns);
                            }
                        }
                        finally {
                            if (stream != null) {
                                try {
                                    stream.close();
                                }
                                catch (IOException ignore) {
                                }
                            }
                        }

                        Desktop.getDesktop().open(tempFile);
                    }
                    catch (Exception ex) {
                        setError("Failed to open rows in external viewer: ", ex);
                    }
                    finally {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

        this.rowAdd.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    JTableModel.stopCellEditing(columnsTable);
                    JTableModel.stopCellEditing(rowsTable);

                    try {
                        Collection<ColumnFamily> columnFamilies = connection.getColumnFamilies(getSelectedTableName());

                        AddRowDialog dialog = new AddRowDialog(getShownTypedColumns(), columnFamilies);
                        if (dialog.showDialog(topPanel)) {
                            DataRow row = dialog.getRow();

                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            try {
                                connection.setRow(getSelectedTableName(), row);

                                if (scanner != null) {
                                    scanner.resetCurrent(row.getKey());
                                }

                                // Update the column types according to the added row.
                                for (DataCell cell : row.getCells()) {
                                    clusterConfig.setTableConfig(
                                        getSelectedTableName(), cell.getColumn().getFullName(), cell.getType().toString());
                                }

                                populateColumnsTable(true, row);
                                populateRowsTable(Direction.Current);
                            }
                            catch (Exception ex) {
                                setError("Failed to update rows in HBase: ", ex);
                            }
                            finally {
                                owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            }
                        }
                    }
                    catch (Exception ex) {
                        setError(String.format("Failed to get column families for table '%s'.", getSelectedTableName()), ex);
                    }
                }
            });

        this.rowDelete.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    JTableModel.stopCellEditing(rowsTable);

                    int decision = JOptionPane.showConfirmDialog(
                        topPanel, "Are you sure you want to delete the selected rows?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);

                    if (decision == JOptionPane.OK_OPTION) {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            int[] selectedRows = rowsTable.getSelectedRows();
                            for (int selectedRow : selectedRows) {
                                try {
                                    DataCell key = (DataCell)rowsTable.getValueAt(selectedRow, 0);
                                    connection.deleteRow(getSelectedTableName(), key.getRow());
                                }
                                catch (Exception ex) {
                                    setError("Failed to delete row in HBase: ", ex);
                                }
                            }

                            if (scanner != null) {
                                scanner.resetCurrent(null);
                            }

                            populateColumnsTable(true);
                            populateRowsTable(Direction.Current);
                        }
                        finally {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });


        this.rowCopy.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    JTableModel.stopCellEditing(rowsTable);

                    copySelectedRowsToClipboard();
                }
            });

        this.rowPaste.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    JTableModel.stopCellEditing(rowsTable);

                    pasteRowsFromClipboard();
                }
            });

        this.rowSave.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    JTableModel.stopCellEditing(rowsTable);

                    int decision = JOptionPane.showConfirmDialog(
                        topPanel,
                        "You are going to save modified rows to hbase; make sure the selected column's types\nare correct otherwise the data will not be read by the application.",
                        "Confirmation", JOptionPane.OK_CANCEL_OPTION);

                    if (decision == JOptionPane.OK_OPTION) {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            for (DataRow row : changeTracker.getChanges()) {
                                try {
                                    connection.setRow(getSelectedTableName(), row);
                                }
                                catch (Exception ex) {
                                    setError("Failed to update rows in HBase: ", ex);
                                }
                            }

                            changeTracker.clear();
                            rowSave.setEnabled(changeTracker.hasChanges());
                        }
                        finally {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.tableAdd.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    AddTableDialog dialog = new AddTableDialog();
                    if (dialog.showDialog(topPanel)) {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            connection.createOrModifyTable(dialog.getTableDescriptor());

                            Filter filter;

                            String value = (String)tableFilters.getSelectedItem();
                            if (value == null || value.isEmpty()) {
                                filter = new EmptyFilter();
                            }
                            else {
                                filter = new PatternFilter(value);
                            }

                            if (filter.match(dialog.getTableDescriptor().getName())) {
                                tablesListModel.addElement(dialog.getTableDescriptor().getName());
                                tablesList.setSelectedValue(dialog.getTableDescriptor().getName(), true);
                            }
                        }
                        catch (Exception ex) {
                            setError(String.format("Failed to create table %s: ", dialog.getTableDescriptor().getName()), ex);
                        }
                        finally {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.tableDelete.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    int decision = JOptionPane.showConfirmDialog(
                        topPanel, "Are you sure you want to delete selected table(s).", "Confirmation", JOptionPane.YES_NO_OPTION);

                    if (decision == JOptionPane.YES_OPTION) {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            for (String tableName : getSelectedTables()) {
                                try {
                                    connection.deleteTable(tableName);
                                    tablesListModel.removeElement(tableName);
                                }
                                catch (Exception ex) {
                                    setError(String.format("Failed to delete table %s: ", tableName), ex);
                                }
                            }
                        }
                        finally {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.tableTruncate.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    int decision = JOptionPane.showConfirmDialog(
                        topPanel, "Are you sure you want to truncate selected table(s).", "Confirmation", JOptionPane.YES_NO_OPTION);

                    if (decision == JOptionPane.YES_OPTION) {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            List<String> selectedTables = getSelectedTables();
                            if (!selectedTables.isEmpty()) {
                                for (String tableName : selectedTables) {
                                    try {
                                        connection.truncateTable(tableName);
                                    }
                                    catch (Exception ex) {
                                        setError(String.format("Failed to truncate table %s: ", tableName), ex);
                                    }
                                }

                                scanner = null;

                                populateColumnsTable(true);
                            }
                        }
                        finally {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.rowsPrev.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    populateRowsTable(Direction.Backward);
                }
            });

        this.rowsNext.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    populateRowsTable(Direction.Forward);
                }
            });

        this.columnCheck.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        for (int i = 1 ; i < columnsTable.getRowCount() ; i++) {
                            columnsTable.setValueAt(true, i, 0);
                        }
                    }
                    finally {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

        this.columnUncheck.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        for (int i = 1 ; i < columnsTable.getRowCount() ; i++) {
                            columnsTable.setValueAt(false, i, 0);
                        }
                    }
                    finally {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

        this.tableFlush.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    int decision = JOptionPane.showConfirmDialog(
                        topPanel, "Are you sure you want to flush selected table(s).", "Confirmation", JOptionPane.YES_NO_OPTION);

                    if (decision == JOptionPane.YES_OPTION) {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            for (String tableName : getSelectedTables()) {
                                try {
                                    connection.flushTable(tableName);
                                }
                                catch (Exception ex) {
                                    setError(String.format("Failed to flush table %s: ", tableName), ex);
                                }
                            }
                        }
                        finally {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.tableRefresh.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    populate();
                }
            });

        this.tableCopy.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    copyTableToClipboard();
                }
            });

        this.tablePaste.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    pasteTableFromClipboard();
                }
            });

        this.tableExport.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    String tableName = getSelectedTableName();
                    if (tableName != null) {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            QueryScanner scanner = connection.getScanner(tableName, null);
                            scanner.setColumnTypes(getColumnTypes());

                            ExportTableDialog dialog = new ExportTableDialog(scanner);
                            dialog.showDialog(topPanel);
                        }
                        catch (Exception ex) {
                            setError(String.format("Failed to export table %s: ", tableName), ex);
                        }
                        finally {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.tableImport.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        String tableName = getSelectedTableName();

                        Collection<ColumnFamily> columnFamilies;
                        if (tableName != null) {
                            columnFamilies = connection.getColumnFamilies(tableName);
                        }
                        else {
                            columnFamilies = new ArrayList<ColumnFamily>();
                        }

                        ImportTableDialog dialog = new ImportTableDialog(
                            connection, tableName, getColumnNameConverter(), getShownTypedColumns(), columnFamilies);

                        dialog.showDialog(topPanel);
                    }
                    catch (Exception ex) {
                        setError("Failed to import to table.", ex);
                    }
                    finally {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

        this.tableMetadata.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    String tableName = getSelectedTableName();
                    if (tableName != null) {
                        try {
                            TableDescriptor tableDescriptor = connection.getTableDescriptor(tableName);

                            UpdateTableMetadata dialog = new UpdateTableMetadata(tableDescriptor);
                            if (dialog.showDialog(topPanel)) {
                                owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                try {
                                    connection.createOrModifyTable(tableDescriptor);
                                }
                                catch (Exception ex) {
                                    setError(String.format("Failed to update table %s metadata: ", tableName), ex);
                                }
                                finally {
                                    owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                }
                            }
                        }
                        catch (Exception ex) {
                            setError(String.format("Failed to retrieve descriptor of the table %s: ", tableName), ex);
                        }
                    }
                }
            });

        this.columnRefresh.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    scanner = null;
                    populateColumnsTable(true);
                }
            });

        this.columnAddConverter.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    CustomConverterDialog dialog = new CustomConverterDialog(null);
                    if (dialog.showDialog(topPanel)) {
                        ConvertersLoader.reload();

                        reloadColumnTypes();
                    }
                }
            });

        this.columnEditConverter.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    ColumnType columnType = getSelectedEditableColumnType();
                    if (columnType != null) {
                        CustomConverterDialog dialog = new CustomConverterDialog(columnType.getConverter());
                        if (dialog.showDialog(topPanel)) {
                            if (!columnType.getName().equals(dialog.getConverterName())) {
                                ConvertersLoader.removeConverter(columnType.getName());
                            }

                            ConvertersLoader.reload();

                            reloadColumnTypes();

                            for (int row = 0 ; row < columnsTable.getRowCount() ; row++) {
                                ColumnType type = (ColumnType)columnsTable.getValueAt(row, 2);
                                if (type.getName().equals(columnType.getName())) {
                                    ColumnQualifier qualifier = (ColumnQualifier)columnsTable.getValueAt(row, 1);

                                    type = ColumnType.fromName(type.getName());
                                    columnsTable.setValueAt(type, row, 2);

                                    updateColumnType(qualifier, type);
                                }
                            }

                            if (lastQuery != null) {
                                lastQuery.setStartKeyType(ColumnType.fromName(lastQuery.getStartKeyType().getName()));
                                lastQuery.setEndKeyType(ColumnType.fromName(lastQuery.getEndKeyType().getName()));
                                lastQuery.setWordType(ColumnType.fromName(lastQuery.getWordType().getName()));
                            }
                        }
                    }
                }
            });

        this.columnDeleteConverter.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    ColumnType columnType = getSelectedEditableColumnType();
                    if (columnType != null) {
                        int decision = JOptionPane.showConfirmDialog(
                            topPanel, String.format(
                            "Are you sure you want to delete '%s' type?%sIt will be lost for good!", columnType.getName(), PathHelper.LINE_SEPARATOR),
                            "Confirmation", JOptionPane.OK_CANCEL_OPTION);

                        if (decision == JOptionPane.OK_OPTION) {
                            ConvertersLoader.removeConverter(columnType.getName());

                            reloadColumnTypes();

                            for (int row = 0 ; row < columnsTable.getRowCount() ; row++) {
                                ColumnType type = (ColumnType)columnsTable.getValueAt(row, 2);
                                if (type.getName().equals(columnType.getName())) {
                                    ColumnQualifier qualifier = (ColumnQualifier)columnsTable.getValueAt(row, 1);

                                    type = ColumnType.fromColumn(qualifier.getName());
                                    columnsTable.setValueAt(type, row, 2);

                                    updateColumnType(qualifier, type);
                                }
                            }

                            if (lastQuery != null) {
                                lastQuery.setStartKeyType(ColumnType.fromNameOrDefault(lastQuery.getStartKeyType().getName(), ColumnType.BinaryString));
                                lastQuery.setEndKeyType(ColumnType.fromNameOrDefault(lastQuery.getEndKeyType().getName(), ColumnType.BinaryString));
                                lastQuery.setWordType(ColumnType.fromNameOrDefault(lastQuery.getWordType().getName(), ColumnType.String));
                            }
                        }
                    }
                }
            });
    }
    //endregion

    //region Public Methods

    /**
     * Populates the view with the data.
     */
    public void populate() {
        loadTables();
    }

    /**
     * Gets the reference to the view.
     *
     * @return A {@link JPanel} that contains the controls.
     */
    public JPanel getView() {
        return this.topPanel;
    }

    /**
     * Gets a reference to the class used to access the hbase.
     *
     * @return A reference to the {@link Connection} class.
     */
    public Connection getConnection() {
        return this.connection;
    }

    //endregion

    //region Private Methods

    /**
     * Clears all rows from the specified table model.
     *
     * @param table The table to clear the rows from.
     */
    private static void clearRows(JTable table) {
        ((DefaultTableModel)table.getModel()).setRowCount(0);
    }

    /**
     * Clears all columns from the specified table model.
     *
     * @param table The table to clear the columns from.
     */
    private static void clearColumns(JTable table) {
        ((DefaultTableModel)table.getModel()).setColumnCount(0);

        TableColumnModel cm = table.getColumnModel();
        while (cm.getColumnCount() > 0) {
            cm.removeColumn(cm.getColumn(0));
        }
    }

    /**
     * Clear all data from the table.
     *
     * @param table The table to clear the data from.
     */
    private static void clearTable(JTable table) {
        clearRows(table);
        clearColumns(table);
    }

    /**
     * Checks if the clipboard has rows to paste.
     *
     * @return True if the clipboard has rows to paste or False otherwise.
     */
    private static boolean hasRowsInClipboard() {
        ClipboardData<DataTable> data = InMemoryClipboard.getData();
        return data != null && data.getData().getRowsCount() > 0;
    }

    /**
     * Checks if the clipboard has table to paste.
     *
     * @return True if the clipboard has table to paste or False otherwise.
     */
    private static boolean hasTableInClipboard() {
        ClipboardData<DataTable> data = InMemoryClipboard.getData();
        return data != null && data.getData().getRowsCount() == 0;
    }

    /**
     * Gets a column from the specified table.
     *
     * @param columnName The name of the column to look.
     * @param table      The table that should contain column.
     * @return A reference to {@link TableColumn} if found or {@code null} otherwise.
     */
    private static TableColumn getColumn(String columnName, JTable table) {
        for (int i = 0 ; i < table.getColumnCount() ; i++) {
            if (columnName.equals(table.getColumnName(i))) {
                return table.getColumnModel().getColumn(i);
            }
        }
        return null;
    }

    /**
     * Clears the previously set error messages.
     */
    private static void clearError() {
        MessageHandler.addInfo("");
    }

    /**
     * Sets the error message.
     *
     * @param message The error message to set.
     * @param ex      An exception.
     */
    private static void setError(String message, Exception ex) {
        MessageHandler.addError(message, ex);
    }

    /**
     * Sets the info message.
     *
     * @param message The info message to set.
     */
    private static void setInfo(String message) {
        MessageHandler.addInfo(message);
    }

    /**
     * Sets the action.
     *
     * @param action The action to set.
     */
    private static void setAction(UIAction action) {
        MessageHandler.addAction(action);
    }

    /**
     * Loads the table names.
     */
    private void loadTables() {
        Object selectedTable = this.tablesList.getSelectedValue();

        clearError();

        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            this.tablesListModel.clear();

            Filter filter;

            String value = (String)tableFilters.getSelectedItem();
            if (value == null || value.isEmpty()) {
                filter = new EmptyFilter();
            }
            else {
                filter = new PatternFilter(value);
            }

            Collection<String> tables = this.connection.getTables();

            for (String table : tables) {
                if (filter.match(table)) {
                    this.tablesListModel.addElement(table);
                }
            }

            toggleTableControls();

            this.tablesNumber.setText(String.format("%s of %s", this.tablesListModel.getSize(), tables.size()));
            this.tablesList.setSelectedValue(selectedTable, true);
        }
        catch (Exception ex) {
            setError("Failed to connect to hadoop: ", ex);
        }
        finally {
            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Initializes a table list used to present all available tables in the hbase cluster.
     */
    private void initializeTablesList() {
        this.tablesListModel = new DefaultListModel();
        this.tablesList.setModel(this.tablesListModel);
        this.tablesList.setCellRenderer(new JListRenderer(this.connection));

        this.tablesList.addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        if (rowsCountAction != null) {
                            rowsCountAction.abort();
                        }

                        toggleTableControls();

                        boolean populate = false;
                        int[] selectedIndices = tablesList.getSelectedIndices();

                        if (selectedIndices.length == 1) {
                            populate = tableEnabled(getSelectedTableName());
                        }

                        if (populate) {
                            scanner = null;

                            String currentFilter = clusterConfig.getSelectedColumnFilter(getSelectedTableName());

                            fillComboBox(columnFilters, columnsFilterListener, clusterConfig.getColumnFilters(getSelectedTableName()));
                            setFilter(columnFilters, columnsFilterListener, currentFilter);

                            String converterType = clusterConfig.getTableConfig(String.class, getSelectedTableName(), "nameConverter");
                            if (converterType != null) {
                                columnConverters.setSelectedItem(ColumnType.fromNameOrDefault(converterType, ColumnType.BinaryString));
                            }
                            else {
                                columnConverters.setSelectedItem(ColumnType.BinaryString);
                            }

                            populateColumnsTable(true);
                        }
                        else {
                            clearRows(columnsTable);
                            clearTable(rowsTable);

                            togglePagingControls();
                            toggleColumnControls(false);
                            toggleRowControls(false);

                            rowsTotal.setText("?");
                            rowsVisible.setText("?");

                            if (selectedIndices.length == 1) {
                                setAction(
                                    new UIAction() {
                                        @Override
                                        public void execute() {
                                            String tableName = getSelectedTableName();

                                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                            try {
                                                connection.enableTable(tableName);

                                                tablesList.clearSelection();
                                                tablesList.setSelectedValue(tableName, true);

                                                setInfo(String.format("The '%s' table has been successfully enabled.", tableName));
                                            }
                                            catch (Exception ex) {
                                                setError(String.format("Failed to enable table '%s'", tableName), ex);
                                            }
                                            finally {
                                                owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                            }
                                        }

                                        @Override
                                        public String[] getFormattedMessage() {
                                            return new String[]{
                                                "The selected table is disabled, do you want to", "enable", "it?"
                                            };
                                        }
                                    });
                            }
                        }
                    }
                }
            });

        this.tablesList.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.isControlDown()) {
                        if (e.getKeyCode() == KeyEvent.VK_C) {
                            clearError();
                            if (tableCopy.isEnabled()) {
                                copyTableToClipboard();
                            }
                        }
                        else if (e.getKeyCode() == KeyEvent.VK_V) {
                            clearError();
                            if (tablePaste.isEnabled()) {
                                pasteTableFromClipboard();
                            }
                        }
                    }
                }
            });
    }

    /**
     * Initializes a columns table used to present the columns of the selected table.
     */
    private void initializeColumnsTable() {
        this.columnsTableModel = new JTableModel(0, 2);
        this.columnsTableModel.addColumn("Show");
        this.columnsTableModel.addColumn("Column Name");
        this.columnsTableModel.addColumn("Column Type");
        this.columnsTable.setRowHeight(this.columnsTable.getFont().getSize() + 8);
        this.columnsTable.setModel(this.columnsTableModel);
        this.columnsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        this.columnsTable.getColumn("Show").setCellRenderer(new JCheckBoxRenderer(new CheckedRow(1, ColumnQualifier.KEY)));
        this.columnsTable.getColumn("Show").setCellEditor(new JCheckBoxRenderer(new CheckedRow(1, ColumnQualifier.KEY)));
        this.columnsTable.getColumn("Show").setPreferredWidth(40);
        this.columnsTable.getColumn("Column Name").setPreferredWidth(110);

        this.cmbColumnTypes = new WideComboBox();

        for (ColumnType columnType : ColumnType.getTypes()) {
            this.cmbColumnTypes.addItem(columnType);
        }

        for (ColumnType columnType : ColumnType.getNameTypes()) {
            this.columnConverters.addItem(columnType);
        }

        this.columnConverters.setSelectedItem(ColumnType.BinaryString);
        this.columnsTable.getColumn("Column Type").setCellEditor(new DefaultCellEditor(this.cmbColumnTypes));

        this.columnsTable.getModel().addTableModelListener(
            new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    clearError();

                    int column = e.getColumn();

                    TableModel model = (TableModel)e.getSource();
                    String columnName = model.getColumnName(column);

                    if ("Show".equals(columnName)) {
                        ColumnQualifier qualifier = (ColumnQualifier)model.getValueAt(e.getFirstRow(), 1);
                        boolean isShown = (Boolean)model.getValueAt(e.getFirstRow(), 0);

                        clusterConfig.setTableConfig(getSelectedTableName(), qualifier.getFullName(), "isShown", Boolean.toString(isShown));

                        setRowsTableColumnVisible(qualifier, isShown);
                    }
                    else if ("Column Type".equals(columnName)) {
                        ColumnQualifier qualifier = (ColumnQualifier)model.getValueAt(e.getFirstRow(), 1);
                        ColumnType type = (ColumnType)model.getValueAt(e.getFirstRow(), column);

                        TypeConverter nameConverter = getColumnNameConverter();

                        columnEditConverter.setEnabled(type.isEditable() || nameConverter.isEditable());
                        columnDeleteConverter.setEnabled(type.isEditable() || nameConverter.isEditable());

                        updateColumnType(qualifier, type);
                    }
                }
            });

        this.columnsTable.addMouseMotionListener(
            new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    int row = columnsTable.rowAtPoint(p);
                    int column = columnsTable.columnAtPoint(p);
                    if (row != -1 && column != -1) {
                        columnsTable.setToolTipText(String.valueOf(columnsTable.getValueAt(row, column)));
                    }
                }
            });

        this.columnsTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    boolean isEditable = getColumnNameConverter().isEditable();

                    int row = columnsTable.getSelectedRow();
                    if (row != -1) {
                        ColumnType type = (ColumnType)columnsTable.getValueAt(row, 2);
                        isEditable |= type.isEditable();
                    }

                    columnEditConverter.setEnabled(isEditable);
                    columnDeleteConverter.setEnabled(isEditable);
                }
            });
    }

    /**
     * Initializes a rows table used to present the content of the selected table.
     */
    private void initializeRowsTable() {
        this.rowsTableModel = new DefaultTableModel();
        this.rowsTable.setModel(this.rowsTableModel);
        this.rowsTable.setTableHeader(new ResizeableTableHeader(this.rowsTable.getColumnModel()));
        this.rowsTable.setCellSelectionEnabled(false);
        this.rowsTable.setRowSelectionAllowed(true);
        this.rowsTable.setAutoCreateColumnsFromModel(false);

        this.rowsTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    toggleRowControls(true);
                }
            });

        this.rowsTable.addMouseMotionListener(
            new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    int row = rowsTable.rowAtPoint(p);
                    int column = rowsTable.columnAtPoint(p);
                    if (row != -1 && column != -1) {
                        rowsTable.setToolTipText(String.valueOf(rowsTable.getValueAt(row, column)));
                    }
                }
            });

        this.rowsTable.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.isControlDown()) {
                        if (e.getKeyCode() == KeyEvent.VK_C) {
                            clearError();

                            if (rowCopy.isEnabled()) {
                                JTableModel.stopCellEditing(rowsTable);
                                copySelectedRowsToClipboard();
                            }
                        }
                        else if (e.getKeyCode() == KeyEvent.VK_V) {
                            clearError();

                            if (rowPaste.isEnabled()) {
                                JTableModel.stopCellEditing(rowsTable);
                                pasteRowsFromClipboard();
                            }
                        }
                    }
                }
            });

        this.rowsTable.addPropertyChangeListener(
            new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("model".equals(evt.getPropertyName())) {
                        changeTracker.clear();

                        toggleRowControls(false);
                    }
                }
            });

        this.rowsTable.getColumnModel().addColumnModelListener(
            new TableColumnModelListener() {
                @Override
                public void columnAdded(TableColumnModelEvent e) {
                }

                @Override
                public void columnRemoved(TableColumnModelEvent e) {
                }

                @Override
                public void columnMoved(TableColumnModelEvent e) {
                }

                @Override
                public void columnMarginChanged(ChangeEvent e) {
                    TableColumn column = rowsTable.getTableHeader().getResizingColumn();
                    if (column != null) {
                        clusterConfig.setTableConfig(
                            getSelectedTableName(), column.getHeaderValue().toString(), "size", String.valueOf(column.getWidth()));
                    }
                }

                @Override
                public void columnSelectionChanged(ListSelectionEvent e) {
                }
            });

        this.changeTracker.addListener(
            new ChangeTrackerListener() {
                @Override
                public void onCellChanged(DataCell cell) {
                    clearError();

                    rowSave.setEnabled(changeTracker.hasChanges());
                }
            });
    }

    /**
     * Populates a columns table with the list of the columns from the selected table.
     *
     * @param clearRows Indicates whether the rows table should be cleared.
     */
    private void populateColumnsTable(boolean clearRows) {
        populateColumnsTable(clearRows, null);
    }

    /**
     * Populates a columns table with the list of the columns from the selected table.
     *
     * @param clearRows Indicates whether the rows table should be cleared.
     * @param row       If this parameter is not null it will be used to start the columns population from. The columns are the collection of keys extracted
     *                  from the hbase rows. Hbase doesn't have a list of columns so in order to present them to the user the tool must go over a number of rows
     *                  to collect their keys. Each row can have different keys.
     */
    private void populateColumnsTable(boolean clearRows, DataRow row) {
        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            clearRows(columnsTable);

            if (clearRows) {
                clearTable(rowsTable);

                this.rowsTotal.setText("?");
                this.rowsVisible.setText("?");
                this.rowsNumber.setEnabled(true);
            }

            String tableName = getSelectedTableName();
            if (tableName != null) {
                loadColumns(tableName, row);
            }

            togglePagingControls();
            toggleColumnControls(columnsTableModel.getRowCount() > 0);
            toggleRowControls(rowsTableModel.getRowCount() > 0);
        }
        finally {
            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Populates a rows table. The method loads the table content. The number of loaded rows depends on the parameter defined by the user
     * in the {@link DesignerView#rowsNumber} control.
     *
     * @param direction Defines what rows should be presented to the user. {@link Direction#Current},
     *                  {@link Direction#Forward} or {@link Direction#Backward}.
     */
    private void populateRowsTable(Direction direction) {
        populateRowsTable(0, direction);
    }

    /**
     * Populates a rows table. The method loads the table content. The number of loaded rows depends on the parameter defined by the user
     * in the {@link DesignerView#rowsNumber} control.
     *
     * @param offset    The first row to start loading from.
     * @param direction Defines what rows should be presented to the user. {@link Direction#Current},
     *                  {@link Direction#Forward} or {@link Direction#Backward}.
     */
    private void populateRowsTable(long offset, Direction direction) {

        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            JTableModel.stopCellEditing(columnsTable);

            String tableName = getSelectedTableName();
            if (tableName != null) {
                clearTable(rowsTable);

                Map<String, ColumnType> columnTypes = getColumnTypes();

                this.scanner.setColumnTypes(columnTypes);
                this.scanner.setQuery(this.lastQuery);

                Collection<DataRow> rows;

                if (direction == Direction.Current) {
                    rows = this.scanner.current(offset, getPageSize());
                }
                else if (direction == Direction.Forward) {
                    rows = this.scanner.next(getPageSize());
                }
                else {
                    rows = this.scanner.prev();
                }

                loadColumns(tableName, null);
                loadRowsTableColumns(tableName);
                loadRows(rows);

                this.togglePagingControls();

                this.rowsVisible.setText(String.format("%s - %s", this.scanner.getLastRow() - rows.size() + 1, this.scanner.getLastRow()));
                this.rowsNumber.setEnabled(this.scanner.getLastRow() <= getPageSize());

                this.rowsCountAction = RunnableAction.run(
                    tableName + "-rowsCount", new Action<Boolean>() {

                    @Override
                    public Boolean run() throws IOException {
                        rowsNumberIcon.setVisible(true);
                        rowsTotal.setVisible(false);

                        long totalNumberOfRows = scanner.getRowsCount(GlobalConfig.instance().getRowCountTimeout());
                        if (totalNumberOfRows == scanner.getCalculatedRowsCount()) {
                            rowsTotal.setText(String.valueOf(totalNumberOfRows));
                        }
                        else {
                            rowsTotal.setText("more than " + totalNumberOfRows);
                        }

                        rowsNumberIcon.setVisible(false);
                        rowsTotal.setVisible(true);

                        togglePagingControls();

                        return true;
                    }

                    @Override
                    public void onError(Exception ex) {
                        setError("Failed to get the number of rows in the table.", ex);
                    }
                });
            }

            toggleRowControls(rowsTableModel.getRowCount() > 0);
        }
        catch (Exception ex) {
            setError("Failed to fill rows: ", ex);
        }
        finally {
            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Populates rows of the specified column. This method is used when the column which wasn't initially populated is checked.
     *
     * @param qualifier The name of the column which rows are need to be populated.
     * @param rows      The data to populate.
     */
    private void populateColumnOnRowsTable(ColumnQualifier qualifier, Iterable<DataRow> rows) {

        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            int rowIndex = 0;
            int columnIndex = this.rowsTable.getColumnModel().getColumnIndex(qualifier);

            for (DataRow row : rows) {
                DataCell cell = row.getCell(qualifier);
                if (cell == null) {
                    cell = new DataCell(row, qualifier, new ConvertibleObject(getColumnType(qualifier.getFullName()), null));
                }

                if (rowIndex >= this.rowsTable.getRowCount()) {
                    break;
                }

                this.rowsTableModel.setValueAt(cell, rowIndex++, columnIndex);
            }
        }
        finally {
            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Loads columns into the columns table.
     *
     * @param tableName The name of the table which columns to be populated.
     * @param row       The row to start the population from. This value can be null.
     */
    private void loadColumns(String tableName, DataRow row) {
        clearRows(this.columnsTable);

        try {
            if (this.scanner == null) {
                this.scanner = connection.getScanner(tableName, null);
                this.updateColumnNameConverter(getColumnNameConverter());
            }

            Filter filter;

            String value = (String)columnFilters.getSelectedItem();
            if (value == null || value.isEmpty()) {
                filter = new EmptyFilter();
            }
            else {
                filter = new PatternFilter(value);
            }

            if (row == null) {
                this.scanner.setColumnTypes(
                    new HashMap<String, ColumnType>() {{
                        put(ColumnQualifier.KEY.getName(), ColumnType.String);
                    }});

                row = this.scanner.getFirstRow();
            }

            Collection<ColumnQualifier> columns = this.scanner.getColumns(getPageSize());
            for (ColumnQualifier column : columns) {
                boolean isColumnVisible = column.isKey() || filter.match(column.getFullName());
                if (isColumnVisible) {
                    addColumnToColumnsTable(tableName, column, row);
                }

                setRowsTableColumnVisible(column, isColumnVisible && isShown(tableName, column.getFullName()));
            }

            this.columnsNumber.setText(String.format("%s of %s", this.columnsTableModel.getRowCount(), columns.size()));
        }
        catch (Exception ex) {
            setError("Failed to fill tables list: ", ex);
        }
    }

    /**
     * Adds a column to the columns table.
     *
     * @param tableName The name of the table the column belongs to. Used to look for the column type in the saved configuration.
     * @param qualifier The column qualifier to add.
     * @param row       The row that might contain column type information.
     */
    private void addColumnToColumnsTable(String tableName, ColumnQualifier qualifier, DataRow row) {
        ColumnType columnType = getSavedColumnType(tableName, qualifier.getFullName());

        if (columnType == null && row != null) {
            DataCell cell = row.getCell(qualifier);
            if (cell != null) {
                columnType = cell.guessType();
            }
        }

        if (columnType == null) {
            columnType = ColumnType.fromColumn(qualifier.getFullName());
        }

        boolean isShown = isShown(tableName, qualifier.getFullName());
        this.columnsTableModel.addRow(new Object[]{isShown, qualifier, columnType});
    }

    /**
     * Adds columns defined in the columns table to the rows table.
     */
    private void loadRowsTableColumns(String tableName) {
        clearColumns(this.rowsTable);

        this.rowsTableRemovedColumns.clear();

        for (int i = 0, j = 0 ; i < this.columnsTable.getRowCount() ; i++) {
            ColumnQualifier qualifier = (ColumnQualifier)this.columnsTableModel.getValueAt(i, 1);

            boolean isShown = (Boolean)this.columnsTableModel.getValueAt(i, 0);
            if (isShown) {
                addColumnToRowsTable(tableName, qualifier, j++);
            }
        }
    }

    /**
     * Shows or hides a column in rows table.
     *
     * @param qualifier The name of the column.
     * @param isVisible Indicates if the columns should be shown or hidden.
     */
    private void setRowsTableColumnVisible(ColumnQualifier qualifier, boolean isVisible) {
        if (this.rowsTable.getRowCount() > 0) {
            if (isVisible) {
                TableColumn tableColumn = this.rowsTableRemovedColumns.get(qualifier);
                if (tableColumn != null) {
                    this.rowsTable.addColumn(tableColumn);
                    this.rowsTableRemovedColumns.remove(qualifier);

                    this.rowsTable.moveColumn(this.rowsTable.getColumnCount() - 1, getColumnIndex(qualifier.getFullName()));
                }
                else {
                    if (getColumn(qualifier.getFullName(), this.rowsTable) == null) {
                        addColumnToRowsTable(getSelectedTableName(), qualifier, this.rowsTable.getColumnCount());

                        if (this.scanner != null) {
                            populateColumnOnRowsTable(qualifier, this.scanner.current());
                        }

                        this.rowsTable.moveColumn(this.rowsTable.getColumnCount() - 1, getColumnIndex(qualifier.getFullName()));
                    }
                }
            }
            else {
                TableColumn tableColumn = getColumn(qualifier.getFullName(), this.rowsTable);
                if (tableColumn != null) {
                    this.rowsTable.removeColumn(tableColumn);
                    this.rowsTableRemovedColumns.put(qualifier, tableColumn);
                }
            }
        }
    }

    /**
     * Adds a single column to the rows table.
     *
     * @param tableName   The name of the table the column belongs to.
     * @param qualifier   The name of the column to add.
     * @param columnIndex The index the column should be added at.
     */
    private void addColumnToRowsTable(String tableName, ColumnQualifier qualifier, int columnIndex) {
        TableColumn tableColumn = new TableColumn(columnIndex);
        tableColumn.setIdentifier(qualifier);
        tableColumn.setHeaderValue(qualifier);
        tableColumn.setCellEditor(new JCellEditor(this.changeTracker, !ColumnQualifier.isKey(qualifier.getFullName())));

        try {
            Integer width = this.clusterConfig.getTableConfig(Integer.class, tableName, qualifier.getFullName(), "size");
            if (width != null) {
                tableColumn.setPreferredWidth(width);
            }
        }
        catch (NumberFormatException ignore) {
        }

        this.rowsTable.addColumn(tableColumn);
        this.rowsTableModel.addColumn(qualifier);
    }

    /**
     * Adds rows to the rows table.
     *
     * @param rows A list of rows to add.
     */
    private void loadRows(Iterable<DataRow> rows) {
        clearRows(this.rowsTable);

        TypeConverter nameConverter = getColumnNameConverter();

        for (DataRow row : rows) {
            Collection<Object> values = new ArrayList<Object>(rowsTable.getColumnCount());
            for (int i = 0 ; i < rowsTable.getColumnCount() ; i++) {
                String columnName = rowsTable.getColumnName(i);
                DataCell cell = row.getCell(columnName);
                if (cell == null) {
                    cell = new DataCell(row, new ColumnQualifier(columnName, nameConverter), new ConvertibleObject(getColumnType(columnName), null));
                }
                values.add(cell);
            }
            this.rowsTableModel.addRow(values.toArray());
        }
    }

    /**
     * Copies selected rows from the rows table to the clipboard.
     */
    private void copySelectedRowsToClipboard() {
        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            int[] selectedRows = this.rowsTable.getSelectedRows();
            if (selectedRows.length > 0) {
                DataTable table = new DataTable(getSelectedTableName());
                for (int selectedRow : selectedRows) {
                    DataCell cell = (DataCell)rowsTable.getValueAt(selectedRow, 0);
                    table.addRow(cell.getRow());
                }

                InMemoryClipboard.setData(new ClipboardData<DataTable>(table));
            }
            else {
                InMemoryClipboard.setData(null);
            }
        }
        finally {
            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Copies selected table to the clipboard.
     */
    private void copyTableToClipboard() {
        String tableName = getSelectedTableName();
        if (tableName != null) {
            InMemoryClipboard.setData(new ClipboardData<DataTable>(new DataTable(tableName, this.connection)));
        }
        else {
            InMemoryClipboard.setData(null);
        }
    }

    /**
     * Pastes rows from the clipboard into the rows table and into the hbase table that the rows table represents.
     */
    @SuppressWarnings("OverlyNestedMethod")
    private void pasteRowsFromClipboard() {
        ClipboardData<DataTable> clipboardData = InMemoryClipboard.getData();
        if (clipboardData != null) {
            DataTable table = clipboardData.getData();
            if (table.getRowsCount() > 0) {
                owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    PasteDialog dialog = new PasteDialog(changeTracker, table.getRows());
                    if (dialog.showDialog(topPanel)) {
                        Collection<DataRow> updatedRows = dialog.getRows();
                        for (DataRow row : updatedRows) {
                            try {
                                connection.setRow(getSelectedTableName(), row);

                                // Update the column types according to the added row.
                                for (DataCell cell : row.getCells()) {
                                    this.clusterConfig.setTableConfig(
                                        getSelectedTableName(), cell.getColumn().getFullName(), cell.getType().toString());
                                }

                                populateColumnsTable(true, row);
                            }
                            catch (Exception ex) {
                                setError("Failed to update rows in HBase: ", ex);
                            }
                        }

                        if (scanner != null) {
                            List<DataRow> sortedRows = new ArrayList<DataRow>(updatedRows);
                            Collections.sort(
                                sortedRows, new Comparator<DataRow>() {
                                @Override
                                public int compare(DataRow o1, DataRow o2) {
                                    byte[] a1 = o1.getKey().getValue();
                                    byte[] a2 = o2.getKey().getValue();

                                    if (a1.length != a2.length) {
                                        return a1.length - a2.length;
                                    }

                                    for (int i = 0 ; i < a1.length ; i++) {
                                        if (a1[i] != a2[i]) {
                                            return a1[i] - a2[i];
                                        }
                                    }
                                    return 0;
                                }
                            });

                            scanner.resetCurrent(sortedRows.get(0).getKey());
                        }

                        populateRowsTable(Direction.Current);
                    }
                }
                catch (Exception ex) {
                    setError("Failed to paste rows: ", ex);
                }
                finally {
                    owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        }
    }

    /**
     * Pastes a table from the clipboard into the application and an hbase cluster.
     */
    @SuppressWarnings("OverlyNestedMethod")
    private void pasteTableFromClipboard() {
        this.tablePaste.setEnabled(false);

        ClipboardData<DataTable> clipboardData = InMemoryClipboard.getData();
        if (clipboardData != null) {

            DataTable table = clipboardData.getData();

            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                TableDescriptor sourceTable = table.getConnection().getTableDescriptor(table.getTableName());

                AddTableDialog dialog = new AddTableDialog(sourceTable);
                if (dialog.showDialog(topPanel)) {
                    TableDescriptor targetTable = dialog.getTableDescriptor();
                    this.connection.copyTable(targetTable, sourceTable, table.getConnection());

                    Filter filter;

                    String value = (String)tableFilters.getSelectedItem();
                    if (value == null || value.isEmpty()) {
                        filter = new EmptyFilter();
                    }
                    else {
                        filter = new PatternFilter(value);
                    }

                    if (filter.match(targetTable.getName())) {
                        if (!this.tablesListModel.contains(targetTable.getName())) {
                            String sourcePrefix = String.format("table.%s.", sourceTable.getName());
                            String targetPrefix = String.format("table.%s.", targetTable.getName());

                            for (Map.Entry<String, String> keyValue : this.clusterConfig.getAll(sourcePrefix).entrySet()) {
                                this.clusterConfig.set(keyValue.getKey().replace(sourcePrefix, targetPrefix), keyValue.getValue());
                            }
                            this.tablesListModel.addElement(targetTable.getName());
                        }

                        this.tablesList.setSelectedValue(targetTable.getName(), true);
                    }
                }
            }
            catch (IOException e) {
                setError("Failed to paste table: ", e);
            }
            finally {
                this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    /**
     * Gets a mapping of column names to column types.
     *
     * @return A column names to column types map.
     */
    private Map<String, ColumnType> getColumnTypes() {
        Map<String, ColumnType> columnTypes = new HashMap<String, ColumnType>();
        for (int i = 0 ; i < columnsTable.getRowCount() ; i++) {
            ColumnQualifier qualifier = (ColumnQualifier)columnsTableModel.getValueAt(i, 1);
            columnTypes.put(qualifier.getFullName(), (ColumnType)columnsTableModel.getValueAt(i, 2));
        }
        return columnTypes;
    }

    /**
     * Gets a list of columns that are checked. Checked columns are the columns to be shown in the rows table.
     *
     * @return A list of checked columns from the columns table.
     */
    private Collection<TypedColumn> getShownTypedColumns() {
        Collection<TypedColumn> typedColumns = new ArrayList<TypedColumn>();
        for (int i = 0 ; i < this.columnsTable.getRowCount() ; i++) {
            boolean isShown = (Boolean)this.columnsTableModel.getValueAt(i, 0);
            if (isShown) {
                typedColumns.add(
                    new TypedColumn(
                        (ColumnQualifier)this.columnsTableModel.getValueAt(i, 1), (ColumnType)this.columnsTableModel.getValueAt(i, 2)));
            }
        }
        return typedColumns;
    }

    /**
     * Gets a list of column names from the rows table.
     *
     * @return A list of column names.
     */
    private List<ColumnQualifier> getShownColumns() {
        List<ColumnQualifier> typedColumns = new ArrayList<ColumnQualifier>();
        for (int i = 0 ; i < this.columnsTable.getRowCount() ; i++) {
            boolean isShown = (Boolean)this.columnsTableModel.getValueAt(i, 0);
            if (isShown) {
                typedColumns.add((ColumnQualifier)this.columnsTableModel.getValueAt(i, 1));
            }
        }
        return typedColumns;
    }

    /**
     * Gets the page size. The page size is the number of rows to be loaded in a batch.
     *
     * @return The size of the page.
     */
    private int getPageSize() {
        return (Integer)this.rowsNumber.getValue();
    }

    /**
     * Gets the index of the column as it appear in the columns table. The index is calculated only for checked columns.
     *
     * @param columnName The name of the column which index should be calculated.
     * @return The index of the specified column.
     */
    private int getColumnIndex(String columnName) {
        for (int i = 0, j = 0 ; i < this.columnsTable.getRowCount() ; i++) {
            ColumnQualifier qualifier = (ColumnQualifier)this.columnsTableModel.getValueAt(i, 1);

            boolean isShown = (Boolean)this.columnsTableModel.getValueAt(i, 0);
            if (isShown) {
                if (qualifier.getFullName().equals(columnName)) {
                    return j;
                }

                j++;
            }
        }
        return 0;
    }

    /**
     * Gets the currently selected table name.
     *
     * @return The name of the selected table from the list of the tables.
     */
    private String getSelectedTableName() {
        return (String)this.tablesList.getSelectedValue();
    }

    /**
     * Gets a list of the selected tables.
     *
     * @return A list of the selected tables.
     */
    private List<String> getSelectedTables() {
        List<String> tables = new ArrayList<String>();
        for (Object table : this.tablesList.getSelectedValues()) {
            tables.add(table.toString());
        }
        return tables;
    }

    /**
     * Gets the type of the column from the configuration.
     *
     * @param tableName  The name of the table that contains the column.
     * @param columnName The name of the column.
     * @return The column type.
     */
    private ColumnType getSavedColumnType(String tableName, String columnName) {
        String columnType = this.clusterConfig.getTableConfig(String.class, tableName, columnName);
        if (columnType != null) {
            return ColumnType.fromName(columnType);
        }
        return null;
    }

    /**
     * Gets the type of the column from the columns table.
     *
     * @param columnName The name of the column.
     * @return The columns type
     */
    private ColumnType getColumnType(String columnName) {
        for (int row = 0 ; row < columnsTable.getRowCount() ; row++) {
            ColumnQualifier qualifier = (ColumnQualifier)columnsTable.getValueAt(row, 1);
            if (qualifier.getFullName().endsWith(columnName)) {
                return (ColumnType)columnsTable.getValueAt(row, 2);
            }
        }
        return getSavedColumnType(getSelectedTableName(), columnName);
    }

    /**
     * Gets type converter for the column names.
     *
     * @return A selected type converter.
     */
    private TypeConverter getColumnNameConverter() {
        ColumnType type = (ColumnType)this.columnConverters.getSelectedItem();
        if (type != null) {
            return type.getConverter();
        }
        return ColumnType.BinaryString.getConverter();
    }

    /**
     * Gets a column type that is currently selected and can be edited. The column type of the column name gets higher priority.
     *
     * @return A selected and editable column type if exists or null otherwise.
     */
    private ColumnType getSelectedEditableColumnType() {
        ColumnType type = (ColumnType)this.columnConverters.getSelectedItem();
        if (type != null && type.isEditable()) {
            return type;
        }

        int row = columnsTable.getSelectedRow();
        if (row != -1) {
            type = (ColumnType)columnsTable.getValueAt(row, 2);
            if (type.isEditable()) {
                return type;
            }
        }

        return null;
    }

    /**
     * Checks in the configuration whether the specified column is checked.
     *
     * @param tableName  The name of the table that contains the column.
     * @param columnName The name of the column.
     * @return True if the specified column is checked or False otherwise.
     */
    private boolean isShown(String tableName, String columnName) {
        Boolean isShown = this.clusterConfig.getTableConfig(Boolean.class, tableName, columnName, "isShown");
        return isShown == null || isShown;
    }

    /**
     * Reloads column types.
     */
    private void reloadColumnTypes() {
        ColumnType selectedNameType = (ColumnType)this.columnConverters.getSelectedItem();

        this.cmbColumnTypes.removeAllItems();
        this.columnConverters.removeAllItems();

        for (ColumnType columnType : ColumnType.getTypes()) {
            this.cmbColumnTypes.addItem(columnType);
        }

        for (ColumnType columnType : ColumnType.getNameTypes()) {
            this.columnConverters.addItem(columnType);
        }

        if (selectedNameType != null) {
            this.columnConverters.setSelectedItem(selectedNameType);
        }
    }

    /**
     * Updates the type of the specified column including already loaded values to the rows table.
     *
     * @param qualifier The column to update.
     * @param type      The new column type.
     */
    private void updateColumnType(ColumnQualifier qualifier, ColumnType type) {
        clusterConfig.setTableConfig(getSelectedTableName(), qualifier.getFullName(), type.toString());

        JTableModel.stopCellEditing(rowsTable);

        if (scanner != null) {
            try {
                scanner.updateColumnType(qualifier.getFullName(), type);
            }
            catch (Exception ex) {
                setError(String.format("The selected type '%s' does not match the data.", type), ex);
            }

            rowsTable.updateUI();
        }
    }

    /**
     * Updates converter for the column name.
     *
     * @param converter The new column name converter.
     */
    private void updateColumnNameConverter(TypeConverter converter) {
        for (int col = 0 ; col < rowsTable.getColumnCount() ; col++) {
            TableColumn column = rowsTable.getColumnModel().getColumn(col);

            ColumnQualifier qualifier = (ColumnQualifier)column.getIdentifier();
            qualifier.setNameConverter(converter);
        }

        rowsTable.updateUI();

        for (int row = 0 ; row < columnsTable.getRowCount() ; row++) {
            ColumnQualifier qualifier = (ColumnQualifier)columnsTable.getValueAt(row, 1);
            qualifier.setNameConverter(converter);
        }

        if (scanner != null) {
            scanner.updateColumnNameConverter(converter);
        }

        columnsTable.updateUI();

        if (!rowsTableRemovedColumns.isEmpty()) {
            Map<ColumnQualifier, TableColumn> removedColumns = new HashMap<ColumnQualifier, TableColumn>();
            for (TableColumn column : rowsTableRemovedColumns.values()) {
                ColumnQualifier qualifier = (ColumnQualifier)column.getIdentifier();
                qualifier.setNameConverter(converter);

                removedColumns.put(qualifier, column);
            }
            rowsTableRemovedColumns = removedColumns;
        }
    }

    /**
     * Enables or disables the paging buttons.
     */
    private void togglePagingControls() {
        rowsPrev.setEnabled(this.scanner != null && this.scanner.hasPrev());
        rowsNext.setEnabled(this.scanner != null && this.scanner.hasNext());
    }

    private void toggleTableControls() {
        if (tablesListModel.getSize() > 0) {
            tableFilters.setEnabled(true);

            int[] selectedIndices = tablesList.getSelectedIndices();
            if (selectedIndices.length > 0) {
                tableDelete.setEnabled(true);
                tableTruncate.setEnabled(true);

                boolean tableEnabled = false;

                if (selectedIndices.length == 1) {
                    String tableName = (String)tablesListModel.getElementAt(selectedIndices[0]);
                    tableEnabled = tableEnabled(tableName);
                }

                tableCopy.setEnabled(tableEnabled);
                tablePaste.setEnabled(tableEnabled && hasTableInClipboard());
                tableExport.setEnabled(tableEnabled);
                tableFlush.setEnabled(tableEnabled || selectedIndices.length > 1);
                tableMetadata.setEnabled(tableEnabled);
            }
        }
        else {
            tableFilters.setEnabled(false);
            tableDelete.setEnabled(false);
            tableTruncate.setEnabled(false);
            tableCopy.setEnabled(false);
            tablePaste.setEnabled(false);
            tableExport.setEnabled(false);
            tableFlush.setEnabled(false);
            tableMetadata.setEnabled(false);
        }
    }

    private void toggleColumnControls(boolean enabled) {
        columnRefresh.setEnabled(enabled);
        columnFilters.setEnabled(enabled);
        columnScan.setEnabled(enabled);
        columnJump.setEnabled(enabled);
        columnPopulate.setEnabled(enabled);
        columnCheck.setEnabled(enabled);
        columnUncheck.setEnabled(enabled);
        columnConverters.setEnabled(enabled);
        columnAddConverter.setEnabled(enabled);

        boolean isEditable = getColumnNameConverter().isEditable();

        int row = columnsTable.getSelectedRow();
        if (row != -1) {
            ColumnType type = (ColumnType)columnsTable.getValueAt(row, 2);
            isEditable |= type.isEditable();
        }

        columnEditConverter.setEnabled(enabled && isEditable);
        columnDeleteConverter.setEnabled(enabled && isEditable);
    }

    private void toggleRowControls(boolean enabled) {
        int count = rowsTable.getSelectedRowCount();

        rowOpen.setEnabled(enabled && rowsTable.getRowCount() > 0);
        rowAdd.setEnabled(enabled);
        rowDelete.setEnabled(enabled && count > 0);
        rowCopy.setEnabled(enabled && count > 0);
        rowPaste.setEnabled(hasRowsInClipboard());
        rowSave.setEnabled(changeTracker.hasChanges());
    }

    private boolean tableEnabled(String tableName) {
        boolean enabled = false;

        try {
            enabled = connection.tableEnabled(tableName);
        }
        catch (Exception ex) {
            setError(String.format("Failed to access table '%s' information.", tableName), ex);
        }

        return enabled;
    }


    /**
     * Sets a filter.
     *
     * @param comboBox A combo box to be set.
     * @param value    A value to be selected on the combo box.
     */
    private static void setFilter(JComboBox comboBox, ItemListener listener, String value) {
        if (listener != null) {
            comboBox.removeItemListener(listener);
        }

        comboBox.setSelectedItem(value);
        comboBox.setToolTipText(value);

        if (listener != null) {
            comboBox.addItemListener(listener);
        }
    }

    /**
     * Fills a combo box with the provided values.
     *
     * @param comboBox A combo box to fill.
     * @param values   A list of values to add.
     */
    private static void fillComboBox(JComboBox comboBox, ItemListener listener, Iterable<String> values) {
        if (values != null) {
            if (listener != null) {
                comboBox.removeItemListener(listener);
            }

            comboBox.removeAllItems();

            comboBox.addItem("");

            for (String value : values) {
                comboBox.addItem(value);
            }

            if (listener != null) {
                comboBox.addItemListener(listener);
            }
        }
    }

    /**
     * Gets a list of filters from the provided combo box.
     *
     * @param comboBox A combo box containing the filters to get.
     * @return A list of filters.
     */
    private static Iterable<String> getFilters(JComboBox comboBox) {
        Collection<String> filters = new ArrayList<String>();
        for (int i = 0 ; i < comboBox.getItemCount() ; i++) {
            String item = (String)comboBox.getItemAt(i);
            if (item != null && !item.isEmpty()) {
                filters.add(item);
            }
        }
        return filters;
    }

    {
        // GUI initializer generated by IntelliJ IDEA GUI Designer
        // >>> IMPORTANT!! <<<
        // DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        topPanel = new JPanel();
        topPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        topSplitPane = new JSplitPane();
        topSplitPane.setContinuousLayout(true);
        topSplitPane.setDividerSize(9);
        topSplitPane.setOneTouchExpandable(true);
        topPanel.add(
            topSplitPane, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        topSplitPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setAlignmentX(0.5f);
        panel1.setAlignmentY(0.5f);
        panel1.setMaximumSize(new Dimension(-1, -1));
        panel1.setMinimumSize(new Dimension(-1, -1));
        panel1.setPreferredSize(new Dimension(230, -1));
        topSplitPane.setLeftComponent(panel1);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setAlignmentX(0.5f);
        panel2.setAlignmentY(0.5f);
        panel1.add(
            panel2, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        panel2.add(
            toolBar1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(-1, 20), null, 0, false));
        tableRefresh = new JButton();
        tableRefresh.setEnabled(true);
        tableRefresh.setHorizontalAlignment(0);
        tableRefresh.setIcon(new ImageIcon(getClass().getResource("/images/db-refresh.png")));
        tableRefresh.setMaximumSize(new Dimension(24, 24));
        tableRefresh.setMinimumSize(new Dimension(24, 24));
        tableRefresh.setPreferredSize(new Dimension(24, 24));
        tableRefresh.setText("");
        tableRefresh.setToolTipText("Refresh tables");
        toolBar1.add(tableRefresh);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        toolBar1.add(toolBar$Separator1);
        tableFilters = new JComboBox();
        tableFilters.setAlignmentX(0.0f);
        tableFilters.setAlignmentY(0.6f);
        tableFilters.setEditable(true);
        tableFilters.setFont(new Font(tableFilters.getFont().getName(), tableFilters.getFont().getStyle(), tableFilters.getFont().getSize()));
        tableFilters.setMaximumSize(new Dimension(32767, 26));
        tableFilters.setMinimumSize(new Dimension(50, 20));
        tableFilters.setPreferredSize(new Dimension(-1, -1));
        toolBar1.add(tableFilters);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setAlignmentX(0.5f);
        scrollPane1.setAlignmentY(0.5f);
        panel1.add(
            scrollPane1, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(256, 286), null, 0, false));
        tablesList = new JList();
        tablesList.setSelectionMode(2);
        scrollPane1.setViewportView(tablesList);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(
            panel3, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JToolBar toolBar2 = new JToolBar();
        toolBar2.setBorderPainted(false);
        toolBar2.setFloatable(false);
        toolBar2.setRollover(true);
        toolBar2.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        panel3.add(
            toolBar2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(-1, 20), null, 0, false));
        tableAdd = new JButton();
        tableAdd.setEnabled(true);
        tableAdd.setHorizontalAlignment(0);
        tableAdd.setIcon(new ImageIcon(getClass().getResource("/images/add-table.png")));
        tableAdd.setMaximumSize(new Dimension(24, 24));
        tableAdd.setMinimumSize(new Dimension(24, 24));
        tableAdd.setPreferredSize(new Dimension(24, 24));
        tableAdd.setText("");
        tableAdd.setToolTipText("Create a new table");
        toolBar2.add(tableAdd);
        tableDelete = new JButton();
        tableDelete.setEnabled(false);
        tableDelete.setHorizontalAlignment(0);
        tableDelete.setIcon(new ImageIcon(getClass().getResource("/images/delete-table.png")));
        tableDelete.setMaximumSize(new Dimension(24, 24));
        tableDelete.setMinimumSize(new Dimension(24, 24));
        tableDelete.setPreferredSize(new Dimension(24, 24));
        tableDelete.setText("");
        tableDelete.setToolTipText("Delete selected table(s)");
        toolBar2.add(tableDelete);
        tableTruncate = new JButton();
        tableTruncate.setEnabled(false);
        tableTruncate.setHorizontalAlignment(0);
        tableTruncate.setIcon(new ImageIcon(getClass().getResource("/images/truncate-table.png")));
        tableTruncate.setMaximumSize(new Dimension(24, 24));
        tableTruncate.setMinimumSize(new Dimension(24, 24));
        tableTruncate.setPreferredSize(new Dimension(24, 24));
        tableTruncate.setText("");
        tableTruncate.setToolTipText("Truncate selected table(s)");
        toolBar2.add(tableTruncate);
        tableCopy = new JButton();
        tableCopy.setEnabled(false);
        tableCopy.setIcon(new ImageIcon(getClass().getResource("/images/db-copy.png")));
        tableCopy.setMaximumSize(new Dimension(24, 24));
        tableCopy.setMinimumSize(new Dimension(24, 24));
        tableCopy.setPreferredSize(new Dimension(24, 24));
        tableCopy.setText("");
        tableCopy.setToolTipText("Copy table information to the clipboard");
        toolBar2.add(tableCopy);
        tablePaste = new JButton();
        tablePaste.setEnabled(false);
        tablePaste.setIcon(new ImageIcon(getClass().getResource("/images/db-paste.png")));
        tablePaste.setMaximumSize(new Dimension(24, 24));
        tablePaste.setMinimumSize(new Dimension(24, 24));
        tablePaste.setPreferredSize(new Dimension(24, 24));
        tablePaste.setText("");
        tablePaste.setToolTipText("Paste table from the clipboard");
        toolBar2.add(tablePaste);
        tableExport = new JButton();
        tableExport.setEnabled(false);
        tableExport.setIcon(new ImageIcon(getClass().getResource("/images/db-export.png")));
        tableExport.setMaximumSize(new Dimension(24, 24));
        tableExport.setMinimumSize(new Dimension(24, 24));
        tableExport.setPreferredSize(new Dimension(24, 24));
        tableExport.setText("");
        tableExport.setToolTipText("Export table to the file");
        toolBar2.add(tableExport);
        tableImport = new JButton();
        tableImport.setEnabled(true);
        tableImport.setIcon(new ImageIcon(getClass().getResource("/images/db-import.png")));
        tableImport.setMaximumSize(new Dimension(24, 24));
        tableImport.setMinimumSize(new Dimension(24, 24));
        tableImport.setPreferredSize(new Dimension(24, 24));
        tableImport.setText("");
        tableImport.setToolTipText("Import table from the file");
        toolBar2.add(tableImport);
        tableFlush = new JButton();
        tableFlush.setEnabled(false);
        tableFlush.setHorizontalAlignment(0);
        tableFlush.setIcon(new ImageIcon(getClass().getResource("/images/db-flush.png")));
        tableFlush.setMaximumSize(new Dimension(24, 24));
        tableFlush.setMinimumSize(new Dimension(24, 24));
        tableFlush.setPreferredSize(new Dimension(24, 24));
        tableFlush.setText("");
        tableFlush.setToolTipText("Force hbase to flush table to HFile");
        toolBar2.add(tableFlush);
        tableMetadata = new JButton();
        tableMetadata.setEnabled(false);
        tableMetadata.setIcon(new ImageIcon(getClass().getResource("/images/db-metadata.png")));
        tableMetadata.setMaximumSize(new Dimension(24, 24));
        tableMetadata.setMinimumSize(new Dimension(24, 24));
        tableMetadata.setPreferredSize(new Dimension(24, 24));
        tableMetadata.setText("");
        tableMetadata.setToolTipText("Show table's metadata");
        toolBar2.add(tableMetadata);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(
            panel4, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Tables:");
        panel4.add(
            label1, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel4.add(
            spacer1, new GridConstraints(
            0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        tablesNumber = new JLabel();
        tablesNumber.setText("0 of 0");
        panel4.add(
            tablesNumber, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        innerSplitPane = new JSplitPane();
        innerSplitPane.setContinuousLayout(true);
        innerSplitPane.setDividerSize(9);
        innerSplitPane.setOneTouchExpandable(true);
        topSplitPane.setRightComponent(innerSplitPane);
        innerSplitPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0), null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 2), -1, -1));
        panel5.setAlignmentX(0.5f);
        panel5.setAlignmentY(0.5f);
        panel5.setMaximumSize(new Dimension(-1, -1));
        panel5.setMinimumSize(new Dimension(-1, -1));
        panel5.setPreferredSize(new Dimension(250, -1));
        innerSplitPane.setLeftComponent(panel5);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(
            panel6, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JToolBar toolBar3 = new JToolBar();
        toolBar3.setFloatable(false);
        panel6.add(
            toolBar3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(-1, 20), null, 0, false));
        columnRefresh = new JButton();
        columnRefresh.setEnabled(false);
        columnRefresh.setHorizontalAlignment(0);
        columnRefresh.setIcon(new ImageIcon(getClass().getResource("/images/refresh.png")));
        columnRefresh.setMaximumSize(new Dimension(24, 24));
        columnRefresh.setMinimumSize(new Dimension(24, 24));
        columnRefresh.setPreferredSize(new Dimension(24, 24));
        columnRefresh.setText("");
        columnRefresh.setToolTipText("Refresh columns");
        toolBar3.add(columnRefresh);
        final JToolBar.Separator toolBar$Separator2 = new JToolBar.Separator();
        toolBar3.add(toolBar$Separator2);
        columnFilters = new JComboBox();
        columnFilters.setAlignmentX(0.0f);
        columnFilters.setAlignmentY(0.6f);
        columnFilters.setEditable(true);
        columnFilters.setMaximumSize(new Dimension(32767, 26));
        columnFilters.setMinimumSize(new Dimension(50, 20));
        columnFilters.setPreferredSize(new Dimension(-1, -1));
        toolBar3.add(columnFilters);
        final JToolBar.Separator toolBar$Separator3 = new JToolBar.Separator();
        toolBar3.add(toolBar$Separator3);
        columnScan = new JButton();
        columnScan.setEnabled(false);
        columnScan.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
        columnScan.setMinimumSize(new Dimension(24, 24));
        columnScan.setPreferredSize(new Dimension(24, 24));
        columnScan.setText("");
        columnScan.setToolTipText("Perform an advanced scan...");
        toolBar3.add(columnScan);
        columnJump = new JButton();
        columnJump.setEnabled(false);
        columnJump.setIcon(new ImageIcon(getClass().getResource("/images/jump.png")));
        columnJump.setMinimumSize(new Dimension(24, 24));
        columnJump.setPreferredSize(new Dimension(24, 24));
        columnJump.setText("");
        columnJump.setToolTipText("Jump to a specific row number");
        toolBar3.add(columnJump);
        columnPopulate = new JButton();
        columnPopulate.setEnabled(false);
        columnPopulate.setIcon(new ImageIcon(getClass().getResource("/images/populate.png")));
        columnPopulate.setMinimumSize(new Dimension(24, 24));
        columnPopulate.setPreferredSize(new Dimension(24, 24));
        columnPopulate.setText("");
        columnPopulate.setToolTipText("Populate rows for the selected columns");
        toolBar3.add(columnPopulate);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setDoubleBuffered(true);
        panel5.add(
            scrollPane2, new GridConstraints(
            3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        columnsTable = new JTable();
        columnsTable.setAutoResizeMode(1);
        scrollPane2.setViewportView(columnsTable);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(
            panel7, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JToolBar toolBar4 = new JToolBar();
        toolBar4.setBorderPainted(false);
        toolBar4.setFloatable(false);
        panel7.add(
            toolBar4, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
            null, new Dimension(-1, 20), null, 0, false));
        columnCheck = new JButton();
        columnCheck.setEnabled(false);
        columnCheck.setIcon(new ImageIcon(getClass().getResource("/images/select.png")));
        columnCheck.setMaximumSize(new Dimension(24, 24));
        columnCheck.setMinimumSize(new Dimension(24, 24));
        columnCheck.setPreferredSize(new Dimension(24, 24));
        columnCheck.setText("");
        columnCheck.setToolTipText("Select all columns");
        toolBar4.add(columnCheck);
        columnUncheck = new JButton();
        columnUncheck.setEnabled(false);
        columnUncheck.setIcon(new ImageIcon(getClass().getResource("/images/unselect.png")));
        columnUncheck.setMaximumSize(new Dimension(24, 24));
        columnUncheck.setMinimumSize(new Dimension(24, 24));
        columnUncheck.setPreferredSize(new Dimension(24, 24));
        columnUncheck.setText("");
        columnUncheck.setToolTipText("Unselect all columns");
        toolBar4.add(columnUncheck);
        final JToolBar.Separator toolBar$Separator4 = new JToolBar.Separator();
        toolBar4.add(toolBar$Separator4);
        columnConverters = new WideComboBox();
        columnConverters.setEnabled(false);
        columnConverters.setMaximumSize(new Dimension(120, 24));
        columnConverters.setMinimumSize(new Dimension(50, 24));
        columnConverters.setPreferredSize(new Dimension(-1, 24));
        columnConverters.setToolTipText("Choose type to be used to convert column name");
        toolBar4.add(columnConverters);
        final JToolBar.Separator toolBar$Separator5 = new JToolBar.Separator();
        toolBar4.add(toolBar$Separator5);
        columnAddConverter = new JButton();
        columnAddConverter.setEnabled(false);
        columnAddConverter.setIcon(new ImageIcon(getClass().getResource("/images/addConverter.png")));
        columnAddConverter.setMaximumSize(new Dimension(24, 24));
        columnAddConverter.setMinimumSize(new Dimension(24, 24));
        columnAddConverter.setPreferredSize(new Dimension(24, 24));
        columnAddConverter.setText("");
        columnAddConverter.setToolTipText("Add custom type converter");
        toolBar4.add(columnAddConverter);
        columnEditConverter = new JButton();
        columnEditConverter.setEnabled(false);
        columnEditConverter.setIcon(new ImageIcon(getClass().getResource("/images/editConverter.png")));
        columnEditConverter.setMaximumSize(new Dimension(24, 24));
        columnEditConverter.setMinimumSize(new Dimension(24, 24));
        columnEditConverter.setPreferredSize(new Dimension(24, 24));
        columnEditConverter.setText("");
        columnEditConverter.setToolTipText("Edit custom type converter");
        toolBar4.add(columnEditConverter);
        columnDeleteConverter = new JButton();
        columnDeleteConverter.setEnabled(false);
        columnDeleteConverter.setIcon(new ImageIcon(getClass().getResource("/images/deleteConverter.png")));
        columnDeleteConverter.setMaximumSize(new Dimension(24, 24));
        columnDeleteConverter.setMinimumSize(new Dimension(24, 24));
        columnDeleteConverter.setPreferredSize(new Dimension(24, 24));
        columnDeleteConverter.setText("");
        columnDeleteConverter.setToolTipText("Delete custom type converter");
        toolBar4.add(columnDeleteConverter);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(
            panel8, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Columns:");
        panel8.add(
            label2, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel8.add(
            spacer2, new GridConstraints(
            0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        columnsNumber = new JLabel();
        columnsNumber.setText("0 of 0");
        panel8.add(
            columnsNumber, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(2, 1, new Insets(0, 4, 0, 0), -1, -1));
        panel9.setAlignmentX(0.5f);
        panel9.setAlignmentY(0.5f);
        panel9.setMaximumSize(new Dimension(-1, -1));
        panel9.setMinimumSize(new Dimension(200, -1));
        panel9.setPreferredSize(new Dimension(-1, -1));
        innerSplitPane.setRightComponent(panel9);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(
            panel10, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Rows:");
        panel10.add(
            label3, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
            null, 0, false));
        final JToolBar toolBar5 = new JToolBar();
        toolBar5.setBorderPainted(false);
        toolBar5.setFloatable(false);
        panel10.add(
            toolBar5, new GridConstraints(
            0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        rowOpen = new JButton();
        rowOpen.setEnabled(false);
        rowOpen.setIcon(new ImageIcon(getClass().getResource("/images/viewer.png")));
        rowOpen.setMinimumSize(new Dimension(24, 24));
        rowOpen.setPreferredSize(new Dimension(24, 24));
        rowOpen.setText("");
        rowOpen.setToolTipText("Open rows in external viewer");
        toolBar5.add(rowOpen);
        rowAdd = new JButton();
        rowAdd.setEnabled(false);
        rowAdd.setIcon(new ImageIcon(getClass().getResource("/images/add.png")));
        rowAdd.setMinimumSize(new Dimension(24, 24));
        rowAdd.setPreferredSize(new Dimension(24, 24));
        rowAdd.setText("");
        rowAdd.setToolTipText("Create a new row");
        toolBar5.add(rowAdd);
        rowDelete = new JButton();
        rowDelete.setEnabled(false);
        rowDelete.setIcon(new ImageIcon(getClass().getResource("/images/delete.png")));
        rowDelete.setMinimumSize(new Dimension(24, 24));
        rowDelete.setPreferredSize(new Dimension(24, 24));
        rowDelete.setText("");
        rowDelete.setToolTipText("Delete selected rows");
        toolBar5.add(rowDelete);
        rowCopy = new JButton();
        rowCopy.setEnabled(false);
        rowCopy.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
        rowCopy.setMaximumSize(new Dimension(49, 27));
        rowCopy.setMinimumSize(new Dimension(24, 24));
        rowCopy.setPreferredSize(new Dimension(24, 24));
        rowCopy.setText("");
        rowCopy.setToolTipText("Copy selected rows to the clipboard");
        toolBar5.add(rowCopy);
        rowPaste = new JButton();
        rowPaste.setEnabled(false);
        rowPaste.setIcon(new ImageIcon(getClass().getResource("/images/paste.png")));
        rowPaste.setMaximumSize(new Dimension(49, 27));
        rowPaste.setMinimumSize(new Dimension(24, 24));
        rowPaste.setPreferredSize(new Dimension(24, 24));
        rowPaste.setText("");
        rowPaste.setToolTipText("Paste rows from the clipboard");
        toolBar5.add(rowPaste);
        rowSave = new JButton();
        rowSave.setEnabled(false);
        rowSave.setIcon(new ImageIcon(getClass().getResource("/images/save.png")));
        rowSave.setMinimumSize(new Dimension(24, 24));
        rowSave.setPreferredSize(new Dimension(24, 24));
        rowSave.setText("");
        rowSave.setToolTipText("Save all modified rows to the hbase");
        toolBar5.add(rowSave);
        final JToolBar toolBar6 = new JToolBar();
        toolBar6.setBorderPainted(false);
        toolBar6.setFloatable(false);
        panel10.add(
            toolBar6, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        rowsNumber = new JSpinner();
        rowsNumber.setDoubleBuffered(true);
        rowsNumber.setEnabled(true);
        rowsNumber.setMinimumSize(new Dimension(45, 24));
        rowsNumber.setPreferredSize(new Dimension(60, 24));
        toolBar6.add(rowsNumber);
        final JToolBar.Separator toolBar$Separator6 = new JToolBar.Separator();
        toolBar6.add(toolBar$Separator6);
        rowsVisible = new JLabel();
        rowsVisible.setText("?");
        toolBar6.add(rowsVisible);
        final JToolBar.Separator toolBar$Separator7 = new JToolBar.Separator();
        toolBar6.add(toolBar$Separator7);
        final JLabel label4 = new JLabel();
        label4.setText("of");
        toolBar6.add(label4);
        final JToolBar.Separator toolBar$Separator8 = new JToolBar.Separator();
        toolBar6.add(toolBar$Separator8);
        rowsTotal = new JLabel();
        rowsTotal.setText("?");
        toolBar6.add(rowsTotal);
        rowsNumberIcon = new JLabel();
        rowsNumberIcon.setIcon(new ImageIcon(getClass().getResource("/images/busy.gif")));
        rowsNumberIcon.setText("");
        rowsNumberIcon.setVisible(false);
        toolBar6.add(rowsNumberIcon);
        final JToolBar.Separator toolBar$Separator9 = new JToolBar.Separator();
        toolBar6.add(toolBar$Separator9);
        rowsPrev = new JButton();
        rowsPrev.setEnabled(false);
        rowsPrev.setIcon(new ImageIcon(getClass().getResource("/images/prev.png")));
        rowsPrev.setMaximumSize(new Dimension(49, 27));
        rowsPrev.setMinimumSize(new Dimension(24, 24));
        rowsPrev.setPreferredSize(new Dimension(24, 24));
        rowsPrev.setText("");
        rowsPrev.setToolTipText("Go to the prvious page");
        toolBar6.add(rowsPrev);
        rowsNext = new JButton();
        rowsNext.setEnabled(false);
        rowsNext.setIcon(new ImageIcon(getClass().getResource("/images/next.png")));
        rowsNext.setMaximumSize(new Dimension(49, 27));
        rowsNext.setMinimumSize(new Dimension(24, 24));
        rowsNext.setPreferredSize(new Dimension(24, 24));
        rowsNext.setText("");
        rowsNext.setToolTipText("Go to the next page");
        toolBar6.add(rowsNext);
        final JScrollPane scrollPane3 = new JScrollPane();
        panel9.add(
            scrollPane3, new GridConstraints(
            1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        rowsTable = new JTable();
        rowsTable.setAutoCreateRowSorter(true);
        rowsTable.setAutoResizeMode(0);
        rowsTable.setCellSelectionEnabled(true);
        rowsTable.setColumnSelectionAllowed(true);
        scrollPane3.setViewportView(rowsTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return topPanel;
    }

    //endregion

    /**
     * Represents the direction of the loading operation.
     */
    private enum Direction {
        /**
         * Reload current data.
         */
        Current,
        /**
         * Load next data.
         */
        Forward,
        /**
         * Load previous data.
         */
        Backward
    }
}
