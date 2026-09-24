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
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunMenuComponent;
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

public class EditorToolbarPanelComponent extends BaseComponent {

    private static final int ACTION_PROBE_MS = DEFAULT_TIMEOUT_MS / 2;
    private static final String PROJECT_ACTION = "xpath=//button[@data-testid='module-%1$s'] | //div[@data-testid='project-actions' or @data-testid='project-actions-overflow']//button[starts-with(@data-testid,'%1$s-') and not(substring(@data-testid, string-length(@data-testid) - 4) = '-more')]";

    private WebElement projectActionsMoreBtn;
    private WebElement verifyBtn;
    private WebElement createTableBtn;
    private WebElement refreshProjectBtn;
    private WebElement allTopToolbarLinks;
    private WebElement factorTextField;

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
        tableToolbar = new TableToolbarComponent(page);
    }

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

    private WebElement projectAction(String action) {
        return new WebElement(page, String.format(PROJECT_ACTION, action), action + "Btn");
    }

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

    private void clickProjectAction(String action) {
        WebElement button = projectAction(action);
        if (!button.isVisible(ACTION_PROBE_MS) && projectActionsMoreBtn.isVisible(ACTION_PROBE_MS)) {
            projectActionsMoreBtn.click();
        }
        button.waitForVisible(DEFAULT_TIMEOUT_MS);
        button.click();
    }

    public void clickSave() {
        clickProjectAction("save");
    }

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

    public void navigateToProjectsInBreadcrumbs() {
        breadcrumbs.navigateToProjectsList();
    }

    public WebElement getBreadcrumbsAllProjects() {
        return breadcrumbs.getAllProjectsLink();
    }

    public void navigateToProjectRoot(String projectName) {
        new EditorLeftProjectModuleSelectorComponent().selectProject(projectName);
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

    public IRunMenu clickRun() {
        return tableToolbar.clickRun();
    }

    public RunMenuComponent getRunLauncher() {
        return new RunMenuComponent(page);
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

    public WebElement getTestDropdownBtn() {
        return new WebElement(page, "xpath=//a[@title='Run Tests']/following-sibling::span[1]", "testDropdownBtn");
    }

    public IRunTestsMenu clickTestDropdown() {
        runTestsMenu.openDropdown();
        return runTestsMenu;
    }

    public String getTestButtonText() {
        return runTestsMenu.getTestButtonText();
    }

    public String getTestCountWhenCounted(String expected) {
        return runTestsMenu.getTestCountWhenCounted(expected);
    }

    public String getTestCount() {
        return runTestsMenu.getTestCount();
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

    public IMoreMenu clickMore() {
        return new MoreMenuComponent(page).open();
    }

    public List<String> getMoreMenuItems() {
        MoreMenuComponent moreMenu = new MoreMenuComponent(page);
        moreMenu.open();
        return moreMenu.getMenuItems();
    }
}
