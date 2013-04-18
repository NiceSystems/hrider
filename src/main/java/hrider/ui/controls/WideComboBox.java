package hrider.ui.controls;

import javax.swing.*;
import java.awt.*;

public class WideComboBox extends JComboBox {

    //region Constants
    private static final long serialVersionUID = -2142335227903444963L;
    //endregion

    //region Variables
    private boolean layingOut;
    //endregion

    //region Constructor
    public WideComboBox() {
    }

    public WideComboBox(Object[] items) {
        super(items);
    }

    public WideComboBox(ComboBoxModel model) {
        super(model);
    }
    //endregion

    //region Public Methods
    @Override
    public void doLayout() {
        try {
            layingOut = true;
            super.doLayout();
        }
        finally {
            layingOut = false;
        }
    }

    @Override
    public Dimension getSize() {
        Dimension dim = super.getSize();
        if (!layingOut) {
            dim.width = Math.max(dim.width, getPreferredSize().width);
        }
        return dim;
    }
    //endregion
}
