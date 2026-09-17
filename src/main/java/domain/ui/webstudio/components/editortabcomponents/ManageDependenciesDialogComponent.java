package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

/**
 * The projects this one is declared to depend on, which stand on the project's own card and are written
 * there: the card is opened for writing, a line is added for each project depended on, and what was written
 * is kept.
 */
public class ManageDependenciesDialogComponent extends BaseComponent {

    private static final String PANEL = "xpath=//div[@data-testid='overview-panel']";
    private static final int PROBE_MS = 2000;

    private WebElement editBtn;
    private WebElement saveBtn;
    private WebElement cancelBtn;
    private WebElement addBtn;
    private WebElement lineSelectTemplate;
    private WebElement lineAutoTemplate;
    private WebElement lines;

    public ManageDependenciesDialogComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public ManageDependenciesDialogComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        editBtn = new WebElement(page, PANEL + "//button[@data-testid='overview-edit']", "overviewEditBtn");
        saveBtn = new WebElement(page, PANEL + "//button[@data-testid='overview-save']", "overviewSaveBtn");
        cancelBtn = new WebElement(page, PANEL + "//button[@data-testid='overview-cancel']", "overviewCancelBtn");
        addBtn = new WebElement(page, PANEL + "//button[@data-testid='edit-dependency-add']", "addDependencyBtn");
        lineSelectTemplate = new WebElement(page,
                PANEL + "//div[@data-testid='edit-dependency-%s']//input", "dependencySelect");
        lineAutoTemplate = new WebElement(page,
                PANEL + "//input[@data-testid='edit-dependency-%s-auto']", "dependencyAutoCheckbox");
        lines = new WebElement(page, PANEL + "//div[starts-with(@data-testid,'edit-dependency-')]"
                + "[substring(@data-testid, string-length(@data-testid) - 3) = '-row']", "dependencyLines");
    }

    /** Opens the card for writing, which is where the dependencies are declared. */
    public void openForEditing() {
        WaitUtil.requireCondition(() -> {
            if (addBtn.isVisible(PROBE_MS)) {
                return true;
            }
            if (editBtn.isVisible(PROBE_MS)) {
                editBtn.click();
            }
            return addBtn.isVisible(PROBE_MS);
        }, DEFAULT_TIMEOUT_MS, 250, "Waiting for the project's dependencies to be offered for writing");
    }

    /**
     * Declares that the project depends on that one project and no other, which is what the old dialog left
     * behind when a reader ticked one project in its list. A project the workspace no longer holds is still
     * declared in the file and would be reported as missing, so the lines standing are taken away first.
     */
    public void dependOnlyOn(String projectName, boolean includeAllModules) {
        for (int standing = lines.getLocator().count(); standing > 0; standing--) {
            new WebElement(page, PANEL + "//button[@data-testid='edit-dependency-" + (standing - 1) + "-remove']",
                    "removeDependencyBtn").click();
        }
        addBtn.click();
        pickInSelect(lineSelectTemplate.format("0"), projectName);
        WebElement auto = lineAutoTemplate.format("0");
        if (auto.isChecked() != includeAllModules) {
            auto.click();
        }
        saveBtn.click();
        WaitUtil.requireCondition(() -> !saveBtn.isVisible(PROBE_MS), DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the declared dependencies to be kept");
    }

    public void clickCancel() {
        cancelBtn.click();
    }
}
