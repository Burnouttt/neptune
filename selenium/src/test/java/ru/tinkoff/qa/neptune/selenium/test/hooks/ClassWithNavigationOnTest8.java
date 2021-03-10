package ru.tinkoff.qa.neptune.selenium.test.hooks;

import ru.tinkoff.qa.neptune.selenium.content.management.Navigate;
import ru.tinkoff.qa.neptune.selenium.content.management.SwitchToFrame;
import ru.tinkoff.qa.neptune.selenium.content.management.UseDefaultBrowserContent;

@Navigate(to = "https://www.google.com")
@UseDefaultBrowserContent(addNavigationParams = true)
public class ClassWithNavigationOnTest8 {

    @SwitchToFrame(index = 1)
    public void test1() {
    }

    public void test2() {
    }
}
