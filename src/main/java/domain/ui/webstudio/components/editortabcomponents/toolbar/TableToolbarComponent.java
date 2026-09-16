package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

/**
 * The second-line (table) toolbar of the editor: Run / Trace / Benchmark launchers with their dropdown
 * arrows, the Edit / Copy / Remove / Create Test table actions and the target-table / available-test-runs
 * sections. Scoped under {@code div#tableToolbarPanel} (verified against the live 6.4.0 DOM).
 */
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
    // These two live inside the run/test dropdowns of this toolbar; they are located by their unique ids
    // at page level because the menus only exist in the DOM while open.
    private final WebElement withinCurrentModuleOnlyInputArgs;
    private final WebElement withinCurrentModuleOnlyTestTables;

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
        withinCurrentModuleOnlyInputArgs = new WebElement(page, "xpath=//input[@data-testid='launch-module-only']", "withinCurrentModuleOnlyInputArgs");
        withinCurrentModuleOnlyTestTables = new WebElement(page, "xpath=//input[@data-testid='launch-module-only']", "withinCurrentModuleOnlyTestTables");
    }

    // ========== Launchers ==========

    public IRunMenu clickRun() {
        runBtn.waitForVisible();
        runBtn.click();
        RunMenuComponent launcher = new RunMenuComponent(page);
        launcher.waitForLauncher();
        return launcher;
    }

    public ITraceMenu clickTrace() {
        traceBtn.click();
        TraceMenuComponent launcher = new TraceMenuComponent(page);
        launcher.waitForLauncher();
        return launcher;
    }

    /**
     * Traces the table the page shows, step by step. The button opens the launcher, and the trace itself
     * starts from there — a table that declares no parameters is asked all the same, because the launcher
     * carries the settings the trace runs under.
     */
    public ITraceWindow clickTraceExpectTraceWindow() {
        waitUntilSpinnerLoaded();
        traceBtn.waitForVisible();
        // The press can land before the rule is ready to be traced, and then no window opens at all, so the
        // whole of opening the launcher and starting from it is tried again rather than waited out.
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

    /**
     * Opens the Run launcher. The settings the old toolbar kept behind a caret beside Run are inside the
     * launcher itself now, so opening it is the same press as running.
     */
    public void clickRunDropdown() {
        runBtn.waitForVisible();
        runBtn.click();
    }

    public void clickBenchmarkDropdown() {
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

    // ========== Table actions ==========

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
        tableActionsTestBtn.click();
    }

    public void clickTableActionsTestDropdown() {
        tableActionsTestBtn.waitForVisible();
        tableActionsTestBtn.click();
    }

    // ========== Target table and available test runs ==========

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
        availableTestRunsExpandLink.click();
        WaitUtil.sleep(300, "Waiting for popup with all Tests/Runs to appear");
    }

    public String getAvailableTestRunsPopupText() {
        return availableTestRunsPopup.getText().trim().replaceAll("\\s*\\n\\s*", "\n");
    }

    // ========== Within Current Module Only (run/test dropdowns) ==========

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
