package hrider.ui.controls.xml;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import hrider.io.Log;
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
 *          This class represents a custom text pane to present the XML in a friendly way.
 */
public class XmlTextPane extends JTextPane {

    //region Constants
    private static final Log  logger           = Log.getLogger(XmlTextPane.class);
    private static final long serialVersionUID = 6270183148379328084L;
    private static final int  LINE_WIDTH       = 500;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link XmlTextPane} class.
     */
    public XmlTextPane() {
        this.setEditorKitForContentType("text/xml", new XmlEditorKit());
        this.setContentType("text/xml");
    }
    //endregion

    //region Public Methods

    /**
     * Replaces the text pane content with the new text.
     *
     * @param t The text to set.
     */
    @Override
    public void setText(String t) {
        if (t != null) {
            super.setText(formatXml(t));
        }
        else {
            super.setText(t);
        }
    }

    /**
     * Validates if the text pane content is a valid XML.
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public void validateXml() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        builder.parse(new InputSource(new StringReader(getText())));
    }
    //endregion

    //region Private Methods

    /**
     * Formats the XML.
     *
     * @param xml The XML to format.
     * @return A formatted XML.
     */
    private static String formatXml(String xml) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            document.getDocumentElement().normalize();

            OutputFormat format = new OutputFormat(document);
            format.setIndenting(true);
            format.setIndent(4);
            format.setLineWidth(LINE_WIDTH);

            StringWriter out = new StringWriter();

            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);

            return out.toString();
        }
        catch (Exception e) {
            logger.error(e, "Failed to format XML '%s'.", xml);
            return xml;
        }
    }
    //endregion
}
