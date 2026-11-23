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
    private static final String DECIMALS_ZIP = "ExactDecimals.zip";
    private static final String DECIMALS_MODULE = "Rules";
    private static final String DIVIDE_TEST = "divideTest";
    private static final int EXPECTED_ROW = 4;
    private static final int EXPECTED_COLUMN = 3;
    private static final String QUOTIENT = "0.6666666666666666666666666666666667";

    @Test
    @TestCaseId({"EPBDS-16890", "EPBDS-17084"})
    @Description("EPBDS-16890: the results of a test table show a double as 6.4.0 did: 1.73*12 reads 20.76. The results "
            + "open in a separate window, and a second run shows its results in that window (EPBDS-16959, EPBDS-16868).")
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

        editorPage.getEditorToolbarPanelComponent().clickRun().clickRunInsideMenu();
        assertThat(results.getTestResult(1)).as("A second run shows its results again").contains("20.76");
        assertThat(results.countResultWindows())
                .as("The results of a second run are shown in the window already open, not in a new one")
                .isEqualTo(1);
    }

    @Test
    @TestCaseId("EPBDS-17043")
    @Description("EPBDS-17043: a failed comparison of a BigDecimal shows the actual value with all its digits: 22 "
            + "divided by 33 reads 0.6666666666666666666666666666666667, not the same 0.6666666666666666 as a double.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testResultsShowABigDecimalWithAllItsDigits() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, DECIMALS_ZIP);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, DECIMALS_MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TEST)
                .selectItemInFolder(TEST, DIVIDE_TEST);

        editorPage.getEditorToolbarPanelComponent().clickRun().clickRunInsideMenu();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        assertThat(results.getFailedTestCount())
                .as("The case of " + DIVIDE_TEST + " fails: it expects 0.6 of 22 divided by 33")
                .isPositive();
        assertThat(results.getTestResult(1))
                .as("EPBDS-17043: the failed case must show the actual quotient with all its digits")
                .anyMatch(cell -> cell.contains(QUOTIENT));
        results.closeResults();

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(EXPECTED_ROW, EXPECTED_COLUMN, QUOTIENT);
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        assertThat(editorPage.getCenterTable().getCellText(EXPECTED_ROW, EXPECTED_COLUMN))
                .as("EPBDS-17043: the saved expected value must keep all its digits")
                .isEqualTo(QUOTIENT);

        editorPage.getEditorToolbarPanelComponent().clickRun().clickRunInsideMenu();
        assertThat(results.getPassedTestCount())
                .as("EPBDS-17043: the case must pass once the expected value is written with all its digits")
                .isPositive();
        assertThat(results.getFailedTestCount())
                .as("EPBDS-17043: the saved expected value must not be rounded to a double, which fails the case")
                .isZero();
    }
}
