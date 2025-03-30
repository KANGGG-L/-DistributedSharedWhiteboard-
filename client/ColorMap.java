package client;

import java.util.HashMap;
import java.util.Map;
import java.awt.*;

public class ColorMap {
    static Map<Color, String> colorMap; 
	static Map<String, Color> colorRevMap; 

    public ColorMap() {
        colorMap = new HashMap<>(); 
        colorRevMap = new HashMap<>();

        colorMap.put(Color.BLACK, "BLACK");
		colorMap.put(Color.BLUE, "BLUE");
		colorMap.put(Color.CYAN, "CYAN");
		colorMap.put(Color.DARK_GRAY, "DARK_GRAY");
		colorMap.put(Color.GRAY, "GRAY");
		colorMap.put(Color.GREEN, "GREEN");
		colorMap.put(Color.LIGHT_GRAY, "LIGHT_GRAY");
		colorMap.put(Color.MAGENTA, "MAGENTA");
		colorMap.put(Color.ORANGE, "ORANGE");
		colorMap.put(Color.RED, "RED");
		colorMap.put(Color.PINK, "PINK");
		colorMap.put(Color.YELLOW, "YELLOW");
		colorMap.put(Color.WHITE, "WHITE");

		for (Map.Entry<Color, String> entry : colorMap.entrySet()) {
            Color color = entry.getKey();
            String colorName = entry.getValue();
            colorRevMap.put(colorName, color);
        }
    }

    public static Map<Color, String> getColorMap() {
        return colorMap;
    }

    public static Map<String, Color> getColorRevMap() {
        return colorRevMap;
    }


}
