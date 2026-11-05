package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunTestsMenuComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.DownloadUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;

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
