package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerPool;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.LogsUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCallRuleWithSpreadsheetResultConstructor extends BaseTest {

    @Test
    @TestCaseId("EPBDS-12238")
    @Description("Test call rule with spreadsheet result constructor")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCallRuleWithSpreadsheetResultConstructor() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "TestCallRuleWithSpreadsheetResultConstructor.xlsx");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "TestCallRuleWithSpreadsheetResultConstructor");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", "test");

        editorPage.getEditorToolbarPanelComponent().clickRun().clickRunInsideMenu();

        assertThat(editorPage.getTestResultValidationComponent().isTestTableFailed())
                .as("Test table '%s' should have failed status", "test")
                .isTrue();

        assertThat(editorPage.getShownErrors())
                .as("Running the test table should not end in an error shown to the user")
                .isEmpty();
        LogsUtil.inspectLogFile(AppContainerPool.get());
    }
}