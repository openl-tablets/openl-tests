package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

/**
 * The breadcrumb strip of the module screen: Projects / repository / project / branch / module.
 *
 * <p>Each of the project, the branch and the module is a switcher: a trigger that opens a list with a search
 * box above it. The lists are rendered into a body-level dropdown, so their items are located at page level.
 */
public class EditorBreadcrumbsComponent extends BaseComponent {

    private static final String HEADER = "xpath=//div[@data-testid='module-header']";
    private static final String OPEN_DROPDOWN = "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]";
    private static final int PROBE_MS = 1000;

    @lombok.Getter
    private final WebElement allProjectsLink;
    private final WebElement projectName;
    private final WebElement projectTrigger;
    private final WebElement projectSearch;
    private final WebElement branchName;
    private final WebElement branchTrigger;
    private final WebElement branchSearch;
    private final WebElement branchSwitchConfirm;
    private final WebElement moduleTrigger;
    private final WebElement moduleSearch;
    private final WebElement dropdownItemTemplate;
    private final WebElement repositoryName;

    public EditorBreadcrumbsComponent(Page page) {
        this(new WebElement(page, HEADER, "moduleHeader"));
    }

    public EditorBreadcrumbsComponent(WebElement rootLocator) {
        super(rootLocator);
        allProjectsLink = new WebElement(page, HEADER + "//a[@href='/projects']", "breadcrumbsAllProjects");
        repositoryName = new WebElement(page, HEADER + "//a[@href='/projects']/following-sibling::span[not(@aria-hidden)][1]", "breadcrumbsRepository");
        projectName = new WebElement(page, HEADER + "//span[@data-testid='crumb-project']", "breadcrumbsProjectName");
        projectTrigger = new WebElement(page, HEADER + "//button[@data-testid='crumb-project-trigger']", "breadcrumbsProjectTrigger");
        projectSearch = new WebElement(page, "xpath=//input[@data-testid='crumb-project-search']", "breadcrumbsProjectSearch");
        branchName = new WebElement(page, HEADER + "//span[@data-testid='crumb-branch']", "breadcrumbsBranchName");
        branchTrigger = new WebElement(page, HEADER + "//button[@data-testid='crumb-branch-trigger']", "breadcrumbsBranchTrigger");
        branchSearch = new WebElement(page, "xpath=//input[@data-testid='crumb-branch-search']", "breadcrumbsBranchSearch");
        branchSwitchConfirm = new WebElement(page, "xpath=//button[@data-testid='crumb-branch-discard-switch-confirm']", "branchSwitchConfirm");
        moduleTrigger = new WebElement(page, HEADER + "//button[@data-testid='module-switcher-trigger']", "breadcrumbsModuleTrigger");
        moduleSearch = new WebElement(page, "xpath=//input[@data-testid='module-switcher-search']", "breadcrumbsModuleSearch");
        dropdownItemTemplate = new WebElement(page, OPEN_DROPDOWN + "//li[contains(@class,'ant-dropdown-menu-item')][normalize-space()='%s']", "breadcrumbsDropdownItem");
    }

    public void navigateToProjectsList() {
        if (allProjectsLink.isVisible(PROBE_MS)) {
            allProjectsLink.click();
            waitUntilSpinnerLoaded();
        }
    }

    public void navigateToProjectRoot(String projectName) {
        selectProjectInDropdown(projectName);
    }

    public void switchBranch(String branchName) {
        WaitUtil.retryOnException(() -> {
            pickInSwitcher(branchTrigger, branchSearch, branchName);
            confirmBranchSwitchIfAsked();
            waitUntilSpinnerLoaded();
            if (!getCurrentBranch().trim().equals(branchName)) {
                throw new RuntimeException("Branch did not switch to " + branchName + ", current: " + getCurrentBranch().trim());
            }
            return true;
        }, 20000, 500, "Switching branch to " + branchName);
    }

    public void selectBranchInDropdown(String branchName) {
        pickInSwitcher(branchTrigger, branchSearch, branchName);
        confirmBranchSwitchIfAsked();
    }

    /**
     * Confirms losing the working copy's own changes, which the switch asks about before it goes ahead.
     * A project with nothing unsaved is switched without asking, so the absence of the question is normal.
     */
    private void confirmBranchSwitchIfAsked() {
        if (branchSwitchConfirm.isVisible(PROBE_MS)) {
            branchSwitchConfirm.click();
        }
    }

    public String getCurrentBranch() {
        return branchName.isVisible(PROBE_MS) ? branchName.getText().replace("Default", "").trim() : "";
    }

    public void selectModuleInDropdown(String moduleName) {
        WaitUtil.retryOnException(() -> {
            pickInSwitcher(moduleTrigger, moduleSearch, moduleName);
            return true;
        }, 10000, 500, "Selecting module " + moduleName + " from the breadcrumbs");
    }

    public void selectProjectInDropdown(String projectName) {
        WaitUtil.retryOnException(() -> {
            pickInSwitcher(projectTrigger, projectSearch, projectName);
            return true;
        }, 10000, 500, "Selecting project " + projectName + " from the breadcrumbs");
        waitUntilSpinnerLoaded();
    }

    /** Opens a switcher, narrows its list by the name and picks it, the way a reader does. */
    private void pickInSwitcher(WebElement trigger, WebElement search, String name) {
        trigger.clickWhenSettled();
        if (search.isVisible(PROBE_MS)) {
            search.fill(name);
        }
        WebElement item = dropdownItemTemplate.format(name);
        item.waitForVisible(DEFAULT_TIMEOUT_MS);
        item.click();
    }

    public String getProjectName() {
        return getProjectName(PROBE_MS);
    }

    public String getProjectName(int timeoutInMillis) {
        return projectName.isVisible(timeoutInMillis) ? projectName.getText().trim() : "";
    }

    public String getModuleName() {
        return getModuleName(PROBE_MS);
    }

    public String getModuleName(int timeoutInMillis) {
        return moduleTrigger.isVisible(timeoutInMillis) ? moduleTrigger.getText().trim() : "";
    }

    public void clickCategory() {
        repositoryName.click();
    }

    public void checkBreadcrumbs(String category, String project, String module) {
        if (!category.isEmpty()) {
            WaitUtil.waitForCondition(() -> repositoryName.isVisible(500)
                            && repositoryName.getText().contains(category),
                    5000, 250, "Waiting for the breadcrumb repository to be: " + category);
        }
        if (!project.isEmpty()) {
            WaitUtil.waitForCondition(() -> projectName.isVisible(500)
                            && projectName.getText().trim().equals(project),
                    5000, 250, "Waiting for the breadcrumb project to be: " + project);
        }
        if (!module.isEmpty()) {
            WaitUtil.waitForCondition(() -> moduleTrigger.isVisible(500)
                            && moduleTrigger.getText().trim().equals(module),
                    5000, 250, "Waiting for the breadcrumb module to be: " + module);
        }
    }
}
