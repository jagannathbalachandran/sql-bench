package io.sqlbench.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvInterpolator {
    private static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    public static String interpolate(String value) {
        if (value == null) return null;
        Matcher m = PATTERN.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1);
            String envValue = System.getenv(varName);
            if (envValue == null) envValue = System.getProperty(varName, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(envValue));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
