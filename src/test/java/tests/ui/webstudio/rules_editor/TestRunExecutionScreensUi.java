package tests.ui.webstudio.rules_editor;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.ScreenWindows;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.BenchmarkMenuComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunMenuComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunTestsMenuComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.service.WorkflowService;
import helpers.utils.DownloadUtil;
import helpers.utils.WaitUtil;
import helpers.utils.XlsxUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.METHOD;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.SPREADSHEET;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TEST;
import static org.assertj.core.api.Assertions.assertThat;

public class TestRunExecutionScreensUi extends BaseTest {

    private static final String EXECUTION_WORKBOOK = "RunExecution.xlsx";
    private static final String EXECUTION_MODULE = "RunExecution";
    private static final String EXAMPLE_3 = "Example 3 - Auto Policy Calculation";
    private static final String EXAMPLE_3_MODULE = "AutoPolicyCalculation";
    private static final String RESULTS_FILE = "test-results.xlsx";
    private static final int BENCHMARK_ENDS_MS = 15000;
    private static final int RESULTS_FILE_MS = 60000;
    private static final String KEYED_LIST_WORKBOOK = "ListByPrimaryKey.xlsx";
    private static final String KEYED_LIST_MODULE = "ListByPrimaryKey";
    private static final String UNKEYED_LIST_WORKBOOK = "ListByFirstColumn.xlsx";
    private static final String UNKEYED_LIST_MODULE = "ListByFirstColumn";
    private static final String MANY_CASES_WORKBOOK = "ManyCases.xlsx";
    private static final String MANY_CASES_MODULE = "ManyCases";
    private static final int CASES_PER_PAGE = 20;
    private static final int CASES_IN_TABLE = 45;
    private static final String TRANSPOSED_WORKBOOK = "TransposedResults.zip";
    private static final String TRANSPOSED_MODULE = "Rules";
    private static final String USUAL_TEST = "PremiumTest";
    private static final String TRANSPOSED_TEST = "TransposedPremiumTest";
    private static final int DEFAULT_WAIT_MS = 15000;
    private static final String DEPENDENCY_PROJECT = "NotClickable";
    private static final String DEPENDENT_PROJECT = "Sample Project";
    private static final String DEPENDENCY_MODULE = "example2";
    private static final String DEPENDENCY_TEST = "mySpr";
    private static final int EXECUTION_TEST_CASES = 26;
    private static final String WINDOW_BLOCKED =
            "The results window did not open. Allow pop-up windows for this site, then try again.";
    private static final String LONG_RUN_WORKBOOK = "LongRun.xlsx";
    private static final String LONG_RUN_MODULE = "LongRun";
    private static final String LONG_TEST = "spinTest";
    private static final String EDITED_RULE = "twice";
    private static final int RULE_BODY_ROW = 2;
    private static final int RULE_BODY_COLUMN = 1;
    private static final String TESTS_STOPPED = "The tests were stopped before they ended";
    private static final String BENCHMARK_STOPPED = "The benchmark was stopped before it ended, and measured nothing";
    private static final String PROJECT_CHANGED = "A project was changed or reloaded since the execution started, "
            + "so its results were dropped. Start it again.";

