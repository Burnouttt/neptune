package ru.tinkoff.qa.neptune.selenium.content.management;

import ru.tinkoff.qa.neptune.selenium.hooks.BrowserUrlVariable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.*;
import static ru.tinkoff.qa.neptune.core.api.utils.URLEncodeUtil.encodePathSubstring;
import static ru.tinkoff.qa.neptune.core.api.utils.URLEncodeUtil.encodeQuerySubstring;
import static ru.tinkoff.qa.neptune.selenium.content.management.BrowserUrlCreator.Styles.*;

/**
 * Util class that reads metadata of some class and field values of an objects and returns URL to navigate to
 */
final class BrowserUrlCreator {

    private static String getSegment(String rawUrl, String separator) {
        var index = rawUrl.indexOf(separator);
        if (index < 0) {
            return null;
        }

        if (index == rawUrl.length() - 1) {
            return EMPTY;
        }

        return rawUrl.substring(index + 1);
    }

    static String createBrowserUrl(Object o, String rawURL) {
        var urlParts = new UrlParts(rawURL);
        var patternParams = urlParts.getVariables();

        var cls = o.getClass();
        var clz = cls;
        var fields = new ArrayList<Field>();

        while (!clz.equals(Object.class)) {
            fields.addAll(stream(clz.getDeclaredFields())
                    .filter(field -> field.getAnnotationsByType(ru.tinkoff.qa.neptune.selenium.hooks.BrowserUrlVariable.class).length > 0)
                    .collect(toList()));
            clz = clz.getSuperclass();
        }

        var browserUrlVars = new ArrayList<BrowserUrlVar>();

        for (var p : patternParams) {
            fields.stream()
                    .filter(field -> {
                        var variables = field.getAnnotationsByType(ru.tinkoff.qa.neptune.selenium.hooks.BrowserUrlVariable.class);
                        return stream(variables).anyMatch(browserUrlVariable -> browserUrlVariable.name().equals(p)
                                || (isBlank(browserUrlVariable.name()) && field.getName().equals(p)));
                    })
                    .findFirst()
                    .ifPresentOrElse(field -> {
                                field.setAccessible(true);
                                try {
                                    var variables = field.getAnnotationsByType(BrowserUrlVariable.class);
                                    var variable = stream(variables)
                                            .filter(browserUrlVariable -> browserUrlVariable.name().equals(p)
                                                    || (isBlank(browserUrlVariable.name()) && field.getName().equals(p)))
                                            .findFirst()
                                            .orElseThrow(RuntimeException::new);

                                    if (isBlank(variable.name())) {
                                        browserUrlVars.add(new BrowserUrlVar(p,
                                                field,
                                                isStatic(field.getModifiers()) ? field.getDeclaringClass() : o,
                                                variable.toEncodeForQueries()));
                                    } else {
                                        if (isBlank(variable.field())) {
                                            browserUrlVars.add(new BrowserUrlVar(p,
                                                    field,
                                                    isStatic(field.getModifiers()) ? field.getDeclaringClass() : o,
                                                    variable.toEncodeForQueries()));
                                        } else {
                                            browserUrlVars.add(new BrowserUrlVar(p,
                                                    field.getType().getDeclaredField(variable.field()),
                                                    isStatic(field.getModifiers()) ? field.get(field.getDeclaringClass()) : field.get(o),
                                                    variable.toEncodeForQueries()));
                                        }
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            () -> {
                                throw new UnsupportedOperationException(format("URL-variable '%s' is not mapped by any " +
                                                "field (fields of superclasses are included) of '%s'. Given raw URL with variables: [%s]",
                                        p,
                                        cls.getName(),
                                        rawURL));
                            });
        }

        for (var v : browserUrlVars) {
            urlParts.changeProtocol(v);
            urlParts.changeHost(v);
            urlParts.changePort(v);
            urlParts.changePath(v);
            urlParts.changeQuery(v);
            urlParts.changeRef(v);
        }

        return urlParts.returnUrl();
    }

    enum Styles {
        TO_NOT_ENCODE(false) {
            @Override
            String encode(String toEncode) {
                return toEncode;
            }
        },
        PATH(true) {
            @Override
            String encode(String toEncode) {
                return encodePathSubstring(toEncode);
            }
        },
        QUERY(null) {
            @Override
            String encode(String toEncode) {
                return encodeQuerySubstring(toEncode, false);
            }
        };


        private final Boolean toEncode;

        Styles(Boolean toEncode) {
            this.toEncode = toEncode;
        }

        private Boolean getToEncode() {
            return toEncode;
        }

        abstract String encode(String toEncode);
    }

    private enum URLProtocols {
        FILE("file"),
        FTP("ftp"),
        HTTP("http"),
        HTTPS("https");

        private final String name;

        URLProtocols(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class BrowserUrlVar {
        private final String name;
        private final Field get;
        private final Object from;
        private final boolean toEncode;

        private BrowserUrlVar(String name, Field get, Object from, boolean toEncode) {
            this.name = name;
            this.get = get;
            this.from = from;
            this.toEncode = toEncode;
        }

        private String replace(String fragment, Styles style) {

            if (fragment == null) {
                return null;
            }

            if (from == null) {
                throw new UnsupportedOperationException(format("Null values can't be used " +
                                "for the getting of values of URL-variables. " +
                                "Variable: %s. Field: %s. Raw fragment of an URL with not replaced variable: %s",
                        name,
                        get,
                        fragment));
            }

            get.setAccessible(true);
            try {
                var value = get.get(from);
                if (value == null) {
                    throw new UnsupportedOperationException(format("Null values can't be used " +
                                    "for the getting of values of URL-variables. " +
                                    "Variable: %s. Field: %s. Raw fragment of an URL with not replaced variable: %s",
                            name,
                            get,
                            fragment));
                }

                var toEncode = ofNullable(style.getToEncode())
                        .orElse(this.toEncode);

                var variableValue = toEncode ? style.encode(valueOf(value)) : valueOf(value);
                return fragment.replace("{" + name + "}", variableValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class UrlParts {
        private String protocolPart;
        private String hostPart;
        private String portPart;
        private String pathPart;
        private String queryPart;
        private String refPart;

        private UrlParts(String rawURL) {
            var rawURL2 = rawURL;

            URLProtocols protocol = null;
            for (var p : URLProtocols.values()) {
                if (rawURL.startsWith(p + ":")) {
                    protocol = p;
                    break;
                }
            }

            if (protocol != null) {
                rawURL2 = rawURL2.replace(protocol.toString(), EMPTY);

                var colons = new StringBuilder();
                while (rawURL2.startsWith(":")) {
                    colons.append(":");
                    rawURL2 = rawURL2.substring(1);
                }

                var slashes = new StringBuilder();
                while (rawURL2.startsWith("/")) {
                    slashes.append("/");
                    rawURL2 = rawURL2.substring(1);
                }

                this.protocolPart = protocol + colons.toString() + slashes.toString();
            }

            this.refPart = ofNullable(getSegment(rawURL2, "#"))
                    .map(s -> "#" + s)
                    .orElse(null);

            if (this.refPart == null) {
                this.queryPart = ofNullable(getSegment(rawURL2, "?"))
                        .map(s -> "?" + s)
                        .orElse(null);
                if (this.queryPart != null) {
                    rawURL2 = rawURL2.replace(this.queryPart, EMPTY);
                }
            } else {
                rawURL2 = rawURL2.replace(this.refPart, EMPTY);
            }

            if (protocolPart == null) {
                this.pathPart = rawURL2;
                return;
            }

            this.pathPart = ofNullable(getSegment(rawURL2, "/"))
                    .map(s -> "/" + s)
                    .orElse(null);

            if (pathPart != null) {
                rawURL2 = rawURL2.replace(this.pathPart, EMPTY);
            }

            this.portPart = ofNullable(getSegment(rawURL2, ":"))
                    .map(s -> ":" + s)
                    .orElse(null);

            if (portPart != null) {
                this.hostPart = rawURL2.replace(this.portPart, EMPTY);
            } else {
                this.hostPart = rawURL2;
            }
        }

        private static List<String> getVariables(String segment) {
            return ofNullable(substringsBetween(segment, "{", "}"))
                    .map(strings -> stream(strings)
                            .distinct()
                            .collect(toList()))
                    .orElseGet(List::of);
        }

        private List<String> getVariables() {
            var patternParams = new ArrayList<String>();
            patternParams.addAll(getVariables(protocolPart));
            patternParams.addAll(getVariables(hostPart));
            patternParams.addAll(getVariables(portPart));

            patternParams.addAll(getVariables(pathPart));
            patternParams.addAll(getVariables(queryPart));
            patternParams.addAll(getVariables(refPart));

            return patternParams.stream()
                    .distinct()
                    .collect(toList());
        }

        private void changeProtocol(BrowserUrlVar var) {
            this.protocolPart = var.replace(protocolPart, TO_NOT_ENCODE);
        }

        private void changeHost(BrowserUrlVar var) {
            this.hostPart = var.replace(hostPart, TO_NOT_ENCODE);
        }

        private void changePort(BrowserUrlVar var) {
            this.portPart = var.replace(portPart, TO_NOT_ENCODE);
        }

        private void changePath(BrowserUrlVar var) {
            this.pathPart = var.replace(pathPart, PATH);
        }

        private void changeQuery(BrowserUrlVar var) {
            this.queryPart = var.replace(queryPart, QUERY);
        }

        private void changeRef(BrowserUrlVar var) {
            this.refPart = var.replace(refPart, TO_NOT_ENCODE);
        }

        private String returnUrl() {
            var sb = new StringBuilder();
            ofNullable(this.protocolPart).ifPresent(sb::append);
            ofNullable(this.hostPart).ifPresent(sb::append);
            ofNullable(this.portPart).ifPresent(sb::append);
            ofNullable(this.pathPart).ifPresent(s -> sb.append(s.startsWith("/") ? s : "/" + s));
            if (this.refPart == null) {
                ofNullable(this.queryPart).ifPresent(sb::append);
            } else {
                sb.append(refPart);
            }

            return sb.toString();
        }
    }
}
