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
import static domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.PlanModule.SERVICES;
import static org.assertj.core.api.Assertions.assertThat;

public class TestImportPathValidationErrors extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String TEMPLATE_NAME = "Example 1 - Bank Rating";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Path validation in the Generate tables dialog: workbook already held by the project, one workbook for both modules, non-Excel and empty workbook")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportPathValidationErrors() {
        String projectName = "TestPathValidation_" + System.currentTimeMillis();

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
        importDialog.clickImportReconciliation();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

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

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        importDialog = editorPage.openImportOpenApiDialog();
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Alg");
        importDialog.setDataModuleName("Mod");
        importDialog.clickImportTablesGeneration();

        OpenApiModuleSettingsDialogComponent settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        assertThat(settingsDialog.getModuleName(SERVICES))
                .as("The dialog must name the services module the specification is written into")
                .isEqualTo("Alg");
        assertThat(settingsDialog.getModuleName(DATA_TYPES))
                .as("The dialog must name the data types module the specification is written into")
                .isEqualTo("Mod");

        settingsDialog.setWorkbook(SERVICES, "rules/Bank Rating.xlsx");
        settingsDialog.clickGenerate();

        assertThat(settingsDialog.getErrorMessagesUntilShown(pathTaken("rules/Bank Rating.xlsx")))
                .as("A new services module cannot be written over a workbook the project already holds")
                .contains(pathTaken("rules/Bank Rating.xlsx"));

        settingsDialog.resetWorkbook(SERVICES);
        settingsDialog.setWorkbook(DATA_TYPES, "rules/Models.xlsx");
        settingsDialog.clickGenerate();

        assertThat(settingsDialog.getErrorMessagesUntilShown(pathTaken("rules/Models.xlsx")))
                .as("A new data types module cannot be written over a workbook the project already holds")
                .contains(pathTaken("rules/Models.xlsx"));

        settingsDialog.setWorkbook(SERVICES, "aaa.xlsx");
        settingsDialog.setWorkbook(DATA_TYPES, "aaa.xlsx");

        assertThat(settingsDialog.getSamePathError())
                .as("Both modules cannot be written to one workbook")
                .isEqualTo("The two modules cannot be written to one workbook");
        assertThat(settingsDialog.isGenerateEnabled())
                .as("Generate must not be offered while both modules name one workbook")
                .isFalse();

        settingsDialog.setWorkbook(DATA_TYPES, "aaa.txt");

        assertThat(settingsDialog.getWorkbookError(DATA_TYPES))
                .as("A module can only be written to an Excel workbook")
                .isEqualTo("A module is written to an Excel workbook: .xlsx, .xls or .xlsm");
        assertThat(settingsDialog.isGenerateEnabled())
                .as("Generate must not be offered while a workbook is not an Excel file")
                .isFalse();

        settingsDialog.clearWorkbook(DATA_TYPES);

        assertThat(settingsDialog.getWorkbookError(DATA_TYPES))
                .as("A module needs a workbook to be written to")
                .isEqualTo("Enter the workbook the module is written to");
        assertThat(settingsDialog.isGenerateEnabled())
                .as("Generate must not be offered while a workbook is empty")
                .isFalse();

        settingsDialog.clickCancel();
        importDialog.clickCancel();
    }

    private static String pathTaken(String path) {
        return String.format("The project already holds a file at '%s'. Name the module after the workbook it should read, "
                + "or move that file away.", path);
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
