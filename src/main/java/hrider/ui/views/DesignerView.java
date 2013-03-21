package hrider.ui.views;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import hrider.config.ClusterConfig;
import hrider.config.GlobalConfig;
import hrider.data.*;
import hrider.export.FileExporter;
import hrider.filters.EmptyFilter;
import hrider.filters.Filter;
import hrider.filters.PatternFilter;
import hrider.hbase.Connection;
import hrider.hbase.HbaseActionListener;
import hrider.hbase.Query;
import hrider.hbase.QueryScanner;
import hrider.system.ClipboardData;
import hrider.system.ClipboardListener;
import hrider.system.InMemoryClipboard;
import hrider.ui.ChangeTracker;
import hrider.ui.ChangeTrackerListener;
import hrider.ui.MessageHandler;
import hrider.ui.design.JCellEditor;
import hrider.ui.design.JCheckBoxRenderer;
import hrider.ui.design.JTableModel;
import hrider.ui.design.ResizeableTableHeader;
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
public class DesignerView {

    //region Variables
    private JPanel                   topPanel;
    private JList                    tablesList;
    private DefaultListModel         tablesListModel;
    private JTable                   rowsTable;
    private JButton                  populateButton;
    private JTable                   columnsTable;
    private JButton                  updateRowButton;
    private JButton                  deleteRowButton;
    private JButton                  addRowButton;
    private JButton                  truncateTableButton;
    private JButton                  scanButton;
    private JButton                  addTableButton;
    private JButton                  deleteTableButton;
    private JSpinner                 rowsNumberSpinner;
    private JButton                  showPrevPageButton;
    private JButton                  showNextPageButton;
    private JLabel                   rowsNumberLabel;
    private JLabel                   visibleRowsLabel;
    private JButton                  checkAllButton;
    private JButton                  copyRowButton;
    private JButton                  pasteRowButton;
    private JButton                  refreshTablesButton;
    private JButton                  copyTableButton;
    private JButton                  pasteTableButton;
    private JButton                  uncheckAllButton;
    private JSplitPane               topSplitPane;
    private JSplitPane               innerSplitPane;
    private JLabel                   columnsNumber;
    private JLabel                   tablesNumber;
    private JComboBox                tablesFilter;
    private DefaultComboBoxModel     tablesFilterModel;
    private JComboBox                columnsFilter;
    private DefaultComboBoxModel     columnsFilterModel;
    private JButton                  jumpButton;
    private JButton                  openInViewerButton;
    private JButton                  importTableButton;
    private JButton                  exportTableButton;
    private JButton                  refreshColumnsButton;
    private JButton                  flushTableButton;
    private JButton                  tableMetadataButton;
    private DefaultTableModel        columnsTableModel;
    private DefaultTableModel        rowsTableModel;
    private Query                    lastQuery;
    private QueryScanner             scanner;
    private JPanel                   owner;
    private Connection               connection;
    private ChangeTracker            changeTracker;
    private Map<String, TableColumn> rowsTableRemovedColumns;
    private ClusterConfig            clusterConfig;
    //endregion

