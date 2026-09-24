package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

public class ModuleProjectClosedComponent extends BaseComponent {

    private final WebElement notice;
    private final WebElement message;
    private final WebElement openBtn;

    public ModuleProjectClosedComponent() {
        super(DriverPool.getPage());
        notice = new WebElement(page, "xpath=//*[@data-testid='module-project-closed']", "moduleProjectClosed");
        message = new WebElement(notice, "xpath=.//div[contains(@class,'ant-empty-description')]", "moduleProjectClosedMessage");
        openBtn = new WebElement(notice, "xpath=.//button[normalize-space()='Open']", "openProjectBtn");
    }

    public String waitForMessage(long timeoutMs) {
        return message.waitForVisible(timeoutMs).getText();
    }

    public void openProject() {
        openBtn.click();
    }
}
