package com.image;
import org.yaml.snakeyaml.Yaml;

import java.awt.Color;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ColorConfigLoader {
    private Map<String, Color> categoryColors;

    public ColorConfigLoader() {
        // Load the YAML file
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yaml")) {
            if (inputStream == null) {
                throw new RuntimeException("application.yaml not found");
            }

            Config config = yaml.loadAs(inputStream, Config.class);
            Map<String, String> colorHexCodes = config.getCategoryColors();

            categoryColors = new HashMap<>();
            for (Map.Entry<String, String> entry : colorHexCodes.entrySet()) {
                categoryColors.put(entry.getKey(), Color.decode(entry.getValue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Color> getCategoryColors() {
        return categoryColors;
    }
}
