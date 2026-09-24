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
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestClickOnOpenApiError extends BaseTest {

    @Test
    @TestCaseId("EPBDS-10252")
    @Description("Test clicking on OpenAPI error from the bottom problems panel by text content - Playwright version")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testClickOnOpenApiError() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN,
                "TestClickOnOpenApiError.zip");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "Algorithm");

        editorPage.getProblemsPanelComponent()
                .selectProblemByText("OpenAPI Reconciliation: Expected method is not found for path ");

        assertThat(editorPage.getShownErrors())
                .as("Opening the OpenAPI problem from the bottom panel should not end in an error shown to the user")
                .isEmpty();

        LogsUtil.inspectLogFile(AppContainerPool.get());
    }
}