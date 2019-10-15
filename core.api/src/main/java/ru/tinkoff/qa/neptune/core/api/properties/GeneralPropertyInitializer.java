package ru.tinkoff.qa.neptune.core.api.properties;

import java.io.*;
import java.util.Properties;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.String.valueOf;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class GeneralPropertyInitializer {

    public static final String GENERAL_PROPERTIES = "neptune.general.properties";
    static boolean arePropertiesRead;

    private GeneralPropertyInitializer() {
        super();
    }

    private static File findPropertyFile() {
        return findPropertyFile(new File(GENERAL_PROPERTIES).getAbsolutePath().replace(GENERAL_PROPERTIES, ""));
    }

    private static File findPropertyFile(String startPath) {
        // attempt to find configuration in the specified directory
        var defaultConfig = new File(startPath);
        var list = defaultConfig
                .listFiles((dir, name) -> name
                        .endsWith(GENERAL_PROPERTIES));

        if (nonNull(list) && list.length > 0) {
            return list[0];
        } else if (nonNull(list)) {
            var inner = defaultConfig.listFiles();
            File result = null;
            for (File element : inner) {
                if (element.isDirectory()) {
                    result = findPropertyFile(element.getPath());
                }
                if (nonNull(result)) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void checkSystemPropertyAndFillIfNecessary(String propertyName, String valueToSet) {
        if (isBlank(valueToSet)) {
            return;
        }

        ofNullable(getProperty(propertyName))
                .ifPresentOrElse(s -> {
                    if (isBlank(s)) {
                        setProperty(propertyName, valueToSet);
                    }
                }, () -> setProperty(propertyName, valueToSet));
    }

    /**
     * Reads properties defined in a file and instantiates system properties.
     *
     * @param file is a file to read.
     */
    public static synchronized void refreshProperties(File file) {
        try {
            refreshProperties(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new PropertyReadException(e.getMessage(), e);
        }
    }

    private static synchronized void refreshProperties(InputStream input) {
        var prop = new Properties();
        try {
            prop.load(input);
        } catch (IOException e) {
            throw new PropertyReadException(e.getMessage(), e);
        }

        prop.forEach((key, value) -> checkSystemPropertyAndFillIfNecessary(valueOf(key),
                nonNull(value) ? valueOf(value) : EMPTY));
        arePropertiesRead = true;
    }

    /**
     * Reads properties defined in {@link #GENERAL_PROPERTIES} which is located in any folder of the project
     * and instantiates system properties.
     */
    public synchronized static void refreshProperties() {
        //Firstly we try to read properties from resources
        ofNullable(getSystemClassLoader().getResourceAsStream(GENERAL_PROPERTIES))
                .ifPresentOrElse(GeneralPropertyInitializer::refreshProperties,
                        () -> {
                            var propertyFile = findPropertyFile();
                            if (nonNull(propertyFile)) {
                                refreshProperties(propertyFile);
                            }
                        });
        arePropertiesRead = true;
    }

    static boolean arePropertiesRead() {
        return arePropertiesRead;
    }

    private static class PropertyReadException extends RuntimeException {
        private PropertyReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
