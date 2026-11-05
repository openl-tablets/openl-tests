package domain.ui.webstudio.components.repositorytabcomponents;

import com.microsoft.playwright.Locator;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

import java.util.ArrayList;
import java.util.List;

public class SaveProjectDialogComponent extends BaseComponent {

    private static final String MODAL_ROOT =
            "//div[contains(@class,'ant-modal')][.//div[contains(@class,'ant-modal-title') and contains(normalize-space(),'Save project')]]";

    private static final String CHANGES = MODAL_ROOT + "//div[@data-testid='save-project-changes']";

    private WebElement commentField;
    private WebElement submitBtn;
    private WebElement cancelBtn;
    private WebElement failureDescription;

    public SaveProjectDialogComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    private void initializeElements() {
        commentField = new WebElement(DriverPool.getPage(),
                "xpath=//textarea[@data-testid='save-project-comment'] | //input[@data-testid='save-project-comment']", "saveProjectComment");
        submitBtn = new WebElement(DriverPool.getPage(),
                "xpath=//button[@data-testid='save-project-submit']", "saveProjectSubmit");
        cancelBtn = new WebElement(DriverPool.getPage(),
                "xpath=" + MODAL_ROOT + "//button[normalize-space(.)='Cancel']", "saveProjectCancel");
        failureDescription = new WebElement(DriverPool.getPage(),
                "xpath=//div[contains(@class,'ant-notification-notice')][.//div[contains(@class,'ant-notification-notice-title')]"
                        + "[normalize-space()='Failed to save project']]//div[contains(@class,'ant-notification-notice-description')]",
                "saveProjectFailureDescription");
    }

    public SaveProjectDialogComponent waitForVisible() {
        commentField.waitForVisible();
        return this;
    }

    public List<String> getChangedFiles() {
        WebElement header = new WebElement(DriverPool.getPage(),
                "xpath=" + CHANGES + "//div[contains(@class,'ant-collapse-header')]", "saveProjectChangesHeader");
        header.waitForVisible();
        if (!"true".equals(header.getAttribute("aria-expanded"))) {
            header.click();
        }
        Locator rows = DriverPool.getPage().locator("xpath=" + CHANGES + "//*[@data-testid='save-project-changes-file']");
        rows.first().waitFor();
        List<String> files = new ArrayList<>();
        for (int i = 0; i < rows.count(); i++) {
            Locator row = rows.nth(i);
            files.add(row.locator("xpath=.//span[@aria-label]").first().getAttribute("aria-label") + " "
                    + row.locator("strong").first().innerText().trim());
        }
        return files;
    }

    public SaveProjectDialogComponent setComment(String comment) {
        commentField.fill(comment);
        return this;
    }

    public void submit() {
        submitBtn.click();
        submitBtn.waitForHidden(10000);
    }

    public void submitThroughShade() {
        submitBtn.clickForce();
        submitBtn.waitForHidden(10000);
    }


    public void clickSubmit() {
        submitBtn.click();
    }

    public void waitForSubmitHidden() {
        submitBtn.waitForHidden(10000);
    }

    public String waitForFailureMessage(int timeoutMs) {
        return failureDescription.waitForVisible(timeoutMs).getText().trim();
    }

    public void cancel() {
        cancelBtn.click();
    }
}
