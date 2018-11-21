package ru.tinkoff.qa.neptune.http.api.test;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ru.tinkoff.qa.neptune.http.api.DesiredResponseHasNotBeenReceivedException;
import ru.tinkoff.qa.neptune.http.api.HttpSteps;
import ru.tinkoff.qa.neptune.http.api.ResponseHasNoDesiredDataException;

import java.util.function.Supplier;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperties;
import static java.lang.System.setProperty;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static ru.tinkoff.qa.neptune.core.api.StoryWriter.condition;
import static ru.tinkoff.qa.neptune.core.api.proxy.ProxyFactory.getProxied;
import static ru.tinkoff.qa.neptune.http.api.HttpBodyDataSequentialGetSupplier.*;
import static ru.tinkoff.qa.neptune.http.api.HttpRequestGetSupplier.GET;
import static ru.tinkoff.qa.neptune.http.api.HttpResponseSequentialGetSupplier.responseOf;
import static ru.tinkoff.qa.neptune.http.api.properties.TimeToGetDesiredResponseProperty.DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY;
import static ru.tinkoff.qa.neptune.http.api.properties.time.TimeUnitToGetDesiredResponseProperty.TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY;
import static ru.tinkoff.qa.neptune.http.api.properties.time.TimeValueToGetDesiredResponseProperty.TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY;
import static ru.tinkoff.qa.neptune.http.api.test.FunctionToGetXMLTagArray.toNodeArray;
import static ru.tinkoff.qa.neptune.http.api.test.FunctionToGetXMLTagList.toNodeList;

public class HttpBodyDataTest extends BaseHttpTest {
    private HttpSteps httpSteps = getProxied(HttpSteps.class);

    @BeforeClass
    public static void beforeClass() {
        clientAndServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/data.html"))
                .respond(response().withBody("<?xml version=\"1.0\" encoding=\"utf-8\"?><a><b></b><c></c></a>"));

        clientAndServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/badData.html"))
                .respond(response().withBody("<?xml version=\"1.0\" encoding=\"utf-8\"?<b></b><c></c></a"));
        //malformed xml
    }

