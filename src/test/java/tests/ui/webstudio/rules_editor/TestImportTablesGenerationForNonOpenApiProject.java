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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestImportTablesGenerationForNonOpenApiProject extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String TEMPLATE_NAME = "Example 1 - Bank Rating";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Tables Generation import for non-OpenAPI project: overwrite existing module and create new Data module; verify module list and properties.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportTablesGenerationForNonOpenApiProject() {
        String projectName = "TestNonOpenApiGen_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

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

        assertThat(editorPage.getOpenApiMode())
                .as("Mode should be 'Reconciliation' after reconciliation import")
                .isEqualTo("Reconciliation");
        assertThat(editorPage.getOpenApiPropertyValue("File"))
                .as("OpenAPI File should be 'openapi2.json'")
                .isEqualTo(OPENAPI_FILE);

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Bank Rating");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        assertThat(editorPage.getProblemsPanelComponent().hasErrors())
                .as("Bank Rating module should have reconciliation errors after openapi2.json Reconciliation import")
                .isTrue();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        importDialog = editorPage.openImportOpenApiDialog();
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Bank Rating");
        importDialog.clickImportTablesGeneration();

        OpenApiModuleSettingsDialogComponent settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();
        assertThat(settingsDialog.getPlanLines())
                .as("Bank Rating is a workbook the project already reads, so it is written over")
                .contains("Services module: Bank Rating — the workbook Bank Rating.xlsx is replaced");
        assertThat(settingsDialog.getPlanLines())
                .as("The data types are written into Models, in the workbook the project names for it")
                .anySatisfy(line -> assertThat(line).contains("Data types module: Models", "rules/Models.xlsx"));
        settingsDialog.clickImportAndOverride();

        editorPage.waitUntilAppIdle();
        editorPage.reloadPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        List<String> modules = editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName);
        assertThat(modules)
                .as("Models module should be present after Tables Generation import")
                .contains("Models");
        assertThat(modules)
                .as("Bank Rating module should be present after overwrite import")
                .contains("Bank Rating");
        assertThat(modules)
                .as("Algorithms module should not be present (was never created in this project)")
                .doesNotContain("Algorithms");

        assertThat(editorPage.getOpenApiMode())
                .as("Mode should be 'Tables generation'")
                .isEqualTo("Tables generation");
        assertThat(editorPage.getOpenApiPropertyValue("File"))
                .as("OpenAPI File should be 'openapi2.json'")
                .isEqualTo(OPENAPI_FILE);
        assertThat(editorPage.getOpenApiPropertyValue("Services module"))
                .as("Rules Module should be 'Bank Rating'")
                .isEqualTo("Bank Rating");
        assertThat(editorPage.getOpenApiPropertyValue("Data types module"))
                .as("Data Module should be 'Models'")
                .isEqualTo("Models");

        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Bank Rating");
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Models");
        editorPage.getProblemsPanelComponent().checkNoProblems();
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
