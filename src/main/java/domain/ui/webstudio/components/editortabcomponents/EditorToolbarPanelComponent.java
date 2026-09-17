package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftProjectModuleSelectorComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.EditorBreadcrumbsComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.IMoreMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.IRunMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.IRunTestsMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceWindow;
import domain.ui.webstudio.components.editortabcomponents.toolbar.MoreMenuComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunTestsMenuComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.TableToolbarComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.TraceMenuComponent;
import helpers.utils.WaitUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * The editor's toolbar, composed of scoped sub-components (see the {@code toolbar} package):
 * <ul>
 *   <li>{@link EditorBreadcrumbsComponent} — the Projects / project / branch / module breadcrumb strip;</li>
 *   <li>{@link RunTestsMenuComponent} — the top-panel Test button with its settings dropdown;</li>
 *   <li>{@link MoreMenuComponent} — the top-panel More dropdown;</li>
 *   <li>{@link TableToolbarComponent} — the second-line table toolbar (Run/Trace/Benchmark, table actions).</li>
 * </ul>
 * The public API is kept flat here so existing tests keep working; new code may also use the
 * sub-component getters directly.
 */
public class EditorToolbarPanelComponent extends BaseComponent {

    // As long as a toolbar button was given before the actions moved: a card still settling is normal.
    private static final int ACTION_PROBE_MS = DEFAULT_TIMEOUT_MS / 2;

    // TOP LINE TOOLBAR — plain buttons that belong to no dropdown
    private WebElement projectActionsMoreBtn;
    private WebElement verifyBtn;
    private WebElement createTableBtn;
    private WebElement refreshProjectBtn;
    private WebElement allTopToolbarLinks;
    // Trace factor input in the launcher form (kept page-level: the menu exists in the DOM only while open)
    private WebElement factorTextField;

    // Scoped sub-components
    @Getter
    private EditorBreadcrumbsComponent breadcrumbs;
    @Getter
    private RunTestsMenuComponent runTestsMenu;
    @Getter
    private TableToolbarComponent tableToolbar;

    public EditorToolbarPanelComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public EditorToolbarPanelComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        verifyBtn = new WebElement(page, "xpath=//button[@data-testid='module-verify']", "verifyBtn");
        projectActionsMoreBtn = new WebElement(page,
                "xpath=//button[@data-testid='project-actions-more']", "projectActionsMoreBtn");
        refreshProjectBtn = new WebElement(page, "xpath=//button[@data-testid='module-refresh']", "refreshProjectBtn");
        createTableBtn = new WebElement(page, "xpath=//button[@data-testid='module-createTable']", "createTableBtn");
        allTopToolbarLinks = new WebElement(page, "xpath=//div[@data-testid='module-actions']//button", "allTopToolbarLinks");
        factorTextField = new WebElement(page, "xpath=//div[@data-testid='table-input-anchor']//input[@type='text']", "factorTextField");

