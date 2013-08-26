package hrider.ui;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class Images {

    private static Map<String, ImageIcon> images;

    private Images() {
    }

    static {
        images = new HashMap<String, ImageIcon>();
    }

    public static ImageIcon get(String name) {
        if (images.containsKey(name)) {
            return images.get(name);
        }

        ImageIcon icon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource(String.format("images/%s.png", name)));
        images.put(name, icon);

        return icon;
    }
}
