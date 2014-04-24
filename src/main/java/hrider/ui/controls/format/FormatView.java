package hrider.ui.controls.format;

import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
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
 *          This class represents a custom XML view. All the drawings of different colors performed in this class.
 */
public class FormatView extends PlainView {

    //region Constants
    private static final Map<Pattern, Color> EMPTY_MAP = new HashMap<Pattern, Color>();
    //endregion

    //region Variables
    private TextFormatter formatter;
    //endregion

    //region Constructor
    public FormatView(TextFormatter formatter, Element element) {
        super(element);

        this.formatter = formatter;

        // Set tab size to 4 (instead of the default 8)
        getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);
    }
    //endregion

    //region Protected Methods
    @Override
    protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
        Map<Pattern, Color> patternColors = formatter.getColorMappings();
        if (patternColors == null) {
            patternColors = EMPTY_MAP;
        }

        Document doc = getDocument();
        String text = doc.getText(p0, p1 - p0);

        Segment segment = getLineBuffer();

        Map<Integer, Integer> startMap = new TreeMap<Integer, Integer>();
        Map<Integer, Color> colorMap = new TreeMap<Integer, Color>();

        // Match all regexes on this snippet, store positions
        for (Map.Entry<Pattern, Color> entry : patternColors.entrySet()) {
            Matcher matcher = entry.getKey().matcher(text);
            while (matcher.find()) {
                int end = matcher.end();
                int start;

                if (matcher.groupCount() > 0) {
                    start = matcher.start(1);
                }
                else {
                    start = matcher.start();
                }

                startMap.put(start, end);
                colorMap.put(start, entry.getValue());
            }
        }

        int lastEnd = -1;

        Map<Integer, Integer> fixedMap = new TreeMap<Integer, Integer>();
        for (Map.Entry<Integer, Integer> entry : startMap.entrySet()) {
            int start = entry.getKey();
            int end = entry.getValue();

            if (start < lastEnd) {
                Color color = colorMap.get(start);
                colorMap.remove(start);

                if (end <= lastEnd) {
                    continue;
                }

                start = lastEnd;
                colorMap.put(start, color);
            }

            fixedMap.put(start, end);
            lastEnd = end;
        }


        int i = 0;

        // Colour the parts
        for (Map.Entry<Integer, Integer> entry : fixedMap.entrySet()) {
            int start = entry.getKey();
            int end = entry.getValue();

            if (i < start) {
                g.setColor(Color.black);
                doc.getText(p0 + i, start - i, segment);
                x = Utilities.drawTabbedText(segment, x, y, g, this, i);
            }

            g.setColor(colorMap.get(start));
            i = end;
            doc.getText(p0 + start, i - start, segment);
            x = Utilities.drawTabbedText(segment, x, y, g, this, start);
        }

        // Paint possible remaining text black
        if (i < text.length()) {
            g.setColor(Color.black);
            doc.getText(p0 + i, text.length() - i, segment);
            x = Utilities.drawTabbedText(segment, x, y, g, this, i);
        }

        return x;
    }
    //endregion
}