        breadcrumbs = new EditorBreadcrumbsComponent(page);
        runTestsMenu = new RunTestsMenuComponent(page);
        // EditorPage passes div#tableToolbarPanel as this component's root — reuse it for the table toolbar.
        tableToolbar = new TableToolbarComponent(page);
    }

    // ========== Top line toolbar ==========

    public void clickVerify() {
        verifyBtn.click();
    }

    public boolean isVerifyButtonPresent() {
        return verifyBtn.isVisible(2000);
    }

    public void clickCopyProjectBtn() {
        clickProjectAction("copy");
    }

    public boolean isCopyProjectBtnVisible() {
        return offersProjectAction("copy");
    }

    public void clickCreateTable() {
        createTableBtn.click();
    }

    /**
     * An action of the project, wherever the project is being looked at: on the module screen it stands in
     * the module's action bar under {@code module-<action>}, and on the project card in the card's own bar
     * under {@code <action>-<project>}, from where it falls into an overflow menu when the bar runs out of
     * room. The element is the one the screen shows now; it is absent while the project does not offer the
     * action at all.
     */
    private WebElement projectAction(String action) {
        return new WebElement(page, "xpath=//button[@data-testid='module-" + action + "']"
                + " | //div[@data-testid='project-actions']//button[starts-with(@data-testid,'" + action + "-')]"
                + " | //div[@data-testid='project-actions-overflow']//button[starts-with(@data-testid,'" + action + "-')]",
                action + "Btn");
    }

    /**
     * Whether the project offers the action right now. The card folds the actions it has no room for into a
     * menu behind a three-dots button, so an action that is not in sight may still be offered there; the
     * menu is opened to look, and left as it was found.
     */
    private boolean offersProjectAction(String action) {
        if (projectAction(action).isVisible(ACTION_PROBE_MS)) {
            return true;
        }
        if (!projectActionsMoreBtn.isVisible(ACTION_PROBE_MS)) {
            return false;
        }
        projectActionsMoreBtn.click();
        boolean offered = projectAction(action).isVisible(ACTION_PROBE_MS);
        projectActionsMoreBtn.click();
        return offered;
    }

    /**
     * Presses the action. What is waited for is the button itself, wherever it stands: pressing the
     * three-dots button to look would close the menu the press before it opened.
     */
    private void clickProjectAction(String action) {
        WebElement button = projectAction(action);
        if (!button.isVisible(ACTION_PROBE_MS) && projectActionsMoreBtn.isVisible(ACTION_PROBE_MS)) {
            projectActionsMoreBtn.click();
        }
        button.waitForVisible(DEFAULT_TIMEOUT_MS);
        button.click();
    }

    /** Saves the project, which is offered only while it has changes of its own to save. */
    public void clickSave() {
        clickProjectAction("save");
    }

    /** The toolbar's Refresh, which reloads the project the editor is showing. */
    public void clickProjectRefresh() {
        refreshProjectBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
    }

    public void clickSync() {
        clickProjectAction("sync");
        WaitUtil.sleep(500, "Waiting for Sync dialog to open");
    }

    public boolean isSyncButtonVisible() {
        return offersProjectAction("sync");
    }


    public void clickExport() {
        clickProjectAction("export");
        new ExportProjectDialogComponent().waitForDialogToAppear();
    }

    public List<String> getAllVisibleTopToolbarActions() {
        List<String> actions = new ArrayList<>();
        com.microsoft.playwright.Locator links = allTopToolbarLinks.getLocator();
        for (int i = 0; i < links.count(); i++) {
            com.microsoft.playwright.Locator link = links.nth(i);
            if (link.isVisible()) {
                String text = link.textContent().trim();
                if (!text.isEmpty()) {
                    actions.add(text);
                }
                String title = link.getAttribute("title");
                if (title != null && !title.isEmpty()) {
                    actions.add(title);
                }
            }
        }
        return actions;
    }

    // ========== Breadcrumbs (delegated) ==========

    public void navigateToProjectsInBreadcrumbs() {
        breadcrumbs.navigateToProjectsList();
    }

    public WebElement getBreadcrumbsAllProjects() {
        return breadcrumbs.getAllProjectsLink();
    }

    public void navigateToProjectRoot(String projectName) {
        breadcrumbs.navigateToProjectRoot(projectName);
    }

    public void switchBranch(String branchName) {
        breadcrumbs.switchBranch(branchName);
    }

    public void selectBranchInDropdown(String branchName) {
        breadcrumbs.selectBranchInDropdown(branchName);
    }

    public String getCurrentBranch() {
        return breadcrumbs.getCurrentBranch();
    }

    public void selectBreadcrumbModule(String projectName, String moduleName) {
        waitUntilSpinnerLoaded();
        String actualProject = breadcrumbs.getProjectName(5000);
        String actualModule = breadcrumbs.getModuleName(5000);

        if (actualProject.equals(projectName) && !actualModule.equals(moduleName)) {
            breadcrumbs.selectModuleInDropdown(moduleName);
        } else if (!actualProject.equals(projectName)) {
            navigateToProjectRoot(projectName);
            new EditorLeftProjectModuleSelectorComponent().selectModule(projectName, moduleName);
        }
        WaitUtil.waitForCondition(
                () -> moduleName.equals(getBreadcrumbsModuleName().trim()),
                10000,
                250,
                "Waiting for breadcrumb module to become " + moduleName);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        waitUntilSpinnerLoaded();
        new ProblemsPanelComponent().waitForCompilationToComplete(60000, 250);
        new EditorLeftRulesTreeComponent().waitForTreeFoldersToLoad();
    }

    public void selectProjectBreadcrumbs(String projectName) {
        breadcrumbs.selectProjectInDropdown(projectName);
    }

    public String getBreadcrumbsProjectName() {
        return breadcrumbs.getProjectName();
    }

    public String getBreadcrumbsModuleName() {
        return breadcrumbs.getModuleName();
    }

    public void clickBreadcrumbsCategory() {
        breadcrumbs.clickCategory();
    }

    public void checkBreadcrumbs(String category, String project, String module) {
        breadcrumbs.checkBreadcrumbs(category, project, module);
    }

    // ========== Run / Trace / Benchmark (delegated to the table toolbar) ==========

    public IRunMenu clickRun() {
        return tableToolbar.clickRun();
    }

    public ITraceMenu clickTrace() {
        return tableToolbar.clickTrace();
    }

    public ITraceWindow clickTraceExpectTraceWindow() {
        return tableToolbar.clickTraceExpectTraceWindow();
    }

    public void clickBenchmark() {
        tableToolbar.clickBenchmark();
    }

    public void clickRunDropdown() {
        tableToolbar.clickRunDropdown();
    }

    public void clickBenchmarkDropdown() {
        tableToolbar.clickBenchmarkDropdown();
    }

    public boolean isRunButtonVisible() {
        return tableToolbar.isRunButtonVisible();
    }

    public boolean isTraceButtonVisible() {
        return tableToolbar.isTraceButtonVisible();
    }

    public boolean isBenchmarkButtonVisible() {
        return tableToolbar.isBenchmarkButtonVisible();
    }

    public TraceMenuComponent setFactorTextField(String text) {
        factorTextField.fill(text);
        return new TraceMenuComponent(page);
    }

    // ========== Table actions (delegated to the table toolbar) ==========

    public WebElement getEditTableBtn() {
        return tableToolbar.getEditTableBtn();
    }

    public CopyTableDialogComponent clickCopy() {
        tableToolbar.getCopyTableBtn().click();
        return new CopyTableDialogComponent();
    }

    public void clickRemove() {
        tableToolbar.getRemoveBtn().click();
        WaitUtil.sleep(100, "Waiting for table removal action to complete");
    }

    public void copyTableAsNew(String newName, String description) {
        CopyTableDialogComponent copyDialog = clickCopy();
        copyDialog.selectCopyAs("New Table").setName(newName);
        if (description != null && !description.isEmpty()) {
            copyDialog.setSaveTo(description);
        }
        copyDialog.clickCopy();
    }

    public void copyTableAsNewVersion(String version) {
        CopyTableDialogComponent copyDialog = clickCopy();
        copyDialog.selectCopyAs("New Version").setVersion(version);
        copyDialog.clickCopy();
    }

    public void copyTableAsBusinessDimension(String propertyLabel, String propertyValue) {
        CopyTableDialogComponent copyDialog = clickCopy();
        copyDialog.selectCopyAs("New Business Dimension Version").setProperty(propertyLabel, propertyValue).clickCopy();
    }

    /**
     * Removes the table the screen shows. The removal clears the table from the sheet it is written on, so
     * it is asked about first, in a window of the screen's own rather than the browser's.
     */
    public void removeCurrentTable() {
        clickRemove();
        WebElement confirmRemove = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal-confirm')]//button[.//span[normalize-space()='Remove']]",
                "confirmRemoveTable");
        confirmRemove.waitForVisible(DEFAULT_TIMEOUT_MS);
        confirmRemove.click();
        waitUntilSpinnerLoaded();
    }

    public void createDefaultTestTable() {
        // Create Test now opens the React Create Table modal with the tested table already filled in, so the
        // default test table is one press of Create away.
        tableToolbar.getCreateTestBtn().click();
        new CreateTableDialogComponent().waitForDialogToAppear().save();
        WaitUtil.sleep(500, "Waiting for created test table to open");
    }

    public void clickTableActionsTestBtn() {
        tableToolbar.clickTableActionsTestBtn();
    }

    public void clickTableActionsTestDropdown() {
        tableToolbar.clickTableActionsTestDropdown();
    }

    // ========== Target table and available test runs (delegated) ==========

    public String getTargetTableText() {
        return tableToolbar.getTargetTableText();
    }

    public boolean isTargetTableVisible() {
        return tableToolbar.isTargetTableVisible();
    }

    public void clickTargetTable() {
        tableToolbar.clickTargetTable();
    }

    public String getAvailableTestRunsLinkText() {
        return tableToolbar.getAvailableTestRunsLinkText();
    }

    public boolean isAvailableTestRunsLinkVisible() {
        return tableToolbar.isAvailableTestRunsLinkVisible();
    }

    public String getAvailableTestRunsInlineLinkText() {
        return tableToolbar.getAvailableTestRunsInlineLinkText();
    }

    public void clickAvailableTestRunsInlineLink() {
        tableToolbar.clickAvailableTestRunsInlineLink();
    }

    public boolean isAvailableTestRunsExpandLinkVisible() {
        return tableToolbar.isAvailableTestRunsExpandLinkVisible();
    }

    public void clickAvailableTestRunsExpandLink() {
        tableToolbar.clickAvailableTestRunsExpandLink();
    }

    public String getAvailableTestRunsPopupText() {
        return tableToolbar.getAvailableTestRunsPopupText();
    }

    // ========== Test menu (delegated) ==========

    public WebElement getTestDropdownBtn() {
        // Kept for tests that click the dropdown toggle directly.
        return new WebElement(page, "xpath=//a[@title='Run Tests']/following-sibling::span[1]", "testDropdownBtn");
    }

    public IRunTestsMenu clickTestDropdown() {
        runTestsMenu.openDropdown();
        return runTestsMenu;
    }

    public String getTestButtonText() {
        return runTestsMenu.getTestButtonText();
    }

    public boolean isTestButtonVisible() {
        return runTestsMenu.isTestButtonVisible();
    }

    public void clickTopPanelTestButton() {
        runTestsMenu.clickTestButton();
    }

    public void runAllTests() {
        runTestsMenu.runAllTests();
    }

    public void clickTopPanelTestDropdown() {
        runTestsMenu.openDropdownAndWaitForSettings();
    }

    public void clickTopPanelRunTestBtn() {
        runTestsMenu.clickRunTestsButton();
    }

    // ========== Within Current Module Only (delegated) ==========

    public boolean isWithinCurrentModuleOnlyInputArgsChecked() {
        return tableToolbar.isWithinCurrentModuleOnlyInputArgsChecked();
    }

    public boolean isWithinCurrentModuleOnlyInputArgsEnabled() {
        return tableToolbar.isWithinCurrentModuleOnlyInputArgsEnabled();
    }

    public boolean isWithinCurrentModuleOnlyTestTablesChecked() {
        return tableToolbar.isWithinCurrentModuleOnlyTestTablesChecked();
    }

    public boolean isWithinCurrentModuleOnlyTestTablesEnabled() {
        return tableToolbar.isWithinCurrentModuleOnlyTestTablesEnabled();
    }

    public boolean isTopPanelWithinCurrentModuleOnlyChecked() {
        return runTestsMenu.isWithinCurrentModuleOnlyChecked();
    }

    public boolean isTopPanelWithinCurrentModuleOnlyEnabled() {
        return runTestsMenu.isWithinCurrentModuleOnlyEnabled();
    }

    public void setTopPanelWithinCurrentModuleOnly(boolean value) {
        runTestsMenu.setWithinCurrentModuleOnly(value);
    }

    // ========== More menu (delegated) ==========

    public IMoreMenu clickMore() {
        return new MoreMenuComponent(page).open();
    }

    public List<String> getMoreMenuItems() {
        MoreMenuComponent moreMenu = new MoreMenuComponent(page);
        moreMenu.open();
        return moreMenu.getMenuItems();
    }
}
