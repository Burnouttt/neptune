package ru.tinkoff.qa.neptune.selenium.test.hooks;

import ru.tinkoff.qa.neptune.selenium.content.management.Navigate;
import ru.tinkoff.qa.neptune.selenium.content.management.SwitchToFrame;
import ru.tinkoff.qa.neptune.selenium.content.management.SwitchToWindow;
import ru.tinkoff.qa.neptune.selenium.content.management.UseDefaultBrowserContent;

import static ru.tinkoff.qa.neptune.selenium.content.management.BrowserContentUsage.FOR_EVERY_METHOD;

@SwitchToWindow(index = 1)
@UseDefaultBrowserContent(howOften = FOR_EVERY_METHOD)
public class ClassWithSwitchingToWindowTest2 {

    @SwitchToFrame(index = 1)
    public void test1() {
    }

    @Navigate(to = "https://github.com")
    public void test2() {
    }

    @SwitchToWindow(index = 2, title = "^.*\\b(Github)\\b.*$", url = "^.*\\b(github)\\b.*$")
    @SwitchToFrame(index = 2)
    public void test3() {

    }

    public void test4() {
    }
}
