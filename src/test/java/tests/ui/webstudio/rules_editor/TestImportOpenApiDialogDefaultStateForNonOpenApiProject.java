package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.ImportOpenApiDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestImportOpenApiDialogDefaultStateForNonOpenApiProject extends BaseTest {

    private static final String TEMPLATE_NAME = "Example 1 - Bank Rating";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Import OpenAPI dialog default state for non-OpenAPI project: Reconciliation is default, file-not-found error shown on import")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportOpenApiDialogDefaultStateForNonOpenApiProject() {
        String projectName = "TestNonOpenApi_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, projectName, TEMPLATE_NAME);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        // Step 8: Open dialog and immediately cancel
        ImportOpenApiDialogComponent importDialog = editorPage.openImportOpenApiDialog();
        importDialog.clickCancel();

        // Step 8.1: Reopen dialog, verify Reconciliation mode is selected by default
        importDialog = editorPage.openImportOpenApiDialog();
        assertThat(importDialog.isVisible())
                .as("Import OpenAPI dialog should be visible")
                .isTrue();
        assertThat(importDialog.isReconciliationModeSelected())
                .as("Reconciliation mode should be selected by default for a non-OpenAPI project")
                .isTrue();
        importDialog.waitForFilePathField();
        importDialog.selectTablesGenerationMode();

        // Step 8.2: The settings point the project at a specification it already holds, chosen from the ones
        // it holds, so a project holding none is offered none — a file that is not there cannot be named
        // here at all, and the refusal the old dialog answered with has nothing left to refuse.
        assertThat(importDialog.getOfferedSpecifications())
                .as("A project holding no specification should be offered none to point at")
                .isEmpty();

        importDialog.clickCancel();
    }
}
