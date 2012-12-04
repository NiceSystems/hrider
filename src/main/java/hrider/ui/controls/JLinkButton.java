package hrider.ui.controls;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.*;
import java.net.URL;

public class JLinkButton extends JButton {

    private static final long serialVersionUID = -7843684977763585402L;

    public static final int ALWAYS_UNDERLINE = 0;
    public static final int HOVER_UNDERLINE  = 1;
    public static final int NEVER_UNDERLINE  = 2;
    public static final int SYSTEM_DEFAULT   = 3;

    private int     linkBehavior;
    private Color   linkColor;
    private Color   colorPressed;
    private Color   visitedLinkColor;
    private Color   disabledLinkColor;
    private URL     buttonURL;
    private Action  defaultAction;
    private boolean isLinkVisited;

    public JLinkButton() {
        this(null, null, null);
    }

    public JLinkButton(Action action) {
        this();
        setAction(action);
    }

    public JLinkButton(Icon icon) {
        this(null, icon, null);
    }

    public JLinkButton(String s) {
        this(s, null, null);
    }

    public JLinkButton(URL url) {
        this(null, null, url);
    }

    public JLinkButton(String s, URL url) {
        this(s, null, url);
    }

    public JLinkButton(Icon icon, URL url) {
        this(null, icon, url);
    }

    public JLinkButton(String text, Icon icon, URL url) {
        super(text, icon);
        this.linkBehavior = SYSTEM_DEFAULT;
        this.linkColor = Color.blue;
        this.colorPressed = Color.red;
        this.visitedLinkColor = new Color(128, 0, 128);

        if (text == null && url != null) {
            setText(url.toExternalForm());
        }

        setLinkURL(url);
        setCursor(Cursor.getPredefinedCursor(12));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setRolloverEnabled(true);
        addActionListener(this.defaultAction);
    }

    @Override
    public void updateUI() {
        setUI(BasicLinkButtonUI.createUI(this));
    }

    @Override
    public String getUIClassID() {
        return "LinkButtonUI";
    }

    protected void setupToolTipText() {
        String tip = null;
        if (this.buttonURL != null) {
            tip = this.buttonURL.toExternalForm();
        }
        setToolTipText(tip);
    }

    public void setLinkBehavior(int bnew) {
        checkLinkBehaviour(bnew);
        int old = this.linkBehavior;
        this.linkBehavior = bnew;
        firePropertyChange("linkBehavior", old, bnew);
        repaint();
    }

    private void checkLinkBehaviour(int beha) {
        if (beha != ALWAYS_UNDERLINE && beha != HOVER_UNDERLINE && beha != NEVER_UNDERLINE && beha != SYSTEM_DEFAULT) {
            throw new IllegalArgumentException("Not a legal LinkBehavior");
        }
        else {
            return;
        }
    }

    public int getLinkBehavior() {
        return this.linkBehavior;
    }

    public void setLinkColor(Color color) {
        Color colorOld = this.linkColor;
        this.linkColor = color;
        firePropertyChange("linkColor", colorOld, color);
        repaint();
    }

    public Color getLinkColor() {
        return this.linkColor;
    }

    public void setActiveLinkColor(Color colorNew) {
        Color colorOld = this.colorPressed;
        this.colorPressed = colorNew;
        firePropertyChange("activeLinkColor", colorOld, colorNew);
        repaint();
    }

    public Color getActiveLinkColor() {
        return this.colorPressed;
    }

    public void setDisabledLinkColor(Color color) {
        Color colorOld = this.disabledLinkColor;
        this.disabledLinkColor = color;
        firePropertyChange("disabledLinkColor", colorOld, color);
        if (!isEnabled()) {
            repaint();
        }
    }

    public Color getDisabledLinkColor() {
        return this.disabledLinkColor;
    }

    public void setVisitedLinkColor(Color colorNew) {
        Color colorOld = this.visitedLinkColor;
        this.visitedLinkColor = colorNew;
        firePropertyChange("visitedLinkColor", colorOld, colorNew);
        repaint();
    }

    public Color getVisitedLinkColor() {
        return this.visitedLinkColor;
    }

    public URL getLinkURL() {
        return this.buttonURL;
    }

