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
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;
import java.util.List;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TEST;

public class TestTestTableRunMenuUi extends BaseTest {

    private static final String WORKBOOK = "UserErrorTestAndVarArgsCall.xlsx";
    private static final String MODULE = "UserErrorTestAndVarArgsCall";
    private static final String TEST_TABLE = "validateTest";
    private static final String FAILING_CASE = "1";
    private static final String PASSING_CASE = "2";
    private static final String RESULTS_FILE = "test-results.xlsx";
    private static final int RESULTS_FILE_MS = 60000;

    @Test
    @TestCaseId({"IPBQA-33073", "EPBDS-17218"})
    @Description("EPBDS-17218: a Test table offers Run, Trace and Benchmark but no Test; Run executes only the picked "
            + "cases, takes every case again when opened anew, and saves the results with Test into File.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTableIsRunFromItsRunMenu() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TEST)
                .selectItemInFolder(TEST, TEST_TABLE);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        SoftAssertions softly = new SoftAssertions();

        List<String> actions = toolbar.getTableActions();
        softly.assertThat(actions).as("A Test table must not offer Test, Run executes its cases").doesNotContain("Test");
        softly.assertThat(actions)
                .as("A Test table must offer exactly Edit, Copy, Remove, Run, Trace and Benchmark")
                .containsExactly("Edit", "Copy", "Remove", "Run", "Trace", "Benchmark");

        toolbar.clickRun();
        RunMenuComponent launcher = toolbar.getRunLauncher();
        launcher.pickCase(FAILING_CASE);
        softly.assertThat(launcher.isCasePicked(FAILING_CASE)).as("The case of 234 must be left out of the run").isFalse();
        launcher.clickRunInsideMenu();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        softly.assertThat(results.getShownCaseIds())
                .as("Run must execute only the picked case " + PASSING_CASE)
                .containsExactly(PASSING_CASE);
        softly.assertThat(results.isTestTablePassed())
                .as("With the case of 234 left out, " + TEST_TABLE + " must be marked passed")
                .isTrue();
        softly.assertThat(results.getPassedTestCount()).as("The case of 50 must carry passed marks").isPositive();
        softly.assertThat(results.getFailedTestCount()).as("No case may fail when the case of 234 is left out").isZero();
        results.closeResults();

        toolbar.clickRun();
        softly.assertThat(launcher.isAllCasesChecked())
                .as("Run opened again must take every case, the one left out before included")
                .isTrue();
        File saved = DownloadUtil.downloadFile(launcher::clickRunIntoFile, RESULTS_FILE_MS);
        try {
            softly.assertThat(saved.getName()).as("Test into File of the Run panel must save the results").endsWith(RESULTS_FILE);
            softly.assertThat(saved).as("The saved results file must not be empty").isNotEmpty();
        } finally {
            DownloadUtil.cleanupDownloadFile(saved);
        }

        softly.assertThat(toolbar.getTableActions())
                .as("The Test table must still not offer Test once its toolbar has long settled")
                .doesNotContain("Test");
        softly.assertAll();
    }
}
