package hrider.ui.controls.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 05/11/12
 * Time: 15:47
 */
public class JsonTextPane extends JTextPane {

    private static final long serialVersionUID = 6270183148379328084L;

    public JsonTextPane() {
    }

    @Override
    public void setText(String text) {
        if (text != null) {
            text = formatJson(text);
        }
        super.setText(text);
    }

    public void validateJson() throws JsonSyntaxException {
        new JsonParser().parse(super.getText());
    }

    private String formatJson(String json) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(new JsonParser().parse(json));
        }
        catch (Exception ignore) {
            return json;
        }
    }
}
