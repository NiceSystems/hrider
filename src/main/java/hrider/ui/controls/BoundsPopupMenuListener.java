package hrider.ui.controls;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;

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
 * <p/>
 * This class will change the bounds of the JComboBox popup menu to support
 * different functionality. It will support the following features:
 * -  a horizontal scrollbar can be displayed when necessary
 * -  the popup can be wider than the combo box
 * -  the popup can be displayed above the combo box
 * <p/>
 * Class will only work for a JComboBox that uses a BasicComboPop.
 */
public class BoundsPopupMenuListener implements PopupMenuListener {

    //region Variables
    private boolean scrollBarRequired = true;
    private boolean popupWider;
    private int maximumWidth = -1;
    private boolean     popupAbove;
    private JScrollPane scrollPane;
    //endregion

    //region Constructor

    /**
     * Convenience constructore to allow the display of a horizontal scrollbar
     * when required.
     */
    public BoundsPopupMenuListener() {
        this(true, false, -1, false);
    }

    /**
     * Convenience constructor that allows you to display the popup
     * wider and/or above the combo box.
     *
     * @param popupWider when true, popup width is based on the popup
     *                   preferred width
     * @param popupAbove when true, popup is displayed above the combobox
     */
    public BoundsPopupMenuListener(boolean popupWider, boolean popupAbove) {
        this(false, popupWider, -1, popupAbove);
    }

    /**
     * Convenience constructor that allows you to display the popup
     * wider than the combo box and to specify the maximum width
     *
     * @param maximumWidth the maximum width of the popup. The
     *                     popupAbove value is set to "true".
     */
    public BoundsPopupMenuListener(int maximumWidth) {
        this(true, true, maximumWidth, false);
    }

    /**
     * General purpose constructor to set all popup properties at once.
     *
     * @param scrollBarRequired display a horizontal scrollbar when the
     *                          preferred width of popup is greater than width of scrollPane.
     * @param popupWider        display the popup at its preferred with
     * @param maximumWidth      limit the popup width to the value specified
     *                          (minimum size will be the width of the combo box)
     * @param popupAbove        display the popup above the combo box
     */
    public BoundsPopupMenuListener(boolean scrollBarRequired, boolean popupWider, int maximumWidth, boolean popupAbove) {
        this.scrollBarRequired = scrollBarRequired;
        this.popupWider = popupWider;
        this.maximumWidth = maximumWidth;
        this.popupAbove = popupAbove;
    }
    //endregion

    //region Public Methods

    /**
     * Return the maximum width of the popup.
     *
     * @return the maximumWidth value
     */
    public int getMaximumWidth() {
        return maximumWidth;
    }

    /**
     * Set the maximum width for the popup. This value is only used when
     * setPopupWider( true ) has been specified. A value of -1 indicates
     * that there is no maximum.
     *
     * @param maximumWidth the maximum width of the popup
     */
    public void setMaximumWidth(int maximumWidth) {
        this.maximumWidth = maximumWidth;
    }

    /**
     * Determine if the popup should be displayed above the combo box.
     *
     * @return the popupAbove value
     */
    public boolean isPopupAbove() {
        return popupAbove;
    }

    /**
     * Change the location of the popup relative to the combo box.
     *
     * @param popupAbove true display popup above the combo box,
     *                   false display popup below the combo box.
     */
    public void setPopupAbove(boolean popupAbove) {
        this.popupAbove = popupAbove;
    }

    /**
     * Determine if the popup might be displayed wider than the combo box
     *
     * @return the popupWider value
     */
    public boolean isPopupWider() {
        return popupWider;
    }

    /**
     * Change the width of the popup to be the greater of the width of the
     * combo box or the preferred width of the popup. Normally the popup width
     * is always the same size as the combo box width.
     *
     * @param popupWider true adjust the width as required.
     */
    public void setPopupWider(boolean popupWider) {
        this.popupWider = popupWider;
    }

    /**
     * Determine if the horizontal scroll bar might be required for the popup
     *
     * @return the scrollBarRequired value
     */
    public boolean isScrollBarRequired() {
        return scrollBarRequired;
    }

    /**
     * For some reason the default implementation of the popup removes the
     * horizontal scrollBar from the popup scroll pane which can result in
     * the truncation of the rendered items in the popop. Adding a scrollBar
     * back to the scrollPane will allow horizontal scrolling if necessary.
     *
     * @param scrollBarRequired true add horizontal scrollBar to scrollPane
     *                          false remove the horizontal scrollBar
     */
    public void setScrollBarRequired(boolean scrollBarRequired) {
        this.scrollBarRequired = scrollBarRequired;
    }

    /**
     * Alter the bounds of the popup just before it is made visible.
     */
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        JComboBox comboBox = (JComboBox)e.getSource();

        if (comboBox.getItemCount() == 0) {
            return;
        }

