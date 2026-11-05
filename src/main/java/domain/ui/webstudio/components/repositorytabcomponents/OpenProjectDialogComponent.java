package domain.ui.webstudio.components.repositorytabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

public class OpenProjectDialogComponent extends BaseComponent {

    private static final String MODAL_ROOT =
            "//div[contains(@class,'ant-modal')][.//*[@data-testid='open-project-dependencies']]";

    private WebElement openDependenciesCheckbox;
    private WebElement branchWarning;
    private WebElement cancelBtn;

    public OpenProjectDialogComponent() {
        super(DriverPool.getPage());
        openDependenciesCheckbox = new WebElement(page, "xpath=//*[@data-testid='open-project-dependencies']", "openDependenciesCheckbox");
        branchWarning = new WebElement(page, "xpath=//*[@data-testid='open-project-branch-warning']", "openProjectBranchWarning");
        cancelBtn = new WebElement(page, "xpath=" + MODAL_ROOT + "//div[contains(@class,'ant-modal-footer')]//button[not(contains(@class,'ant-btn-primary'))]", "openProjectCancelBtn");
    }

    public OpenProjectDialogComponent waitForVisible() {
        openDependenciesCheckbox.waitForVisible(DEFAULT_TIMEOUT_MS);
        return this;
    }

    public String getBranchWarning() {
        return branchWarning.waitForVisible(DEFAULT_TIMEOUT_MS).getInnerText().trim().replaceAll("\\s+", " ");
    }

    public void cancel() {
        cancelBtn.click();
        openDependenciesCheckbox.waitForHidden(DEFAULT_TIMEOUT_MS);
    }
}
