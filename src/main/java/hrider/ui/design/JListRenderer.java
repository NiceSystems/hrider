package hrider.ui.design;

import hrider.hbase.Connection;
import hrider.hbase.TableUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

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
 */
public class JListRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 8219559461829225540L;

    private Connection connection;

    public JListRenderer(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof String) {
            try {
                if (isSelected || cellHasFocus) {
                    if (TableUtil.isMetaTable((String)value)) {
                        setForeground(Color.darkGray);
                    }
                    else if (!connection.tableEnabled((String)value)) {
                        setForeground(Color.gray);
                    }
                }
            }
            catch (Exception ignore) {
            }
        }
        return component;
    }
}
