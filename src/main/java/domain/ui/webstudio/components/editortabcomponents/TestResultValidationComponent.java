package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.common.TableComponent;
import helpers.utils.WaitUtil;

import java.util.List;

/**
 * The results of running a module's tests. A test table is named by a link that carries the outcome of the
 * whole table, and the cases below it are marked one by one.
 */
public class TestResultValidationComponent extends BaseComponent {

    // The window a run reports in, whichever run it was: a rule reports what it returned, a test table how
    // its cases went, and both are the same window with the same way out of it.
    private static final String RESULTS = "xpath=//div[contains(@class,'ant-modal-container')]"
            + "[.//button[@data-testid='execution-close']]";
    private static final String TABLE_LINK = RESULTS + "//a[starts-with(@data-testid,'test-table-')]";
    private static final String CASE_STATUS = RESULTS + "//table[starts-with(@data-testid,'test-results-')]//span[@title='%s']";
    // A rule's run shows the one row it produced; a test table shows a table of cases for each of its tables.
    private static final String RESULT_TABLE = "(" + RESULTS.substring("xpath=".length())
            + "//table[@data-testid='run-result-table' or starts-with(@data-testid,'test-results-')])[1]";
    private static final int PROBE_MS = 500;
    private static final long RESULTS_TIMEOUT_MS = 30000;

    private WebElement resultsWindow;
    private WebElement resultsTitle;
    private WebElement resultTableHeader;
    private TableComponent resultTable;
    private WebElement failedTableLinkTemplate;
    private List<WebElement> tableLinks;
    private List<WebElement> failedTableLinks;
    private List<WebElement> passedCases;
    private List<WebElement> failedCases;
    private WebElement currentModuleOnlyCheckbox;
    private WebElement failuresOnlyCheckbox;

