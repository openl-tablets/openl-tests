package domain.ui.webstudio.components.common;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

public class ThemeMenuComponent extends BaseComponent {

    private static final int MENU_CLOSED_MS = 5000;

    private WebElement themeSwitchBtn;
    private WebElement excelFormattingSwitch;

    public ThemeMenuComponent() {
        super(DriverPool.getPage());
        initializeComponents();
    }

    private void initializeComponents() {
        themeSwitchBtn = new WebElement(page, "xpath=//button[@data-testid='theme-switch']", "Theme Switch Button");
        excelFormattingSwitch = new WebElement(page, "xpath=//button[@data-testid='theme-excel-formatting']", "Excel Formatting Switch");
    }

    public void setExcelFormatting(boolean enable) {
        themeSwitchBtn.click();
        if (enable != excelFormattingSwitch.isChecked()) {
            excelFormattingSwitch.click();
        }
        themeSwitchBtn.click();
        excelFormattingSwitch.waitForHidden(MENU_CLOSED_MS);
    }
}
