package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

public class EditProjectDialogComponent extends BaseComponent {

    private WebElement descriptionField;
    private WebElement updateBtn;
    private WebElement cancelBtn;

    public EditProjectDialogComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public EditProjectDialogComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        descriptionField = new WebElement(page, "xpath=//textarea[@data-testid='edit-description']", "descriptionField");
        updateBtn = new WebElement(page, "xpath=//button[@data-testid='overview-save']", "updateBtn");
        cancelBtn = new WebElement(page, "xpath=//button[@data-testid='overview-cancel']", "cancelBtn");
    }

    public boolean isProjectNameFieldVisible() {
        return false;
    }

    public boolean isUpdateButtonEnabled() {
        return updateBtn.isEnabled();
    }

    public EditProjectDialogComponent setDescription(String description) {
        descriptionField.waitForVisible(DEFAULT_TIMEOUT_MS);
        descriptionField.fill(description);
        return this;
    }

    public String getDescription() {
        return descriptionField.getAttribute("value");
    }

    public void clickUpdateButton() {
        updateBtn.click();
        WaitUtil.requireCondition(() -> !updateBtn.isVisible(1000), DEFAULT_TIMEOUT_MS, 200,
                "Waiting for what the project says about itself to be kept");
        waitUntilSpinnerLoaded();
    }

    public void clickCancelButton() {
        cancelBtn.click();
    }

    public boolean isDialogVisible() {
        return descriptionField.isVisible(1000);
    }

    public void waitForDialogToAppear() {
        WaitUtil.waitForCondition(this::isDialogVisible, DEFAULT_TIMEOUT_MS, 100,
                "Waiting for the project settings to be open for writing");
    }

    public void waitForDialogToClose() {
        WaitUtil.waitForCondition(() -> !isDialogVisible(), 5000, 100, "Waiting for Edit Project dialog to close");
    }
}
