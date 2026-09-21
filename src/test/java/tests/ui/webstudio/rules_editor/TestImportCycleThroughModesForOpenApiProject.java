package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.ImportOpenApiDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.CONFIGURATION;

public class TestImportCycleThroughModesForOpenApiProject extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String OPENAPI_FILE_3 = "openapi3.json";
    private static final String NORMALIZED_SPEC = "openapi.json";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Steps 6-6.3: Project created from openapi3.json – cycle through Reconciliation and Tables Generation modes with different files")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportCycleThroughModesForOpenApiProject() {
        String projectName = "TestCycleModes_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProjectFromOpenApi(OPENAPI_FILE_3, projectName);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        assertThat(editorPage.getOpenApiMode()).isEqualTo("Reconciliation");
        assertThat(editorPage.getOpenApiPropertyValue("File")).isEqualTo(NORMALIZED_SPEC);
        assertThat(editorPage.hasOpenApiProperty("Services module")).isFalse();
        assertThat(editorPage.hasOpenApiProperty("Data types module")).isFalse();

        ImportOpenApiDialogComponent importDialog = editorPage.openImportOpenApiDialog();
        importDialog.clickImportReconciliation();
        editorPage.waitUntilSpinnerLoaded();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);

        assertThat(editorPage.getOpenApiMode()).isEqualTo("Reconciliation");
        assertThat(editorPage.getOpenApiPropertyValue("File")).isEqualTo(NORMALIZED_SPEC);

        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        uploadFileToProject(repositoryPage, projectName, OPENAPI_FILE);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        importDialog = editorPage.openImportOpenApiDialog();
        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(OPENAPI_FILE);
        importDialog.selectTablesGenerationMode();
        importDialog.clickImportTablesGeneration();

        OpenApiModuleSettingsDialogComponent settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        assertThat(settingsDialog.getPlanLines())
                .as("Both modules already stand, so their workbooks are replaced")
                .contains("Services module: Algorithms — the workbook rules/Algorithms.xlsx is replaced", "Data types module: Models — the workbook rules/Models.xlsx is replaced");

        settingsDialog.clickImportAndOverride();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);

        assertThat(editorPage.getOpenApiMode()).isEqualTo("Tables generation");
        assertThat(editorPage.getOpenApiPropertyValue("File")).isEqualTo(OPENAPI_FILE);
        assertThat(editorPage.getOpenApiPropertyValue("Services module")).isEqualTo("Algorithms");
        assertThat(editorPage.getOpenApiPropertyValue("Data types module")).isEqualTo("Models");

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Algorithms");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Models");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        importDialog = editorPage.openImportOpenApiDialog();
        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(NORMALIZED_SPEC);
        importDialog.selectTablesGenerationMode();
        importDialog.clickImportTablesGeneration();

        settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();
        settingsDialog.clickImportAndOverride();

        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "Algorithms");
        editorPage.getProblemsPanelComponent()
                .waitForCompilationToComplete();
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);

        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Spreadsheet"))
                .as("Algorithms should contain Spreadsheet tables after openapi3.json import")
                .isTrue();
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree(CONFIGURATION))
                .as("Algorithms should contain Configuration tables after openapi3.json import")
                .isTrue();
        editorPage.getProblemsPanelComponent().checkNoProblems();
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
