package domain.ui.webstudio.components.editortabcomponents.leftmenu;

import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ProjectsTableComponent;
import helpers.utils.WaitUtil;

import java.util.List;

/**
 * Reaches a project and a module of it the way a reader does: from the Projects list to the project's card,
 * and from the card into the module's screen.
 */
public class EditorLeftProjectModuleSelectorComponent extends BaseComponent {

    private static final int PROBE_MS = 1000;
    private static final long MODULE_READY_TIMEOUT_MS = 60000;
    private static final int MODULE_READY_POLL_MS = 250;

    private TabSwitcherComponent tabSwitcher;
    private ProjectsTableComponent projectsTable;
    private WebElement projectDetail;
    private WebElement projectCardName;
    private WebElement projectCrumb;
    private WebElement projectCrumbFollow;
    private WebElement moduleOpenTemplate;
    private List<WebElement> moduleOpenLinks;
    private WebElement moduleWorkspace;
    private WebElement moduleCompiling;
    private WebElement moduleTablesTree;
    private WebElement moduleTablesEmpty;
    private WebElement moduleCompileFailed;

    public EditorLeftProjectModuleSelectorComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public EditorLeftProjectModuleSelectorComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        tabSwitcher = new TabSwitcherComponent(new WebElement(page,
                "xpath=//ul[@role='menu' and contains(@class,'ant-menu-horizontal')]", "tabSwitcher"));
        projectsTable = new ProjectsTableComponent(page);
        projectDetail = new WebElement(page, "xpath=//div[@data-testid='project-detail']", "projectDetail");
        // The card's own heading is the first one on it; the panels below carry headings of their own.
        projectCardName = new WebElement(page, "xpath=(//div[@data-testid='project-detail']//h3)[1]", "projectCardName");
        projectCrumb = new WebElement(page, "xpath=//span[@data-testid='crumb-project']", "projectCrumb");
        projectCrumbFollow = new WebElement(page, "xpath=//button[@data-testid='crumb-project-follow']", "projectCrumbFollow");
        moduleOpenTemplate = new WebElement(page, "xpath=//a[@data-testid='module-open-%s']", "moduleOpenLink");
        moduleOpenLinks = createElementList("xpath=//a[starts-with(@data-testid,'module-open-')]", "moduleOpenLinks");
        moduleWorkspace = new WebElement(page, "xpath=//div[@data-testid='module-workspace']", "moduleWorkspace");
        moduleCompiling = new WebElement(page, "xpath=//div[@data-testid='module-compiling']", "moduleCompiling");
        moduleTablesTree = new WebElement(page, "xpath=//div[@data-testid='module-tables-tree']", "moduleTablesTree");
        moduleTablesEmpty = new WebElement(page, "xpath=//div[@data-testid='module-tables-empty']", "moduleTablesEmpty");
        moduleCompileFailed = new WebElement(page, "xpath=//div[@data-testid='module-compile-failed']", "moduleCompileFailed");
    }

    /**
     * Opens the project's card, which is where its modules are listed. A reader already inside the project —
     * on one of its modules — walks back up through the breadcrumb rather than through the whole list.
     */
    public void selectProject(String projectName) {
        waitUntilSpinnerLoaded();
        if (isCardOf(projectName)) {
            return;
        }
        // A window a reader opened over the screen — the module's local history — is closed before they
        // leave it, because the screen underneath cannot be reached while it stands there.
        new ChangesDialogComponent().closeIfOpen();
        if (isModuleOf(projectName)) {
            projectCrumbFollow.click();
        } else {
            tabSwitcher.selectTab(TabSwitcherComponent.TabName.REPOSITORY);
            projectsTable.clickProjectName(projectName);
        }
        WaitUtil.requireCondition(() -> isCardOf(projectName), DEFAULT_TIMEOUT_MS, MODULE_READY_POLL_MS,
                "Waiting for the card of project '" + projectName + "' to open");
        waitUntilSpinnerLoaded();
    }

    public void selectModule(String projectName, String projectModuleName) {
        selectProject(projectName);
        WebElement moduleLink = moduleOpenTemplate.format(projectModuleName);
        moduleLink.waitForVisible(DEFAULT_TIMEOUT_MS);
        moduleLink.click();
        waitForModuleScreenReady();
    }

    /**
     * Opens the project at whichever module it lists first. The tables of a project are reached through a
     * module of it — the tables rail, its search and the table screens all belong to a module — so a reader
     * who wants the project's tables opens one.
     */
    public void selectFirstModule(String projectName) {
        selectProject(projectName);
        WaitUtil.waitForListNotEmpty(() -> moduleOpenLinks, DEFAULT_TIMEOUT_MS, MODULE_READY_POLL_MS,
                "Waiting for the modules of '" + projectName + "' to be listed");
        moduleOpenLinks.get(0).click();
        waitForModuleScreenReady();
    }

    public List<String> getAllModuleNames(String projectName) {
        selectProject(projectName);
        WaitUtil.waitForListNotEmpty(() -> moduleOpenLinks, DEFAULT_TIMEOUT_MS, MODULE_READY_POLL_MS,
                "Waiting for the modules of '" + projectName + "' to be listed");
        return moduleOpenLinks.stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    private boolean isCardOf(String projectName) {
        return projectDetail.isVisible(PROBE_MS)
                && projectCardName.isVisible(PROBE_MS)
                && projectName.equals(projectCardName.getText().trim());
    }

    private boolean isModuleOf(String projectName) {
        return projectCrumb.isVisible(PROBE_MS) && projectName.equals(projectCrumb.getText().trim());
    }

    /**
     * Waits until the module screen is usable: the workspace is drawn and the module has finished compiling,
     * so the tables tree answers what the module holds rather than an empty in-between state.
     */
    private void waitForModuleScreenReady() {
        moduleWorkspace.waitForVisible(DEFAULT_TIMEOUT_MS);
        WaitUtil.requireCondition(() -> !moduleCompiling.isVisible(MODULE_READY_POLL_MS),
                MODULE_READY_TIMEOUT_MS, MODULE_READY_POLL_MS, "Waiting for the module to finish compiling");
        WaitUtil.requireCondition(() -> moduleTablesTree.isVisible(MODULE_READY_POLL_MS)
                        || moduleTablesEmpty.isVisible(MODULE_READY_POLL_MS)
                        || moduleCompileFailed.isVisible(MODULE_READY_POLL_MS),
                MODULE_READY_TIMEOUT_MS, MODULE_READY_POLL_MS, "Waiting for the module tables tree to be drawn");
        waitUntilSpinnerLoaded();
    }
}
