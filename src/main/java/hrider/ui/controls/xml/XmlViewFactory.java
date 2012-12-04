package hrider.ui.controls.xml;

import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 04/11/12
 * Time: 09:43
 */
public class XmlViewFactory implements ViewFactory {

    /**
     * @see javax.swing.text.ViewFactory#create(javax.swing.text.Element)
     */
    public View create(Element element) {

        return new XmlView(element);
    }
}
