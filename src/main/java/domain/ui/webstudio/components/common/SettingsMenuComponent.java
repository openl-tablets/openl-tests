package domain.ui.webstudio.components.common;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;
import lombok.Getter;

public class SettingsMenuComponent extends BaseComponent {

    private static final int ALL_TESTS_ON_A_PAGE = -1;
    private static final int MENU_TIMEOUT_MS = 5000;
    private static final int OPTION_PROBE_MS = 1000;
    private static final int POLL_MS = 100;
    private static final String TESTS_PER_PAGE_LABEL = "Tests Per Page";
    private static final String ALL_TESTS_LABEL = "All";
    private static final String MENU = "//div[contains(concat(' ',normalize-space(@class),' '),' ant-dropdown ')]"
            + "[not(contains(concat(' ',normalize-space(@class),' '),' ant-dropdown-hidden '))]"
            + "[.//button[starts-with(@data-testid,'settings-')]]";

    private WebElement settingsButton;
    private WebElement menu;
    private WebElement switchTemplate;
    private WebElement testsPerPageTitle;
    private WebElement testsPerPageOptionTemplate;

    public SettingsMenuComponent() {
        super(DriverPool.getPage());
        initializeComponents();
    }

    private void initializeComponents() {
        settingsButton = new WebElement(page, "xpath=//button[@data-testid='settings-menu']", "Settings Menu Button");
        menu = new WebElement(page, "xpath=" + MENU, "Settings Menu");
        switchTemplate = new WebElement(page, "xpath=//button[@data-testid='settings-%s']", "Settings Switch");
        testsPerPageTitle = new WebElement(page, "xpath=" + MENU
                + "//div[@aria-haspopup='true'][contains(normalize-space(.),'" + TESTS_PER_PAGE_LABEL + "')]",
                "Tests Per Page Submenu");
        testsPerPageOptionTemplate = new WebElement(page, "xpath=//ul[contains(concat(' ',normalize-space(@class),' '),' ant-dropdown-menu-sub ')]"
                + "//li[@role='menuitem'][normalize-space(.)='%s']", "Tests Per Page Option");
    }

    private void open() {
        if (!isOpen()) {
            settingsButton.click();
            menu.waitForVisible(MENU_TIMEOUT_MS);
        }
    }

    private void close() {
        if (isOpen()) {
            settingsButton.click();
            menu.waitForHidden(MENU_TIMEOUT_MS);
        }
    }

    private boolean isOpen() {
        return menu.getLocator().isVisible();
    }

    public boolean isOn(Setting setting) {
        open();
        boolean on = isChecked(setting);
        close();
        return on;
    }

    public SettingsMenuComponent set(Setting setting, boolean on) {
        open();
        if (isChecked(setting) != on) {
            switchOf(setting).click();
            WaitUtil.requireCondition(() -> isChecked(setting) == on, MENU_TIMEOUT_MS, POLL_MS,
                    "Waiting for the setting " + setting.getKey() + " to be " + (on ? "on" : "off"));
        }
        close();
        return this;
    }

    public int getTestsPerPage() {
        open();
        int size = readTestsPerPage();
        close();
        return size;
    }

    public SettingsMenuComponent setTestsPerPage(int size) {
        open();
        if (readTestsPerPage() != size) {
            WebElement option = testsPerPageOptionTemplate.format(sizeLabel(size));
            testsPerPageTitle.hover();
            if (!option.isVisible(OPTION_PROBE_MS)) {
                testsPerPageTitle.click();
            }
            option.click();
            WaitUtil.requireCondition(() -> readTestsPerPage() == size, MENU_TIMEOUT_MS, POLL_MS,
                    "Waiting for Tests Per Page to be " + sizeLabel(size));
        }
        close();
        return this;
    }

    private boolean isChecked(Setting setting) {
        return switchOf(setting).isChecked();
    }

    private WebElement switchOf(Setting setting) {
        return switchTemplate.format(setting.getKey());
    }

    private int readTestsPerPage() {
        String value = testsPerPageTitle.getText().replace(TESTS_PER_PAGE_LABEL, "").trim();
        return ALL_TESTS_LABEL.equals(value) ? ALL_TESTS_ON_A_PAGE : Integer.parseInt(value);
    }

    private static String sizeLabel(int size) {
        return size == ALL_TESTS_ON_A_PAGE ? ALL_TESTS_LABEL : String.valueOf(size);
    }

    @Getter
    public enum Setting {
        SHOW_HEADER("showHeader"),
        SHOW_FORMULAS("showFormulas"),
        FAILURES_ONLY("testsFailuresOnly"),
        COMPOUND_RESULT("showComplexResult"),
        SHOW_NUMBERS_WITHOUT_FORMATTING("showRealNumbers");

        private final String key;

        Setting(String key) {
            this.key = key;
        }
    }
}
