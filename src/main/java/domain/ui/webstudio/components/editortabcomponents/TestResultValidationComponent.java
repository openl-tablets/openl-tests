package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.common.TableComponent;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.stream.Stream;

public class TestResultValidationComponent extends BaseComponent {

    private static final String RESULTS_WINDOW_PATH = "/execution-results";
    private static final String READ_MARK = "data-aqa-read";
    private static final String RESULTS = "//div[contains(@class,'ant-modal-container')][.//button[@data-testid='execution-close']]"
            + " | //main[not(@" + READ_MARK + ")][.//button[@data-testid='execution-close']]";
    private static final String IN_RESULTS = "(" + RESULTS + ")";
    private static final String TABLE_LINK = IN_RESULTS + "//a[starts-with(@data-testid,'test-table-')]";
    private static final String FAILED_TABLE_LINK = TABLE_LINK + "[contains(@class,'ant-typography-danger')]";
    private static final String CASES = IN_RESULTS + "//table[starts-with(@data-testid,'test-results-')]";
    private static final String PASSED_CASE = CASES + "//span[@title='Passed']";
    private static final String FAILED_CASE = CASES + "//span[@title='Failed' or @title='Error']";
    private static final String RESULT_TABLE = "(" + IN_RESULTS
            + "//table[@data-testid='run-result-table' or starts-with(@data-testid,'test-results-')])[1]";
    private static final int PROBE_MS = 500;
    private static final long RESULTS_TIMEOUT_MS = 30000;

    public TestResultValidationComponent() {
        super(DriverPool.getPage());
    }

    public TestResultValidationComponent(WebElement rootLocator) {
        super(rootLocator);
    }

    public static void putAwayShownResults(Page editor) {
        resultWindows(editor).forEach(window -> window.evaluate("mark => document.querySelectorAll('main')"
                + ".forEach(shown => shown.setAttribute(mark, 'true'))", READ_MARK));
    }

    private static Stream<Page> resultWindows(Page any) {
        return any.context().pages().stream()
                .filter(opened -> !opened.isClosed() && opened.url().contains(RESULTS_WINDOW_PATH));
    }

    private Page resultsPage() {
        return resultWindows(page).reduce((first, last) -> last).orElse(page);
    }

    private Locator results(String xpath) {
        return resultsPage().locator("xpath=" + xpath);
    }

    private WebElement resultsWindow() {
        return new WebElement(resultsPage(), "xpath=" + RESULTS, "testResultsWindow");
    }

    private TableComponent resultTable() {
        return new TableComponent(new WebElement(resultsPage(), "xpath=" + RESULT_TABLE, "resultTable"));
    }

    private void waitUntilResultsOpen() {
        WaitUtil.requireCondition(() -> results(RESULTS).count() > 0, RESULTS_TIMEOUT_MS, 250,
                "Waiting for the results of the run to open");
    }

    private void waitForResults() {
        waitUntilResultsOpen();
        resultsWindow().waitForVisible(RESULTS_TIMEOUT_MS);
        WaitUtil.requireCondition(() -> results(TABLE_LINK).count() > 0 || resultTable().isVisible(), RESULTS_TIMEOUT_MS, 250,
                "Waiting for the run to report its results");
    }

    public int rowsTheRunReported() {
        waitUntilResultsOpen();
        boolean drawn = WaitUtil.waitForCondition(() -> resultTable().isVisible(PROBE_MS),
                RESULTS_TIMEOUT_MS, 250, "Waiting for the run to draw what it returned");
        return drawn ? resultTable().getRows().size() : 0;
    }

    public TableComponent getResultTable() {
        waitForResults();
        TableComponent table = resultTable();
        WaitUtil.requireCondition(() -> table.isVisible() && !table.getRows().isEmpty(),
                DEFAULT_TIMEOUT_MS, 100, "Waiting for the table of test cases to be drawn");
        return table;
    }

    public int countResultWindows() {
        return (int) resultWindows(page).count();
    }

    public boolean isTestTableFailed() {
        waitForResults();
        return results(FAILED_TABLE_LINK).count() > 0;
    }

    public boolean isTestTablePassed() {
        waitForResults();
        return results(FAILED_TABLE_LINK).count() == 0;
    }

    public int getFailedTestCount() {
        waitForResults();
        return results(FAILED_CASE).count();
    }

    public int getPassedTestCount() {
        waitForResults();
        return results(PASSED_CASE).count();
    }

