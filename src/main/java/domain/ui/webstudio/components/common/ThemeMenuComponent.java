package domain.ui.webstudio.components.common;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

public class ThemeMenuComponent extends BaseComponent {

    private static final int MENU_CLOSED_MS = 5000;

    private WebElement themeSwitchBtn;
    private WebElement appearanceSwitch;
    private WebElement excelFormattingSwitch;
    private WebElement pageBody;

    public ThemeMenuComponent() {
        super(DriverPool.getPage());
        initializeComponents();
    }

    private void initializeComponents() {
        themeSwitchBtn = new WebElement(page, "xpath=//button[@data-testid='theme-switch']", "Theme Switch Button");
        appearanceSwitch = new WebElement(page, "xpath=//button[@data-testid='theme-appearance']", "Appearance Switch");
        pageBody = new WebElement(page, "xpath=//body", "Page Body");
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

    public void setDarkAppearance(boolean enable) {
        themeSwitchBtn.click();
        if (enable != appearanceSwitch.isChecked()) {
            appearanceSwitch.click();
        }
        themeSwitchBtn.click();
        appearanceSwitch.waitForHidden(MENU_CLOSED_MS);
    }

    public String getPageBackgroundColor() {
        return pageBody.getCssValue("background-color");
    }
}