    public void setLinkURL(URL url) {
        URL urlOld = this.buttonURL;
        this.buttonURL = url;
        setupToolTipText();
        firePropertyChange("linkURL", urlOld, url);
        revalidate();
        repaint();
    }

    public void setLinkVisited(boolean flagNew) {
        boolean flagOld = this.isLinkVisited;
        this.isLinkVisited = flagNew;
        firePropertyChange("linkVisited", flagOld, flagNew);
        repaint();
    }

    public boolean isLinkVisited() {
        return this.isLinkVisited;
    }

    public void setDefaultAction(Action actionNew) {
        Action actionOld = this.defaultAction;
        this.defaultAction = actionNew;
        firePropertyChange("defaultAction", actionOld, actionNew);
    }

    public Action getDefaultAction() {
        return this.defaultAction;
    }

    @Override
    protected String paramString() {
        String str;
        if (this.linkBehavior == ALWAYS_UNDERLINE) {
            str = "ALWAYS_UNDERLINE";
        }
        else if (this.linkBehavior == HOVER_UNDERLINE) {
            str = "HOVER_UNDERLINE";
        }
        else if (this.linkBehavior == NEVER_UNDERLINE) {
            str = "NEVER_UNDERLINE";
        }
        else {
            str = "SYSTEM_DEFAULT";
        }

        String colorStr = this.linkColor == null ? "" : this.linkColor.toString();
        String colorPressStr = this.colorPressed == null ? "" : this.colorPressed.toString();
        String disabledLinkColorStr = this.disabledLinkColor == null ? "" : this.disabledLinkColor.toString();
        String visitedLinkColorStr = this.visitedLinkColor == null ? "" : this.visitedLinkColor.toString();
        String buttonURLStr = this.buttonURL == null ? "" : this.buttonURL.toString();
        String isLinkVisitedStr = this.isLinkVisited ? "true" : "false";

        return super.paramString() + ",linkBehavior=" + str + ",linkURL=" + buttonURLStr + ",linkColor=" + colorStr + ",activeLinkColor=" + colorPressStr +
               ",disabledLinkColor=" + disabledLinkColorStr + ",visitedLinkColor=" + visitedLinkColorStr + ",linkvisitedString=" + isLinkVisitedStr;
    }

    private static class BasicLinkButtonUI extends MetalButtonUI {

        private static final BasicLinkButtonUI ui = new BasicLinkButtonUI();

        private BasicLinkButtonUI() {
        }

        public static ComponentUI createUI(JComponent jcomponent) {
            return ui;
        }

        @Override
        protected void paintText(
            Graphics g, JComponent com, Rectangle rect, String s) {
            JLinkButton bn = (JLinkButton)com;
            ButtonModel bnModel = bn.getModel();
            if (bnModel.isEnabled()) {
                if (bnModel.isPressed()) {
                    bn.setForeground(bn.getActiveLinkColor());
                }
                else if (bn.isLinkVisited()) {
                    bn.setForeground(bn.getVisitedLinkColor());
                }

                else {
                    bn.setForeground(bn.getLinkColor());
                }
            }
            else {
                if (bn.getDisabledLinkColor() != null) {
                    bn.setForeground(bn.getDisabledLinkColor());
                }
            }
            super.paintText(g, com, rect, s);
            int behaviour = bn.getLinkBehavior();
            boolean drawLine = false;
            if (behaviour == JLinkButton.HOVER_UNDERLINE) {
                if (bnModel.isRollover()) {
                    drawLine = true;
                }
            }
            else if (behaviour == JLinkButton.ALWAYS_UNDERLINE || behaviour == JLinkButton.SYSTEM_DEFAULT) {
                drawLine = true;
            }
            if (!drawLine) {
                return;
            }
            FontMetrics fm = g.getFontMetrics();
            int x = rect.x + getTextShiftOffset();
            int y = rect.y + fm.getAscent() + fm.getDescent() + getTextShiftOffset() - 1;
            if (bnModel.isEnabled()) {
                g.setColor(bn.getForeground());
                g.drawLine(x, y, x + rect.width - 1, y);
            }
            else {
                g.setColor(bn.getBackground().brighter());
                g.drawLine(x, y, x + rect.width - 1, y);
            }
        }
    }
}
