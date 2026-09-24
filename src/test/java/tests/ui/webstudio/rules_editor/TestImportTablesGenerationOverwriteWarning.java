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
import domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.ModulePlan;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.PlanModule.DATA_TYPES;
import static domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.PlanModule.SERVICES;
import static domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.NoticeTone.WARNING;
import static org.assertj.core.api.Assertions.assertThat;

public class TestImportTablesGenerationOverwriteWarning extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String OPENAPI_FILE_1 = "openapi1.json";
    private static final String OVERWRITTEN = "Warning! This module already exists and all of its content is going to be overwritten.";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Tables Generation import shows overwrite warning for existing modules; verify warning text, button label, and properties after import")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportTablesGenerationOverwriteWarning() {
        String projectName = "TestOpenApiOverwrite_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.getCreateProjectLink().click();
        CreateNewProjectComponent openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(OPENAPI_FILE_1);
        openApiComponent.setDataModuleName("Models_test");
        openApiComponent.setDataModulePath("rules2/Models_test2.xlsx");
        openApiComponent.setRulesModuleName("Algorithms_test");
        openApiComponent.setRulesModulePath("rules1/Algorithms_test1.xlsx");
        openApiComponent.setProjectName(projectName);
        openApiComponent.clickCreate();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();

        uploadFileToProject(repositoryPage, projectName, OPENAPI_FILE);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        ImportOpenApiDialogComponent importDialog = editorPage.openImportOpenApiDialog();
        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(OPENAPI_FILE);
        importDialog.clickImportReconciliation();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Algorithms_test");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        importDialog = editorPage.openImportOpenApiDialog();
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Algorithms_test");
        importDialog.setDataModuleName("Models_test");
        importDialog.clickImportTablesGeneration();

        OpenApiModuleSettingsDialogComponent settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        assertThat(settingsDialog.getModulePlan(SERVICES))
                .as("Algorithms_test already stands, so the dialog warns that the workbook the project declares for it is overwritten")
                .isEqualTo(new ModulePlan(WARNING, OVERWRITTEN, "Algorithms_test", "rules1/Algorithms_test1.xlsx"));
        assertThat(settingsDialog.getModulePlan(DATA_TYPES))
                .as("Models_test already stands, so the dialog warns that the workbook the project declares for it is overwritten")
                .isEqualTo(new ModulePlan(WARNING, OVERWRITTEN, "Models_test", "rules2/Models_test2.xlsx"));
        assertThat(settingsDialog.getGenerateButtonText())
                .as("The button says it overwrites when existing modules would be overwritten")
                .isEqualTo("Generate and overwrite");
        assertThat(settingsDialog.isVisible())
                .as("Settings dialog with Cancel button should be visible")
                .isTrue();

        settingsDialog.clickCancel();
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Algorithms_test");
        importDialog.setDataModuleName("Models_test");
        importDialog.clickImportTablesGeneration();
        editorPage.getOpenApiModuleSettingsDialogComponent().waitForVisible();
        editorPage.getOpenApiModuleSettingsDialogComponent().clickImportAndOverride();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Algorithms_test");
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        assertThat(editorPage.getOpenApiMode())
                .as("Mode should be 'Tables generation' after Tables Generation import with overwrite")
                .isEqualTo("Tables generation");
        assertThat(editorPage.getOpenApiPropertyValue("File"))
                .as("OpenAPI File should be 'openapi2.json'")
                .isEqualTo(OPENAPI_FILE);
        assertThat(editorPage.getOpenApiPropertyValue("Services module"))
                .as("Rules Module should remain 'Algorithms_test'")
                .isEqualTo("Algorithms_test");
        assertThat(editorPage.getOpenApiPropertyValue("Data types module"))
                .as("Data Module should remain 'Models_test'")
                .isEqualTo("Models_test");
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
