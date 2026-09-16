package domain.ui.webstudio.components.editortabcomponents;

import domain.ui.webstudio.components.BaseComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import helpers.utils.WaitUtil;
import lombok.Getter;

import java.util.List;

/**
 * The details panel beside the table. A property is found by the name the panel shows for it: the test ids
 * the panel carries are spelled with the property's technical name instead.
 */
public class RightTableDetailsComponent extends BaseComponent {


    private static final String PANEL = "xpath=//aside[@data-testid='table-details']";
    private static final String ROW_BY_LABEL = PANEL + "//tr[contains(@class,'ant-descriptions-row')][.//th[normalize-space()='%s']]";
    private static final int PROBE_MS = 1000;
    private static final int SETTLE_MS = 200;

    private WebElement panel;
    private WebElement tableNameTemplate;
    private WebElement editBtn;
    private WebElement saveBtn;
    private WebElement cancelBtn;
    private WebElement addPropertySelect;
    private WebElement propertyRowTemplate;
    private WebElement propertyValueTemplate;
    private WebElement propertyInputTemplate;
    private WebElement propertyCheckboxTemplate;
    private WebElement propertySelectTemplate;
    private WebElement propertyDeleteTemplate;
    private WebElement inheritedSourceTemplate;
    private List<WebElement> propertyLabels;

