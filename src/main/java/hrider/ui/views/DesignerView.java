package hrider.ui.views;

import hrider.config.Configurator;
import hrider.data.*;
import hrider.export.FileExporter;
import hrider.hbase.HbaseActionListener;
import hrider.hbase.HbaseHelper;
import hrider.hbase.Query;
import hrider.hbase.QueryScanner;
import hrider.hbase.Scanner;
import hrider.system.ClipboardData;
import hrider.system.ClipboardListener;
import hrider.system.InMemoryClipboard;
import hrider.ui.ChangeTracker;
import hrider.ui.ChangeTrackerListener;
import hrider.ui.MessageHandler;
import hrider.ui.design.JCellEditor;
import hrider.ui.design.JCheckBoxRenderer;
import hrider.ui.design.ResizeableTableHeader;
import hrider.ui.forms.*;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.table.*;
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
    private JButton                  refreshButton;
    private JButton                  copyTableButton;
    private JButton                  pasteTableButton;
    private JButton                  uncheckAllButton;
    private JSplitPane               topSplitPane;
    private JSplitPane               innerSplitPane;
    private JLabel                   columnsNumber;
    private JLabel                   tablesNumber;
    private JTextField               tablesFilter;
    private JTextField               columnsFilter;
    private JButton                  jumpButton;
    private JButton                  openInViewerButton;
    private JButton                  importTableButton;
    private JButton                  exportTableButton;
    private DefaultTableModel        columnsTableModel;
    private DefaultTableModel        rowsTableModel;
    private Query                    lastQuery;
    private QueryScanner             scanner;
    private JPanel                   owner;
    private HbaseHelper              hbaseHelper;
    private ChangeTracker            changeTracker;
    private Map<String, TableColumn> rowsTableRemovedColumns;
    private Map<String, TableColumn> rowsTableAddedColumns;
    //endregion

    //region Constructor
    public DesignerView(JPanel owner, HbaseHelper hbaseHelper) {

        this.owner = owner;
        this.hbaseHelper = hbaseHelper;
        this.changeTracker = new ChangeTracker();
        this.rowsTableRemovedColumns = new HashMap<String, TableColumn>();
        this.rowsTableAddedColumns = new HashMap<String, TableColumn>();

        InMemoryClipboard.addListener(
            new ClipboardListener() {
                @Override
                public void onChanged(ClipboardData data) {
                    DesignerView.this.pasteTableButton.setEnabled(hasTableInClipboard());
                    DesignerView.this.pasteRowButton.setEnabled(hasRowsInClipboard());
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

        this.tablesFilter.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    loadTables();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    loadTables();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });

        this.columnsFilter.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    populateColumnsTable();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    populateColumnsTable();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });

        this.hbaseHelper.addListener(
            new HbaseActionListener() {
                @Override
                public void copyOperation(String source, String target, String table, Result result) {
                    setInfo(String.format("Copying row '%s' from '%s.%s' to '%s.%s'", Bytes.toStringBinary(result.getRow()), source, table, target, table));
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
                        DesignerView.this.topPanel, "Row number:", "Jump to specific row", JOptionPane.PLAIN_MESSAGE);

                    if (rowNumber != null) {
                        try {
                            long offset = Long.parseLong(rowNumber);

                            DesignerView.this.lastQuery = null;

                            populateRowsTable(offset, Direction.Current);
                        }
                        catch (NumberFormatException ignore) {
                            JOptionPane.showMessageDialog(DesignerView.this.topPanel, "Row number must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

        this.populateButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    DesignerView.this.lastQuery = null;

                    populateRowsTable(Direction.Current);
                }
            });

        this.scanButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    ScanDialog dialog = new ScanDialog(DesignerView.this.lastQuery, getCheckedColumns());
                    dialog.showDialog(DesignerView.this.topPanel);

                    DesignerView.this.lastQuery = dialog.getQuery();
                    if (DesignerView.this.lastQuery != null) {
                        populateRowsTable(Direction.Current);
                    }
                }
            });

        this.openInViewerButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        File tempFile = File.createTempFile("h-rider", Configurator.getExternalViewerFileExtension());
                        FileOutputStream stream = null;

                        try {
                            stream = new FileOutputStream(tempFile);
                            FileExporter exporter = new FileExporter(stream, Configurator.getExternalViewerDelimeter());

                            List<String> columns = getShownColumnNames();

                            for (int i = 0 ; i < DesignerView.this.rowsTable.getRowCount() ; i++) {
                                exporter.write(((DataCell)DesignerView.this.rowsTable.getValueAt(i, 0)).getRow(), columns);
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
                        DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

        this.addRowButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    stopCellEditing(DesignerView.this.columnsTable);
                    stopCellEditing(DesignerView.this.rowsTable);

                    try {
                        Collection<String> columnFamilies = DesignerView.this.hbaseHelper.getColumnFamilies(getSelectedTableName());

                        AddRowDialog dialog = new AddRowDialog(DesignerView.this.changeTracker, getCheckedColumns(), columnFamilies);
                        dialog.showDialog(DesignerView.this.topPanel);

                        DataRow row = dialog.getRow();
                        if (row != null) {
                            DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            try {
                                DesignerView.this.hbaseHelper.setRow(getSelectedTableName(), row);

                                if (DesignerView.this.scanner != null) {
                                    DesignerView.this.scanner.resetCurrent(row.getKey());
                                }

                                // Update the column types according to the added row.
                                for (DataCell cell : row.getCells()) {
                                    Configurator.set(
                                        String.format(
                                            "table.%s.%s", getSelectedTableName(), cell.getColumnName()), cell.getTypedValue().getType().toString());
                                }
                                Configurator.save();

                                populateColumnsTable(row);
                                populateRowsTable(Direction.Current);
                            }
                            catch (Exception ex) {
                                setError("Failed to update rows in HBase: ", ex);
                            }
                            finally {
                                DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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

                    stopCellEditing(DesignerView.this.rowsTable);

                    int decision = JOptionPane.showConfirmDialog(
                        DesignerView.this.topPanel, "Are you sure you want to delete the selected rows?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);

                    if (decision == JOptionPane.OK_OPTION) {
                        DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            int[] selectedRows = DesignerView.this.rowsTable.getSelectedRows();
                            for (int selectedRow : selectedRows) {
                                try {
                                    DataCell key = (DataCell)DesignerView.this.rowsTable.getValueAt(selectedRow, 0);
                                    DesignerView.this.hbaseHelper.deleteRow(getSelectedTableName(), key.getRow());
                                }
                                catch (Exception ex) {
                                    setError("Failed to delete row in HBase: ", ex);
                                }
                            }

                            if (DesignerView.this.scanner != null) {
                                DesignerView.this.scanner.resetCurrent(null);
                            }

                            populateColumnsTable();
                            populateRowsTable(Direction.Current);
                        }
                        finally {
                            DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });


        this.copyRowButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    stopCellEditing(DesignerView.this.rowsTable);

                    copySelectedRowsToClipboard();
                }
            });

        this.pasteRowButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    stopCellEditing(DesignerView.this.rowsTable);

                    pasteRowsFromClipboard();
                }
            });

        this.updateRowButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    stopCellEditing(DesignerView.this.rowsTable);

                    int decision = JOptionPane.showConfirmDialog(
                        DesignerView.this.topPanel,
                        "You are going to save modified rows to hbase; make sure the selected column's types\nare correct otherwise the data will not be read by the application.",
                        "Confirmation", JOptionPane.OK_CANCEL_OPTION);

                    if (decision == JOptionPane.OK_OPTION) {
                        DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            for (DataRow row : DesignerView.this.changeTracker.getChanges()) {
                                try {
                                    DesignerView.this.hbaseHelper.setRow(getSelectedTableName(), row);
                                }
                                catch (Exception ex) {
                                    setError("Failed to update rows in HBase: ", ex);
                                }
                            }

                            DesignerView.this.changeTracker.clear();
                            DesignerView.this.updateRowButton.setEnabled(DesignerView.this.changeTracker.hasChanges());
                        }
                        finally {
                            DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
                    dialog.showDialog(DesignerView.this.topPanel);

                    String tableName = dialog.getTableName();
                    if (tableName != null) {
                        DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            DesignerView.this.hbaseHelper.createTable(tableName, dialog.getColumnFamilies());
                            DesignerView.this.tablesListModel.addElement(tableName);
                            DesignerView.this.tablesList.setSelectedValue(tableName, true);
                        }
                        catch (Exception ex) {
                            setError(String.format("Failed to create table %s: ", tableName), ex);
                        }
                        finally {
                            DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
                        DesignerView.this.topPanel, "Are you sure you want to delete selected table(s).", "Confirmation", JOptionPane.YES_NO_OPTION);

                    if (decision == JOptionPane.YES_OPTION) {
                        DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            for (String tableName : getSelectedTables()) {
                                try {
                                    DesignerView.this.hbaseHelper.deleteTable(tableName);
                                    DesignerView.this.tablesListModel.removeElement(tableName);
                                }
                                catch (Exception ex) {
                                    setError(String.format("Failed to delete table %s: ", tableName), ex);
                                }
                            }
                        }
                        finally {
                            DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
                        DesignerView.this.topPanel, "Are you sure you want to truncate selected table(s).", "Confirmation", JOptionPane.YES_NO_OPTION);

                    if (decision == JOptionPane.YES_OPTION) {
                        DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            List<String> selectedTables = getSelectedTables();
                            if (!selectedTables.isEmpty()) {
                                for (String tableName : selectedTables) {
                                    try {
                                        DesignerView.this.hbaseHelper.truncateTable(tableName);
                                    }
                                    catch (Exception ex) {
                                        setError(String.format("Failed to truncate table %s: ", tableName), ex);
                                    }
                                }

                                DesignerView.this.scanner = null;

                                populateColumnsTable();
                            }
                        }
                        finally {
                            DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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

                    DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        for (int i = 1 ; i < DesignerView.this.columnsTable.getRowCount() ; i++) {
                            DesignerView.this.columnsTable.setValueAt(true, i, 0);
                        }
                    }
                    finally {
                        DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

        this.uncheckAllButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        for (int i = 1 ; i < DesignerView.this.columnsTable.getRowCount() ; i++) {
                            DesignerView.this.columnsTable.setValueAt(false, i, 0);
                        }
                    }
                    finally {
                        DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

        this.refreshButton.addActionListener(
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
                        DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        try {
                            QueryScanner scanner = DesignerView.this.hbaseHelper.getScanner(tableName, null);
                            scanner.setColumnTypes(getColumnTypes());

                            ExportTableDialog dialog = new ExportTableDialog(scanner);
                            dialog.showDialog(DesignerView.this.topPanel);
                        }
                        catch (Exception ex) {
                            setError(String.format("Failed to export table %s: ", tableName), ex);
                        }
                        finally {
                            DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                }
            });

        this.importTableButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //To change body of implemented methods use File | Settings | File Templates.
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
     * @return A reference to the {@link HbaseHelper} class.
     */
    public HbaseHelper getHbaseHelper() {
        return this.hbaseHelper;
    }
    //endregion

    //region Private Methods

    /**
     * Loads the table names.
     */
    private void loadTables() {
        Object selectedTable = this.tablesList.getSelectedValue();

        clearError();

        this.refreshButton.setEnabled(false);
        this.addTableButton.setEnabled(false);
        this.deleteTableButton.setEnabled(false);
        this.truncateTableButton.setEnabled(false);
        this.copyTableButton.setEnabled(false);
        this.exportTableButton.setEnabled(false);
        this.importTableButton.setEnabled(false);

        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            this.tablesListModel.clear();

            String filter = this.tablesFilter.getText().trim().toLowerCase();
            for (String table : this.hbaseHelper.getTables()) {
                if (filter.isEmpty() || table.toLowerCase().contains(filter)) {
                    this.tablesListModel.addElement(table);
                }
            }

            this.addTableButton.setEnabled(true);
            this.importTableButton.setEnabled(true);
            this.tablesNumber.setText(String.valueOf(this.tablesListModel.getSize()));
            this.tablesList.setSelectedValue(selectedTable, true);
        }
        catch (Exception ex) {
            setError("Failed to connect to hadoop: ", ex);
        }
        finally {
            this.refreshButton.setEnabled(true);
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
                    int selectedIndices = DesignerView.this.tablesList.getSelectedIndices().length;

                    DesignerView.this.deleteTableButton.setEnabled(selectedIndices > 0);
                    DesignerView.this.truncateTableButton.setEnabled(selectedIndices > 0);

                    if (selectedIndices == 1) {
                        DesignerView.this.copyTableButton.setEnabled(true);
                        DesignerView.this.exportTableButton.setEnabled(true);
                        DesignerView.this.scanner = null;

                        populateColumnsTable();
                    }
                    else {
                        clearRows(DesignerView.this.columnsTable);
                        clearTable(DesignerView.this.rowsTable);

                        enableDisablePagingButtons();

                        DesignerView.this.copyTableButton.setEnabled(false);
                        DesignerView.this.exportTableButton.setEnabled(false);
                        DesignerView.this.rowsNumberLabel.setText("?");
                        DesignerView.this.visibleRowsLabel.setText("?");
                        DesignerView.this.pasteRowButton.setEnabled(false);
                        DesignerView.this.populateButton.setEnabled(false);
                        DesignerView.this.jumpButton.setEnabled(false);
                        DesignerView.this.scanButton.setEnabled(false);
                        DesignerView.this.addRowButton.setEnabled(false);
                        DesignerView.this.checkAllButton.setEnabled(false);
                        DesignerView.this.uncheckAllButton.setEnabled(false);
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
                            if (DesignerView.this.copyTableButton.isEnabled()) {
                                copyTableToClipboard();
                            }
                        }
                        else if (e.getKeyCode() == KeyEvent.VK_V) {
                            clearError();
                            if (DesignerView.this.pasteTableButton.isEnabled()) {
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
        this.columnsTableModel = new DefaultTableModel();
        this.columnsTableModel.addColumn("Is Shown");
        this.columnsTableModel.addColumn("Column Name");
        this.columnsTableModel.addColumn("Column Type");
        this.columnsTable.setRowHeight(this.columnsTable.getFont().getSize() + 8);
        this.columnsTable.setModel(this.columnsTableModel);
        this.columnsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        this.columnsTable.getColumn("Is Shown").setCellRenderer(new JCheckBoxRenderer(new CheckedRow(1, "key")));
        this.columnsTable.getColumn("Is Shown").setCellEditor(new JCheckBoxRenderer(new CheckedRow(1, "key")));
        this.columnsTable.getColumn("Is Shown").setPreferredWidth(55);
        this.columnsTable.getColumn("Column Name").setPreferredWidth(110);

        JComboBox comboBox = new JComboBox();

        for (ObjectType objectType : ObjectType.values()) {
            comboBox.addItem(objectType);
        }

        this.columnsTable.getColumn("Column Name").setCellEditor(new JCellEditor(this.changeTracker, false));
        this.columnsTable.getColumn("Column Type").setCellEditor(new DefaultCellEditor(comboBox));

        this.columnsTable.getModel().addTableModelListener(
            new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    clearError();

                    int column = e.getColumn();

                    TableModel model = (TableModel)e.getSource();
                    String columnName = model.getColumnName(column);

                    if ("Is Shown".equals(columnName)) {
                        String name = (String)model.getValueAt(e.getFirstRow(), 1);
                        boolean isShown = (Boolean)model.getValueAt(e.getFirstRow(), 0);

                        Configurator.set(String.format("table.%s.%s.isShown", getSelectedTableName(), name), Boolean.toString(isShown));
                        Configurator.save();

                        if (isShown) {
                            TableColumn tableColumn = DesignerView.this.rowsTableRemovedColumns.get(name);
                            if (tableColumn != null) {
                                DesignerView.this.rowsTable.addColumn(tableColumn);
                                DesignerView.this.rowsTableAddedColumns.put(name, tableColumn);
                                DesignerView.this.rowsTableRemovedColumns.remove(name);

                                DesignerView.this.rowsTable.moveColumn(DesignerView.this.rowsTable.getColumnCount() - 1, getColumnIndex(name));
                            }
                            else {
                                if (DesignerView.this.rowsTable.getRowCount() > 0) {
                                    addColumnToRowsTable(getSelectedTableName(), name, DesignerView.this.rowsTable.getColumnCount());

                                    if (DesignerView.this.scanner != null) {
                                        populateColumnOnRowsTable(getSelectedTableName(), name, DesignerView.this.scanner.current());
                                    }

                                    DesignerView.this.rowsTable.moveColumn(DesignerView.this.rowsTable.getColumnCount() - 1, getColumnIndex(name));
                                }
                            }
                        }
                        else {
                            TableColumn tableColumn = DesignerView.this.rowsTableAddedColumns.get(name);
                            if (tableColumn != null) {
                                DesignerView.this.rowsTable.removeColumn(tableColumn);
                                DesignerView.this.rowsTableAddedColumns.remove(name);
                                DesignerView.this.rowsTableRemovedColumns.put(name, tableColumn);
                            }
                        }
                    }
                    else if ("Column Type".equals(columnName)) {
                        String name = (String)model.getValueAt(e.getFirstRow(), 1);
                        ObjectType type = (ObjectType)model.getValueAt(e.getFirstRow(), column);

                        Configurator.set(String.format("table.%s.%s", getSelectedTableName(), name), type.toString());
                        Configurator.save();

                        if (DesignerView.this.scanner != null) {
                            try {
                                DesignerView.this.scanner.updateColumnType(name, type);
                            }
                            catch (Exception ex) {
                                setError(String.format("The selected type '%s' does not match the data.", type), ex);
                            }

                            DesignerView.this.rowsTable.updateUI();
                        }
                    }
                }
            });

        this.columnsTable.addMouseMotionListener(
            new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    int row = DesignerView.this.columnsTable.rowAtPoint(p);
                    int column = DesignerView.this.columnsTable.columnAtPoint(p);
                    if (row != -1 && column != -1) {
                        DesignerView.this.columnsTable.setToolTipText(String.valueOf(DesignerView.this.columnsTable.getValueAt(row, column)));
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
                    DesignerView.this.deleteRowButton.setEnabled(true);
                    DesignerView.this.copyRowButton.setEnabled(true);
                }
            });

        this.rowsTable.addMouseMotionListener(
            new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    int row = DesignerView.this.rowsTable.rowAtPoint(p);
                    int column = DesignerView.this.rowsTable.columnAtPoint(p);
                    if (row != -1 && column != -1) {
                        DesignerView.this.rowsTable.setToolTipText(String.valueOf(DesignerView.this.rowsTable.getValueAt(row, column)));
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

                            if (DesignerView.this.copyRowButton.isEnabled()) {
                                stopCellEditing(DesignerView.this.rowsTable);
                                copySelectedRowsToClipboard();
                            }
                        }
                        else if (e.getKeyCode() == KeyEvent.VK_V) {
                            clearError();

                            if (DesignerView.this.pasteRowButton.isEnabled()) {
                                stopCellEditing(DesignerView.this.rowsTable);
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
                        DesignerView.this.changeTracker.clear();
                        DesignerView.this.updateRowButton.setEnabled(DesignerView.this.changeTracker.hasChanges());
                        DesignerView.this.deleteRowButton.setEnabled(false);
                        DesignerView.this.copyRowButton.setEnabled(false);
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
                    TableColumn column = DesignerView.this.rowsTable.getTableHeader().getResizingColumn();
                    if (column != null) {
                        Configurator.set(String.format("table.%s.%s.size", getSelectedTableName(), column.getHeaderValue()), String.valueOf(column.getWidth()));
                    }
                }

                @Override
                public void columnSelectionChanged(ListSelectionEvent e) {
                }
            });

        DesignerView.this.changeTracker.addListener(
            new ChangeTrackerListener() {
                @Override
                public void onItemChanged(DataCell cell) {
                    clearError();

                    DesignerView.this.updateRowButton.setEnabled(DesignerView.this.changeTracker.hasChanges());
                }
            });
    }

    /**
     * Populates a columns table with the list of the columns from the selected table.
     */
    private void populateColumnsTable() {
        populateColumnsTable(null);
    }

    /**
     * Populates a columns table with the list of the columns from the selected table.
     *
     * @param row If this parameter is not null it will be used to start the columns population from. The columns are the collection of keys extracted
     *            from the hbase rows. Hbase doesn't have a list of columns so in order to present them to the user the tool must go over a number of rows
     *            to collect their keys. Each row can have different keys.
     */
    private void populateColumnsTable(DataRow row) {
        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            clearRows(DesignerView.this.columnsTable);
            clearTable(DesignerView.this.rowsTable);

            enableDisablePagingButtons();

            this.rowsNumberLabel.setText("?");
            this.visibleRowsLabel.setText("?");
            this.rowsNumberSpinner.setEnabled(true);
            this.pasteRowButton.setEnabled(hasRowsInClipboard());

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
            }
            else {
                this.populateButton.setEnabled(false);
                this.jumpButton.setEnabled(false);
                this.scanButton.setEnabled(false);
                this.addRowButton.setEnabled(false);
                this.checkAllButton.setEnabled(false);
                this.uncheckAllButton.setEnabled(false);
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
            stopCellEditing(DesignerView.this.columnsTable);

            this.rowsNumberSpinner.setEnabled(true);

            String tableName = getSelectedTableName();
            if (tableName != null) {
                clearTable(DesignerView.this.rowsTable);

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
                                long totalNumberOfRows = DesignerView.this.scanner.getRowsCount();
                                DesignerView.this.rowsNumberLabel.setText(String.valueOf(totalNumberOfRows));

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
                    cell = new DataCell(row, columnName, new TypedObject(getColumnType(tableName, columnName), null));
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
                this.scanner = DesignerView.this.hbaseHelper.getScanner(tableName, null);
            }

            String filter = this.columnsFilter.getText().toLowerCase().trim();
            for (String column : DesignerView.this.scanner.getColumns(getPageSize())) {
                if (filter.isEmpty() || column.toLowerCase().contains(filter)) {
                    addColumnToColumnsTable(tableName, column, row);
                }
            }

            this.columnsNumber.setText(String.valueOf(this.columnsTableModel.getRowCount()));
        }
        catch (Exception ex) {
            setError("Failed to fill tables list: ", ex);
        }
    }

    /**
     * Adds a column to the columns table.
     *
     * @param tableName  The name of the table the column belongs to. Used to look for the column type in the saved configuration.
     * @param columnName The name of the column to add.
     * @param row        The row that might contain column type information.
     */
    private void addColumnToColumnsTable(String tableName, String columnName, DataRow row) {
        ObjectType columnType = getColumnType(tableName, columnName);
        if (columnType == null && row != null) {
            DataCell cell = row.getCell(columnName);
            if (cell != null) {
                columnType = cell.getTypedValue().getType();
            }
        }

        if (columnType == null) {
            columnType = ObjectType.fromColumn(columnName);
        }

        boolean isShown = isShown(tableName, columnName);
        this.columnsTableModel.addRow(new Object[]{isShown, columnName, columnType});
    }

    /**
     * Adds columns defined in the columns table to the rows table.
     */
    private void loadRowsTableColumns(String tableName) {
        clearColumns(this.rowsTable);

        this.rowsTableAddedColumns.clear();
        this.rowsTableRemovedColumns.clear();

        for (int i = 0, j = 0 ; i < this.columnsTable.getRowCount() ; i++) {
            String columnName = (String)this.columnsTableModel.getValueAt(i, 1);

            boolean isShown = (Boolean)this.columnsTableModel.getValueAt(i, 0);
            if (isShown) {
                addColumnToRowsTable(tableName, columnName, j++);
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
        tableColumn.setCellEditor(new JCellEditor(this.changeTracker, !"key".equals(columnName)));

        try {
            String width = Configurator.get(String.format("table.%s.%s.size", tableName, columnName));
            if (width != null) {
                tableColumn.setPreferredWidth(Integer.parseInt(width));
            }
        }
        catch (NumberFormatException ignore) {
        }

        this.rowsTable.addColumn(tableColumn);
        this.rowsTableModel.addColumn(columnName);
        this.rowsTableAddedColumns.put(columnName, tableColumn);
    }

    /**
     * Adds rows to the rows table.
     *
     * @param rows A list of rows to add.
     */
    private void loadRows(String tableName, Iterable<DataRow> rows) {
        clearRows(this.rowsTable);

        for (DataRow row : rows) {
            Collection<Object> values = new ArrayList<Object>(DesignerView.this.rowsTable.getColumnCount());
            for (int i = 0 ; i < DesignerView.this.rowsTable.getColumnCount() ; i++) {
                String columnName = DesignerView.this.rowsTable.getColumnName(i);
                DataCell cell = row.getCell(columnName);
                if (cell == null) {
                    cell = new DataCell(row, columnName, new TypedObject(getColumnType(tableName, columnName), null));
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
                    DataCell cell = (DataCell)DesignerView.this.rowsTable.getValueAt(selectedRow, 0);
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
            InMemoryClipboard.setData(new ClipboardData<DataTable>(new DataTable(tableName, this.hbaseHelper)));
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
                DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    PasteDialog dialog = new PasteDialog(DesignerView.this.changeTracker, table.getRows());
                    dialog.showDialog(DesignerView.this.topPanel);

                    Collection<DataRow> updatedRows = dialog.getRows();
                    if (updatedRows != null) {
                        for (DataRow row : updatedRows) {
                            try {
                                DesignerView.this.hbaseHelper.setRow(getSelectedTableName(), row);

                                // Update the column types according to the added row.
                                for (DataCell cell : row.getCells()) {
                                    Configurator.set(
                                        String.format(
                                            "table.%s.%s", getSelectedTableName(), cell.getColumnName()), cell.getTypedValue().getType().toString());
                                }
                                Configurator.save();

                                populateColumnsTable(row);
                            }
                            catch (Exception ex) {
                                setError("Failed to update rows in HBase: ", ex);
                            }
                        }

                        if (DesignerView.this.scanner != null) {
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

                            DesignerView.this.scanner.resetCurrent(sortedRows.get(0).getKey());
                        }

                        populateRowsTable(Direction.Current);
                    }
                }
                catch (Exception ex) {
                    setError("Failed to paste rows: ", ex);
                }
                finally {
                    DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
                String targetTable = getSelectedTableName();
                String sourceTable = table.getTableName();

                if (targetTable == null) {
                    targetTable = sourceTable;
                }

                boolean proceed = true;

                while (this.hbaseHelper.tableExists(targetTable)) {
                    String tableName = (String)JOptionPane.showInputDialog(
                        DesignerView.this.topPanel,
                        String.format("The specified table '%s' already exists.\nProvide a new name for the table or the data will be merged.", targetTable),
                        "Provide a new name for the table", JOptionPane.PLAIN_MESSAGE, null, null, targetTable);

                    proceed = tableName != null;

                    // Canceled by the user.
                    if (!proceed) {
                        break;
                    }

                    // The table name has not been changed.
                    if (tableName.isEmpty() || tableName.equals(targetTable)) {
                        break;
                    }

                    targetTable = tableName;
                }

                if (proceed) {
                    if (!this.hbaseHelper.tableExists(targetTable)) {
                        this.hbaseHelper.createTable(targetTable);
                        setInfo(String.format("The '%s' table has been created on '%s'.", targetTable, this.hbaseHelper.getServerName()));
                    }

                    this.hbaseHelper.copyTable(targetTable, sourceTable, table.getHbaseHelper());

                    if (!this.tablesListModel.contains(targetTable)) {
                        String sourcePrefix = String.format("table.%s.", sourceTable);
                        String targetPrefix = String.format("table.%s.", targetTable);

                        for (Map.Entry<String, String> keyValue : Configurator.getAll(sourcePrefix).entrySet()) {
                            Configurator.set(keyValue.getKey().replace(sourcePrefix, targetPrefix), keyValue.getValue());
                        }
                        this.tablesListModel.addElement(targetTable);
                    }

                    this.tablesList.setSelectedValue(targetTable, true);
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
        for (int i = 0 ; i < DesignerView.this.columnsTable.getRowCount() ; i++) {
            String columnName = (String)DesignerView.this.columnsTableModel.getValueAt(i, 1);
            columnTypes.put(columnName, (ObjectType)DesignerView.this.columnsTableModel.getValueAt(i, 2));
        }
        return columnTypes;
    }

    /**
     * Gets a list of columns that are checked. Checked columns are the columns to be shown in the rows table.
     *
     * @return A list of checked columns from the columns table.
     */
    private Collection<TypedColumn> getCheckedColumns() {
        Collection<TypedColumn> typedColumns = new ArrayList<TypedColumn>();
        for (int i = 0 ; i < this.columnsTable.getRowCount() ; i++) {
            boolean isShown = (Boolean)this.columnsTableModel.getValueAt(i, 0);
            if (isShown) {
                typedColumns.add(
                    new TypedColumn(
                        (String)this.columnsTableModel.getValueAt(i, 1), (ObjectType)this.columnsTableModel.getValueAt(i, 2)));
            }
        }
        return typedColumns;
    }

    /**
     * Gets a list of column names from the rows table.
     *
     * @return A list of column names.
     */
    private List<String> getShownColumnNames() {
        List<String> columnNames = new ArrayList<String>();
        Enumeration<TableColumn> columns = this.rowsTable.getColumnModel().getColumns();
        while (columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
            columnNames.add(column.getIdentifier().toString());
        }
        return columnNames;
    }

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
     * Stops editing of the cell if there is any.
     *
     * @param table The table that contains the cell.
     */
    private static void stopCellEditing(JTable table) {
        TableCellEditor editor = table.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
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
            String name = (String)this.columnsTableModel.getValueAt(i, 1);

            boolean isShown = (Boolean)this.columnsTableModel.getValueAt(i, 0);
            if (isShown) {
                if (name.equals(columnName)) {
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
    private static ObjectType getColumnType(String tableName, String columnName) {
        String type = Configurator.get(String.format("table.%s.%s", tableName, columnName));
        if (type != null) {
            return ObjectType.valueOf(type);
        }
        return ObjectType.fromColumn(columnName);
    }

    /**
     * Checks in the configuration whether the specified column is checked.
     *
     * @param tableName  The name of the table that contains the column.
     * @param columnName The name of the column.
     * @return True if the specified column is checked or False otherwise.
     */
    private static boolean isShown(String tableName, String columnName) {
        String value = Configurator.get(String.format("table.%s.%s.isShown", tableName, columnName));
        return value == null || Boolean.parseBoolean(value);
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
     * Enables or disables the paging buttons.
     */
    private void enableDisablePagingButtons() {
        this.showPrevPageButton.setEnabled(this.scanner != null && this.scanner.hasPrev());
        this.showNextPageButton.setEnabled(this.scanner != null && this.scanner.hasNext());
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
