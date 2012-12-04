package hrider.ui.forms;

import hrider.config.Configurator;
import hrider.data.ServerDetails;
import hrider.hbase.HbaseHelper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;

public class ConnectionDetailsDialog extends JDialog {

    private JPanel      contentPane;
    private JButton     buttonConnect;
    private JButton     buttonCancel;
    private JTextField  hbaseServer;
    private JTextField  zooKeeperServer;
    private JSpinner    zooKeeperPort;
    private JSpinner    hbasePort;
    private boolean     okPressed;
    private HbaseHelper hbaseHelper;

    public ConnectionDetailsDialog() {
        setContentPane(this.contentPane);
        setModal(true);
        setTitle("Connect to an Hbase server...");
        getRootPane().setDefaultButton(this.buttonConnect);

        this.hbasePort.setValue(9000);
        this.zooKeeperPort.setValue(2181);

        this.zooKeeperServer.setText(Configurator.get("connection.zookeeper.host"));
        this.hbaseServer.setText(Configurator.get("connection.hbase.host"));

        this.buttonConnect.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onOK();
                }
            });

        this.buttonCancel.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    onCancel();
                }
            });

        // call onCancel() on ESCAPE
        this.contentPane.registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        this.hbaseServer.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    ConnectionDetailsDialog.this.zooKeeperServer.setText(ConnectionDetailsDialog.this.hbaseServer.getText());
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    ConnectionDetailsDialog.this.zooKeeperServer.setText(ConnectionDetailsDialog.this.hbaseServer.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    ConnectionDetailsDialog.this.zooKeeperServer.setText(ConnectionDetailsDialog.this.hbaseServer.getText());
                }
            });
    }

    public void showDialog(Component owner) {
        this.setComponentOrientation(owner.getComponentOrientation());
        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }

    public HbaseHelper getHBaseHelper() {
        if (this.okPressed) {
            return this.hbaseHelper;
        }
        return null;
    }

    public ServerDetails getHBaseServer() {
        if (this.okPressed) {
            return new ServerDetails(this.hbaseServer.getText(), this.hbasePort.getValue().toString());
        }
        return null;
    }

    public ServerDetails getZooKeeperServer() {
        if (this.okPressed) {
            return new ServerDetails(this.zooKeeperServer.getText(), this.zooKeeperPort.getValue().toString());
        }
        return null;
    }

    private void onOK() {
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", this.zooKeeperServer.getText());
        config.set("hbase.zookeeper.property.clientPort", this.zooKeeperPort.getValue().toString());
        config.set("hbase.master", this.hbaseServer.getText() + ':' + this.hbasePort.getValue().toString());

        this.contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            this.hbaseHelper = new HbaseHelper(config);
            this.hbaseHelper.getTables();

            Configurator.set("connection.zookeeper.host", this.zooKeeperServer.getText());
            Configurator.set("connection.hbase.host", this.hbaseServer.getText());
            Configurator.save();

            this.okPressed = true;
            dispose();
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this, String.format(
                "%s\n\nMake sure you have access to all nodes of the cluster you try\nto connect to. In case you don't, map the nodes in your hosts file.",
                ex.getMessage()), "Failed to connect...", JOptionPane.ERROR_MESSAGE);
        }
        finally {
            this.contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void onCancel() {
        dispose();
    }
}
