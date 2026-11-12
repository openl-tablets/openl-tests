package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunMenuComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.DownloadUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TEST;
import static org.assertj.core.api.Assertions.assertThat;

public class TestTestTableRunsWithoutTestButtonUi extends BaseTest {

    private static final String WORKBOOK = "UserErrorTestAndVarArgsCall.xlsx";
    private static final String MODULE = "UserErrorTestAndVarArgsCall";
    private static final String TEST_TABLE = "validateTest";
    private static final String FAILING_CASE = "1";
    private static final String PASSING_CASE = "2";
    private static final String RESULTS_FILE = "test-results.xlsx";
    private static final int RESULTS_FILE_MS = 60000;

    @Test
    @TestCaseId({"IPBQA-33073", "EPBDS-17218"})
    @Description("EPBDS-17218: a Test table shows Run, Trace and Benchmark but no Test; Run executes the picked cases, "
            + "marks each passed or failed and saves the results with Test into File; a rule table and the module "
            + "toolbar still show Test.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTableRunsItsCasesWithoutTestButton() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TEST)
                .selectItemInFolder(TEST, TEST_TABLE);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();

        assertThat(toolbar.isRunButtonVisible()).as("A Test table must offer Run").isTrue();
        assertThat(toolbar.isTableActionsTestBtnVisible())
                .as("A Test table must not offer Test: Run already executes its cases")
                .isFalse();
        assertThat(toolbar.isTraceButtonVisible()).as("A Test table must still offer Trace").isTrue();
        assertThat(toolbar.isBenchmarkButtonVisible()).as("A Test table must still offer Benchmark").isTrue();

        toolbar.clickRun();
        RunMenuComponent launcher = toolbar.getRunLauncher();
        assertThat(launcher.getDrawnCaseCount()).as("Run must list both cases of " + TEST_TABLE).isEqualTo(2);
        assertThat(launcher.isAllCasesChecked()).as("Run must take every case unless told otherwise").isTrue();
        launcher.pickCase(FAILING_CASE);
        assertThat(launcher.isCasePicked(FAILING_CASE)).as("Case " + FAILING_CASE + " must be left out").isFalse();
        launcher.clickRunInsideMenu();

        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        assertThat(results.getShownCaseIds())
                .as("Run must execute only the picked case " + PASSING_CASE)
                .containsExactly(PASSING_CASE);
        assertThat(results.isTestTablePassed())
                .as("The case of 50 must pass, so " + TEST_TABLE + " must be marked passed")
                .isTrue();
        assertThat(results.getPassedTestCount()).as("The case of 50 must carry passed marks").isPositive();
        assertThat(results.getFailedTestCount()).as("No case may fail when case 234 is left out").isZero();
        results.closeResults();

        toolbar.clickRun();
        assertThat(toolbar.getRunLauncher().isAllCasesChecked())
                .as("Run opened again must take every case, the one left out before included")
                .isTrue();
        toolbar.getRunLauncher().clickRunInsideMenu();
        assertThat(results.getShownCaseIds())
                .as("Run with every case taken must execute both cases")
                .hasSize(2);
        assertThat(results.isTestTableFailed())
                .as("Run of both cases must mark " + TEST_TABLE + " failed: the rule throws Too big for 234")
                .isTrue();
        assertThat(results.getTestResult(1).get(2).replaceAll("\\s+", " "))
                .as("The failed case of 234 must show its expected value next to the result")
                .contains("Expected: 234");
        assertThat(results.getTestResult(2).stream().map(String::trim).toList())
                .as("The case of 50 must show the values 50 and 100")
                .contains("50", "100");
        results.closeResults();

        toolbar.clickRun();
        File saved = DownloadUtil.downloadFile(() -> toolbar.getRunLauncher().clickRunIntoFile(), RESULTS_FILE_MS);
        try {
            assertThat(saved.getName()).as("Test into File of the Run panel must save the results").endsWith(RESULTS_FILE);
            assertThat(saved).as("The saved results file must not be empty").isNotEmpty();
        } finally {
            DownloadUtil.cleanupDownloadFile(saved);
        }

        toolbar.clickTargetTable();
        assertThat(toolbar.getAvailableTestRunsLinkText())
                .as("The rule table must list " + TEST_TABLE + " among its tests")
                .contains(TEST_TABLE + " (2 test cases)");
        assertThat(toolbar.isTableActionsTestBtnVisible())
                .as("A rule table covered by " + TEST_TABLE + " must still offer Test")
                .isTrue();
        toolbar.clickTableActionsTestBtn();
        assertThat(results.getShownCaseIds())
                .as("Test of the rule table must run both cases of " + TEST_TABLE)
                .hasSize(2);
        assertThat(results.isTestTableFailed())
                .as("Test of the rule table must report " + TEST_TABLE + " failed, as its case of 234 fails")
                .isTrue();
        results.closeResults();

        assertThat(toolbar.isTestButtonVisible()).as("The module toolbar must still offer Test").isTrue();
    }
}
