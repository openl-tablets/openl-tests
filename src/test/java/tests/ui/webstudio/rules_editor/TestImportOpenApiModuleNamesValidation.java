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

import static domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.PlanModule.DATA_TYPES;
import static org.assertj.core.api.Assertions.assertThat;

public class TestImportOpenApiModuleNamesValidation extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String TEMPLATE_NAME = "Example 1 - Bank Rating";
    private static final String SAME_MODULE_REFUSAL = "The rules and the data types need a module each; one module cannot hold both.";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Import OpenAPI Tables Generation mode: verify same-module-names validation error")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportOpenApiModuleNamesValidation() {
        String projectName = "TestOpenApiValidation_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, projectName, TEMPLATE_NAME);
        uploadFileToProject(repositoryPage, projectName, OPENAPI_FILE);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        ImportOpenApiDialogComponent importDialog = editorPage.openImportOpenApiDialog();

        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(OPENAPI_FILE);
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("SameModule");
        importDialog.setDataModuleName("SameModule");
        importDialog.clickImportTablesGeneration();

        OpenApiModuleSettingsDialogComponent settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();
        settingsDialog.setWorkbook(DATA_TYPES, "rules/SameModuleTypes.xlsx");
        settingsDialog.clickGenerate();

        assertThat(settingsDialog.getErrorMessagesUntilShown(SAME_MODULE_REFUSAL))
                .as("The generation should be refused when the rules and the data types name one module")
                .contains(SAME_MODULE_REFUSAL);
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
