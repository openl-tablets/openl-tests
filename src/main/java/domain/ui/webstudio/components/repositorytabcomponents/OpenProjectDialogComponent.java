package domain.ui.webstudio.components.repositorytabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

public class OpenProjectDialogComponent extends BaseComponent {

    private static final String MODAL_ROOT =
            "//div[contains(@class,'ant-modal')][.//*[@data-testid='open-project-dependencies']]";

    private WebElement openDependenciesCheckbox;
    private WebElement switchDependenciesCheckbox;
    private WebElement branchWarning;
    private WebElement cancelBtn;
    private WebElement openBtn;

    public OpenProjectDialogComponent() {
        super(DriverPool.getPage());
        openDependenciesCheckbox = new WebElement(page, "xpath=//*[@data-testid='open-project-dependencies']", "openDependenciesCheckbox");
        switchDependenciesCheckbox = new WebElement(page, "xpath=//*[@data-testid='open-project-switch-dependencies']", "openProjectSwitchDependencies");
        branchWarning = new WebElement(page, "xpath=//*[@data-testid='open-project-branch-warning']", "openProjectBranchWarning");
        cancelBtn = new WebElement(page, "xpath=" + MODAL_ROOT + "//div[contains(@class,'ant-modal-footer')]//button[not(contains(@class,'ant-btn-primary'))]", "openProjectCancelBtn");
        openBtn = new WebElement(page, "xpath=" + MODAL_ROOT + "//div[contains(@class,'ant-modal-footer')]//button[contains(@class,'ant-btn-primary')]", "openProjectOpenBtn");
    }

    public OpenProjectDialogComponent waitForVisible() {
        openDependenciesCheckbox.waitForVisible(DEFAULT_TIMEOUT_MS);
        return this;
    }

    public boolean isSwitchDependenciesShown() {
        return switchDependenciesCheckbox.isVisible();
    }

    public boolean isSwitchDependenciesChecked() {
        return isSwitchDependenciesShown() && switchDependenciesCheckbox.isChecked();
    }

    public String getBranchWarning() {
        return branchWarning.waitForVisible(DEFAULT_TIMEOUT_MS).getInnerText().trim().replaceAll("\\s+", " ");
    }

    public void open() {
        openBtn.click();
        openDependenciesCheckbox.waitForHidden(DEFAULT_TIMEOUT_MS);
        waitUntilSpinnerLoaded();
    }

    public void cancel() {
        cancelBtn.click();
        openDependenciesCheckbox.waitForHidden(DEFAULT_TIMEOUT_MS);
    }
}
