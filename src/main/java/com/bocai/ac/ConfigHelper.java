package com.bocai.ac;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;

public class ConfigHelper {
    private static Map<String, String> kv = Maps.newHashMap();

    private static Properties properties = null;

    public static String getConfigValue(final String name) {
        String value = kv.get(name);
        if (value == null) {
            if (properties == null) {
                properties = new Properties();
                try {
                    properties.load(ConfigHelper.class.getClassLoader().getResourceAsStream("config.properties"));
                } catch (final IOException e) {
                    e.printStackTrace();
                    properties = null;
                }
            }
            if (properties != null) {
                value = properties.getProperty(name);
                kv.put(name, name);
            }
        }
        return value;
    }

}
