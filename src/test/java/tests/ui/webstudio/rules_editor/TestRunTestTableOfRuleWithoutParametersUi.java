package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunMenuComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TEST;
import static org.assertj.core.api.Assertions.assertThat;

public class TestRunTestTableOfRuleWithoutParametersUi extends BaseTest {

    private static final String WORKBOOK = "RuleWithoutParameters.xlsx";
    private static final String MODULE = "RuleWithoutParameters";
    private static final String TEST_TABLE = "mySprTest";

    @Test
    @TestCaseId("EPBDS-16889")
    @Description("EPBDS-16889: Run on a test table of a rule that takes no parameters opens the launcher with the "
            + "case of the table and runs it; 6.4.0 ran the same table.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void runOfTestTableOfRuleWithoutParametersListsAndRunsItsCase() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TEST)
                .selectItemInFolder(TEST, TEST_TABLE);

        editorPage.getEditorToolbarPanelComponent().clickRunDropdown();
        assertThat(editorPage.getShownErrors())
                .as("Run on a test table of a rule without parameters must open its launcher, not an error page")
                .isEmpty();

        RunMenuComponent launcher = editorPage.getEditorToolbarPanelComponent().getRunLauncher();
        launcher.waitForLauncher();
        assertThat(launcher.getDrawnCaseCount())
                .as("The launcher must list the one case of " + TEST_TABLE)
                .isEqualTo(1);

        launcher.clickRunInsideMenu();
        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        assertThat(results.getPassedTestCount())
                .as("The case of " + TEST_TABLE + " must run and be marked passed")
                .isPositive();
        assertThat(results.getFailedTestCount())
                .as("Nothing of " + TEST_TABLE + " may fail: 2*12 is 24")
                .isZero();
    }
}