    @Test
    @TestCaseId({"EPBDS-16581", "EPBDS-16582", "EPBDS-16583"})
    @Description("EPBDS-16581: the window of a test run over several test tables counts the tables finished while the "
            + "run goes on. EPBDS-16582: Test into File of a table still saves the file when the Run panel of the same "
            + "table is opened while it works. EPBDS-16583: a benchmark of a rule that throws says it could not be "
            + "measured, and a benchmark closed unread is still offered with the next one.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void runWindowsCountTablesSaveFilesAndExplainBenchmarks() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, EXECUTION_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, EXECUTION_MODULE);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();

        toolbar.clickTestDropdown().runTests();
        assertThat(results.mostTestTablesDoneWhileRunning())
                .as("EPBDS-16581: the window of the run must count the test tables finished while the run goes on")
                .isGreaterThanOrEqualTo(2);
        results.closeResults();

        selectTable(editorPage, METHOD, "fib");
        RunTestsMenuComponent testMenu = toolbar.getRunTestsMenu();
        toolbar.clickTableActionsTestDropdown();
        File saved = DownloadUtil.downloadFile(() -> {
            testMenu.clickTestIntoFile();
            toolbar.clickRunBesideOpenLauncher();
        }, RESULTS_FILE_MS);
        try {
            assertThat(saved.getName())
                    .as("EPBDS-16582: Test into File must save the results while the Run panel of the table is open")
                    .endsWith(RESULTS_FILE);
            assertThat(saved).as("EPBDS-16582: the saved results file must not be empty").isNotEmpty();
        } finally {
            DownloadUtil.cleanupDownloadFile(saved);
        }
        testMenu.waitForTestLauncherToClose();

        selectTable(editorPage, TEST, "failingNegTest");
        toolbar.clickBenchmarkStart();
        assertThat(results.getResultsTextMatching("could not be measured"))
                .as("EPBDS-16583: a benchmark of a rule that throws must say it could not be measured and why")
                .contains("The table could not be measured")
                .contains("failingNegTest cannot be measured");
        results.closeResults();

        selectTable(editorPage, TEST, "fibTest1");
        toolbar.clickBenchmarkStart();
        results.closeResultsOnceOpen();
        WaitUtil.sleep(BENCHMARK_ENDS_MS, "Waiting for the benchmark closed unread to end");
        selectTable(editorPage, TEST, "fibTest2");
        toolbar.clickBenchmarkStart();
        assertThat(results.getResultsTextMatching("(?<!\\d)2 measurements"))
                .as("EPBDS-16583: the benchmark closed unread must be offered with the next one")
                .contains("fibTest1")
                .contains("fibTest2");
        results.closeResults();
    }

    @Test
    @TestCaseId("EPBDS-15404")
    @Description("EPBDS-15404: Test into File of a test whose list input comes from a Data table writes the list on "
            + "both sheets: with a primary key the Result sheet names the list by its key and the Parameters sheet "
            + "has a key column beside the elements, without one the Result sheet lists the values and the "
            + "Parameters sheet fills the list column, and the neighbouring map columns stay where they are.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testResultsOfListInputsFromDataTableAreSavedWithTheirData() {
        String keyedProject = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, KEYED_LIST_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        File keyed = saveTestResultsOfCalculate(editorPage, keyedProject, KEYED_LIST_MODULE);
        try {
            assertThat(XlsxUtil.readSheetFromRow(keyed, "Result 1", "ID"))
                    .as("EPBDS-15404: the Result sheet must name the list by its primary key, not null")
                    .containsExactly(
                            List.of("ID", "Status", "Values", "Options", "Result"),
                            List.of("1", "Passed", "LIST-7", "OPTIONS-7"),
                            List.of("2", "Passed", "", "OPTIONS-7"));
            assertThat(XlsxUtil.readSheetFromRow(keyed, "Parameters 1", "ID"))
                    .as("EPBDS-15404: the Parameters sheet must have the key column and the list elements in their own column")
                    .containsExactly(
                            List.of("ID", "values._PK_", "values", "options._PK_", "options[\"label\"]:String"),
                            List.of("1", "LIST-7", "42", "OPTIONS-7", "Blue"),
                            List.of("", "", "Blue"),
                            List.of("2", "", "", "OPTIONS-7", "Blue"));
        } finally {
            DownloadUtil.cleanupDownloadFile(keyed);
        }

        String unkeyedProject = WorkflowService.createProjectFromExcelFile(UNKEYED_LIST_WORKBOOK);
        File unkeyed = saveTestResultsOfCalculate(editorPage, unkeyedProject, UNKEYED_LIST_MODULE);
        try {
            assertThat(XlsxUtil.readSheetFromRow(unkeyed, "Result 1", "ID"))
                    .as("EPBDS-15404: the Result sheet must list the values of the list given by its first column")
                    .containsExactly(
                            List.of("ID", "Status", "Values", "Options", "Result"),
                            List.of("1", "Passed", "42.0,Blue", "OPTIONS-7"),
                            List.of("2", "Passed", "", "OPTIONS-7"));
            assertThat(XlsxUtil.readSheetFromRow(unkeyed, "Parameters 1", "ID"))
                    .as("EPBDS-15404: the Parameters sheet must fill the list column")
                    .containsExactly(
                            List.of("ID", "values", "options._PK_", "options[\"label\"]:String"),
                            List.of("1", "42", "OPTIONS-7", "Blue"),
                            List.of("", "Blue"),
                            List.of("2", "", "OPTIONS-7", "Blue"));
        } finally {
            DownloadUtil.cleanupDownloadFile(unkeyed);
        }
    }