        final Object child = comboBox.getAccessibleContext().getAccessibleChild(0);

        if (child instanceof BasicComboPopup) {
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        customizePopup((BasicComboPopup)child);
                    }
                });
        }
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        //  In its normal state the scrollpane does not have a scrollbar
        if (scrollPane != null) {
            scrollPane.setHorizontalScrollBar(null);
        }
    }
    //endregion

    //region Protected Methods
    protected void customizePopup(BasicComboPopup popup) {
        scrollPane = getScrollPane(popup);

        if (popupWider) {
            popupWider(popup);
        }

        checkHorizontalScrollBar(popup);

        //  For some reason in JDK7 the popup will not display at its preferred
        //  width unless its location has been changed from its default
        //  (ie. for normal "pop down" shift the popup and reset)

        Component comboBox = popup.getInvoker();
        Point location = comboBox.getLocationOnScreen();

        if (popupAbove) {
            int height = popup.getPreferredSize().height;
            popup.setLocation(location.x, location.y - height);
        }
        else {
            int height = comboBox.getPreferredSize().height;
            popup.setLocation(location.x, location.y + height - 1);
            popup.setLocation(location.x, location.y + height);
        }
    }

    /*
      *  Adjust the width of the scrollpane used by the popup
      */
    protected void popupWider(BasicComboPopup popup) {
        JList list = popup.getList();

        //  Determine the maximimum width to use:
        //  a) determine the popup preferred width
        //  b) limit width to the maximum if specified
        //  c) ensure width is not less than the scroll pane width

        int popupWidth = list.getPreferredSize().width + 5  // make sure horizontal scrollbar doesn't appear
                         + getScrollBarWidth(popup, scrollPane);

        if (maximumWidth != -1) {
            popupWidth = Math.min(popupWidth, maximumWidth);
        }

        Dimension scrollPaneSize = scrollPane.getPreferredSize();
        popupWidth = Math.max(popupWidth, scrollPaneSize.width);

        //  Adjust the width

        scrollPaneSize.width = popupWidth;
        scrollPane.setPreferredSize(scrollPaneSize);
        scrollPane.setMaximumSize(scrollPaneSize);
    }

    /*
      *  Get the scroll pane used by the popup so its bounds can be adjusted
      */
    protected static JScrollPane getScrollPane(ComboPopup popup) {
        JList list = popup.getList();
        return (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
    }

    /*
      *  I can't find any property on the scrollBar to determine if it will be
      *  displayed or not so use brute force to determine this.
      */
    protected static int getScrollBarWidth(BasicComboPopup popup, JScrollPane scrollPane) {
        int scrollBarWidth = 0;
        JComboBox comboBox = (JComboBox)popup.getInvoker();

        if (comboBox.getItemCount() > comboBox.getMaximumRowCount()) {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            scrollBarWidth = vertical.getPreferredSize().width;
        }

        return scrollBarWidth;
    }

    /*
      *  I can't find any property on the scrollBar to determine if it will be
      *  displayed or not so use brute force to determine this.
      */
    protected static boolean horizontalScrollBarWillBeVisible(BasicComboPopup popup, JScrollPane scrollPane) {
        JList list = popup.getList();
        int scrollBarWidth = getScrollBarWidth(popup, scrollPane);
        int popupWidth = list.getPreferredSize().width + scrollBarWidth;

        return popupWidth > scrollPane.getPreferredSize().width;
    }
    //endregion

    //region Private Methods
    /*
    *  This method is called every time:
    *  - to make sure the viewport is returned to its default position
    *  - to remove the horizontal scrollbar when it is not wanted
    */
    private void checkHorizontalScrollBar(BasicComboPopup popup) {
        //  Reset the viewport to the left
        JViewport viewport = scrollPane.getViewport();
        Point p = viewport.getViewPosition();
        p.x = 0;
        viewport.setViewPosition(p);

        //  Remove the scrollbar so it is never painted
        if (!scrollBarRequired) {
            scrollPane.setHorizontalScrollBar(null);
            return;
        }

        //	Make sure a horizontal scrollbar exists in the scrollpane
        JScrollBar horizontal = scrollPane.getHorizontalScrollBar();

        if (horizontal == null) {
            horizontal = new JScrollBar(Adjustable.HORIZONTAL);
            scrollPane.setHorizontalScrollBar(horizontal);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }

        //	Potentially increase height of scroll pane to display the scrollbar
        if (horizontalScrollBarWillBeVisible(popup, scrollPane)) {
            Dimension scrollPaneSize = scrollPane.getPreferredSize();
            scrollPaneSize.height += horizontal.getPreferredSize().height;
            scrollPane.setPreferredSize(scrollPaneSize);
            scrollPane.setMaximumSize(scrollPaneSize);
            scrollPane.revalidate();
        }
    }
    //endregion
}
