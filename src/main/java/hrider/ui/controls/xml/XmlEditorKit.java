package hrider.ui.controls.xml;

import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 04/11/12
 * Time: 09:43
 */
public class XmlEditorKit extends StyledEditorKit {

    private static final long serialVersionUID = 2969169649596107757L;
    private ViewFactory xmlViewFactory;

    public XmlEditorKit() {
        xmlViewFactory = new XmlViewFactory();
    }

    @Override
    public ViewFactory getViewFactory() {
        return xmlViewFactory;
    }

    @Override
    public String getContentType() {
        return "text/xml";
    }
}
