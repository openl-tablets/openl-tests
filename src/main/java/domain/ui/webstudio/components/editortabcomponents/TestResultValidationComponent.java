package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Locator;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.common.TableComponent;
import helpers.utils.WaitUtil;

import java.util.List;

public class TestResultValidationComponent extends BaseComponent {

    private static final String RESULTS = "xpath=//div[contains(@class,'ant-modal-container')]"
            + "[.//button[@data-testid='execution-close']]";
    private static final String TABLE_LINK = RESULTS + "//a[starts-with(@data-testid,'test-table-')]";
    private static final String CASE_STATUS = RESULTS + "//table[starts-with(@data-testid,'test-results-')]//span[@title='%s']";
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
        failuresOnlyCheckbox = new WebElement(page, RESULTS + "//input[@data-testid='tests-failures-only']", "failuresOnlyCheckbox");
    }

    private void waitForResults() {
        resultsWindow.waitForVisible(RESULTS_TIMEOUT_MS);
        WaitUtil.requireCondition(() -> !tableLinks.isEmpty() || resultTable.isVisible(), RESULTS_TIMEOUT_MS, 250,
                "Waiting for the run to report its results");
    }

    public int rowsTheRunReported() {
        resultsWindow.waitForVisible(RESULTS_TIMEOUT_MS);
        boolean drawn = WaitUtil.waitForCondition(() -> resultTable.isVisible(PROBE_MS),
                RESULTS_TIMEOUT_MS, 250, "Waiting for the run to draw what it returned");
        return drawn ? resultTable.getRows().size() : 0;
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

    public String getResultTableHeader() {
        waitForResults();
        return resultTableHeader.getText().trim();
    }

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

    public void closeResults() {
        WebElement closeBtn = new WebElement(page, RESULTS + "//button[@data-testid='execution-close']", "closeResultsBtn");
        if (closeBtn.isVisible(PROBE_MS)) {
            closeBtn.click();
            resultsWindow.waitForHidden(DEFAULT_TIMEOUT_MS);
        }
    }

    public List<String> getRunColumnHeadings() {
        waitForResults();
        return runResultTable().locator("xpath=./thead/tr[1]/th").allInnerTexts().stream().map(String::trim).toList();
    }

    public String getUnfoldedRunCellText(String heading) {
        List<String> headings = getRunColumnHeadings();
        int column = 0;
        for (int index = 0; index < headings.size() && column == 0; index++) {
            if (headings.get(index).equalsIgnoreCase(heading)) {
                column = index + 1;
            }
        }
        if (column == 0) {
            throw new AssertionError("The run results have no column " + heading + ": " + headings);
        }
        Locator cell = runResultTable().locator("xpath=./tbody/tr[1]/td[" + column + "]");
        WaitUtil.requireCondition(() -> {
            Locator folded = cell.locator("xpath=.//span[contains(@class,'ant-tree-switcher_close')]");
            if (folded.count() == 0) {
                return true;
            }
            folded.first().click();
            return false;
        }, DEFAULT_TIMEOUT_MS, 300, "Waiting for the " + heading + " cell of the run results to unfold");
        return cell.innerText().trim();
    }

    private Locator runResultTable() {
        return page.locator(RESULTS + "//table[@data-testid='run-result-table']");
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
