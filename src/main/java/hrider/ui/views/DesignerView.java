package hrider.ui.views;

import hrider.config.Configurator;
import hrider.data.*;
import hrider.hbase.HbaseActionListener;
import hrider.hbase.HbaseHelper;
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
import hrider.ui.design.ResizeableTableHeader;
import hrider.ui.forms.AddRowDialog;
import hrider.ui.forms.AddTableDialog;
import hrider.ui.forms.PasteDialog;
import hrider.ui.forms.ScanDialog;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 10/15/12
 * Time: 6:25 PM
 */
@SuppressWarnings({"OverlyComplexAnonymousInnerClass", "OverlyComplexClass", "ClassWithTooManyFields", "ClassWithTooManyMethods", "OverlyCoupledClass"})
public class DesignerView {

    private JPanel                   topPanel;
    private JList                    tablesList;
    private DefaultListModel         tablesListModel;
    private JTable                   rowsTable;
    private JButton                  populateButton;
    private JTable                   columnsTable;
    private JScrollPane              rowsPane;
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
    private DefaultTableModel        columnsTableModel;
    private JPopupMenu               columnsMenu;
    private Map<String, TableColumn> removedColumns;
    private Query                    lastQuery;
    private int                      lastRow;
    private QueryScanner             scanner;
    private JPanel                   owner;
    private HbaseHelper              hbaseHelper;
    private ChangeTracker            changeTracker;

