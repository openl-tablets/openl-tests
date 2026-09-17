package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

public class EditProjectDialogComponent extends BaseComponent {

    private WebElement projectNameField;
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
        // What the project says about itself is written on the project's own screen now: the Overview is
        // opened for writing, the description is a field of it, and Save keeps the whole card at once.
        projectNameField = new WebElement(page, "xpath=//input[@data-testid='edit-project-name']", "projectNameField");
        descriptionField = new WebElement(page, "xpath=//textarea[@data-testid='edit-description']", "descriptionField");
        updateBtn = new WebElement(page, "xpath=//button[@data-testid='overview-save']", "updateBtn");
        cancelBtn = new WebElement(page, "xpath=//button[@data-testid='overview-cancel']", "cancelBtn");
    }

    public boolean isProjectNameFieldVisible() {
        return projectNameField.isVisible(1000);
    }

    public EditProjectDialogComponent setProjectName(String name) {
        projectNameField.clear();
        projectNameField.fillSequentially(name);
        return this;
    }

    public String getProjectName() {
        return projectNameField.getCurrentInputValue();
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
