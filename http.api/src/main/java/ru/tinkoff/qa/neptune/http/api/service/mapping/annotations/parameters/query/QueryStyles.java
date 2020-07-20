package ru.tinkoff.qa.neptune.http.api.service.mapping.annotations.parameters.query;

import com.google.common.annotations.Beta;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static ru.tinkoff.qa.neptune.http.api.request.QueryValueDelimiters.*;
import static ru.tinkoff.qa.neptune.http.api.service.mapping.annotations.parameters.ParameterUtil.*;

/**
 * Query parameters support the following style values:
 * <ul>
 *     <li>Form - ampersand-separated values, also known as form-style query expansion.</li>
 *     <li>Space delimited - space-separated array values.</li>
 *     <li>Pipe delimited – pipeline-separated array values.</li>
 *     <li>Deep object – a simple way of rendering nested objects using form parameters (applies to objects only).</li>
 * </ul>
 */
public enum QueryStyles {
    /**
     * Form (default) - ampersand-separated values, also known as form-style query expansion.
     */
    FORM {
        @Override
        List<Query> arrayValue(Stream<?> valueSource,
                               String varName,
                               boolean explode,
                               boolean allowReserved) {
            return of(new Query(varName, explode, explode ? null : COMMA, allowReserved, valueSource.toArray()));
        }

        @Override
        List<Query> mapValue(Map<?, ?> map,
                             String varName,
                             boolean explode,
                             boolean allowReserved) {
            var result = new LinkedList<Query>();
            if (explode) {
                map.forEach((o, o2) -> result.add(new Query(String.valueOf(o), false, COMMA, allowReserved, getObjectFlat(o2))));
            } else {
                result.add(new Query(varName,
                        false,
                        COMMA,
                        allowReserved, getObjectFlat(map.entrySet()
                        .stream()
                        .filter(entry -> {
                            var cls = entry.getValue().getClass();
                            return !Map.class.isAssignableFrom(cls) && !isAMethodParameter(cls);
                        })
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)))));
            }
            return result;
        }
    },
    /**
     * Space delimited - space-separated array values. Has effect only for non-exploded arrays.
     */
    SPACE_DELIMITED {
        List<Query> arrayValue(Stream<?> valueSource,
                               String varName,
                               boolean explode,
                               boolean allowReserved) {
            if (!explode) {
                return of(new Query(varName, false, SPACE, allowReserved, valueSource.toArray()));
            }
            return FORM.arrayValue(valueSource, varName, true, allowReserved);
        }
    },
    /**
     * Pipe delimited – pipeline-separated array values. Has effect only for non-exploded arrays.
     */
    PIPE_DELIMITED {
        List<Query> arrayValue(Stream<?> valueSource,
                               String varName,
                               boolean explode,
                               boolean allowReserved) {
            if (!explode) {
                return of(new Query(varName, false, PIPE, allowReserved, valueSource.toArray()));
            }
            return FORM.arrayValue(valueSource, varName, true, allowReserved);
        }
    },
    /**
     * Deep object – a simple way of rendering nested objects using form parameters (applies to objects only)
     */
    @Beta
    DEEP_OBJECT {
        @Override
        List<Query> mapValue(Map<?, ?> map,
                             String varName,
                             boolean explode,
                             boolean allowReserved) {
            var result = new LinkedList<Query>();
            map.entrySet()
                    .stream()
                    .filter(entry -> {
                        var cls = entry.getValue().getClass();
                        return !Map.class.isAssignableFrom(cls) && !isAMethodParameter(cls)
                                && !cls.isArray() && !Iterable.class.isAssignableFrom(cls);
                    })
                    .forEach(entry -> result
                            .add(new Query(varName + "[" + entry.getKey() + "]",
                                    true,
                                    null,
                                    allowReserved, getObjectFlat(entry.getValue()))));


            return result;
        }
    };

    private static Object[] getObjectFlat(Object object) {
        var map = objectToMap(object);
        if (map != null) {
            var result = new Object[]{};
            for (var e : map.entrySet()) {
                var v = e.getValue();
                if (objectToMap(v) == null) {
                    result = addAll(result, e.getKey(), e.getValue());
                }
            }
            return result;
        } else {
            return new Object[]{object};
        }
    }

    /**
     * Returns a a representation of a query parameter or query part
     *
     * @param queryParameterValue a value to be transformed into query parameter and its values/
     *                            string part of a query
     * @param parameterName       is a name of a query parameter
     * @param explode             to explode value or not
     * @return a list of {@link Query}.
     */
    List<Query> getQueryParameterValue(Object queryParameterValue,
                                       String parameterName,
                                       boolean explode,
                                       boolean allowReserved) {
        var stream = toStream(queryParameterValue);
        if (stream != null) {
            return arrayValue(stream,
                    parameterName,
                    explode,
                    allowReserved);
        }

        return ofNullable(objectToMap(queryParameterValue))
                .map(map -> mapValue(map, parameterName, explode, allowReserved))
                .orElseGet(() -> arrayValue(of(queryParameterValue).stream(), parameterName, explode, allowReserved));

    }

    List<Query> arrayValue(Stream<?> valueSource,
                           String varName,
                           boolean explode,
                           boolean allowReserved) {
        throw new UnsupportedOperationException(format("Query parameter %s doesn't support array/collection values " +
                        "due to defined style: %s",
                varName,
                name()));
    }

    List<Query> mapValue(Map<?, ?> map,
                         String varName,
                         boolean explode,
                         boolean allowReserved) {
        throw new UnsupportedOperationException(format("Query parameter %s doesn't support object values " +
                        "due to defined style: %s",
                varName,
                name()));
    }
}
