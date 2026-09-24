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
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.PlanModule.DATA_TYPES;
import static domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.PlanModule.SERVICES;
import static domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.NoticeTone.SECONDARY;
import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.CONFIGURATION;

public class TestImportTablesGenerationForCorporateRatingProject extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String TEMPLATE_CORPORATE = "Example 2 - Corporate Rating";
    private static final String CREATED = "This module does not exist yet and is going to be created.";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Step 14: Tables Generation import for Corporate Rating template project with openapi2.json – creates Algorithms and Models modules")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportTablesGenerationForCorporateRatingProject() {
        String projectName = "TestCorporate_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, projectName, TEMPLATE_CORPORATE);
        uploadFileToProject(repositoryPage, projectName, OPENAPI_FILE);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        ImportOpenApiDialogComponent importDialog = editorPage.openImportOpenApiDialog();
        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(OPENAPI_FILE);
        importDialog.selectTablesGenerationMode();
        importDialog.clickImportTablesGeneration();

        OpenApiModuleSettingsDialogComponent settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        assertThat(settingsDialog.getModulePlan(SERVICES))
                .as("Corporate Rating has no Algorithms module, so it is created in the proposed workbook")
                .isEqualTo(new ModulePlan(SECONDARY, CREATED, "Algorithms", "rules/Algorithms.xlsx"));
        assertThat(settingsDialog.getModulePlan(DATA_TYPES))
                .as("Corporate Rating has no Models module, so it is created in the proposed workbook")
                .isEqualTo(new ModulePlan(SECONDARY, CREATED, "Models", "rules/Models.xlsx"));
        assertThat(settingsDialog.getGenerateButtonText())
                .as("The button says it generates when neither module exists yet")
                .isEqualTo("Generate tables");

        settingsDialog.clickImportAndOverride();
        editorPage.waitUntilSpinnerLoaded();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);

        List<String> modules = editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName);
        assertThat(modules).as("Models module should be created").contains("Models");
        assertThat(modules).as("Corporate Rating should still be present").contains("Corporate Rating");
        assertThat(modules).as("Algorithms module should be created").contains("Algorithms");

        assertThat(editorPage.getOpenApiMode()).isEqualTo("Tables generation");
        assertThat(editorPage.getOpenApiPropertyValue("File")).isEqualTo(OPENAPI_FILE);
        assertThat(editorPage.getOpenApiPropertyValue("Services module")).isEqualTo("Algorithms");
        assertThat(editorPage.getOpenApiPropertyValue("Data types module")).isEqualTo("Models");

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Algorithms");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorLeftRulesTreeComponent().setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);

        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Spreadsheet"))
                .as("Algorithms should contain Spreadsheet tables").isTrue();
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree(CONFIGURATION))
                .as("Algorithms should contain Configuration tables").isTrue();
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
