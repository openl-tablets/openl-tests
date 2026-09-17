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

public class TestImportOpenApiModuleNamesValidation extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String TEMPLATE_NAME = "Example 1 - Bank Rating";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Import OpenAPI Tables Generation mode: verify same-module-names validation error")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportOpenApiModuleNamesValidation() {
        String projectName = "TestOpenApiValidation_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        // Step 1: Create project from template and upload OpenAPI file
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, projectName, TEMPLATE_NAME);
        uploadFileToProject(repositoryPage, projectName, OPENAPI_FILE);

        // Step 2: Navigate to Editor and open Import OpenAPI dialog
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);



        ImportOpenApiDialogComponent importDialog = editorPage.openImportOpenApiDialog();

        // Step 3: Select "Uploaded in Repository", set file, Tables generation, same module names
        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(OPENAPI_FILE);
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("SameModule");
        importDialog.setDataModuleName("SameModule");
        importDialog.clickImportTablesGeneration();

        // Step 4: Two modules named the same are two workbooks named the same, which is what is refused —
        // and it is refused when the generation is asked for, not while the names are being written.
        OpenApiModuleSettingsDialogComponent settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();
        settingsDialog.clickImportAndOverride();

        assertThat(settingsDialog.getErrorMessages())
                .as("The generation should be refused when the rules and the data types name one workbook")
                .contains("The rules and the data types need a workbook each; one workbook cannot hold both.");

        settingsDialog.clickCancel();
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        // React Files tab: upload through the project's own screen, then commit from the projects list.
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
