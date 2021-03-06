package ru.tinkoff.qa.neptune.http.api.properties.executor;

import ru.tinkoff.qa.neptune.core.api.properties.PropertyDescription;
import ru.tinkoff.qa.neptune.core.api.properties.PropertyName;
import ru.tinkoff.qa.neptune.core.api.properties.object.ObjectPropertySupplier;

import java.util.concurrent.Executor;
import java.util.function.Supplier;


@PropertyDescription(description = {"Defines full name of a class which extends DefaultHttpExecutorProperty.ExecutorSupplier",
        "and whose objects supply instances of java.util.concurrent.Executor"},
        section = "Http client. General")
@PropertyName("DEFAULT_HTTP_EXECUTOR")
public final class DefaultHttpExecutorProperty implements ObjectPropertySupplier<Executor, DefaultHttpExecutorProperty.ExecutorSupplier> {

    /**
     * This instance reads value of the property {@code 'DEFAULT_HTTP_EXECUTOR'} and returns an {@link Executor}.
     */
    public static final DefaultHttpExecutorProperty DEFAULT_HTTP_EXECUTOR_PROPERTY =
            new DefaultHttpExecutorProperty();

    private DefaultHttpExecutorProperty() {
        super();
    }

    public static abstract class ExecutorSupplier implements Supplier<Executor> {
    }
}