    public RightTableDetailsComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public RightTableDetailsComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        panel = new WebElement(page, PANEL, "tableDetailsPanel");
        tableNameTemplate = new WebElement(page, PANEL + "//span[@title='%s']", "tableDetailsName");
        editBtn = new WebElement(page, PANEL + "//button[@data-testid='table-details-edit']", "editPropertiesBtn");
        saveBtn = new WebElement(page, PANEL + "//button[@data-testid='table-details-save']", "savePropertiesBtn");
        cancelBtn = new WebElement(page, PANEL + "//button[@data-testid='table-details-cancel']", "cancelPropertiesBtn");
        addPropertySelect = new WebElement(page, PANEL + "//div[@data-testid='table-details-add']//input", "addPropertySelect");
        propertyRowTemplate = new WebElement(page, ROW_BY_LABEL, "propertyRow");
        propertyValueTemplate = new WebElement(page, ROW_BY_LABEL + "//td[contains(@class,'ant-descriptions-item-content')]", "propertyValue");
        propertyInputTemplate = new WebElement(page, ROW_BY_LABEL + "//input[not(@type='checkbox')]", "propertyInput");
        propertyCheckboxTemplate = new WebElement(page, ROW_BY_LABEL + "//input[@type='checkbox']", "propertyCheckbox");
        propertySelectTemplate = new WebElement(page, ROW_BY_LABEL + "//div[contains(@class,'ant-select')]//input", "propertySelect");
        propertyDeleteTemplate = new WebElement(page, ROW_BY_LABEL + "//button[starts-with(@data-testid,'table-details-remove-')]", "propertyDeleteBtn");
        inheritedSourceTemplate = new WebElement(page, ROW_BY_LABEL + "//button[starts-with(@data-testid,'table-details-source-')]", "inheritedSourceBtn");
        propertyLabels = createElementList(PANEL + "//tr[contains(@class,'ant-descriptions-row')]/th", "propertyLabels");
    }

    public void clickSaveBtn() {
        saveBtn.click();
        WaitUtil.requireCondition(() -> !saveBtn.isVisible(PROBE_MS / 2), DEFAULT_TIMEOUT_MS, SETTLE_MS,
                "Waiting for the table properties to be saved");
        waitUntilSpinnerLoaded();
    }

    public RightTableDetailsComponent startEditing() {
        if (!saveBtn.isVisible(PROBE_MS)) {
            editBtn.click();
            saveBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        }
        return this;
    }

    public RightTableDetailsComponent addProperty(String propertyName) {
        startEditing();
        pickInSelect(addPropertySelect, propertyName);
        propertyRowTemplate.format(propertyName).waitForVisible(DEFAULT_TIMEOUT_MS);
        return this;
    }


    public RightTableDetailsComponent setProperty(String propertyName, String propertyValue) {
        startEditing();
        WebElement input = propertyInputTemplate.format(propertyName);
        input.clear();
        input.fillSequentially(propertyValue);
        return this;
    }

    public boolean isPropertySet(String propertyName, String propertyValue) {
        try {
            return getPropertyValue(propertyName).contains(propertyValue);
        } catch (RuntimeException propertyIsNotOnThePanel) {
            return false;
        }
    }

    public String getPropertyValue(String propertyName) {
        return propertyValueTemplate.format(propertyName).getText().trim();
    }

    public RightTableDetailsComponent waitForTableLoaded(String tableName) {
        tableNameTemplate.format(tableName).waitForVisible(DEFAULT_TIMEOUT_MS);
        return this;
    }

    public WebElement getPropertyRow(String propertyName) {
        return propertyRowTemplate.format(propertyName);
    }

    /**
     * Whether the value shown comes from somewhere above the table. The panel says so only while it is read,
     * so a panel left open for writing is closed first.
     */
    public boolean isPropertyInherited(String propertyName) {
        cancelEditing();
        return inheritedSourceTemplate.format(propertyName).isVisible(PROBE_MS);
    }

    public String getInheritedPropertyTitle(String propertyName) {
        cancelEditing();
        return inheritedSourceTemplate.format(propertyName).getAttribute("aria-label");
    }

    public WebElement getGoToPropertiesTableArrow(String propertyName) {
        return inheritedSourceTemplate.format(propertyName);
    }

    public void clickGoToPropertiesTableArrow(String propertyName) {
        getGoToPropertiesTableArrow(propertyName).click();
        waitUntilSpinnerLoaded();
    }

    public int getPropertiesRowCount() {
        WaitUtil.requireCondition(() -> !propertyLabels.isEmpty(), DEFAULT_TIMEOUT_MS, 100,
                "Waiting for the table properties to be listed");
        return propertyLabels.size();
    }

    public String getPropertyNameInRow(int rowIndex) {
        if (rowIndex < 1) {
            throw new IllegalArgumentException("Row index must be >= 1, got: " + rowIndex);
        }
        return propertyLabels.get(rowIndex - 1).getText().trim();
    }

    public void clickPropertyValue(String propertyName) {
        propertyValueTemplate.format(propertyName).click();
    }

    public void editTextProperty(String propertyName, String newValue) {
        setProperty(propertyName, newValue);
    }

    public void editBooleanProperty(String propertyName, boolean value) {
        startEditing();
        WebElement checkbox = propertyCheckboxTemplate.format(propertyName);
        if (value) {
            checkbox.check();
        } else {
            checkbox.uncheck();
        }
    }

    public void editDropdownProperty(String propertyName, String value) {
        startEditing();
        pickInSelect(propertySelectTemplate.format(propertyName), value);
    }

    /** Picks several values of a property that holds more than one. */
    public void editCheckboxProperty(String propertyName, String... values) {
        startEditing();
        WebElement select = propertySelectTemplate.format(propertyName);
        for (String value : values) {
            pickInSelect(select, value);
        }
        page.keyboard().press("Escape");
    }

    public void editDateProperty(String propertyName, String dateValue) {
        startEditing();
        WebElement input = propertyInputTemplate.format(propertyName);
        input.click();
        input.clear();
        input.fillSequentially(dateValue);
        input.press("Enter");
    }

    public void deleteProperty(String propertyName) {
        startEditing();
        propertyDeleteTemplate.format(propertyName).click();
    }

    public boolean isPanelVisible() {
        return panel.isVisible(PROBE_MS);
    }

    public void cancelEditing() {
        if (cancelBtn.isVisible(PROBE_MS)) {
            cancelBtn.click();
        }
    }

    @Getter
    public enum DropdownOptions {
        DESCRIPTION("Description"),
        CATEGORY("Category"),
        TAGS("Tags");

        private String value;

        DropdownOptions(String value) {
            this.value = value;
        }
    }
}