    public int getTotalTestCount() {
        waitForResults();
        return getPassedTestCount() + getFailedTestCount();
    }

    public String getTestResultSummary() {
        return String.format("Total: %d, Passed: %d, Failed: %d",
                getTotalTestCount(), getPassedTestCount(), getFailedTestCount());
    }

    public String getResultTableHeader() {
        waitForResults();
        return new WebElement(resultsPage(), "xpath=" + RESULT_TABLE + "/thead/tr", "resultTableHeader").getText().trim();
    }

    public String getRunSummary() {
        waitForResults();
        return results(IN_RESULTS + "//div[contains(@class,'ant-modal-title')] | " + IN_RESULTS + "//h1").first()
                .innerText().trim();
    }

    public List<String> getTestResult(int rowIndex) {
        return getResultTable().getRow(rowIndex).getValue();
    }

    public int countTestTables() {
        waitForResults();
        return results(TABLE_LINK).count();
    }

    public void checkAllTablesPassed() {
        waitForResults();
        if (results(FAILED_TABLE_LINK).count() > 0) {
            throw new AssertionError("Expected all test tables to pass, but found failures: " + getAllFailedTests());
        }
    }

    public void checkTestTableFailed(String tableName) {
        waitForResults();
        boolean listed = results(TABLE_LINK).allInnerTexts().stream().anyMatch(link -> link.contains(tableName));
        if (!listed) {
            throw new AssertionError("Test table '" + tableName + "' not found in results");
        }
        if (!new WebElement(resultsPage(), "xpath=" + FAILED_TABLE_LINK + "[normalize-space()='" + tableName + "']",
                "failedTestTableLink").isVisible(PROBE_MS)) {
            throw new AssertionError("Expected test table '" + tableName + "' to have failures, but it passed");
        }
    }

    public boolean isFailuresOnlyFilterChecked() {
        WebElement failuresOnly = failuresOnlyCheckbox();
        return failuresOnly.isVisible(PROBE_MS) && failuresOnly.isChecked();
    }

    public void setFailuresOnlyFilter(boolean enabled) {
        WebElement failuresOnly = failuresOnlyCheckbox();
        failuresOnly.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (failuresOnly.isChecked() == enabled) {
            return;
        }
        failuresOnly.click();
        WaitUtil.requireCondition(() -> failuresOnly.isChecked() == enabled,
                DEFAULT_TIMEOUT_MS, 100, "Waiting for the 'Failures only' filter to switch to " + enabled);
        waitUntilSpinnerLoaded();
    }

    private WebElement failuresOnlyCheckbox() {
        return new WebElement(resultsPage(), "xpath=" + IN_RESULTS + "//input[@data-testid='tests-failures-only']",
                "failuresOnlyCheckbox");
    }

    public List<String> getFailedTestNamesLenient() {
        return getAllFailedTests();
    }

    public void assertNoTestFailures(String contextMsg) {
        waitForResults();
        int failedTables = results(FAILED_TABLE_LINK).count();
        int failedCases = results(FAILED_CASE).count();
        if (failedTables == 0 && failedCases == 0) {
            return;
        }
        throw new AssertionError(String.format(
                "Test failures detected. %s%nFailed test tables (%d): %s%nFailed cases: %d%nResults: %s",
                contextMsg, failedTables, getAllFailedTests(), failedCases, getRunSummary()));
    }

    public void closeResults() {
        Page shown = resultsPage();
        WebElement closeBtn = closeButtonOn(shown);
        if (!closeBtn.isVisible(PROBE_MS)) {
            return;
        }
        if (shown == page) {
            closeResultsOverTheEditor();
        } else {
            shown.waitForClose(closeBtn::click);
        }
    }

    public void closeResultsOverTheEditor() {
        WebElement closeBtn = closeButtonOn(page);
        if (closeBtn.isVisible(PROBE_MS)) {
            closeBtn.click();
            new WebElement(page, "xpath=" + RESULTS, "testResultsDialog").waitForHidden(DEFAULT_TIMEOUT_MS);
        }
    }

    private WebElement closeButtonOn(Page shown) {
        return new WebElement(shown, "xpath=" + IN_RESULTS + "//button[@data-testid='execution-close']", "closeResultsBtn");
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
        return results(IN_RESULTS + "//table[@data-testid='run-result-table']");
    }

    public List<String> getAllFailedTests() {
        waitForResults();
        return results(FAILED_TABLE_LINK).allInnerTexts().stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
    }
}
