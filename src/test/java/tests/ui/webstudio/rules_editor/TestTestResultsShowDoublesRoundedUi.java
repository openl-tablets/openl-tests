package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TEST;
import static org.assertj.core.api.Assertions.assertThat;

public class TestTestResultsShowDoublesRoundedUi extends BaseTest {

    private static final String WORKBOOK = "DoubleResult.xlsx";
    private static final String MODULE = "DoubleResult";
    private static final String TEST_TABLE = "mySprTest";

    @Test
    @TestCaseId("EPBDS-16890")
    @Description("EPBDS-16890: the results of a test table show a double as 6.4.0 did: 1.73*12 reads 20.76.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testResultsShowADoubleRounded() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TEST)
                .selectItemInFolder(TEST, TEST_TABLE);

        editorPage.getEditorToolbarPanelComponent().clickRun().clickRunInsideMenu();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        assertThat(results.getPassedTestCount())
                .as("The case of " + TEST_TABLE + " must run and be marked passed")
                .isPositive();
        assertThat(results.getFailedTestCount())
                .as("The case of " + TEST_TABLE + " passes: a test takes 1.73*12 for 20.76")
                .isZero();
        assertThat(results.getTestResult(1))
                .as("The result of 1.73*12 must read 20.76, as 6.4.0 showed it")
                .contains("20.76");
    }
}
