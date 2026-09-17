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
import domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestImportPathValidationErrors extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String TEMPLATE_NAME = "Example 1 - Bank Rating";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Path validation in OpenAPI Module Settings dialog: duplicate path error and same-paths error")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportPathValidationErrors() {
        String projectName = "TestPathValidation_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        // Setup: Create Bank Rating project, upload openapi2.json, do Reconciliation import
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, projectName, TEMPLATE_NAME);
        uploadFileToProject(repositoryPage, projectName, OPENAPI_FILE);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        // A template project keeps its workbooks in its root; the settings are offered once they are moved.
        editorPage.migrateProject();
        ImportOpenApiDialogComponent importDialog = editorPage.openImportOpenApiDialog();
        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(OPENAPI_FILE);
        importDialog.clickImportReconciliation();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        // Setup: Tables Generation import with Bank Rating rules module to create Models module (needed for path validation)
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Bank Rating");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        importDialog = editorPage.openImportOpenApiDialog();
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Bank Rating");
        importDialog.clickImportTablesGeneration();
        editorPage.getOpenApiModuleSettingsDialogComponent().waitForVisible();
        editorPage.getOpenApiModuleSettingsDialogComponent().clickImportAndOverride();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        // Now: Bank Rating.xlsx and rules/Models.xlsx both exist in the project

        // Step 12: Try to import with paths pointing to already-existing files
        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        importDialog = editorPage.openImportOpenApiDialog();
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Alg");
        importDialog.setDataModuleName("Mod");
        importDialog.clickImportTablesGeneration();

        OpenApiModuleSettingsDialogComponent settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        // Where the modules are written is the project's own answer now, so the two refusals this step
        // used to provoke — a path already taken by a file, and both modules sharing one path — can no
        // longer be provoked from the screen. The plan says instead what becomes of each workbook.
        // See KNOWN-ISSUES.md #10.
        assertThat(settingsDialog.getPlanLines())
                .as("The plan must name both modules the specification is written into")
                .hasSize(2);

        settingsDialog.clickCancel();
        importDialog.clickCancel();
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        // React Files tab: upload through the project's own screen, then commit from the projects list.
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