    @Test
    @TestCaseId({"EPBDS-16713", "EPBDS-16589"})
    @Description("EPBDS-16713: the results window lists a test table of 45 cases 20 at a time, with a pager for the "
            + "rest, instead of drawing every case at once. EPBDS-16589: when the browser refuses to open the trace "
            + "window, the Trace panel stays open and says to allow pop-up windows.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testResultsOfLargeTestTableAreListedAPageAtATime() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, MANY_CASES_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MANY_CASES_MODULE);
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();

        editorPage.getEditorToolbarPanelComponent().clickTestDropdown().runTests();
        assertThat(results.getResultsTextMatching(CASES_IN_TABLE + " test cases"))
                .as("EPBDS-16713: the run of all %d cases must report them", CASES_IN_TABLE)
                .contains(CASES_IN_TABLE + " test cases");
        assertThat(results.getShownCaseIds())
                .as("EPBDS-16713: the first page must list only the first %d cases", CASES_PER_PAGE)
                .containsExactlyElementsOf(caseIds(1, CASES_PER_PAGE));
        assertThat(results.countCasesPages())
                .as("EPBDS-16713: the pager must reach the rest of the %d cases in pages of %d", CASES_IN_TABLE, CASES_PER_PAGE)
                .isEqualTo(3);

        results.openCasesPage(3);
        assertThat(results.getShownCaseIds())
                .as("EPBDS-16713: the last page must list the cases after the second page")
                .containsExactlyElementsOf(caseIds(2 * CASES_PER_PAGE + 1, CASES_IN_TABLE));
        results.closeResults();

        selectTable(editorPage, METHOD, "twice");
        ITraceMenu traceMenu = editorPage.getEditorToolbarPanelComponent().clickTrace();
        traceMenu.blockPopupWindows();
        traceMenu.clickTraceInsideMenu(false);
        assertThat(traceMenu.waitForLaunchError())
                .as("EPBDS-16589: a trace window the browser refused to open must be reported in the Trace panel")
                .isEqualTo("The trace window did not open. Allow pop-up windows for this site and try again.");
    }

    @Test
    @TestCaseId("EPBDS-17053")
    @Description("EPBDS-17053: the tests of a project run the test tables of the project it depends on, and the name "
            + "of such a test in the results is a link that opens its test table in the dependency project.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testOfDependencyProjectIsOpenedFromResults() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, DEPENDENCY_PROJECT, "NotClickable.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, DEPENDENT_PROJECT, "Sample-Project.zip");
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(DEPENDENT_PROJECT, "Main");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();

        editorPage.getEditorToolbarPanelComponent().clickTestDropdown().runTests();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        assertThat(results.isTestTableNameALink(DEPENDENCY_TEST))
                .as("EPBDS-17053: the name of the test of the dependency project must be a link, not plain text")
                .isTrue();