    //region Constructor
    public DesignerView(final JPanel owner, final Connection connection) {

        this.owner = owner;
        this.connection = connection;
        this.changeTracker = new ChangeTracker();
        this.rowsTableRemovedColumns = new HashMap<String, TableColumn>();
        this.clusterConfig = new ClusterConfig(this.connection.getServerName());
        this.clusterConfig.setConnection(connection.getConnectionDetails());
        this.tablesFilterModel = new DefaultComboBoxModel();
        this.tablesFilter.setModel(this.tablesFilterModel);
        this.columnsFilterModel = new DefaultComboBoxModel();
        this.columnsFilter.setModel(this.columnsFilterModel);

        fillComboBox(tablesFilter, this.clusterConfig.getTableFilters());

        String tableFilter = this.clusterConfig.getSelectedTableFilter();
        if (tableFilter != null) {
            this.tablesFilter.setSelectedItem(tableFilter);
        }

        InMemoryClipboard.addListener(
            new ClipboardListener() {
                @Override
                public void onChanged(ClipboardData data) {
                    pasteTableButton.setEnabled(hasTableInClipboard());
                    pasteRowButton.setEnabled(hasRowsInClipboard());
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

        this.rowsNumberSpinner.setModel(new SpinnerNumberModel(100, 1, 10000, 100));

        this.tablesFilter.addItemListener(
            new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        clusterConfig.setSelectedTableFilter((String)e.getItem());

                        loadTables();
                    }
                }
            });

        this.tablesFilter.getEditor().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String item = (String)tablesFilter.getEditor().getItem();
                    if (item != null && !item.isEmpty()) {
                        if (tablesFilterModel.getIndexOf(item) == -1) {
                            tablesFilter.addItem(item);
                        }

                        tablesFilter.setSelectedItem(item);
                        clusterConfig.setTablesFilter(getFilters(tablesFilter));
                    }
                    else {
                        Object selectedItem = tablesFilter.getSelectedItem();
                        if (selectedItem != null) {
                            tablesFilter.removeItem(selectedItem);
                            clusterConfig.setTablesFilter(getFilters(tablesFilter));
                        }
                    }
                }
            });

        this.columnsFilter.addItemListener(
            new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        clusterConfig.setSelectedColumnFilter(getSelectedTableName(), (String)e.getItem());

                        populateColumnsTable(false);
                    }
                }
            });

        this.columnsFilter.getEditor().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String item = (String)columnsFilter.getEditor().getItem();
                    if (item != null && !item.isEmpty()) {
                        if (columnsFilterModel.getIndexOf(item) == -1) {
                            columnsFilter.addItem(item);
                        }

                        columnsFilter.setSelectedItem(item);
                        clusterConfig.setColumnsFilter(getSelectedTableName(), getFilters(columnsFilter));
                    }
                    else {
                        Object selectedItem = columnsFilter.getSelectedItem();
                        if (selectedItem != null) {
                            columnsFilter.removeItem(selectedItem);
                            clusterConfig.setColumnsFilter(getSelectedTableName(), getFilters(columnsFilter));
                        }
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

        this.jumpButton.addActionListener(
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

        this.populateButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    lastQuery = null;

                    populateRowsTable(Direction.Current);
                }
            });

        this.scanButton.addActionListener(
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

        this.openInViewerButton.addActionListener(
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

        this.addRowButton.addActionListener(
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
                                        getSelectedTableName(), cell.getColumn().getFullName(), cell.getTypedValue().getType().toString());
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

        this.deleteRowButton.addActionListener(
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


        this.copyRowButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    JTableModel.stopCellEditing(rowsTable);

                    copySelectedRowsToClipboard();
                }
            });

        this.pasteRowButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    JTableModel.stopCellEditing(rowsTable);

                    pasteRowsFromClipboard();
                }
            });

        this.updateRowButton.addActionListener(
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
                            updateRowButton.setEnabled(changeTracker.hasChanges());
                        }
                        finally {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.addTableButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    AddTableDialog dialog = new AddTableDialog();
                    if (dialog.showDialog(topPanel)) {
                        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            connection.createOrModifyTable(dialog.getTableDescriptor());

                            tablesListModel.addElement(dialog.getTableDescriptor().getName());
                            tablesList.setSelectedValue(dialog.getTableDescriptor().getName(), true);
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

        this.deleteTableButton.addActionListener(
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

        this.truncateTableButton.addActionListener(
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

        this.showPrevPageButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    populateRowsTable(Direction.Backward);
                }
            });

        this.showNextPageButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    populateRowsTable(Direction.Forward);
                }
            });

        this.checkAllButton.addActionListener(
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

        this.uncheckAllButton.addActionListener(
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

        this.flushTableButton.addActionListener(
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

        this.refreshTablesButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    populate();
                }
            });

        this.copyTableButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    copyTableToClipboard();
                }
            });

        this.pasteTableButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    pasteTableFromClipboard();
                }
            });

        this.exportTableButton.addActionListener(
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

        this.importTableButton.addActionListener(
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

                        ImportTableDialog dialog = new ImportTableDialog(connection, tableName, getShownTypedColumns(), columnFamilies);
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

        this.tableMetadataButton.addActionListener(
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

        this.refreshColumnsButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    scanner = null;
                    populateColumnsTable(true);
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
        MessageHandler.addError("", null);
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
     * Loads the table names.
     */
    private void loadTables() {
        Object selectedTable = this.tablesList.getSelectedValue();

        clearError();

        this.refreshTablesButton.setEnabled(false);
        this.addTableButton.setEnabled(false);
        this.deleteTableButton.setEnabled(false);
        this.truncateTableButton.setEnabled(false);
        this.copyTableButton.setEnabled(false);
        this.exportTableButton.setEnabled(false);
        this.tableMetadataButton.setEnabled(false);
        this.importTableButton.setEnabled(false);

        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            this.tablesListModel.clear();

            Filter filter;

            String value = (String)tablesFilter.getSelectedItem();
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

            this.addTableButton.setEnabled(true);
            this.importTableButton.setEnabled(true);
            this.tablesNumber.setText(String.format("%s of %s", this.tablesListModel.getSize(), tables.size()));
            this.tablesList.setSelectedValue(selectedTable, true);
        }
        catch (Exception ex) {
            setError("Failed to connect to hadoop: ", ex);
        }
        finally {
            this.refreshTablesButton.setEnabled(true);
            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Initializes a table list used to present all available tables in the hbase cluster.
     */
    private void initializeTablesList() {
        this.pasteTableButton.setEnabled(hasTableInClipboard());

        this.tablesListModel = new DefaultListModel();
        this.tablesList.setModel(this.tablesListModel);

        this.tablesList.addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    int selectedIndices = tablesList.getSelectedIndices().length;

                    flushTableButton.setEnabled(selectedIndices > 0);
                    deleteTableButton.setEnabled(selectedIndices > 0);
                    truncateTableButton.setEnabled(selectedIndices > 0);

                    if (selectedIndices == 1) {
                        copyTableButton.setEnabled(true);
                        exportTableButton.setEnabled(true);
                        tableMetadataButton.setEnabled(true);

                        scanner = null;

                        String currentFilter = clusterConfig.getSelectedColumnFilter(getSelectedTableName());

                        fillComboBox(columnsFilter, clusterConfig.getColumnFilters(getSelectedTableName()));
                        setFilter(columnsFilter, currentFilter);

                        populateColumnsTable(true);
                    }
                    else {
                        clearRows(columnsTable);
                        clearTable(rowsTable);

                        enableDisablePagingButtons();

                        copyTableButton.setEnabled(false);
                        exportTableButton.setEnabled(false);
                        tableMetadataButton.setEnabled(false);
                        rowsNumberLabel.setText("?");
                        visibleRowsLabel.setText("?");
                        pasteRowButton.setEnabled(false);
                        populateButton.setEnabled(false);
                        jumpButton.setEnabled(false);
                        scanButton.setEnabled(false);
                        addRowButton.setEnabled(false);
                        checkAllButton.setEnabled(false);
                        uncheckAllButton.setEnabled(false);
                        refreshColumnsButton.setEnabled(false);
                    }
                }
            });

        this.tablesList.addKeyListener(
            new

                KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        if (e.isControlDown()) {
                            if (e.getKeyCode() == KeyEvent.VK_C) {
                                clearError();
                                if (copyTableButton.isEnabled()) {
                                    copyTableToClipboard();
                                }
                            }
                            else if (e.getKeyCode() == KeyEvent.VK_V) {
                                clearError();
                                if (pasteTableButton.isEnabled()) {
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

        JComboBox comboBox = new JComboBox();

        for (ObjectType objectType : ObjectType.values()) {
            comboBox.addItem(objectType);
        }

        this.columnsTable.getColumn("Column Type").setCellEditor(new DefaultCellEditor(comboBox));

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

                        setRowsTableColumnVisible(qualifier.getFullName(), isShown);
                    }
                    else if ("Column Type".equals(columnName)) {
                        ColumnQualifier qualifier = (ColumnQualifier)model.getValueAt(e.getFirstRow(), 1);
                        ObjectType type = (ObjectType)model.getValueAt(e.getFirstRow(), column);

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
                    deleteRowButton.setEnabled(true);
                    copyRowButton.setEnabled(true);
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

                            if (copyRowButton.isEnabled()) {
                                JTableModel.stopCellEditing(rowsTable);
                                copySelectedRowsToClipboard();
                            }
                        }
                        else if (e.getKeyCode() == KeyEvent.VK_V) {
                            clearError();

                            if (pasteRowButton.isEnabled()) {
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
                        updateRowButton.setEnabled(changeTracker.hasChanges());
                        deleteRowButton.setEnabled(false);
                        copyRowButton.setEnabled(false);
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

                    updateRowButton.setEnabled(changeTracker.hasChanges());
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

                this.rowsNumberLabel.setText("?");
                this.visibleRowsLabel.setText("?");
                this.rowsNumberSpinner.setEnabled(true);
                this.pasteRowButton.setEnabled(hasRowsInClipboard());
            }

            enableDisablePagingButtons();

            String tableName = getSelectedTableName();
            if (tableName != null) {
                loadColumns(tableName, row);
            }

            if (this.columnsTableModel.getRowCount() > 0) {
                this.populateButton.setEnabled(true);
                this.jumpButton.setEnabled(true);
                this.scanButton.setEnabled(true);
                this.addRowButton.setEnabled(true);
                this.checkAllButton.setEnabled(true);
                this.uncheckAllButton.setEnabled(true);
                this.refreshColumnsButton.setEnabled(true);
            }
            else {
                this.populateButton.setEnabled(false);
                this.jumpButton.setEnabled(false);
                this.scanButton.setEnabled(false);
                this.addRowButton.setEnabled(false);
                this.checkAllButton.setEnabled(false);
                this.uncheckAllButton.setEnabled(false);
                this.refreshColumnsButton.setEnabled(false);
            }
        }
        finally {
            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Populates a rows table. The method loads the table content. The number of loaded rows depends on the parameter defined by the user
     * in the {@link DesignerView#rowsNumberSpinner} control.
     *
     * @param direction Defines what rows should be presented to the user. {@link Direction#Current},
     *                  {@link Direction#Forward} or {@link Direction#Backward}.
     */
    private void populateRowsTable(Direction direction) {
        populateRowsTable(0, direction);
    }

    /**
     * Populates a rows table. The method loads the table content. The number of loaded rows depends on the parameter defined by the user
     * in the {@link DesignerView#rowsNumberSpinner} control.
     *
     * @param offset    The first row to start loading from.
     * @param direction Defines what rows should be presented to the user. {@link Direction#Current},
     *                  {@link Direction#Forward} or {@link Direction#Backward}.
     */
    private void populateRowsTable(long offset, Direction direction) {

        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            JTableModel.stopCellEditing(columnsTable);

            this.rowsNumberSpinner.setEnabled(true);

            String tableName = getSelectedTableName();
            if (tableName != null) {
                clearTable(rowsTable);

                Map<String, ObjectType> columnTypes = getColumnTypes();

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
                loadRows(tableName, rows);

                this.enableDisablePagingButtons();

                this.visibleRowsLabel.setText(String.format("%s - %s", this.scanner.getLastRow() - rows.size() + 1, this.scanner.getLastRow()));
                this.rowsNumberSpinner.setEnabled(this.scanner.getLastRow() <= getPageSize());

                // To get the number of rows can take the time.
                Thread thread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                long totalNumberOfRows = scanner.getRowsCount();
                                rowsNumberLabel.setText(String.valueOf(totalNumberOfRows));

                                enableDisablePagingButtons();
                            }
                            catch (Exception e) {
                                setError("Failed to get the number of rows in the table.", e);
                            }
                        }
                    });
                thread.start();
            }

            this.openInViewerButton.setEnabled(this.rowsTable.getRowCount() > 0);
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
     * @param tableName  The name of the table the column belongs to.
     * @param columnName The name of the column which rows are need to be populated.
     * @param rows       The data to populate.
     */
    private void populateColumnOnRowsTable(String tableName, String columnName, Iterable<DataRow> rows) {

        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            int rowIndex = 0;
            int columnIndex = this.rowsTable.getColumnModel().getColumnIndex(columnName);

            for (DataRow row : rows) {
                DataCell cell = row.getCell(columnName);
                if (cell == null) {
                    cell = new DataCell(row, new ColumnQualifier(columnName), new TypedObject(getColumnType(tableName, columnName), null));
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
            }

            Filter filter;

            String value = (String)columnsFilter.getSelectedItem();
            if (value == null || value.isEmpty()) {
                filter = new EmptyFilter();
            }
            else {
                filter = new PatternFilter(value);
            }

            if (row == null) {
                this.scanner.setColumnTypes(
                    new HashMap<String, ObjectType>() {{
                        put(ColumnQualifier.KEY.getName(), ObjectType.String);
                    }});

                row = this.scanner.getFirstRow();
            }

            Collection<ColumnQualifier> columns = this.scanner.getColumns(getPageSize());
            for (ColumnQualifier column : columns) {
                boolean isColumnVisible = column.isKey() || filter.match(column.getFullName());
                if (isColumnVisible) {
                    addColumnToColumnsTable(tableName, column, row);
                }

                setRowsTableColumnVisible(column.getFullName(), isColumnVisible && isShown(tableName, column.getFullName()));
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
        ObjectType columnType = getColumnType(tableName, qualifier.getFullName());

        if (columnType == null && row != null) {
            DataCell cell = row.getCell(qualifier);
            if (cell != null) {
                columnType = cell.guessType();
            }
        }

        if (columnType == null) {
            columnType = ObjectType.fromColumn(qualifier.getFullName());
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
                addColumnToRowsTable(tableName, qualifier.getFullName(), j++);
            }
        }
    }

    /**
     * Shows or hides a column in rows table.
     *
     * @param columnName The name of the column.
     * @param isVisible  Indicates if the columns should be shown or hidden.
     */
    private void setRowsTableColumnVisible(String columnName, boolean isVisible) {
        if (this.rowsTable.getRowCount() > 0) {
            if (isVisible) {
                TableColumn tableColumn = this.rowsTableRemovedColumns.get(columnName);
                if (tableColumn != null) {
                    this.rowsTable.addColumn(tableColumn);
                    this.rowsTableRemovedColumns.remove(columnName);

                    this.rowsTable.moveColumn(this.rowsTable.getColumnCount() - 1, getColumnIndex(columnName));
                }
                else {
                    if (getColumn(columnName, this.rowsTable) == null) {
                        addColumnToRowsTable(getSelectedTableName(), columnName, this.rowsTable.getColumnCount());

                        if (this.scanner != null) {
                            populateColumnOnRowsTable(getSelectedTableName(), columnName, this.scanner.current());
                        }

                        this.rowsTable.moveColumn(this.rowsTable.getColumnCount() - 1, getColumnIndex(columnName));
                    }
                }
            }
            else {
                TableColumn tableColumn = getColumn(columnName, this.rowsTable);
                if (tableColumn != null) {
                    this.rowsTable.removeColumn(tableColumn);
                    this.rowsTableRemovedColumns.put(columnName, tableColumn);
                }
            }
        }
    }

    /**
     * Adds a single column to the rows table.
     *
     * @param tableName   The name of the table the column belongs to.
     * @param columnName  The name of the column to add.
     * @param columnIndex The index the column should be added at.
     */
    private void addColumnToRowsTable(String tableName, String columnName, int columnIndex) {
        TableColumn tableColumn = new TableColumn(columnIndex);
        tableColumn.setIdentifier(columnName);
        tableColumn.setHeaderValue(columnName);
        tableColumn.setCellEditor(new JCellEditor(this.changeTracker, !ColumnQualifier.isKey(columnName)));

        try {
            Integer width = this.clusterConfig.getTableConfig(Integer.class, tableName, columnName, "size");
            if (width != null) {
                tableColumn.setPreferredWidth(width);
            }
        }
        catch (NumberFormatException ignore) {
        }

        this.rowsTable.addColumn(tableColumn);
        this.rowsTableModel.addColumn(columnName);
    }

    /**
     * Adds rows to the rows table.
     *
     * @param rows A list of rows to add.
     */
    private void loadRows(String tableName, Iterable<DataRow> rows) {
        clearRows(this.rowsTable);

        for (DataRow row : rows) {
            Collection<Object> values = new ArrayList<Object>(rowsTable.getColumnCount());
            for (int i = 0 ; i < rowsTable.getColumnCount() ; i++) {
                String columnName = rowsTable.getColumnName(i);
                DataCell cell = row.getCell(columnName);
                if (cell == null) {
                    cell = new DataCell(row, new ColumnQualifier(columnName), new TypedObject(getColumnType(tableName, columnName), null));
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
    private void pasteRowsFromClipboard() {
        this.pasteRowButton.setEnabled(false);

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
                                        getSelectedTableName(), cell.getColumn().getFullName(), cell.getTypedValue().getType().toString());
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
                                    byte[] a1 = o1.getKey().toByteArray();
                                    byte[] a2 = o2.getKey().toByteArray();

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
    private void pasteTableFromClipboard() {
        this.pasteTableButton.setEnabled(false);

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
    private Map<String, ObjectType> getColumnTypes() {
        Map<String, ObjectType> columnTypes = new HashMap<String, ObjectType>();
        for (int i = 0 ; i < columnsTable.getRowCount() ; i++) {
            ColumnQualifier qualifier = (ColumnQualifier)columnsTableModel.getValueAt(i, 1);
            columnTypes.put(qualifier.getFullName(), (ObjectType)columnsTableModel.getValueAt(i, 2));
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
                        (ColumnQualifier)this.columnsTableModel.getValueAt(i, 1), (ObjectType)this.columnsTableModel.getValueAt(i, 2)));
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
        return (Integer)this.rowsNumberSpinner.getValue();
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
     * Gets a list of populated tables.
     *
     * @return A list of tables.
     */
    private List<String> getTables() {
        List<String> tables = new ArrayList<String>();
        for (int i = 0 ; i < this.tablesListModel.size() ; i++) {
            tables.add(this.tablesListModel.getElementAt(i).toString());
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
    private ObjectType getColumnType(String tableName, String columnName) {
        return this.clusterConfig.getTableConfig(ObjectType.class, tableName, columnName);
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
     * Enables or disables the paging buttons.
     */
    private void enableDisablePagingButtons() {
        this.showPrevPageButton.setEnabled(this.scanner != null && this.scanner.hasPrev());
        this.showNextPageButton.setEnabled(this.scanner != null && this.scanner.hasNext());
    }

    /**
     * Sets a filter.
     *
     * @param comboBox A combo box to be set.
     * @param value    A value to be selected on the combo box.
     */
    private static void setFilter(JComboBox comboBox, String value) {
        comboBox.setSelectedItem(value);
        comboBox.setToolTipText(value);
    }

    /**
     * Fills a combo box with the provided values.
     *
     * @param comboBox A combo box to fill.
     * @param values   A list of values to add.
     */
    private static void fillComboBox(JComboBox comboBox, Iterable<String> values) {
        if (values != null) {
            comboBox.removeAllItems();

            comboBox.addItem("");

            for (String value : values) {
                comboBox.addItem(value);
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
        refreshTablesButton = new JButton();
        refreshTablesButton.setEnabled(true);
        refreshTablesButton.setHorizontalAlignment(0);
        refreshTablesButton.setIcon(new ImageIcon(getClass().getResource("/images/db-refresh.png")));
        refreshTablesButton.setMaximumSize(new Dimension(24, 24));
        refreshTablesButton.setMinimumSize(new Dimension(24, 24));
        refreshTablesButton.setPreferredSize(new Dimension(24, 24));
        refreshTablesButton.setText("");
        refreshTablesButton.setToolTipText("Refresh tables");
        toolBar1.add(refreshTablesButton);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        toolBar1.add(toolBar$Separator1);
        tablesFilter = new JComboBox();
        tablesFilter.setAlignmentX(0.0f);
        tablesFilter.setAlignmentY(0.6f);
        tablesFilter.setEditable(true);
        tablesFilter.setFont(new Font(tablesFilter.getFont().getName(), tablesFilter.getFont().getStyle(), tablesFilter.getFont().getSize()));
        tablesFilter.setMaximumSize(new Dimension(32767, 26));
        tablesFilter.setMinimumSize(new Dimension(50, 20));
        tablesFilter.setPreferredSize(new Dimension(-1, -1));
        toolBar1.add(tablesFilter);
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
        addTableButton = new JButton();
        addTableButton.setEnabled(false);
        addTableButton.setHorizontalAlignment(0);
        addTableButton.setIcon(new ImageIcon(getClass().getResource("/images/add-table.png")));
        addTableButton.setMaximumSize(new Dimension(24, 24));
        addTableButton.setMinimumSize(new Dimension(24, 24));
        addTableButton.setPreferredSize(new Dimension(24, 24));
        addTableButton.setText("");
        addTableButton.setToolTipText("Create a new table");
        toolBar2.add(addTableButton);
        deleteTableButton = new JButton();
        deleteTableButton.setEnabled(false);
        deleteTableButton.setHorizontalAlignment(0);
        deleteTableButton.setIcon(new ImageIcon(getClass().getResource("/images/delete-table.png")));
        deleteTableButton.setMaximumSize(new Dimension(24, 24));
        deleteTableButton.setMinimumSize(new Dimension(24, 24));
        deleteTableButton.setPreferredSize(new Dimension(24, 24));
        deleteTableButton.setText("");
        deleteTableButton.setToolTipText("Delete selected table(s)");
        toolBar2.add(deleteTableButton);
        truncateTableButton = new JButton();
        truncateTableButton.setEnabled(false);
        truncateTableButton.setHorizontalAlignment(0);
        truncateTableButton.setIcon(new ImageIcon(getClass().getResource("/images/truncate-table.png")));
        truncateTableButton.setMaximumSize(new Dimension(24, 24));
        truncateTableButton.setMinimumSize(new Dimension(24, 24));
        truncateTableButton.setPreferredSize(new Dimension(24, 24));
        truncateTableButton.setText("");
        truncateTableButton.setToolTipText("Truncate selected table(s)");
        toolBar2.add(truncateTableButton);
        copyTableButton = new JButton();
        copyTableButton.setEnabled(false);
        copyTableButton.setIcon(new ImageIcon(getClass().getResource("/images/db-copy.png")));
        copyTableButton.setMaximumSize(new Dimension(24, 24));
        copyTableButton.setMinimumSize(new Dimension(24, 24));
        copyTableButton.setPreferredSize(new Dimension(24, 24));
        copyTableButton.setText("");
        copyTableButton.setToolTipText("Copy table information to the clipboard");
        toolBar2.add(copyTableButton);
        pasteTableButton = new JButton();
        pasteTableButton.setEnabled(false);
        pasteTableButton.setIcon(new ImageIcon(getClass().getResource("/images/db-paste.png")));
        pasteTableButton.setMaximumSize(new Dimension(24, 24));
        pasteTableButton.setMinimumSize(new Dimension(24, 24));
        pasteTableButton.setPreferredSize(new Dimension(24, 24));
        pasteTableButton.setText("");
        pasteTableButton.setToolTipText("Paste table from the clipboard");
        toolBar2.add(pasteTableButton);
        exportTableButton = new JButton();
        exportTableButton.setEnabled(false);
        exportTableButton.setIcon(new ImageIcon(getClass().getResource("/images/db-export.png")));
        exportTableButton.setMaximumSize(new Dimension(24, 24));
        exportTableButton.setMinimumSize(new Dimension(24, 24));
        exportTableButton.setPreferredSize(new Dimension(24, 24));
        exportTableButton.setText("");
        exportTableButton.setToolTipText("Export table to the file");
        toolBar2.add(exportTableButton);
        importTableButton = new JButton();
        importTableButton.setEnabled(false);
        importTableButton.setIcon(new ImageIcon(getClass().getResource("/images/db-import.png")));
        importTableButton.setMaximumSize(new Dimension(24, 24));
        importTableButton.setMinimumSize(new Dimension(24, 24));
        importTableButton.setPreferredSize(new Dimension(24, 24));
        importTableButton.setText("");
        importTableButton.setToolTipText("Import table from the file");
        toolBar2.add(importTableButton);
        flushTableButton = new JButton();
        flushTableButton.setEnabled(false);
        flushTableButton.setHorizontalAlignment(0);
        flushTableButton.setIcon(new ImageIcon(getClass().getResource("/images/db-flush.png")));
        flushTableButton.setMaximumSize(new Dimension(24, 24));
        flushTableButton.setMinimumSize(new Dimension(24, 24));
        flushTableButton.setPreferredSize(new Dimension(24, 24));
        flushTableButton.setText("");
        flushTableButton.setToolTipText("Forse hbase to flush table to HFile");
        toolBar2.add(flushTableButton);
        tableMetadataButton = new JButton();
        tableMetadataButton.setEnabled(false);
        tableMetadataButton.setIcon(new ImageIcon(getClass().getResource("/images/db-metadata.png")));
        tableMetadataButton.setMaximumSize(new Dimension(24, 24));
        tableMetadataButton.setMinimumSize(new Dimension(24, 24));
        tableMetadataButton.setPreferredSize(new Dimension(24, 24));
        tableMetadataButton.setText("");
        toolBar2.add(tableMetadataButton);
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
        refreshColumnsButton = new JButton();
        refreshColumnsButton.setEnabled(false);
        refreshColumnsButton.setHorizontalAlignment(0);
        refreshColumnsButton.setIcon(new ImageIcon(getClass().getResource("/images/refresh.png")));
        refreshColumnsButton.setMaximumSize(new Dimension(24, 24));
        refreshColumnsButton.setMinimumSize(new Dimension(24, 24));
        refreshColumnsButton.setPreferredSize(new Dimension(24, 24));
        refreshColumnsButton.setText("");
        refreshColumnsButton.setToolTipText("Refresh columns");
        toolBar3.add(refreshColumnsButton);
        final JToolBar.Separator toolBar$Separator2 = new JToolBar.Separator();
        toolBar3.add(toolBar$Separator2);
        columnsFilter = new JComboBox();
        columnsFilter.setAlignmentX(0.0f);
        columnsFilter.setAlignmentY(0.6f);
        columnsFilter.setEditable(true);
        columnsFilter.setMaximumSize(new Dimension(32767, 26));
        columnsFilter.setMinimumSize(new Dimension(50, 20));
        columnsFilter.setPreferredSize(new Dimension(-1, -1));
        toolBar3.add(columnsFilter);
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
        panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(
            panel7, new GridConstraints(
            2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JToolBar toolBar4 = new JToolBar();
        toolBar4.setBorderPainted(false);
        toolBar4.setFloatable(false);
        panel7.add(
            toolBar4, new GridConstraints(
            0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        checkAllButton = new JButton();
        checkAllButton.setEnabled(false);
        checkAllButton.setIcon(new ImageIcon(getClass().getResource("/images/select.png")));
        checkAllButton.setMaximumSize(new Dimension(27, 27));
        checkAllButton.setMinimumSize(new Dimension(24, 24));
        checkAllButton.setPreferredSize(new Dimension(24, 24));
        checkAllButton.setText("");
        checkAllButton.setToolTipText("Select all columns");
        toolBar4.add(checkAllButton);
        uncheckAllButton = new JButton();
        uncheckAllButton.setEnabled(false);
        uncheckAllButton.setIcon(new ImageIcon(getClass().getResource("/images/unselect.png")));
        uncheckAllButton.setMaximumSize(new Dimension(27, 27));
        uncheckAllButton.setMinimumSize(new Dimension(24, 24));
        uncheckAllButton.setPreferredSize(new Dimension(24, 24));
        uncheckAllButton.setText("");
        uncheckAllButton.setToolTipText("Unselect all columns");
        toolBar4.add(uncheckAllButton);
        final JToolBar toolBar5 = new JToolBar();
        toolBar5.setBorderPainted(false);
        toolBar5.setFloatable(false);
        panel7.add(
            toolBar5, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        scanButton = new JButton();
        scanButton.setEnabled(false);
        scanButton.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
        scanButton.setMinimumSize(new Dimension(24, 24));
        scanButton.setPreferredSize(new Dimension(24, 24));
        scanButton.setText("");
        scanButton.setToolTipText("Perform an advanced scan...");
        toolBar5.add(scanButton);
        jumpButton = new JButton();
        jumpButton.setEnabled(false);
        jumpButton.setIcon(new ImageIcon(getClass().getResource("/images/jump.png")));
        jumpButton.setMinimumSize(new Dimension(24, 24));
        jumpButton.setPreferredSize(new Dimension(24, 24));
        jumpButton.setText("");
        jumpButton.setToolTipText("Jump to a specific row number");
        toolBar5.add(jumpButton);
        populateButton = new JButton();
        populateButton.setEnabled(false);
        populateButton.setIcon(new ImageIcon(getClass().getResource("/images/populate.png")));
        populateButton.setMinimumSize(new Dimension(24, 24));
        populateButton.setPreferredSize(new Dimension(24, 24));
        populateButton.setText("");
        populateButton.setToolTipText("Populate rows for the selected columns");
        toolBar5.add(populateButton);
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
        final JToolBar toolBar6 = new JToolBar();
        toolBar6.setBorderPainted(false);
        toolBar6.setFloatable(false);
        panel10.add(
            toolBar6, new GridConstraints(
            0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        openInViewerButton = new JButton();
        openInViewerButton.setEnabled(false);
        openInViewerButton.setIcon(new ImageIcon(getClass().getResource("/images/viewer.png")));
        openInViewerButton.setMinimumSize(new Dimension(24, 24));
        openInViewerButton.setPreferredSize(new Dimension(24, 24));
        openInViewerButton.setText("");
        openInViewerButton.setToolTipText("Open rows in external viewer");
        toolBar6.add(openInViewerButton);
        addRowButton = new JButton();
        addRowButton.setEnabled(false);
        addRowButton.setIcon(new ImageIcon(getClass().getResource("/images/add.png")));
        addRowButton.setMinimumSize(new Dimension(24, 24));
        addRowButton.setPreferredSize(new Dimension(24, 24));
        addRowButton.setText("");
        addRowButton.setToolTipText("Create a new row");
        toolBar6.add(addRowButton);
        deleteRowButton = new JButton();
        deleteRowButton.setEnabled(false);
        deleteRowButton.setIcon(new ImageIcon(getClass().getResource("/images/delete.png")));
        deleteRowButton.setMinimumSize(new Dimension(24, 24));
        deleteRowButton.setPreferredSize(new Dimension(24, 24));
        deleteRowButton.setText("");
        deleteRowButton.setToolTipText("Delete selected rows");
        toolBar6.add(deleteRowButton);
        copyRowButton = new JButton();
        copyRowButton.setEnabled(false);
        copyRowButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
        copyRowButton.setMaximumSize(new Dimension(49, 27));
        copyRowButton.setMinimumSize(new Dimension(24, 24));
        copyRowButton.setPreferredSize(new Dimension(24, 24));
        copyRowButton.setText("");
        copyRowButton.setToolTipText("Copy selected rows to the clipboard");
        toolBar6.add(copyRowButton);
        pasteRowButton = new JButton();
        pasteRowButton.setEnabled(false);
        pasteRowButton.setIcon(new ImageIcon(getClass().getResource("/images/paste.png")));
        pasteRowButton.setMaximumSize(new Dimension(49, 27));
        pasteRowButton.setMinimumSize(new Dimension(24, 24));
        pasteRowButton.setPreferredSize(new Dimension(24, 24));
        pasteRowButton.setText("");
        pasteRowButton.setToolTipText("Paste rows from the clipboard");
        toolBar6.add(pasteRowButton);
        updateRowButton = new JButton();
        updateRowButton.setEnabled(false);
        updateRowButton.setIcon(new ImageIcon(getClass().getResource("/images/save.png")));
        updateRowButton.setMinimumSize(new Dimension(24, 24));
        updateRowButton.setPreferredSize(new Dimension(24, 24));
        updateRowButton.setText("");
        updateRowButton.setToolTipText("Save all modified rows to the hbase");
        toolBar6.add(updateRowButton);
        final JToolBar toolBar7 = new JToolBar();
        toolBar7.setBorderPainted(false);
        toolBar7.setFloatable(false);
        panel10.add(
            toolBar7, new GridConstraints(
            0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(-1, 20), null, 0, false));
        rowsNumberSpinner = new JSpinner();
        rowsNumberSpinner.setDoubleBuffered(true);
        rowsNumberSpinner.setEnabled(true);
        rowsNumberSpinner.setMinimumSize(new Dimension(45, 24));
        rowsNumberSpinner.setPreferredSize(new Dimension(60, 24));
        toolBar7.add(rowsNumberSpinner);
        final JToolBar.Separator toolBar$Separator3 = new JToolBar.Separator();
        toolBar7.add(toolBar$Separator3);
        visibleRowsLabel = new JLabel();
        visibleRowsLabel.setText("?");
        toolBar7.add(visibleRowsLabel);
        final JToolBar.Separator toolBar$Separator4 = new JToolBar.Separator();
        toolBar7.add(toolBar$Separator4);
        final JLabel label4 = new JLabel();
        label4.setText("of");
        toolBar7.add(label4);
        final JToolBar.Separator toolBar$Separator5 = new JToolBar.Separator();
        toolBar7.add(toolBar$Separator5);
        rowsNumberLabel = new JLabel();
        rowsNumberLabel.setText("?");
        toolBar7.add(rowsNumberLabel);
        final JToolBar.Separator toolBar$Separator6 = new JToolBar.Separator();
        toolBar7.add(toolBar$Separator6);
        showPrevPageButton = new JButton();
        showPrevPageButton.setEnabled(false);
        showPrevPageButton.setIcon(new ImageIcon(getClass().getResource("/images/prev.png")));
        showPrevPageButton.setMaximumSize(new Dimension(49, 27));
        showPrevPageButton.setMinimumSize(new Dimension(24, 24));
        showPrevPageButton.setPreferredSize(new Dimension(24, 24));
        showPrevPageButton.setText("");
        showPrevPageButton.setToolTipText("Go to the prvious page");
        toolBar7.add(showPrevPageButton);
        showNextPageButton = new JButton();
        showNextPageButton.setEnabled(false);
        showNextPageButton.setIcon(new ImageIcon(getClass().getResource("/images/next.png")));
        showNextPageButton.setMaximumSize(new Dimension(49, 27));
        showNextPageButton.setMinimumSize(new Dimension(24, 24));
        showNextPageButton.setPreferredSize(new Dimension(24, 24));
        showNextPageButton.setText("");
        showNextPageButton.setToolTipText("Go to the next page");
        toolBar7.add(showNextPageButton);
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