    public DesignerView(JPanel owner, HbaseHelper hbaseHelper) {

        this.owner = owner;
        this.hbaseHelper = hbaseHelper;
        this.changeTracker = new ChangeTracker();

        InMemoryClipboard.addListener(
            new ClipboardListener() {
                @Override
                public void onChanged(ClipboardData data) {
                    DesignerView.this.pasteTableButton.setEnabled(hasTableInClipboard());
                    DesignerView.this.pasteRowButton.setEnabled(hasRawsInClipboard());
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

        this.removedColumns = new HashMap<String, TableColumn>();
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
                    setInfo(String.format("Copying row '%s' from '%s.%s' to '%s.%s'", Bytes.toString(result.getRow()), source, table, target, table));
                }

                @Override
                public void tableOperation(String tableName, String operation) {
                    setInfo(String.format("The %s table has been %s", tableName, operation));
                }

                @Override
                public void rowOperation(String tableName, DataRow row, String operation) {
                    setInfo(
                        String.format(
                            "The %s row has been %s %s the %s table", row.getKey(), operation, operation.equals("added") ? "to" : "from", tableName));
                }

                @Override
                public void columnOperation(String tableName, String column, String operation) {
                    setInfo(
                        String.format(
                            "The %s column has been %s %s the %s table", column, operation, operation.equals("added") ? "to" : "from", tableName));
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

                    ScanDialog dialog = new ScanDialog(DesignerView.this.lastQuery, getVisibleColumns());
                    dialog.showDialog(DesignerView.this.topPanel);

                    DesignerView.this.lastQuery = dialog.getQuery();
                    if (DesignerView.this.lastQuery != null) {
                        populateRowsTable(Direction.Current);
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
                        Collection<String> columnFamilies = DesignerView.this.hbaseHelper.getColumnFamilies(getTableName());

                        AddRowDialog dialog = new AddRowDialog(DesignerView.this.changeTracker, getVisibleColumns(), columnFamilies);
                        dialog.showDialog(DesignerView.this.topPanel);

                        DataRow row = dialog.getRow();
                        if (row != null) {
                            DesignerView.this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            try {
                                DesignerView.this.hbaseHelper.setRow(getTableName(), row);

                                if (DesignerView.this.scanner != null) {
                                    DesignerView.this.scanner.resetCurrent(row.getKey());
                                }

                                // Update the column types according to the added row.
                                for (DataCell cell : row.getCells()) {
                                    Configurator.set(
                                        String.format(
                                            "table.%s.%s", getTableName(), cell.getColumnName()), cell.getTypedValue().getType().toString());
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
                        setError(String.format("Failed to get column families for table '%s'.", getTableName()), ex);
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
                                    DesignerView.this.hbaseHelper.deleteRow(getTableName(), key.getRow());
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
                                    DesignerView.this.hbaseHelper.setRow(getTableName(), row);
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
                    enableDisablePagingButtons();
                }
            });

        this.showNextPageButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearError();

                    reloadColumns();
                    populateRowsTable(Direction.Forward);
                    enableDisablePagingButtons();
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

                    load();
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
    }

    public void load() {
        loadTables();
    }

    public JPanel getView() {
        return this.topPanel;
    }

    public HbaseHelper getHbaseHelper() {
        return this.hbaseHelper;
    }

    private void initializeTablesList() {
        this.pasteTableButton.setEnabled(hasTableInClipboard());

        this.tablesListModel = new DefaultListModel();
        this.tablesList.setModel(this.tablesListModel);

        this.tablesList.addListSelectionListener(
            new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    DesignerView.this.removedColumns.clear();

                    int selectedIndeces = DesignerView.this.tablesList.getSelectedIndices().length;

                    DesignerView.this.deleteTableButton.setEnabled(selectedIndeces > 0);
                    DesignerView.this.truncateTableButton.setEnabled(selectedIndeces > 0);

                    if (selectedIndeces == 1) {
                        DesignerView.this.copyTableButton.setEnabled(true);

                        DesignerView.this.lastRow = 0;
                        DesignerView.this.scanner = null;

                        populateColumnsTable();
                    }
                    else {
                        clearRows(DesignerView.this.columnsTableModel);
                        clearTable(DesignerView.this.rowsTable);
                        enableDisablePagingButtons();

                        DesignerView.this.copyTableButton.setEnabled(false);
                        DesignerView.this.rowsNumberLabel.setText("?");
                        DesignerView.this.visibleRowsLabel.setText("?");
                        DesignerView.this.pasteRowButton.setEnabled(false);
                        DesignerView.this.populateButton.setEnabled(false);
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

    @SuppressWarnings("MagicNumber")
    private void initializeColumnsTable() {
        this.columnsTableModel = new DefaultTableModel();
        this.columnsTableModel.addColumn("Is Shown");
        this.columnsTableModel.addColumn("Column Name");
        this.columnsTableModel.addColumn("Column Type");
        this.columnsTable.setRowHeight(this.columnsTable.getFont().getSize() + 8);
        this.columnsTable.setModel(this.columnsTableModel);
        this.columnsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.columnsTable.getColumn("Is Shown").setCellRenderer(new JCheckBoxRenderer(new RequiredRow(1, "key")));
        this.columnsTable.getColumn("Is Shown").setCellEditor(new JCheckBoxRenderer(new RequiredRow(1, "key")));
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
                        Configurator.set(
                            String.format(
                                "table.%s.%s.isShown", getTableName(), model.getValueAt(e.getFirstRow(), 1)), model.getValueAt(e.getFirstRow(), 0).toString());

                        Configurator.save();

                        clearTable(DesignerView.this.rowsTable);
                        enableDisablePagingButtons();

                        boolean enabled = false;
                        for (int i = 0 ; i < model.getRowCount() ; i++) {
                            enabled |= (Boolean)model.getValueAt(i, column);
                        }
                        DesignerView.this.populateButton.setEnabled(enabled);
                        DesignerView.this.scanButton.setEnabled(enabled);
                    }
                    else if ("Column Type".equals(columnName)) {
                        String name = (String)model.getValueAt(e.getFirstRow(), 1);
                        ObjectType type = (ObjectType)model.getValueAt(e.getFirstRow(), column);

                        Configurator.set(String.format("table.%s.%s", getTableName(), name), type.toString());
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

    private void initializeRowsTable() {
        this.rowsTable.setTableHeader(new ResizeableTableHeader(this.rowsTable.getColumnModel()));
        this.rowsTable.setCellSelectionEnabled(false);
        this.rowsTable.setRowSelectionAllowed(true);

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

        this.rowsTable.getTableHeader().addMouseListener(
            new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        DesignerView.this.columnsMenu.show(DesignerView.this.rowsPane, e.getX(), e.getY());
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
                        Configurator.set(String.format("table.%s.%s.size", getTableName(), column.getHeaderValue()), String.valueOf(column.getWidth()));
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

    private Map<String, ObjectType> updateColumns(DefaultTableModel model) {
        Map<String, ObjectType> columnTypes = new HashMap<String, ObjectType>();
        for (int i = 0 ; i < DesignerView.this.columnsTable.getRowCount() ; i++) {
            String columnName = (String)DesignerView.this.columnsTableModel.getValueAt(i, 1);

            boolean isShown = (Boolean)DesignerView.this.columnsTableModel.getValueAt(i, 0);
            if (isShown) {
                if (!this.removedColumns.containsKey(columnName)) {
                    model.addColumn(columnName);
                }
            }

            columnTypes.put(columnName, (ObjectType)DesignerView.this.columnsTableModel.getValueAt(i, 2));
        }
        return columnTypes;
    }

    private void reloadColumns() {
        if (this.scanner != null) {
            List<String> columns = new ArrayList<String>(this.scanner.getColumns(getPageSize()));
            if (!columns.isEmpty()) {
                for (int i = 0 ; i < DesignerView.this.columnsTable.getRowCount() ; i++) {
                    String columnName = (String)DesignerView.this.columnsTableModel.getValueAt(i, 1);
                    columns.remove(columnName);
                }

                String tableName = getTableName();

                while (!columns.isEmpty()) {
                    addColumn(tableName, columns.get(0), null);
                    columns.remove(0);
                }
            }
        }
    }

    private void addRows(DefaultTableModel model, Iterable<DataRow> rows) {
        for (DataRow row : rows) {
            Collection<Object> values = new ArrayList<Object>(DesignerView.this.rowsTable.getColumnCount());
            for (int i = 0 ; i < DesignerView.this.rowsTable.getColumnCount() ; i++) {
                String columnName = DesignerView.this.rowsTable.getColumnName(i);
                DataCell cell = row.getCell(columnName);
                if (cell == null) {
                    cell = new DataCell(row, columnName, new TypedObject(getColumnType(getTableName(), columnName), null));
                }
                values.add(cell);
            }
            model.addRow(values.toArray());
        }
    }

    private void loadTables() {
        Object selectedTable = this.tablesList.getSelectedValue();

        clearError();

        this.refreshButton.setEnabled(false);
        this.addTableButton.setEnabled(false);
        this.deleteTableButton.setEnabled(false);
        this.truncateTableButton.setEnabled(false);
        this.copyTableButton.setEnabled(false);

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

    private void populateRowsTable(Direction direction) {

        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            stopCellEditing(DesignerView.this.columnsTable);

            this.rowsNumberLabel.setText("?");
            this.visibleRowsLabel.setText("?");
            this.rowsNumberSpinner.setEnabled(true);

            String tableName = getTableName();
            if (tableName != null) {
                int rowsCount = this.rowsTable.getRowCount();

                DefaultTableModel model = new DefaultTableModel();
                this.rowsTable.setModel(model);

                Map<String, ObjectType> columnTypes = updateColumns(model);

                this.columnsMenu = createColumnsPopupMenu();

                // Replace the cell editor to support custom objects as cell values.
                for (int i = 0 ; i < this.rowsTable.getColumnCount() ; i++) {
                    TableColumn column = this.rowsTable.getColumnModel().getColumn(i);
                    column.setCellEditor(new JCellEditor(this.changeTracker, !"key".equals(column.getIdentifier())));

                    try {
                        String width = Configurator.get(String.format("table.%s.%s.size", getTableName(), column.getHeaderValue()));
                        if (width != null) {
                            column.setPreferredWidth(Integer.parseInt(width));
                        }
                    }
                    catch (NumberFormatException ignore) {
                    }
                }

                try {
                    this.scanner.setQuery(this.lastQuery);
                    this.scanner.setColumnTypes(columnTypes);

                    Collection<DataRow> rows;

                    if (direction == Direction.Current) {
                        rows = this.scanner.current(getPageSize());
                        this.lastRow = rows.size();
                    }
                    else if (direction == Direction.Forward) {
                        rows = this.scanner.next(getPageSize());
                        this.lastRow += rows.size();
                    }
                    else {
                        rows = this.scanner.prev();
                        this.lastRow -= rowsCount;
                    }

                    addRows(model, rows);

                    this.enableDisablePagingButtons();
                    this.visibleRowsLabel.setText(String.format("%s - %s", this.lastRow - rows.size() + 1, this.lastRow));
                    this.rowsNumberSpinner.setEnabled(this.lastRow <= getPageSize());

                    // To get the number of rows can take the time.
                    Thread thread = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    DesignerView.this.rowsNumberLabel.setText(String.valueOf(DesignerView.this.scanner.getRowsCount()));
                                }
                                catch (Exception e) {
                                    setError("Failed to get the number of rows in the table.", e);
                                }
                            }
                        });
                    thread.start();
                }
                catch (Exception ex) {
                    setError("Failed to fill rows: ", ex);
                }
            }
        }
        finally {
            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void populateColumnsTable() {
        populateColumnsTable(null);
    }

    private void populateColumnsTable(DataRow row) {
        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            clearRows(DesignerView.this.columnsTableModel);
            clearTable(DesignerView.this.rowsTable);
            enableDisablePagingButtons();

            this.rowsNumberLabel.setText("?");
            this.visibleRowsLabel.setText("?");
            this.rowsNumberSpinner.setEnabled(true);
            this.pasteRowButton.setEnabled(hasRawsInClipboard());

            String tableName = getTableName();
            if (tableName != null) {
                try {
                    if (this.scanner == null) {
                        this.scanner = DesignerView.this.hbaseHelper.getScanner(getTableName(), null);
                    }

                    ObjectType keyType = getColumnType(tableName, "key");
                    if (keyType == null) {
                        keyType = ObjectType.String;
                    }

                    DesignerView.this.columnsTableModel.addRow(new Object[]{Boolean.TRUE, "key", keyType});

                    String filter = this.columnsFilter.getText().toLowerCase().trim();

                    for (String column : DesignerView.this.scanner.getColumns(getPageSize())) {
                        if (filter.isEmpty() || column.toLowerCase().contains(filter)) {
                            addColumn(tableName, column, row);
                        }
                    }

                    this.columnsNumber.setText(String.valueOf(this.columnsTableModel.getRowCount()));

                    if (this.columnsTableModel.getRowCount() > 0) {
                        this.populateButton.setEnabled(true);
                        this.scanButton.setEnabled(true);
                        this.addRowButton.setEnabled(true);
                        this.checkAllButton.setEnabled(true);
                        this.uncheckAllButton.setEnabled(true);
                    }
                    else {
                        this.populateButton.setEnabled(false);
                        this.scanButton.setEnabled(false);
                        this.addRowButton.setEnabled(false);
                        this.checkAllButton.setEnabled(false);
                        this.uncheckAllButton.setEnabled(false);
                    }
                }
                catch (Exception ex) {
                    setError("Failed to fill tables list: ", ex);
                }
            }
        }
        finally {
            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void copySelectedRowsToClipboard() {
        this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            int[] selectedRows = this.rowsTable.getSelectedRows();
            if (selectedRows.length > 0) {
                DataTable table = new DataTable(getTableName());
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

    private void copyTableToClipboard() {
        String tableName = getTableName();
        if (tableName != null) {
            InMemoryClipboard.setData(new ClipboardData<DataTable>(new DataTable(tableName, this.hbaseHelper)));
        }
        else {
            InMemoryClipboard.setData(null);
        }
    }

    @SuppressWarnings("OverlyNestedMethod")
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
                                DesignerView.this.hbaseHelper.setRow(getTableName(), row);

                                // Update the column types according to the added row.
                                for (DataCell cell : row.getCells()) {
                                    Configurator.set(
                                        String.format(
                                            "table.%s.%s", getTableName(), cell.getColumnName()), cell.getTypedValue().getType().toString());
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

    @SuppressWarnings("OverlyNestedMethod")
    private void pasteTableFromClipboard() {
        this.pasteTableButton.setEnabled(false);

        ClipboardData<DataTable> clipboardData = InMemoryClipboard.getData();
        if (clipboardData != null) {

            DataTable table = clipboardData.getData();

            this.owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                String targetTable = getTableName();
                String sourceTable = table.getTableName();

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

    private void addColumn(String tableName, String column, DataRow row) {
        ObjectType columnType = getColumnType(tableName, column);
        if (columnType == null && row != null) {
            DataCell cell = row.getCell(column);
            if (cell != null) {
                columnType = cell.getTypedValue().getType();
            }
        }

        if (columnType == null) {
            columnType = ObjectType.fromColumn(column);
        }

        boolean isShown = isShown(tableName, column);
        this.columnsTableModel.addRow(new Object[]{isShown, column, columnType});
    }

    private Collection<TypedColumn> getVisibleColumns() {
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

    private JPopupMenu createColumnsPopupMenu() {
        JPopupMenu menu = new JPopupMenu("Columns to show");

        ActionListener menuListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() instanceof JCheckBoxMenuItem) {
                    JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();

                    if (item.getState()) {
                        TableColumn column = DesignerView.this.removedColumns.get(item.getText());
                        DesignerView.this.rowsTable.addColumn(column);
                    }
                    else {
                        TableColumn column = DesignerView.this.rowsTable.getColumn(item.getText());
                        DesignerView.this.removedColumns.put(item.getText(), column);

                        DesignerView.this.rowsTable.removeColumn(column);
                    }
                }
            }
        };

        Collection<String> visibleColumns = new ArrayList<String>();
        for (int i = 0 ; i < DesignerView.this.rowsTable.getColumnCount() ; i++) {
            visibleColumns.add(DesignerView.this.rowsTable.getColumnName(i));
        }

        for (int i = 0 ; i < DesignerView.this.columnsTable.getRowCount() ; i++) {
            boolean isShown = (Boolean)DesignerView.this.columnsTableModel.getValueAt(i, 0);
            if (isShown) {
                String columnName = (String)DesignerView.this.columnsTableModel.getValueAt(i, 1);
                if (!"key".equals(columnName)) {
                    JMenuItem item = new JCheckBoxMenuItem(columnName, visibleColumns.contains(columnName));
                    item.addActionListener(menuListener);

                    menu.add(item);
                }
            }
        }

        return menu;
    }

    private static void clearRows(DefaultTableModel model) {
        if (model != null) {
            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }
        }
    }

    private static void clearTable(JTable table) {
        table.setModel(new DefaultTableModel());
    }

    private static void stopCellEditing(JTable table) {
        TableCellEditor editor = table.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }

    private int getPageSize() {
        return (Integer)this.rowsNumberSpinner.getValue();
    }

    private String getTableName() {
        return (String)this.tablesList.getSelectedValue();
    }

    private List<String> getSelectedTables() {
        List<String> tables = new ArrayList<String>();
        for (Object table : this.tablesList.getSelectedValues()) {
            tables.add(table.toString());
        }
        return tables;
    }

    private static ObjectType getColumnType(String tableName, String columnName) {
        String type = Configurator.get(String.format("table.%s.%s", tableName, columnName));
        if (type != null) {
            return ObjectType.valueOf(type);
        }
        return ObjectType.fromColumn(columnName);
    }

    private static boolean isShown(String tableName, String columnName) {
        String value = Configurator.get(String.format("table.%s.%s.isShown", tableName, columnName));
        return value == null || Boolean.parseBoolean(value);
    }

    private static boolean hasRawsInClipboard() {
        ClipboardData<DataTable> data = InMemoryClipboard.getData();
        return data != null && data.getData().getRowsCount() > 0;
    }

    private static boolean hasTableInClipboard() {
        ClipboardData<DataTable> data = InMemoryClipboard.getData();
        return data != null && data.getData().getRowsCount() == 0;
    }

    private void enableDisablePagingButtons() {
        try {
            this.showPrevPageButton.setEnabled(this.lastRow > getPageSize());
            this.showNextPageButton.setEnabled(this.rowsTable.getRowCount() == getPageSize());
        }
        catch (Exception ex) {
            setError("Failed to read from hbase: ", ex);
        }
    }

    private static void clearError() {
        MessageHandler.addError("", null);
    }

    private static void setError(String message, Exception ex) {
        MessageHandler.addError(message, ex);
    }

    private static void setInfo(String message) {
        MessageHandler.addInfo(message);
    }

    private enum Direction {
        Current,
        Forward,
        Backward
    }
}
