package ru.tinkoff.qa.neptune.data.base.api.query;

import ru.tinkoff.qa.neptune.data.base.api.DataBaseSteps;
import ru.tinkoff.qa.neptune.data.base.api.PersistableObject;

import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;

@SuppressWarnings("unchecked")
public abstract class ByIdsSequentialGetStepSupplier<T extends PersistableObject, S, Q extends ByIdsSequentialGetStepSupplier<T, S, Q>>
        extends SelectSequentialGetStepSupplier<S, DataBaseSteps, Q> {

    final Class<T> ofType;
    final Object[] ids;
    Predicate<T> condition;

    ByIdsSequentialGetStepSupplier(Class<T> ofType, Object... ids) {
        checkArgument(ofType != null, "A class of objects to be selected by ids should be defined");
        checkArgument(ids != null, "Ids of objects to be selected should not be passed as a null-value");
        checkArgument(ids.length > 0, "At least one id to be found should be defined");
        this.ofType = ofType;
        this.ids = ids;
    }

    /**
     * This methods defines the criteria to get the final result.
     *
     * @param condition is a predicate to filter the result.
     * @return self-reference
     */
    public Q withCondition(Predicate<T> condition) {
        checkArgument(condition != null, "Condition should be defined");
        this.condition = condition;
        return (Q) this;
    }

    @Override
    public Function<DataBaseSteps, S> get() {
        super.from(dataBaseSteps -> dataBaseSteps);
        return super.get();
    }
}