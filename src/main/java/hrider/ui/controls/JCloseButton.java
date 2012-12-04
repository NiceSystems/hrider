package hrider.ui.controls;

import hrider.ui.TabClosedListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 10/25/12
 * Time: 9:22 AM
 */
public class JCloseButton extends JPanel {

    private static final long serialVersionUID = 3457653319908745576L;

    private List<TabClosedListener> listeners;

    public JCloseButton(final String title, final JTabbedPane pane) {

        this.listeners = new ArrayList<TabClosedListener>();

        JLabel label = new JLabel();
        label.setText(title);
        label.setHorizontalTextPosition(SwingConstants.TRAILING);
        label.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("images/server.png")));

        JButton button = new JButton();

        setOpaque(false);

        button.setPreferredSize(new Dimension(16, 16));
        button.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (pane.getTabCount() > 1) {
                        pane.removeTabAt(pane.indexOfTab(title));

                        for (TabClosedListener listener : JCloseButton.this.listeners) {
                            listener.onTabeClosed(JCloseButton.this);
                        }
                    }
                }
            });

        button.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("images/close.png")));
        button.setFocusPainted(false);
        button.setOpaque(false);

        add(label);
        add(button);
    }

    public void addTabClosedListener(TabClosedListener listener) {
        this.listeners.add(listener);
    }

    public void removeTabClosedListener(TabClosedListener listener) {
        this.listeners.remove(listener);
    }
}
