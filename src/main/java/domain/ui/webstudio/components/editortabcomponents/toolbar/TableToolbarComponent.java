package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import helpers.utils.WaitUtil;

import java.util.List;

public class TableToolbarComponent extends BaseComponent {

    private static final int TRACE_WINDOW_TIMEOUT_MS = 65000;

    private final WebElement runBtn;
    private final WebElement runDropdownBtn;
    private final WebElement traceBtn;
    private final WebElement traceDropdownBtn;
    private final WebElement benchmarkBtn;
    private final WebElement benchmarkDropdownBtn;
    private final WebElement editTableBtn;
    private final WebElement copyTableBtn;
    private final WebElement removeBtn;
    private final WebElement createTestBtn;
    private final WebElement targetTableLink;
    private final WebElement availableTestRunsLink;
    private final WebElement availableTestRunsInlineLink;
    private final WebElement availableTestRunsExpandLink;
    private final WebElement availableTestRunsPopup;
    private final WebElement tableActionsTestBtn;
    private final WebElement tableActionsTestDropdownBtn;
    private final WebElement tableActions;
    private final WebElement withinCurrentModuleOnlyInputArgs;
    private final WebElement withinCurrentModuleOnlyTestTables;
    private final List<WebElement> openLaunchers;

    public TableToolbarComponent(Page page) {
        this(new WebElement(page, "xpath=//div[@data-testid='table-toolbar']", "tableToolbarPanel"));
    }

    public TableToolbarComponent(WebElement rootLocator) {
        super(rootLocator);
        runBtn = createScopedElement("xpath=.//button[@data-testid='table-run']", "runBtn");
        runDropdownBtn = createScopedElement("xpath=.//button[@data-testid='table-run']", "runDropdownBtn");
        traceBtn = createScopedElement("xpath=.//button[@data-testid='table-trace']", "traceBtn");
        traceDropdownBtn = createScopedElement("xpath=.//button[@data-testid='table-trace']", "traceDropdownBtn");
        benchmarkBtn = createScopedElement("xpath=.//button[@data-testid='table-benchmark']", "benchmarkBtn");
        benchmarkDropdownBtn = createScopedElement("xpath=.//button[@data-testid='table-benchmark']", "benchmarkDropdownBtn");
        editTableBtn = createScopedElement("xpath=.//button[@data-testid='table-edit']", "editBtn");
        copyTableBtn = createScopedElement("xpath=.//button[@data-testid='table-copy']", "copyBtn");
        removeBtn = createScopedElement("xpath=.//button[@data-testid='table-remove']", "removeBtn");
        createTestBtn = createScopedElement("xpath=.//button[@data-testid='table-createTest']", "createTestBtn");
        targetTableLink = createScopedElement("xpath=.//div[@data-testid='table-target-tables']//button[starts-with(@data-testid,'table-target-')]", "targetTableLink");
        availableTestRunsLink = createScopedElement("xpath=.//div[@data-testid='table-available-tests']", "availableTestRunsLink");
        availableTestRunsInlineLink = createScopedElement("xpath=.//div[@data-testid='table-available-tests']//button[starts-with(@data-testid,'table-test-')]", "availableTestRunsInlineLink");
        availableTestRunsExpandLink = createScopedElement("xpath=.//button[@data-testid='table-available-tests-more']", "availableTestRunsExpandLink");
        availableTestRunsPopup = new WebElement(page, "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]//ul[contains(@class,'ant-dropdown-menu')]", "availableTestRunsPopup");
        tableActionsTestBtn = createScopedElement("xpath=.//button[@data-testid='table-tests']", "tableActionsTestBtn");
        tableActionsTestDropdownBtn = createScopedElement("xpath=.//button[@data-testid='table-tests']", "tableActionsTestDropdownBtn");
        tableActions = createScopedElement("xpath=.//button[starts-with(@data-testid,'table-')]"
                + "[not(ancestor::div[@data-testid='table-target-tables' or @data-testid='table-available-tests'])]",
                "tableActions");
        withinCurrentModuleOnlyInputArgs = new WebElement(page, "xpath=//input[@data-testid='launch-module-only']", "withinCurrentModuleOnlyInputArgs");
        withinCurrentModuleOnlyTestTables = new WebElement(page, "xpath=//input[@data-testid='tests-module-only']", "withinCurrentModuleOnlyTestTables");
        openLaunchers = createElementList("xpath=//button[@data-testid='run-start' or @data-testid='trace-start'"
                + " or @data-testid='tests-start' or @data-testid='benchmark-start']", "openLaunchers");
    }

