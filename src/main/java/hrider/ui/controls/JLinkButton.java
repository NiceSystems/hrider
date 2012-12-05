package hrider.ui.controls;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.*;
import java.net.URL;

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
 *          This class represents a custom button that behaves as a link.
 */
public class JLinkButton extends JButton {

    //region Constants
    private static final long serialVersionUID = -7843684977763585402L;

    public static final int ALWAYS_UNDERLINE = 0;
    public static final int HOVER_UNDERLINE  = 1;
    public static final int NEVER_UNDERLINE  = 2;
    public static final int SYSTEM_DEFAULT   = 3;
    //endregion

    //region Variables
    private int     linkBehavior;
    private Color   linkColor;
    private Color   colorPressed;
    private Color   visitedLinkColor;
    private Color   disabledLinkColor;
    private URL     buttonURL;
    private Action  defaultAction;
    private boolean isLinkVisited;
    //endregion

    //region Constructor
    public JLinkButton() {
        this(null, null, null);
    }

    public JLinkButton(String text, Icon icon, URL url) {
        super(text, icon);

        this.defaultAction = null;
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
    //endregion

    //region Public Methods
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
        validateLinkBehaviour(bnew);
        int old = this.linkBehavior;
        this.linkBehavior = bnew;
        firePropertyChange("linkBehavior", old, bnew);
        repaint();
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
    //endregion

    //region Protected Methods
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
    //endregion

    //region Private Methods
    private static void validateLinkBehaviour(int beha) {
        if (beha != ALWAYS_UNDERLINE && beha != HOVER_UNDERLINE && beha != NEVER_UNDERLINE && beha != SYSTEM_DEFAULT) {
            throw new IllegalArgumentException("Not a legal LinkBehavior");
        }
    }
    //endregion

    private static class BasicLinkButtonUI extends MetalButtonUI {

        //region Variables
        private static final BasicLinkButtonUI ui = new BasicLinkButtonUI();
        //endregion

        //region Constructor
        private BasicLinkButtonUI() {
        }
        //endregion

        //region Public Methods
        public static ComponentUI createUI(JComponent jcomponent) {
            return ui;
        }
        //endregion

        //region Protected Methods
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
        //endregion
    }
}