        results.openTestTable(DEPENDENCY_TEST);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        EditorLeftRulesTreeComponent tree = editorPage.getEditorLeftRulesTreeComponent();
        WaitUtil.waitForCondition(() -> DEPENDENCY_PROJECT.equals(toolbar.getBreadcrumbsProjectName().trim())
                        && DEPENDENCY_MODULE.equals(toolbar.getBreadcrumbsModuleName().trim())
                        && DEPENDENCY_TEST.equals(tree.getSelectedItemText()),
                DEFAULT_WAIT_MS, 250, "Waiting for the test table of the dependency project to be opened");
        assertThat(List.of(toolbar.getBreadcrumbsProjectName(), toolbar.getBreadcrumbsModuleName()))
                .as("EPBDS-17053: the link must open the test table in the module of the dependency project")
                .containsExactly(DEPENDENCY_PROJECT, DEPENDENCY_MODULE);
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getSelectedItemText())
                .as("EPBDS-17053: the test table of the dependency project must be the selected table")
                .isEqualTo(DEPENDENCY_TEST);
    }

    @Test
    @TestCaseId("EPBDS-16960")
    @Description("EPBDS-16960: the results of a test table are laid out as the test table is written: a case to a row "
            + "for the usual table and a case to a column for the transposed one, and Transpose of the results "
            + "switches the layout and back.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testResultsAreLaidOutAsTheirTestTableIs() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, TRANSPOSED_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, TRANSPOSED_MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();

        editorPage.getEditorToolbarPanelComponent().clickTestDropdown().runTests();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        assertThat(results.isTestResultsTransposed(USUAL_TEST))
                .as("EPBDS-16960: the results of a test table written the usual way round must list a case to a row")
                .isFalse();
        assertThat(results.isTestResultsTransposed(TRANSPOSED_TEST))
                .as("EPBDS-16960: the results of a transposed test table must list a case to a column")
                .isTrue();

        results.transposeTestResults(USUAL_TEST);
        assertThat(results.isTestResultsTransposed(USUAL_TEST))
                .as("EPBDS-16960: Transpose must lay the results of the usual test table out a case to a column")
                .isTrue();
        results.transposeTestResults(USUAL_TEST);
        assertThat(results.isTestResultsTransposed(USUAL_TEST))
                .as("EPBDS-16960: Transpose clicked again must bring the usual layout back")
                .isFalse();
    }

    @Test
    @TestCaseId({"IPBQA-33086", "EPBDS-16959"})
    @Description("EPBDS-16959: Test of the module and of a table, Run of a test table and Benchmark open their results "
            + "straight in one separate window, with no results dialog over the editor, and Test into File and Run into "
            + "File save their results without opening a window.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void resultsOpenStraightInOneSeparateWindow() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, EXECUTION_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, EXECUTION_MODULE);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();

        toolbar.clickTestDropdown().runTests();
        String everyCaseRun = "(?<!\\d)" + EXECUTION_TEST_CASES + " tests in";
        assertThat(results.getResultsTextMatching(everyCaseRun))
                .as("EPBDS-16959: Test of the module must show the results of every test case in the results window")
                .containsPattern(everyCaseRun);
        assertResultsInOneWindowOnly(results, "Test of the module");

        selectTable(editorPage, METHOD, "failing");
        toolbar.clickTableActionsTestBtn();
        assertThat(results.countTestTables())
                .as("EPBDS-16959: Test of a table must show the results of the tables that test it in the window")
                .isEqualTo(2);
        assertResultsInOneWindowOnly(results, "Test of a table");

        selectTable(editorPage, TEST, "failingPosTest");
        toolbar.clickRun().clickRunInsideMenu();
        assertThat(results.getShownCaseIds())
                .as("EPBDS-16959: Run of a test table must show its case in the window")
                .containsExactly("1");
        assertResultsInOneWindowOnly(results, "Run of a test table");

        toolbar.clickBenchmarkStart();
        assertThat(results.getResultsTextMatching("(?<!\\d)1 measurement"))
                .as("EPBDS-16959: Benchmark must show its measurement in the window")
                .contains("failingPosTest");
        assertResultsInOneWindowOnly(results, "Benchmark");

        results.closeResults();
        List<Page> openedWindows = new CopyOnWriteArrayList<>();
        Consumer<Page> windowWatcher = openedWindows::add;
        BrowserContext browser = DriverPool.getPage().context();
        browser.onPage(windowWatcher);
        try {
            selectTable(editorPage, METHOD, "failing");
            toolbar.clickTableActionsTestDropdown();
            File testedIntoFile = DownloadUtil.downloadFile(toolbar.getRunTestsMenu()::clickTestIntoFile, RESULTS_FILE_MS);
            try {
                assertThat(testedIntoFile.getName())
                        .as("EPBDS-16959: Test into File must save the results file")
                        .endsWith(RESULTS_FILE);
            } finally {
                DownloadUtil.cleanupDownloadFile(testedIntoFile);
            }

            selectTable(editorPage, TEST, "failingPosTest");
            toolbar.clickRun();
            File runIntoFile = DownloadUtil.downloadFile(() -> toolbar.getRunLauncher().clickRunIntoFile(), RESULTS_FILE_MS);
            try {
                assertThat(runIntoFile.getName())
                        .as("EPBDS-16959: Run into File of a test table must save the results file of its cases")
                        .endsWith(RESULTS_FILE);
            } finally {
                DownloadUtil.cleanupDownloadFile(runIntoFile);
            }
        } finally {
            browser.offPage(windowWatcher);
        }
        assertThat(openedWindows)
                .as("EPBDS-16959: Test into File and Run into File must open no results window")
                .isEmpty();
    }

    @Test
    @TestCaseId({"IPBQA-33086", "EPBDS-16959"})
    @Description("EPBDS-16959: when the browser blocks pop-up windows, Test of the module and of a table, Run and "
            + "Benchmark start nothing, and their panel says to allow pop-up windows for the site.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void blockedResultsWindowStartsNothing() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, EXECUTION_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, EXECUTION_MODULE);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        RunTestsMenuComponent testMenu = toolbar.getRunTestsMenu();
        Page editor = DriverPool.getPage();
        List<String> executionsStarted = new CopyOnWriteArrayList<>();
        Consumer<Request> executionWatcher = request -> {
            if (!"GET".equals(request.method()) && request.url().contains("/rest/projects/")) {
                executionsStarted.add(request.method() + " " + request.url());
            }
        };
        ScreenWindows.blockPopupWindows(editor);
        editor.onRequest(executionWatcher);

        toolbar.clickTopPanelTestDropdown();
        toolbar.clickTopPanelRunTestBtn();
        assertThat(testMenu.waitForLaunchError())
                .as("EPBDS-16959: Test of the module must say the results window was blocked")
                .isEqualTo(WINDOW_BLOCKED);

        selectTable(editorPage, METHOD, "failing");
        toolbar.clickTableActionsTestDropdown();
        testMenu.clickRunTestsButton();
        assertThat(testMenu.waitForLaunchError())
                .as("EPBDS-16959: Test of a table must say the results window was blocked")
                .isEqualTo(WINDOW_BLOCKED);

        selectTable(editorPage, TEST, "failingPosTest");
        toolbar.clickRun();
        RunMenuComponent runMenu = toolbar.getRunLauncher();
        assertThat(runMenu.getLaunchError()).as("Precondition: the Run panel must open without an error").isEmpty();
        runMenu.clickRunInsideMenu();
        assertThat(runMenu.waitForLaunchError())
                .as("EPBDS-16959: Run must say the results window was blocked")
                .isEqualTo(WINDOW_BLOCKED);

        toolbar.clickBenchmarkDropdown();
        BenchmarkMenuComponent benchmarkMenu = toolbar.getBenchmarkLauncher();
        assertThat(benchmarkMenu.getLaunchError())
                .as("Precondition: the Benchmark panel must open without the error of the Run panel")
                .isEmpty();
        benchmarkMenu.clickBenchmarkInsideMenu();
        assertThat(benchmarkMenu.waitForLaunchError())
                .as("EPBDS-16959: Benchmark must say the results window was blocked")
                .isEqualTo(WINDOW_BLOCKED);
        editor.offRequest(executionWatcher);
        assertThat(executionsStarted)
                .as("EPBDS-16959: no Test, Run or Benchmark must be started while the results window is blocked")
                .isEmpty();
    }

    @Test
    @TestCaseId({"IPBQA-33086", "EPBDS-16959"})
    @Description("EPBDS-16959: Stop in the results window ends running tests, a run of a test table and a benchmark, "
            + "and the window says it was stopped; the stopped benchmark measures nothing.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void stopEndsTestsRunAndBenchmark() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, LONG_RUN_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, LONG_RUN_MODULE);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();

        toolbar.clickTestDropdown().runTests();
        results.stop();
        assertThat(results.getResultsTextMatching(TESTS_STOPPED))
                .as("EPBDS-16959: Stop must end the tests and the window must say they were stopped")
                .contains(TESTS_STOPPED);

        selectTable(editorPage, TEST, LONG_TEST);
        toolbar.clickRun().clickRunInsideMenu();
        results.stop();
        assertThat(results.getResultsTextMatching(TESTS_STOPPED))
                .as("EPBDS-16959: Stop must end the run of a test table and the window must say it was stopped")
                .contains(TESTS_STOPPED);

        toolbar.clickBenchmarkStart();
        results.stop();
        assertThat(results.getResultsTextMatching(BENCHMARK_STOPPED))
                .as("EPBDS-16959: Stop must end the benchmark, which then measures nothing")
                .contains(BENCHMARK_STOPPED)
                .containsPattern("(?<!\\d)0 measurements");
    }

    @Test
    @TestCaseId({"IPBQA-33086", "EPBDS-16959"})
    @Description("EPBDS-16959: while the tests run, a table of the project opens and is edited at once, and the edit "
            + "stops the tests and drops their results: the window says the project changed since they started.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void editDuringTestsStopsThemAndDropsTheirResults() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, LONG_RUN_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, LONG_RUN_MODULE);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();

        toolbar.clickTestDropdown().runTests();
        assertThat(results.isRunning()).as("Precondition: the tests of the long run must be running").isTrue();

        selectTable(editorPage, METHOD, EDITED_RULE);
        toolbar.getEditTableBtn().click();
        editorPage.getCenterTable().editCell(RULE_BODY_ROW, RULE_BODY_COLUMN, "return n * 3;");
        assertThat(results.isRunning())
                .as("EPBDS-16959: a table must open and take an edit while the tests still run")
                .isTrue();

        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        assertThat(results.getResultsTextMatching("changed or reloaded"))
                .as("EPBDS-16959: the edit must stop the tests and the window must say the project changed")
                .contains(PROJECT_CHANGED);
    }

    private void assertResultsInOneWindowOnly(TestResultValidationComponent results, String launch) {
        assertThat(results.countResultWindows())
                .as("EPBDS-16959: %s must show its results in the one results window", launch)
                .isEqualTo(1);
        assertThat(results.isShownOverTheEditor())
                .as("EPBDS-16959: %s must show no results dialog and no Open in Window over the editor", launch)
                .isFalse();
    }

    private List<String> caseIds(int first, int last) {
        return IntStream.rangeClosed(first, last).mapToObj(String::valueOf).toList();
    }

    private File saveTestResultsOfCalculate(EditorPage editorPage, String projectName, String module) {
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, module);
        selectTable(editorPage, SPREADSHEET, "calculate");
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        RunTestsMenuComponent testMenu = toolbar.getRunTestsMenu();
        toolbar.clickTableActionsTestDropdown();
        return DownloadUtil.downloadFile(testMenu::clickTestIntoFile);
    }

    @Test
    @TestCaseId("EPBDS-16615")
    @Description("EPBDS-16615: DetermineDriverPremium of Example 3 has its tests in another module, so with Within Current "
            + "Module Only both Test into File and Save to Excel of the results give a file, not Internal server error.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testResultsOfTableWithoutTestsInItsModuleAreSavedToFile() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, EXAMPLE_3);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, EXAMPLE_3_MODULE);
        selectTable(editorPage, SPREADSHEET, "DetermineDriverPremium");
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        RunTestsMenuComponent testMenu = toolbar.getRunTestsMenu();

        toolbar.clickTableActionsTestDropdown();
        testMenu.setWithinCurrentModuleOnly(true);
        File intoFile = DownloadUtil.downloadFile(testMenu::clickTestIntoFile);
        try {
            assertThat(intoFile.getName())
                    .as("EPBDS-16615: Test into File must give the results file, not Internal server error")
                    .endsWith(RESULTS_FILE);
            assertThat(intoFile).as("EPBDS-16615: the results file of Test into File must not be empty").isNotEmpty();
        } finally {
            DownloadUtil.cleanupDownloadFile(intoFile);
        }

        toolbar.clickTableActionsTestDropdown();
        testMenu.setWithinCurrentModuleOnly(true);
        testMenu.clickRunTestsButton();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        assertThat(results.getResultsTextMatching("No test results"))
                .as("EPBDS-16615: the tests are in another module, so the results must say there are none")
                .containsPattern("(?<!\\d)0 tests in");
        File saved = results.saveResultsToExcel();
        try {
            assertThat(saved.getName())
                    .as("EPBDS-16615: Save to Excel must give the results file, not Internal server error")
                    .endsWith(RESULTS_FILE);
            assertThat(saved).as("EPBDS-16615: the results file of Save to Excel must not be empty").isNotEmpty();
        } finally {
            DownloadUtil.cleanupDownloadFile(saved);
        }
    }

    private void selectTable(EditorPage editorPage, String folder, String table) {
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(folder)
                .selectItemInFolder(folder, table);
    }
}
