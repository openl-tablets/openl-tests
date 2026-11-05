package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunTestsMenuComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.DownloadUtil;
import helpers.utils.WaitUtil;
import helpers.utils.XlsxUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;
import java.util.List;
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
    private static final String KEYED_LIST_WORKBOOK = "ListByPrimaryKey.xlsx";
    private static final String KEYED_LIST_MODULE = "ListByPrimaryKey";
    private static final String UNKEYED_LIST_WORKBOOK = "ListByFirstColumn.xlsx";
    private static final String UNKEYED_LIST_MODULE = "ListByFirstColumn";
    private static final String MANY_CASES_WORKBOOK = "ManyCases.xlsx";
    private static final String MANY_CASES_MODULE = "ManyCases";
    private static final int CASES_PER_PAGE = 20;
    private static final int CASES_IN_TABLE = 45;

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
        });
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