    private void closeOpenLauncher() {
        closeWindowsOverTheScreen();
        if (openLaunchers.isEmpty()) {
            return;
        }
        page.keyboard().press("Escape");
        WaitUtil.requireCondition(openLaunchers::isEmpty, DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the launcher standing open to close");
    }

    public IRunMenu clickRun() {
        closeOpenLauncher();
        runBtn.waitForVisible();
        runBtn.click();
        RunMenuComponent launcher = new RunMenuComponent(page);
        launcher.waitForLauncher();
        return launcher;
    }

    public ITraceMenu clickTrace() {
        closeOpenLauncher();
        traceBtn.click();
        TraceMenuComponent launcher = new TraceMenuComponent(page);
        launcher.waitForLauncher();
        return launcher;
    }

    public ITraceWindow clickTraceExpectTraceWindow() {
        waitUntilSpinnerLoaded();
        traceBtn.waitForVisible();
        return WaitUtil.retryOnException(() -> {
            traceBtn.click();
            TraceMenuComponent launcher = new TraceMenuComponent(page);
            launcher.waitForLauncher();
            return launcher.clickTraceInsideMenu();
        }, TRACE_WINDOW_TIMEOUT_MS, 1000, "Opening the trace window");
    }

    public void clickBenchmark() {
        benchmarkBtn.click();
    }

    public void clickRunBesideOpenLauncher() {
        runBtn.waitForVisible();
        runBtn.click();
    }

    public void clickBenchmarkStart() {
        clickBenchmarkDropdown();
        WebElement start = new WebElement(page, "xpath=//button[@data-testid='benchmark-start']", "benchmarkStartBtn");
        start.waitForVisible(DEFAULT_TIMEOUT_MS);
        TestResultValidationComponent.putAwayShownResults(page);
        start.click();
    }

    public void clickRunDropdown() {
        closeOpenLauncher();
        runBtn.waitForVisible();
        runBtn.click();
    }

    public void clickBenchmarkDropdown() {
        closeOpenLauncher();
        benchmarkBtn.waitForVisible();
        benchmarkBtn.click();
    }

    public boolean isRunButtonVisible() {
        return runBtn.isVisible(1000);
    }

    public boolean isTraceButtonVisible() {
        return traceBtn.isVisible(1000);
    }

    public boolean isBenchmarkButtonVisible() {
        return benchmarkBtn.isVisible(1000);
    }

    public boolean isTableActionsTestBtnVisible() {
        return tableActionsTestBtn.isVisible(1000);
    }

    public List<String> getTableActions() {
        runBtn.waitForVisible();
        WaitUtil.waitForStableSize(() -> tableActions.getLocator().count(), DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the actions of the table toolbar to stop changing");
        return tableActions.getLocator().allInnerTexts().stream().map(String::trim).toList();
    }

    public WebElement getEditTableBtn() {
        return editTableBtn;
    }

    public WebElement getCopyTableBtn() {
        return copyTableBtn;
    }

    public WebElement getRemoveBtn() {
        return removeBtn;
    }

    public WebElement getCreateTestBtn() {
        return createTestBtn;
    }

    public void clickTableActionsTestBtn() {
        closeOpenLauncher();
        tableActionsTestBtn.click();
        WebElement start = new WebElement(page, "xpath=//button[@data-testid='tests-start']", "testsStartBtn");
        start.waitForVisible(DEFAULT_TIMEOUT_MS);
        TestResultValidationComponent.putAwayShownResults(page);
        start.click();
    }

    public void clickTableActionsTestDropdown() {
        closeOpenLauncher();
        tableActionsTestBtn.waitForVisible();
        tableActionsTestBtn.click();
    }

    public String getTargetTableText() {
        if (targetTableLink.isVisible(2000)) {
            return targetTableLink.getText().trim();
        }
        return "";
    }

    public boolean isTargetTableVisible() {
        return targetTableLink.isVisible(2000);
    }

    public void clickTargetTable() {
        targetTableLink.click();
        WaitUtil.sleep(500, "Waiting for target table navigation");
    }

    public String getAvailableTestRunsLinkText() {
        if (availableTestRunsLink.isVisible(2000)) {
            return availableTestRunsLink.getText().trim();
        }
        return "";
    }

    public boolean isAvailableTestRunsLinkVisible() {
        return availableTestRunsLink.isVisible(1000);
    }

    public String getAvailableTestRunsInlineLinkText() {
        return availableTestRunsInlineLink.getText().trim();
    }

    public void clickAvailableTestRunsInlineLink() {
        availableTestRunsInlineLink.click();
        WaitUtil.sleep(500, "Waiting for navigation to Test/Run table");
    }

    public boolean isAvailableTestRunsExpandLinkVisible() {
        return availableTestRunsExpandLink.isVisible(1000);
    }

    public void clickAvailableTestRunsExpandLink() {
        WebElement openProperties = new WebElement(page,
                "xpath=//div[@data-testid='table-details-body']", "openPropertiesPanel");
        if (openProperties.isVisible(1000)) {
            new WebElement(page, "xpath=//button[@data-testid='table-details-toggle']", "foldPropertiesBtn").click();
            openProperties.waitForHidden(DEFAULT_TIMEOUT_MS);
        }
        WaitUtil.retryOnException(() -> {
            availableTestRunsExpandLink.click(DEFAULT_TIMEOUT_MS / 2);
            availableTestRunsPopup.waitForVisible(DEFAULT_TIMEOUT_MS / 2);
            return true;
        }, DEFAULT_TIMEOUT_MS, 500, "Opening the rest of the tables beside this one");
    }

    public String getAvailableTestRunsPopupText() {
        List<WebElement> folded = createElementList(
                "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]"
                        + "//li[contains(@class,'ant-dropdown-menu-item')]", "foldedRelatedTables");
        WaitUtil.waitForListNotEmpty(() -> folded, DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the tables the band folded away to be listed");
        return folded.stream().map(WebElement::getText).map(String::trim)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    public boolean isWithinCurrentModuleOnlyInputArgsChecked() {
        return withinCurrentModuleOnlyInputArgs.isChecked();
    }

    public boolean isWithinCurrentModuleOnlyInputArgsEnabled() {
        return withinCurrentModuleOnlyInputArgs.isEnabled();
    }

    public boolean isWithinCurrentModuleOnlyTestTablesChecked() {
        return withinCurrentModuleOnlyTestTables.isChecked();
    }

    public boolean isWithinCurrentModuleOnlyTestTablesEnabled() {
        return withinCurrentModuleOnlyTestTables.isEnabled();
    }
}