    @Test
    public void getAnObjectFromBodyPositiveTest1() {
        var result = httpSteps.get(bodyDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has 1 tag <a>",
                        nodeList -> nodeList.size() == 1)));

        assertThat(result, hasSize(1));
    }

    @Test
    public void getAnObjectFromBodyPositiveTest2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyDataOf(response,
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has 1 tag <a>",
                        nodeList -> nodeList.size() == 1)));

        assertThat(result, hasSize(1));
    }

    @Test
    public void getAnObjectFromBodyNegativeTestWithNullResult1() {
        var result = httpSteps.get(bodyDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has 2 tags <a>",
                        nodeList -> nodeList.size() == 2)));

        assertThat(result, nullValue());
    }

    @Test
    public void getAnObjectFromBodyNegativeTestWithNullResult2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyDataOf(response,
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has 2 tags <a>",
                        nodeList -> nodeList.size() == 2)));

        assertThat(result, nullValue());
    }

    @Test(expectedExceptions = DesiredResponseHasNotBeenReceivedException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getAnObjectFromBodyNegativeTestWithExceptionThrowing1() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        try {
            httpSteps.get(bodyDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "List of tags <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has 2 tags <a>",
                            nodeList -> nodeList.size() == 2))
                    .toThrowIfNotReceived(supplier));
        } catch (Throwable e) {
            assertThat(e.getCause(), not(nullValue()));
            assertThat(e.getCause(), equalTo(exceptionToBeThrown));
            throw e;
        }
    }

    @Test(expectedExceptions = ResponseHasNoDesiredDataException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getAnObjectFromBodyNegativeTestWithExceptionThrowing2() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        httpSteps.get(bodyDataOf(response,
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has 2 tags <a>",
                        nodeList -> nodeList.size() == 2))
                .toThrowIfNotReceived(supplier));
    }

    @Test
    public void getAnObjectFromBodyNegativeTestWithDefaultTimeOut1() {
        var start = currentTimeMillis();
        httpSteps.get(bodyDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "List of tags <a>",
                toNodeList("a"))
                .withConditionToGetData(condition("Has 2 tags <a>",
                        nodeList -> nodeList.size() == 2)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis() + 500));
        assertThat(time, greaterThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis()));
    }

    @Test
    public void getAnObjectFromBodyNegativeTestWithDefaultTimeOut2() {
        var start = currentTimeMillis();

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
        httpSteps.get(bodyDataOf(response,
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has 2 tags <a>",
                        nodeList -> nodeList.size() == 2)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
    }

    @Test
    public void getAnObjectFromBodyNegativeTestWithTimeDefinedInProperties1() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "List of tags <a>",
                    toNodeList("a"))
                    .withConditionToGetData(condition("Has 2 tags <a>", nodeList -> nodeList.size() == 2)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(10).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(10).toMillis()));
        } finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getAnObjectFromBodyNegativeTestWithTimeDefinedInProperties2() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();

            var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
            httpSteps.get(bodyDataOf(response,
                    "List of tags <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has 2 tags <a>",
                            nodeList -> nodeList.size() == 2)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
        } finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getAnObjectFromBodyNegativeTestWithTimeDefinedExplicitly() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString())
                            .timeToReceiveDesiredResponse(ofSeconds(5)),
                    "List of tags <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has 2 tags <a>",
                            nodeList -> nodeList.size() == 2)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(5).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(5).toMillis()));
        } finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getSubIterableFromBodyPositiveTest1() {
        var result = httpSteps.get(bodyIterableDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Node has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, hasSize(1));
    }

    @Test
    public void getSubIterableFromBodyPositiveTest2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyIterableDataOf(response,
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Node has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, hasSize(1));
    }

    @Test
    public void getSubIterableFromBodyNegativeTestWithEmptyCollectionResult1() {
        var result = httpSteps.get(bodyIterableDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        assertThat(result, nullValue());
    }

    @Test
    public void getSubIterableFromBodyNegativeTestWithEmptyCollectionResult2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyIterableDataOf(response,
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        assertThat(result, emptyIterable());
    }

    @Test
    public void getSubIterableFromBodyNegativeTestWithEmptyCollectionResult3() {
        var result = httpSteps.get(bodyIterableDataOf(responseOf(GET(format("%s/badData.html", REQUEST_URI)), ofString()),
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, nullValue());
    }

    @Test(expectedExceptions = DesiredResponseHasNotBeenReceivedException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getSubIterableFromBodyNegativeTestWithExceptionThrowing1() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        try {
            httpSteps.get(bodyIterableDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "List of tags <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0))
                    .toThrowIfNotReceived(supplier));
        } catch (Throwable e) {
            assertThat(e.getCause(), not(nullValue()));
            assertThat(e.getCause(), equalTo(exceptionToBeThrown));
            throw e;
        }
    }

    @Test(expectedExceptions = ResponseHasNoDesiredDataException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getSubIterableFromBodyNegativeTestWithExceptionThrowing2() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        httpSteps.get(bodyIterableDataOf(response,
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0))
                .toThrowIfNotReceived(supplier));
    }

    @Test
    public void getSubIterableFromBodyNegativeTestWithDefaultTimeOut1() {
        var start = currentTimeMillis();
        httpSteps.get(bodyIterableDataOf(
                responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis() + 500));
        assertThat(time, greaterThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis()));
    }

    @Test
    public void getSubIterableFromBodyNegativeTestWithDefaultTimeOut2() {
        var start = currentTimeMillis();

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
        httpSteps.get(bodyIterableDataOf(response,
                "List of tags <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
    }

    @Test
    public void getSubIterableFromBodyNegativeTestWithTimeDefinedInProperties1() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyIterableDataOf(
                    responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "List of tags <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(10).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(10).toMillis()));
        }
        finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getSubIterableFromBodyNegativeTestWithTimeDefinedInProperties2() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();

            var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
            httpSteps.get(bodyIterableDataOf(response,
                    "List of tags <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
        }
        finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getSubIterableFromBodyNegativeTestWithTimeDefinedExplicitly() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyIterableDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString())
                            .timeToReceiveDesiredResponse(ofSeconds(5)),
                    "List of tags <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(5).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(5).toMillis()));
        } finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getArrayFromBodyPositiveTest1() {
        var result = httpSteps.get(bodyArrayDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "Array of tags <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Node has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, arrayWithSize(1));
    }

    @Test
    public void getArrayFromBodyPositiveTest2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyArrayDataOf(response,
                "Array of tags <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Node has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, arrayWithSize(1));
    }

    @Test
    public void getArrayFromBodyNegativeTestWithEmptyCollectionResult1() {
        var result = httpSteps.get(bodyArrayDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "Array of tags <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        assertThat(result, nullValue());
    }

    @Test
    public void getArrayFromBodyNegativeTestWithEmptyCollectionResult2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyArrayDataOf(response,
                "Array of tags <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        assertThat(result, emptyArray());
    }

    @Test
    public void getArrayFromBodyNegativeTestWithEmptyCollectionResult3() {
        var result = httpSteps.get(bodyArrayDataOf(responseOf(GET(format("%s/badData.html", REQUEST_URI)), ofString()),
                "Array of tags <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, nullValue());
    }

    @Test(expectedExceptions = DesiredResponseHasNotBeenReceivedException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getArrayFromBodyNegativeTestWithExceptionThrowing1() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        try {
            httpSteps.get(bodyArrayDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "Array of tags <a>",
                    toNodeArray("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0))
                    .toThrowIfNotReceived(supplier));

        } catch (Throwable e) {
            assertThat(e.getCause(), not(nullValue()));
            assertThat(e.getCause(), equalTo(exceptionToBeThrown));
            throw e;
        }
    }

    @Test(expectedExceptions = ResponseHasNoDesiredDataException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getArrayFromBodyNegativeTestWithExceptionThrowing2() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        httpSteps.get(bodyArrayDataOf(response,
                "Array of tags <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0))
                .toThrowIfNotReceived(supplier));
    }

    @Test
    public void getArrayFromBodyNegativeTestWithDefaultTimeOut1() {
        var start = currentTimeMillis();
        httpSteps.get(bodyArrayDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "Array of tags <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis() + 500));
        assertThat(time, greaterThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis()));
    }

    @Test
    public void getArrayFromBodyNegativeTestWithDefaultTimeOut2() {
        var start = currentTimeMillis();

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
        httpSteps.get(bodyArrayDataOf(response,
                "Array of tags <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
    }

    @Test
    public void getArrayFromBodyNegativeTestWithTimeDefinedInProperties1() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyArrayDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "Array of tags <a>",
                    toNodeArray("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(10).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(10).toMillis()));
        }
        finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getArrayFromBodyNegativeTestWithTimeDefinedInProperties2() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();

            var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
            httpSteps.get(bodyArrayDataOf(response,
                    "Array of tags <a>",
                    toNodeArray("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
        }
        finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getArrayFromBodyNegativeTestWithTimeDefinedExplicitly() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyArrayDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString())
                            .timeToReceiveDesiredResponse(ofSeconds(5)),
                    "Array of tags <a>",
                    toNodeArray("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(5).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(5).toMillis()));
        } finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getObjectFromIterableBodyPositiveTest1() {
        var result = httpSteps.get(bodyDataFromIterable(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "Tag <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Node has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, not(nullValue()));
    }

    @Test
    public void getObjectFromIterableBodyPositiveTest2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyDataFromIterable(response,
                "Tag <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Node has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, not(nullValue()));
    }

    @Test
    public void getObjectFromIterableBodyNegativeTestWithEmptyCollectionResult1() {
        var result = httpSteps.get(bodyDataFromIterable(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "Tag <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        assertThat(result, nullValue());
    }

    @Test
    public void getObjectFromIterableBodyNegativeTestWithEmptyCollectionResult2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyDataFromIterable(response,
                "Tag <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        assertThat(result, nullValue());
    }

    @Test
    public void getObjectFromIterableBodyNegativeTestWithEmptyCollectionResult3() {
        var result = httpSteps.get(bodyDataFromIterable(responseOf(GET(format("%s/badData.html", REQUEST_URI)), ofString()),
                "Tag <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, nullValue());
    }

    @Test(expectedExceptions = DesiredResponseHasNotBeenReceivedException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getObjectFromIterableBodyNegativeTestWithExceptionThrowing1() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        try {
            httpSteps.get(bodyDataFromIterable(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "Tag <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0))
                    .toThrowIfNotReceived(supplier));
        } catch (Throwable e) {
            assertThat(e.getCause(), not(nullValue()));
            assertThat(e.getCause(), equalTo(exceptionToBeThrown));
            throw e;
        }
    }

    @Test(expectedExceptions = ResponseHasNoDesiredDataException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getObjectFromIterableBodyNegativeTestWithExceptionThrowing2() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        httpSteps.get(bodyDataFromIterable(response,
                "Tag <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0))
                .toThrowIfNotReceived(supplier));
    }

    @Test
    public void getObjectFromIterableBodyNegativeTestWithDefaultTimeOut1() {
        var start = currentTimeMillis();
        httpSteps.get(bodyDataFromIterable(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "Tag <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis() + 500));
        assertThat(time, greaterThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis()));
    }

    @Test
    public void getObjectFromIterableBodyNegativeTestWithDefaultTimeOut2() {
        var start = currentTimeMillis();

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
        httpSteps.get(bodyDataFromIterable(response,
                "Tag <a>",
                toNodeList("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
    }

    @Test
    public void getObjectFromIterableBodyNegativeTestWithTimeDefinedInProperties1() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyDataFromIterable(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "Tag <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(10).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(10).toMillis()));
        }
        finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getObjectFromIterableBodyNegativeTestWithTimeDefinedInProperties2() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();

            var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
            httpSteps.get(bodyDataFromIterable(response,
                    "Tag <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
        }
        finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getObjectFromIterableBodyNegativeTestWithTimeDefinedExplicitly() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyDataFromIterable(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString())
                            .timeToReceiveDesiredResponse(ofSeconds(5)),
                    "Tag <a>",
                    toNodeList("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(5).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(5).toMillis()));
        } finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getObjectFromArrayBodyPositiveTest1() {
        var result = httpSteps.get(bodyDataFromArray(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "Tag <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Node has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, not(nullValue()));
    }

    @Test
    public void getObjectFromArrayBodyPositiveTest2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyDataFromArray(response,
                "Tag <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Node has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, not(nullValue()));
    }

    @Test
    public void getObjectFromArrayBodyNegativeTestWithEmptyCollectionResult1() {
        var result = httpSteps.get(bodyArrayDataOf(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "Array of tags <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        assertThat(result, nullValue());
    }

    @Test
    public void getObjectFromArrayBodyNegativeTestWithEmptyCollectionResult2() {
        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        var result = httpSteps.get(bodyDataFromArray(response,
                "Tag <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        assertThat(result, nullValue());
    }

    @Test
    public void getObjectFromArrayBodyNegativeTestWithEmptyCollectionResult3() {
        var result = httpSteps.get(bodyDataFromArray(responseOf(GET(format("%s/badData.html", REQUEST_URI)), ofString()),
                "Tag <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has children",
                        node -> node.getChildNodes().getLength() > 0)));

        assertThat(result, nullValue());
    }

    @Test(expectedExceptions = DesiredResponseHasNotBeenReceivedException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getObjectFromArrayBodyNegativeTestWithExceptionThrowing1() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        try {
            httpSteps.get(bodyDataFromArray(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "Tag <a>",
                    toNodeArray("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0))
                    .toThrowIfNotReceived(supplier));
        } catch (Throwable e) {
            assertThat(e.getCause(), not(nullValue()));
            assertThat(e.getCause(), equalTo(exceptionToBeThrown));
            throw e;
        }
    }

    @Test(expectedExceptions = ResponseHasNoDesiredDataException.class, expectedExceptionsMessageRegExp = "Test exception")
    public void getObjectFromArrayBodyNegativeTestWithExceptionThrowing2() {
        var exceptionToBeThrown = new ResponseHasNoDesiredDataException("Test exception");
        Supplier<ResponseHasNoDesiredDataException> supplier = () -> exceptionToBeThrown;

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));

        httpSteps.get(bodyDataFromArray(response,
                "Tag <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0))
                .toThrowIfNotReceived(supplier));
    }

    @Test
    public void getObjectFromArrayBodyNegativeTestWithDefaultTimeOut1() {
        var start = currentTimeMillis();
        httpSteps.get(bodyDataFromArray(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                "Tag <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis() + 500));
        assertThat(time, greaterThanOrEqualTo(DEFAULT_TIME_TO_GET_DESIRED_RESPONSE_PROPERTY.get().toMillis()));
    }

    @Test
    public void getObjectFromArrayBodyNegativeTestWithDefaultTimeOut2() {
        var start = currentTimeMillis();

        var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
        httpSteps.get(bodyDataFromArray(response,
                "Tag <a>",
                toNodeArray("a"))

                .withConditionToGetData(condition("Has no children",
                        node -> node.getChildNodes().getLength() == 0)));

        var stop = currentTimeMillis();
        var time = stop - start;
        assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
    }

    @Test
    public void getObjectFromArrayBodyNegativeTestWithTimeDefinedInProperties1() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyDataFromArray(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()),
                    "Tag <a>",
                    toNodeArray("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(10).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(10).toMillis()));
        }
        finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getObjectFromArrayBodyNegativeTestWithTimeDefinedInProperties2() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();

            var response = httpSteps.get(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString()));
            httpSteps.get(bodyDataFromArray(response,
                    "Tag <a>",
                    toNodeArray("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(1).toMillis() + 500));
        }
        finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }

    @Test
    public void getObjectFromArrayBodyNegativeTestWithTimeDefinedExplicitly() {
        setProperty(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), SECONDS.name());
        setProperty(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName(), "10");

        try {
            var start = currentTimeMillis();
            httpSteps.get(bodyDataFromArray(responseOf(GET(format("%s/data.html", REQUEST_URI)), ofString())
                            .timeToReceiveDesiredResponse(ofSeconds(5)),
                    "Tag <a>",
                    toNodeArray("a"))

                    .withConditionToGetData(condition("Has no children",
                            node -> node.getChildNodes().getLength() == 0)));

            var stop = currentTimeMillis();
            var time = stop - start;
            assertThat(time, lessThanOrEqualTo(ofSeconds(5).toMillis() + 500));
            assertThat(time, greaterThanOrEqualTo(ofSeconds(5).toMillis()));
        } finally {
            getProperties().remove(TIME_UNIT_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
            getProperties().remove(TIME_VALUE_TO_GET_DESIRED_RESPONSE_PROPERTY.getPropertyName());
        }
    }
}
