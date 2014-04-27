package hrider.converters;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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
 *          This class is a place holder for XML editors.
 */
public class XmlConverter extends StringConverter {

    private static final long serialVersionUID = -2217840069549275853L;
    private static final int  LINE_WIDTH       = 500;

    @Override
    public boolean isValidForNameConversion() {
        return false;
    }

    @Override
    public boolean supportsFormatting() {
        return true;
    }

    @Override
    public String format(String value) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(value)));
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
            logger.error(e, "Failed to format XML '%s'.", value);
            return value;
        }
    }

    @Override
    public Map<Pattern, Color> getColorMappings() {
        Map<Pattern, Color> colorMap = new HashMap<Pattern, Color>();
        colorMap.put(Pattern.compile("(<!\\[CDATA\\[)(?=.*)"), new Color(128, 128, 128));
        colorMap.put(Pattern.compile("(?<=.*)(]]>)"), new Color(128, 128, 128));
        colorMap.put(Pattern.compile("(<\\s*/?\\s*[a-zA-Z-_:\\.]*)(?=\\s*[ >/])"), new Color(63, 127, 127));
        colorMap.put(Pattern.compile("(/?\\s*>)"), new Color(63, 127, 127));
        colorMap.put(Pattern.compile("(?<=\\s)(\\w*)(?==)"), new Color(127, 0, 127));
        colorMap.put(Pattern.compile("(?<=[a-zA-Z-]*=)(\"[^\"]*\")"), new Color(42, 0, 255));
        colorMap.put(Pattern.compile("(<!--.*-->)"), new Color(63, 95, 191));

        return colorMap;
    }
}
