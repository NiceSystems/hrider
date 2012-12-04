package hrider.ui.controls.xml;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 04/11/12
 * Time: 09:42
 */
public class XmlTextPane extends JTextPane {

    private static final long serialVersionUID = 6270183148379328084L;

    public XmlTextPane() {
        this.setEditorKitForContentType("text/xml", new XmlEditorKit());
        this.setContentType("text/xml");
    }

    @Override
    public void setText(String text) {
        if (text != null) {
            text = formatXml(text);
        }
        super.setText(text);
    }

    public void validateXml() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        builder.parse(new InputSource(new StringReader(super.getText())));
    }

    private String formatXml(String xml) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            document.getDocumentElement().normalize();

            OutputFormat format = new OutputFormat(document);
            format.setIndenting(true);
            format.setIndent(4);
            format.setLineWidth(500);

            Writer out = new StringWriter();

            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);

            return out.toString();
        }
        catch (Exception ignore) {
            return xml;
        }
    }
}
