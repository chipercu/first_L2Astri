package fuzzy.Html_Constructor.tags;

import fuzzy.Html_Constructor.tags.parameters.Color;

public class Font implements Build{

    private Color color;
    private String text;

//    <font color=F2C202>Баффер</font>

    public Font(Color color, String text){
        this.color = color;
        this.text = text;
    }



    @Override
    public String build() {
        return "<font color=" + color.getValue() +">" + text + "</font>";
    }
}
