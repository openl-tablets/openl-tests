package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerPool;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.LogsUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestClickDatatypeNotFoundError extends BaseTest {

    private static final String TABLE_THE_TYPE_IS_WRITTEN_IN = "SmartRule2";

    @Test
    @TestCaseId("EPBDS-11609")
    @Description("Test clicking on datatype not found error and validating tree selection")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testClickDatatypeNotFoundError() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, "TestClickDatatypeNotFoundError.zip");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "module_NJ");
        
        // Click on the datatype not found error in the problems panel
        editorPage.getProblemsPanelComponent().selectProblemByText("is not found.");

        // The message was raised in another module of the project, so following it opens that module and
        // the table the type was written in; both are read from the server before the tree can name it.
        WaitUtil.waitForCondition(
                () -> TABLE_THE_TYPE_IS_WRITTEN_IN.equals(editorPage.getEditorLeftRulesTreeComponent().getSelectedItemText()),
                60000, 500, "Waiting for the table the message was raised in to be selected in the tree");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getSelectedItemText())
                .as("Selected tree item should be 'SmartRule2'")
                .isEqualTo(TABLE_THE_TYPE_IS_WRITTEN_IN);

        LogsUtil.inspectLogFile(AppContainerPool.get());
    }
}