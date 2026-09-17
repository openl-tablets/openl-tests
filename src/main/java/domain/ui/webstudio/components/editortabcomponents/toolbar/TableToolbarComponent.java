package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;

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

    /**
     * Opens what the band keeps behind its caret. The band names the first of the tables beside it and
     * folds the rest away, so the caret stands there only while there is more than one.
     */
    public void clickAvailableTestRunsExpandLink() {
        // The band shares its line with the panel of properties, which squeezes it when it is open. A
        // reader who cannot see the end of the band folds that panel away, which is what is done here.
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

    /**
     * The tables the band folded away, one per line. The band names the first of them itself and keeps the
     * rest behind the caret, so what is read here is the rest — as it was when the whole lot stood in a
     * list of its own.
     */
    public String getAvailableTestRunsPopupText() {
        List<WebElement> folded = createElementList(
                "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]"
                        + "//li[contains(@class,'ant-dropdown-menu-item')]", "foldedRelatedTables");
        WaitUtil.waitForListNotEmpty(() -> folded, DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the tables the band folded away to be listed");
        return folded.stream().map(WebElement::getText).map(String::trim)
                .collect(java.util.stream.Collectors.joining("\n"));
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