    public TestResultValidationComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public TestResultValidationComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        resultsWindow = new WebElement(page, RESULTS, "testResultsWindow");
        resultsTitle = new WebElement(page, RESULTS + "//div[contains(@class,'ant-modal-title')]", "testResultsTitle");
        resultTableHeader = new WebElement(page, "xpath=" + RESULT_TABLE + "/thead/tr", "resultTableHeader");
        resultTable = createScopedComponent(TableComponent.class, "xpath=" + RESULT_TABLE, "resultTable");
        tableLinks = createElementList(TABLE_LINK, "testTableLinks");
        failedTableLinks = createElementList(TABLE_LINK + "[contains(@class,'ant-typography-danger')]", "failedTestTableLinks");
        failedTableLinkTemplate = new WebElement(page,
                TABLE_LINK + "[contains(@class,'ant-typography-danger')][normalize-space()='%s']", "failedTestTableLink");
        passedCases = createElementList(String.format(CASE_STATUS, "Passed"), "passedCases");
        failedCases = createElementList(RESULTS + "//table[starts-with(@data-testid,'test-results-')]"
                + "//span[@title='Failed' or @title='Error']", "failedCases");
        // The setting belongs to the launcher the run is started from, not to the window the results arrive in.
        currentModuleOnlyCheckbox = new WebElement(page,
                "xpath="
                        + "//input[@data-testid='tests-module-only']", "currentModuleOnlyCheckbox");
        failuresOnlyCheckbox = new WebElement(page, RESULTS + "//input[@data-testid='tests-failures-only']", "failuresOnlyCheckbox");
    }

    /**
     * Waits for the run to report. The window opens while the run is still on its way, so what is waited for
     * is what it has to show: the test tables it ran, or the row a rule's run produced.
     */
    private void waitForResults() {
        resultsWindow.waitForVisible(RESULTS_TIMEOUT_MS);
        WaitUtil.requireCondition(() -> !tableLinks.isEmpty() || resultTable.isVisible(), RESULTS_TIMEOUT_MS, 250,
                "Waiting for the run to report its results");
    }

    public TableComponent getResultTable() {
        waitForResults();
        WaitUtil.requireCondition(() -> resultTable.isVisible() && !resultTable.getRows().isEmpty(),
                DEFAULT_TIMEOUT_MS, 100, "Waiting for the table of test cases to be drawn");
        return resultTable;
    }

    public boolean isTestTableFailed() {
        waitForResults();
        return !failedTableLinks.isEmpty();
    }

    public boolean isTestTablePassed() {
        waitForResults();
        return failedTableLinks.isEmpty();
    }

    public int getFailedTestCount() {
        waitForResults();
        return failedCases.size();
    }

    public int getPassedTestCount() {
        waitForResults();
        return passedCases.size();
    }

    public int getTotalTestCount() {
        waitForResults();
        return passedCases.size() + failedCases.size();
    }

    public String getTestResultSummary() {
        return String.format("Total: %d, Passed: %d, Failed: %d",
                getTotalTestCount(), getPassedTestCount(), getFailedTestCount());
    }

    /** The column headings of the first table of results: what each case was run with, and what it returned. */
    public String getResultTableHeader() {
        waitForResults();
        return resultTableHeader.getText().trim();
    }

    /** What the window says about the run as a whole: how many tests were run, and how long they took. */
    public String getRunSummary() {
        waitForResults();
        return resultsTitle.getText().trim();
    }

    public List<String> getTestResult(int rowIndex) {
        return getResultTable().getRow(rowIndex).getValue();
    }

    public int countTestTables() {
        waitForResults();
        return tableLinks.size();
    }

    public void checkAllTablesPassed() {
        waitForResults();
        if (!failedTableLinks.isEmpty()) {
            throw new AssertionError("Expected all test tables to pass, but found failures: " + getAllFailedTests());
        }
    }

    public void checkTestTableFailed(String tableName) {
        waitForResults();
        boolean listed = tableLinks.stream().anyMatch(link -> link.getText().contains(tableName));
        if (!listed) {
            throw new AssertionError("Test table '" + tableName + "' not found in results");
        }
        if (!failedTableLinkTemplate.format(tableName).isVisible(PROBE_MS)) {
            throw new AssertionError("Expected test table '" + tableName + "' to have failures, but it passed");
        }
    }

    public boolean isCurrentModuleOnlyChecked() {
        return currentModuleOnlyCheckbox.isChecked();
    }

    public boolean isCurrentModuleOnlyEnabled() {
        return currentModuleOnlyCheckbox.isEnabled();
    }

    public boolean isFailuresOnlyFilterChecked() {
        return failuresOnlyCheckbox.isVisible(PROBE_MS) && failuresOnlyCheckbox.isChecked();
    }

    public void setFailuresOnlyFilter(boolean enabled) {
        failuresOnlyCheckbox.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (failuresOnlyCheckbox.isChecked() == enabled) {
            return;
        }
        failuresOnlyCheckbox.click();
        WaitUtil.requireCondition(() -> failuresOnlyCheckbox.isChecked() == enabled,
                DEFAULT_TIMEOUT_MS, 100, "Waiting for the 'Failures only' filter to switch to " + enabled);
        waitUntilSpinnerLoaded();
    }

    public List<String> getFailedTestNamesLenient() {
        return getAllFailedTests();
    }

    public void assertNoTestFailures(String contextMsg) {
        waitForResults();
        if (failedTableLinks.isEmpty() && failedCases.isEmpty()) {
            return;
        }
        throw new AssertionError(String.format(
                "Test failures detected. %s%nFailed test tables (%d): %s%nFailed cases: %d%nResults: %s",
                contextMsg, failedTableLinks.size(), getAllFailedTests(), failedCases.size(), getRunSummary()));
    }

    /**
     * Closes the window the run reported in. It covers the screen while it stands open, so the module cannot
     * be worked on again until it is out of the way.
     */
    public void closeResults() {
        WebElement closeBtn = new WebElement(page, RESULTS + "//button[@data-testid='execution-close']", "closeResultsBtn");
        if (closeBtn.isVisible(PROBE_MS)) {
            closeBtn.click();
            resultsWindow.waitForHidden(DEFAULT_TIMEOUT_MS);
        }
    }

    public List<String> getAllFailedTests() {
        waitForResults();
        return failedTableLinks.stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
    }
}
