package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

public class ModuleTableMissingComponent extends BaseComponent {

    private final WebElement message;

    public ModuleTableMissingComponent() {
        super(DriverPool.getPage());
        WebElement notice = new WebElement(page, "xpath=//*[@data-testid='module-table-missing']", "moduleTableMissing");
        message = new WebElement(notice, "xpath=.//div[contains(@class,'ant-empty-description')]", "moduleTableMissingMessage");
    }

    public String waitForMessage(long timeoutMs) {
        return message.waitForVisible(timeoutMs).getText();
    }
}
