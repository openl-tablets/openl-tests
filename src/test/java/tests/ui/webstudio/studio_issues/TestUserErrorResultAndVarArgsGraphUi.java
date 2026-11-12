package tests.ui.webstudio.studio_issues;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.TableDependenciesGraphComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunMenuComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.DownloadUtil;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;
import java.util.List;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TEST;

public class TestUserErrorResultAndVarArgsGraphUi extends BaseTest {

    private static final String WORKBOOK = "UserErrorTestAndVarArgsCall.xlsx";
    private static final String MODULE = "UserErrorTestAndVarArgsCall";
    private static final String TEST_TABLE = "validateTest";
    private static final String FAILING_CASE = "1";
    private static final String PASSING_CASE = "2";
    private static final String RESULTS_FILE = "test-results.xlsx";
    private static final int RESULTS_FILE_MS = 60000;

    @Test
    @TestCaseId({"EPBDS-14801", "EPBDS-14623", "IPBQA-33073", "EPBDS-17218"})
    @Description("EPBDS-14801: a test case that expects cell values of a Spreadsheet which throws a user error shows "
            + "the expected value of each tested cell (234, 468) next to the error, not the Spreadsheet itself. "
            + "EPBDS-14623: Table Dependencies connects a table with the Spreadsheet it calls through varargs "
            + "(theCall calls mySPR(int[] param) as mySPR(myParam, myParam + 1)). "
            + "EPBDS-17218 (IPBQA-33073): a Test table offers Run, Trace and Benchmark but no Test; Run executes only "
            + "the picked cases and saves the results with Test into File.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testUserErrorExpectedValuesAndVarArgsDependency() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TEST)
                .selectItemInFolder(TEST, TEST_TABLE);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        SoftAssertions softly = new SoftAssertions();
        List<String> actions = toolbar.getTableActions();
        softly.assertThat(actions).as("EPBDS-17218: a Test table must not offer Test, Run executes its cases")
                .doesNotContain("Test");
        softly.assertThat(actions)
                .as("EPBDS-17218: a Test table must offer exactly Edit, Copy, Remove, Run, Trace and Benchmark")
                .containsExactly("Edit", "Copy", "Remove", "Run", "Trace", "Benchmark");
        toolbar.clickRun().clickRunInsideMenu();

        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        softly.assertThat(results.isTestTableFailed())
                .as("The test table fails: the Spreadsheet throws a user error for the case of 234")
                .isTrue();
        List<String> failedCase = results.getTestResult(1).stream().map(cell -> cell.replaceAll("\\s+", " ")).toList();
        softly.assertThat(failedCase.get(2))
                .as("The Check cell of the failed case shows the expected cell value 234, not the Spreadsheet")
                .contains("Expected: 234");
        softly.assertThat(failedCase.get(3))
                .as("The Doubled cell of the failed case shows the expected cell value 468, not the Spreadsheet")
                .contains("Expected: 468");
        softly.assertThat(failedCase.get(4))
                .as("The Error cell of the failed case shows the user error")
                .contains("Too big");
        List<String> passedCase = results.getTestResult(2).stream().map(String::trim).toList();
        softly.assertThat(passedCase)
                .as("The case of 50 gets no error and shows the values 50 and 100")
                .contains("50", "100");
        results.closeResults();

        toolbar.clickRun();
        RunMenuComponent launcher = toolbar.getRunLauncher();
        launcher.pickCase(FAILING_CASE);
        softly.assertThat(launcher.isCasePicked(FAILING_CASE))
                .as("EPBDS-17218: the case of 234 must be left out of the run")
                .isFalse();
        launcher.clickRunInsideMenu();
        softly.assertThat(results.getShownCaseIds())
                .as("EPBDS-17218: Run must execute only the picked case " + PASSING_CASE)
                .containsExactly(PASSING_CASE);
        softly.assertThat(results.isTestTablePassed())
                .as("EPBDS-17218: with the case of 234 left out, " + TEST_TABLE + " must be marked passed")
                .isTrue();
        softly.assertThat(results.getPassedTestCount())
                .as("EPBDS-17218: the case of 50 must carry passed marks")
                .isPositive();
        results.closeResults();

        toolbar.clickRun();
        softly.assertThat(launcher.isAllCasesChecked())
                .as("EPBDS-17218: Run opened again must take every case, the one left out before included")
                .isTrue();
        File saved = DownloadUtil.downloadFile(launcher::clickRunIntoFile, RESULTS_FILE_MS);
        try {
            softly.assertThat(saved.getName())
                    .as("EPBDS-17218: Test into File of the Run panel must save the results")
                    .endsWith(RESULTS_FILE);
            softly.assertThat(saved).as("EPBDS-17218: the saved results file must not be empty").isNotEmpty();
        } finally {
            DownloadUtil.cleanupDownloadFile(saved);
        }
        softly.assertThat(toolbar.getTableActions())
                .as("EPBDS-17218: the Test table must still not offer Test once its toolbar has long settled")
                .doesNotContain("Test");

        toolbar.clickMore().clickTableDependencies();
        TableDependenciesGraphComponent graph = new TableDependenciesGraphComponent(DriverPool.getPage());
        softly.assertThat(graph.getSummary())
                .as("The graph counts the call of theCall to mySPR and the test of validate as 2 dependencies")
                .containsPattern("\\b4 tables\\b")
                .containsPattern("\\b2 dependencies\\b");
        graph.selectTable("mySPR");
        softly.assertThat(graph.getUsedBy())
                .as("mySPR is used by theCall, which calls it through varargs")
                .containsExactly("theCall");
        softly.assertAll();
    }
}
