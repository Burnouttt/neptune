package ru.tinkoff.qa.neptune.http.api.test;

import org.hamcrest.Matcher;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import ru.tinkoff.qa.neptune.http.api.test.request.body.JsonBodyObject;
import ru.tinkoff.qa.neptune.http.api.test.request.body.XmlBodyObject;

import java.net.http.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static ru.tinkoff.qa.neptune.http.api.HttpStepContext.http;
import static ru.tinkoff.qa.neptune.http.api.hamcrest.response.HasBody.hasBody;
import static ru.tinkoff.qa.neptune.http.api.request.RequestBuilder.GET;
import static ru.tinkoff.qa.neptune.http.api.response.body.data.MappedBodyHandler.*;

public class CustomResponseBodyTest extends BaseHttpTest {

    private static final JsonBodyObject JSON_BODY_OBJECT = new JsonBodyObject().setA("Some String 2")
            .setB(777)
            .setC(false);

    private static final XmlBodyObject XML_BODY_OBJECT = new XmlBodyObject().setA("Some String 2")
            .setB(777)
            .setC(false);

    private static final String RESPONSE_GSON = "{\"A\":\"Some String 2\",\"B\":777,\"C\":false}";
    private static final String RESPONSE_MAPPED = "<XmlBodyObject><wstxns1:A1 xmlns:wstxns1=\"http://www.test.com\">Some String 2</wstxns1:A1>" +
            "<wstxns2:B1 xmlns:wstxns2=\"http://www.test.com\">777</wstxns2:B1>" +
            "<wstxns3:C1 xmlns:wstxns3=\"http://www.test.com\">false</wstxns3:C1></XmlBodyObject>";

    private static final String XML_FOR_DOCUMENT = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?><a><b/><c/></a>";
    private static final String HTML_FOR_DOCUMENT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html>\n" +
            "    <head>\n" +
            "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\n" +
            "        <title>Login Page</title>\n" +
            "    </head>\n" +
            "    <body>\n" +
            "        <div id=\"login\" class=\"simple\" >\n" +
            "            <form action=\"login.do\">\n" +
            "                Username : <input id=\"username\" type=\"text\" /><br>\n" +
            "                Password : <input id=\"password\" type=\"password\" /><br>\n" +
            "                <input id=\"submit\" type=\"submit\" />\n" +
            "                <input id=\"reset\" type=\"reset\" />\n" +
            "            </form>\n" +
            "        </div>\n" +
            "    </body>\n" +
            "</html>";

    private static final String PATH_TO_GSON = "/gson";
    private static final String PATH_TO_JACKSON = "/jackson_xml";
    private static final String PATH_DOCUMENT_XML = "/document_xml";
    private static final String PATH_DOCUMENT_HTML = "/document_html";

    @BeforeClass
    public static void prepareMock() {
        clientAndServer.when(
                request()
                        .withMethod("GET")
                        .withPath(PATH_TO_GSON))
                .respond(response().withBody(RESPONSE_GSON));

        clientAndServer.when(
                request()
                        .withMethod("GET")
                        .withPath(PATH_TO_JACKSON))
                .respond(response().withBody(RESPONSE_MAPPED));

        clientAndServer.when(
                request()
                        .withMethod("GET")
                        .withPath(PATH_DOCUMENT_XML))
                .respond(response().withBody(XML_FOR_DOCUMENT));

        clientAndServer.when(
                request()
                        .withMethod("GET")
                        .withPath(PATH_DOCUMENT_HTML))
                .respond(response().withBody(HTML_FOR_DOCUMENT));
    }

    @DataProvider
    public static Object[][] data() {
        return new Object[][]{
                {PATH_TO_GSON,
                        jsonDTO(JsonBodyObject.class),
                        equalTo(JSON_BODY_OBJECT)},

                {PATH_TO_JACKSON,
                        xmlDTO(XmlBodyObject.class),
                        equalTo(XML_BODY_OBJECT)},

                {PATH_DOCUMENT_XML,
                        w3cDocument(),
                        instanceOf(Document.class)},

                {PATH_DOCUMENT_HTML,
                        jsoupDocument(),
                        instanceOf(org.jsoup.nodes.Document.class)},
        };
    }

    @DataProvider
    public static Object[][] data2() {
        return new Object[][]{
                {PATH_TO_GSON, xmlDTO(XmlBodyObject.class)},
                {PATH_TO_GSON, w3cDocument()},
                {PATH_DOCUMENT_XML, jsonDTO(JsonBodyObject.class)},
        };
    }

    @Test(dataProvider = "data")
    public <T> void customResponseBodyTest2(String urlPath,
                                            HttpResponse.BodyHandler<T> handler,
                                            Matcher<? super T> matcher) {
        assertThat(http().responseOf(GET(REQUEST_URI + urlPath), handler),
                hasBody(matcher));
    }

    @Test(dataProvider = "data2")
    public <T> void negativeTest(String urlPath,
                                 HttpResponse.BodyHandler<T> handler) {
        assertThat(http().responseOf(GET(REQUEST_URI + urlPath), handler),
                hasBody(nullValue()));
    }
}
