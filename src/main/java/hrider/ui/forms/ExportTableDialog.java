package hrider.ui.forms;

import hrider.config.Configurator;
import hrider.data.DataRow;
import hrider.data.ObjectType;
import hrider.data.TypedColumn;
import hrider.export.FileExporter;
import hrider.hbase.Query;
import hrider.hbase.QueryScanner;
import hrider.hbase.Scanner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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
 */
public class ExportTableDialog extends JDialog {

    //region Variables
    private JPanel     contentPane;
    private JButton    btExport;
    private JButton    btCancel;
    private JTextField tfFilePath;
    private JButton    btBrowse;
    private JLabel     writtenRowsCount;
    private JLabel     totalRowsCount;
    private JComboBox  cmbDelimiter;
    private JButton    btOpen;
    private JButton    btClose;
    private JButton    btExportWithQueryButton;
    private String     filePath;
    private boolean    canceled;
    //endregion

    //region Constructor
    public ExportTableDialog(final QueryScanner scanner) {

        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Export table to file");
        getRootPane().setDefaultButton(this.btExport);

        this.btExport.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String delimiter = ExportTableDialog.this.cmbDelimiter.getSelectedItem().toString().trim();
                    if (delimiter == null || delimiter.isEmpty() || delimiter.length() != 1) {
                        JOptionPane.showMessageDialog(
                            ExportTableDialog.this.contentPane, "The delimiter field must contain exactly one character.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String path = ExportTableDialog.this.tfFilePath.getText().trim();
                    if (path.isEmpty()) {
                        JOptionPane.showMessageDialog(
                            ExportTableDialog.this.contentPane, "The file path must be provided.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    onExport(scanner);
                }
            });

        this.btExportWithQueryButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String delimiter = ExportTableDialog.this.cmbDelimiter.getSelectedItem().toString().trim();
                    if (delimiter == null || delimiter.isEmpty() || delimiter.length() != 1) {
                        JOptionPane.showMessageDialog(
                            ExportTableDialog.this.contentPane, "The delimiter field must contain exactly one character.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String path = ExportTableDialog.this.tfFilePath.getText().trim();
                    if (path.isEmpty()) {
                        JOptionPane.showMessageDialog(
                            ExportTableDialog.this.contentPane, "The file path must be provided.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    ExportTableDialog.this.contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        Collection<TypedColumn> columns = new ArrayList<TypedColumn>();
                        for (String columnName : scanner.getColumns(100)) {
                            columns.add(new TypedColumn(columnName, ObjectType.String));
                        }

                        ScanDialog dialog = new ScanDialog(null, columns);
                        dialog.showDialog(ExportTableDialog.this.contentPane);

                        Query query = dialog.getQuery();
                        if (query != null) {
                            scanner.setQuery(query);

                            onExport(scanner);
                        }
                    }
                    catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                            ExportTableDialog.this.contentPane,
                            String.format("Failed to load columns for '%s' table.\nError: %s", scanner.getTableName(), ex.getMessage()), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    finally {
                        ExportTableDialog.this.contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

        this.btCancel.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ExportTableDialog.this.canceled = true;
                }
            });

        this.btOpen.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (ExportTableDialog.this.filePath != null) {
                        try {
                            Desktop.getDesktop().open(new File(ExportTableDialog.this.filePath));
                        }
                        catch (IOException ex) {
                            JOptionPane.showMessageDialog(
                                ExportTableDialog.this.contentPane,
                                String.format("Failed to open file %s.\nError: %s", ExportTableDialog.this.filePath, ex.getMessage()), "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

        this.btBrowse.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser dialog = new JFileChooser();
                    int returnVal = dialog.showSaveDialog(ExportTableDialog.this.contentPane);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        ExportTableDialog.this.tfFilePath.setText(dialog.getSelectedFile().getAbsolutePath());
                    }
                }
            });

        this.btClose.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });

        // call onCancel() on ESCAPE
        this.contentPane.registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        ExportTableDialog.this.totalRowsCount.setText(String.valueOf(scanner.getRowsCount()));
                    }
                    catch (Exception ignore) {
                    }
                }
            }).start();
    }
    //endregion

    //region Public Methods
    public void showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }
    //endregion

    //region Private Methods
    private void onExport(final Scanner scanner) {
        try {
            final File file = new File(this.tfFilePath.getText());
            if (!file.exists()) {
                file.createNewFile();
            }

            new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        ExportTableDialog.this.tfFilePath.setEnabled(false);
                        ExportTableDialog.this.cmbDelimiter.setEnabled(false);
                        ExportTableDialog.this.btExport.setEnabled(false);
                        ExportTableDialog.this.btExportWithQueryButton.setEnabled(false);
                        ExportTableDialog.this.btCancel.setEnabled(true);
                        ExportTableDialog.this.btOpen.setEnabled(false);
                        ExportTableDialog.this.btClose.setEnabled(false);
                        ExportTableDialog.this.btBrowse.setEnabled(false);

                        ExportTableDialog.this.canceled = false;

                        FileOutputStream stream = null;
                        try {
                            stream = new FileOutputStream(file);
                            FileExporter exporter = new FileExporter(stream, Configurator.getExternalViewerDelimeter());

                            scanner.resetCurrent(null);

                            Collection<DataRow> rows = scanner.next(1000);
                            Collection<String> columnNames = scanner.getColumns(0);

                            int counter = 1;

                            while (!rows.isEmpty() && !ExportTableDialog.this.canceled) {
                                for (DataRow row : rows) {
                                    exporter.write(row, columnNames);

                                    ExportTableDialog.this.writtenRowsCount.setText(Long.toString(counter++));
                                    ExportTableDialog.this.writtenRowsCount.paintImmediately(ExportTableDialog.this.writtenRowsCount.getBounds());
                                }

                                rows = scanner.next(1000);
                            }

                            ExportTableDialog.this.filePath = file.getAbsolutePath();
                            ExportTableDialog.this.btOpen.setEnabled(true);
                        }
                        catch (Exception e) {
                            JOptionPane.showMessageDialog(
                                ExportTableDialog.this.contentPane,
                                String.format("Failed to export to file %s.\nError: %s", ExportTableDialog.this.tfFilePath.getText(), e.getMessage()), "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                        finally {
                            if (stream != null) {
                                try {
                                    stream.close();
                                }
                                catch (IOException ignore) {
                                }
                            }

                            ExportTableDialog.this.tfFilePath.setEnabled(true);
                            ExportTableDialog.this.cmbDelimiter.setEnabled(true);
                            ExportTableDialog.this.btExport.setEnabled(true);
                            ExportTableDialog.this.btExportWithQueryButton.setEnabled(true);
                            ExportTableDialog.this.btCancel.setEnabled(false);
                            ExportTableDialog.this.btClose.setEnabled(true);
                            ExportTableDialog.this.btBrowse.setEnabled(true);
                        }
                    }
                }).start();
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(
                this.contentPane, String.format("Failed to export to file %s.\nError: %s", this.tfFilePath.getText(), e.getMessage()), "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    //endregion
}
