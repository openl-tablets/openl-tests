package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
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
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
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
import static domain.ui.webstudio.components.editortabcomponents.OpenApiModuleSettingsDialogComponent.NoticeTone.WARNING;
import static org.assertj.core.api.Assertions.assertThat;

public class TestImportNewModulesWithPathEditingAndMixedScenarios extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String OPENAPI_FILE_1 = "openapi1.json";
    private static final String NORMALIZED_SPEC = "openapi.json";
    private static final String OVERWRITTEN = "Warning! This module already exists and all of its content is going to be overwritten.";
    private static final String CREATED = "This module does not exist yet and is going to be created.";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Steps 4-5.2: Import new modules with path editing/reset, module names retained after cancel, mixed new/existing modules scenario.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportNewModulesWithPathEditingAndMixedScenarios() {
        String projectName = "TestNewModulesPath_" + System.currentTimeMillis();
        String moduleName = "Mod-123";

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
        settingsDialog.clickImportAndOverride();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        importDialog = editorPage.openImportOpenApiDialog();
        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(NORMALIZED_SPEC);
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Alg");
        importDialog.setDataModuleName(moduleName);
        importDialog.clickImportTablesGeneration();

        settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        assertThat(settingsDialog.getModulePlan(SERVICES))
                .as("Alg does not exist yet, so it is created in the proposed workbook")
                .isEqualTo(new ModulePlan(SECONDARY, CREATED, "Alg", "rules/Alg.xlsx"));
        assertThat(settingsDialog.getModulePlan(DATA_TYPES))
                .as("Mod-123 does not exist yet, so it is created in the proposed workbook")
                .isEqualTo(new ModulePlan(SECONDARY, CREATED, moduleName, String.format("rules/%s.xlsx", moduleName)));
        assertThat(settingsDialog.getGenerateButtonText())
                .as("The button says it generates when neither module exists yet")
                .isEqualTo("Generate tables");

        settingsDialog.setWorkbook(SERVICES, "rules1/Alg12.xlsx");
        assertThat(settingsDialog.getWorkbook(SERVICES))
                .as("The workbook of the new module Alg can be written over")
                .isEqualTo("rules1/Alg12.xlsx");
        settingsDialog.resetWorkbook(SERVICES);
        assertThat(settingsDialog.getWorkbook(SERVICES))
                .as("Reset puts the proposed workbook of Alg back")
                .isEqualTo("rules/Alg.xlsx");

        settingsDialog.setWorkbook(DATA_TYPES, "rules1/Mod1.xlsx");
        assertThat(settingsDialog.getWorkbook(DATA_TYPES))
                .as("The workbook of the new module Mod-123 can be written over")
                .isEqualTo("rules1/Mod1.xlsx");
        settingsDialog.resetWorkbook(DATA_TYPES);
        assertThat(settingsDialog.getWorkbook(DATA_TYPES))
                .as("Reset puts the proposed workbook of Mod-123 back")
                .isEqualTo(String.format("rules/%s.xlsx", moduleName));

        settingsDialog.clickCancel();
        importDialog.selectTablesGenerationMode();

        assertThat(importDialog.getRulesModuleName())
                .as("Rules module name 'Alg' should be retained after cancel")
                .isEqualTo("Alg");
        assertThat(importDialog.getDataModuleName())
                .as("Data module name 'Mod-123' should be retained after cancel")
                .isEqualTo(moduleName);

        importDialog.clickImportTablesGeneration();
        settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();
        settingsDialog.setWorkbook(SERVICES, "rules1/Alg12.xlsx");
        settingsDialog.setWorkbook(DATA_TYPES, "rules1/Mod1.xlsx");
        settingsDialog.clickImportAndOverride();
        editorPage.waitUntilSpinnerLoaded();

        List<String> modules = editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName);
        assertThat(modules).as("Algorithms_test should still be present").contains("Algorithms_test");
        assertThat(modules).as("Models_test should still be present").contains("Models_test");
        assertThat(modules).as("Alg should be created").contains("Alg");
        assertThat(modules).as("Mod-123 should be created").contains(moduleName);

        assertThat(editorPage.getOpenApiMode()).isEqualTo("Tables generation");
        assertThat(editorPage.getOpenApiPropertyValue("File")).isEqualTo(NORMALIZED_SPEC);
        assertThat(editorPage.getOpenApiPropertyValue("Services module")).isEqualTo("Alg");
        assertThat(editorPage.getOpenApiPropertyValue("Data types module")).isEqualTo(moduleName);

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        importDialog = editorPage.openImportOpenApiDialog();
        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(OPENAPI_FILE);
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Alg1");
        importDialog.clickImportTablesGeneration();

        settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        assertThat(settingsDialog.getModulePlan(SERVICES))
                .as("Alg1 does not exist yet, so it is created in the proposed workbook")
                .isEqualTo(new ModulePlan(SECONDARY, CREATED, "Alg1", "rules/Alg1.xlsx"));
        assertThat(settingsDialog.getModulePlan(DATA_TYPES))
                .as("Mod-123 already exists in the workbook entered for it, so the dialog warns that this workbook is overwritten")
                .isEqualTo(new ModulePlan(WARNING, OVERWRITTEN, moduleName, "rules1/Mod1.xlsx"));
        assertThat(settingsDialog.isWorkbookEditable(DATA_TYPES))
                .as("The workbook of a module the project already reads is stated, not offered for editing")
                .isFalse();
        assertThat(settingsDialog.getGenerateButtonText())
                .as("The button says it overwrites when one of the modules already exists")
                .isEqualTo("Generate and overwrite");

        settingsDialog.clickCancel();
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Alg");
        importDialog.setDataModuleName("Mod1");
        importDialog.clickImportTablesGeneration();

        settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        assertThat(settingsDialog.getModulePlan(SERVICES))
                .as("Alg already exists in the workbook entered for it, so the dialog warns that this workbook is overwritten")
                .isEqualTo(new ModulePlan(WARNING, OVERWRITTEN, "Alg", "rules1/Alg12.xlsx"));
        assertThat(settingsDialog.getModulePlan(DATA_TYPES))
                .as("Mod1 does not exist yet, so it is created in the proposed workbook")
                .isEqualTo(new ModulePlan(SECONDARY, CREATED, "Mod1", "rules/Mod1.xlsx"));

        settingsDialog.setWorkbook(DATA_TYPES, "rules3/Mod5.xlsx");
        settingsDialog.clickImportAndOverride();
        editorPage.waitUntilSpinnerLoaded();

        modules = editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName);
        assertThat(modules).as("Alg should remain in module list").contains("Alg");
        assertThat(modules).as("Mod-123 should remain in module list").contains(moduleName);
        assertThat(modules).as("Mod1 should be newly created").contains("Mod1");

        assertThat(editorPage.getOpenApiMode()).isEqualTo("Tables generation");
        assertThat(editorPage.getOpenApiPropertyValue("File")).isEqualTo(OPENAPI_FILE);
        assertThat(editorPage.getOpenApiPropertyValue("Services module")).isEqualTo("Alg");
        assertThat(editorPage.getOpenApiPropertyValue("Data types module")).isEqualTo("Mod1");
    }

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("A generation refused over a workbook path the default rules pattern already reads leaves no workbook of either module in the project.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue(value = "EPBDS-16743", failsWith = "The refused generation must not leave the workbook it was refused over")
    public void testRefusedGenerationWritesNoWorkbook() {
        String projectName = "TestRefusedGeneration_" + System.currentTimeMillis();
        String refusal = "The path 'rules/Alg12.xlsx' is already read by another module.";

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        createProjectFromSpecification(repositoryPage, projectName);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        ImportOpenApiDialogComponent importDialog = editorPage.openImportOpenApiDialog();
        importDialog.waitForFilePathField();
        importDialog.setOpenApiFilePath(NORMALIZED_SPEC);
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Alg");
        importDialog.setDataModuleName("Mod-123");
        importDialog.clickImportTablesGeneration();

        OpenApiModuleSettingsDialogComponent settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();
        assertThat(settingsDialog.getModulePlan(DATA_TYPES))
                .as("Mod-123 does not exist yet, so it is proposed a workbook of its own")
                .isEqualTo(new ModulePlan(SECONDARY, CREATED, "Mod-123", "rules/Mod-123.xlsx"));
        settingsDialog.setWorkbook(SERVICES, "rules/Alg12.xlsx");
        settingsDialog.clickGenerate();

        assertThat(settingsDialog.getErrorMessagesUntilShown(refusal))
                .as("The default rules pattern already reads rules/Alg12.xlsx as a module of its own, so the generation is refused")
                .contains(refusal);

        settingsDialog.clickCancel();
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(projectName);

        assertThat(projectDetail.openFilesTab().isFilePresent("Algorithms_test1.xlsx"))
                .as("The Files tab lists the workbooks inside the folders of the project")
                .isTrue();
        assertThat(projectDetail.isFilePresent("Alg12.xlsx"))
                .as("The refused generation must not leave the workbook it was refused over")
                .isFalse();
        assertThat(projectDetail.isFilePresent("Mod-123.xlsx"))
                .as("The refused generation must not leave the workbook of the other module")
                .isFalse();
    }

    private void createProjectFromSpecification(RepositoryPage repositoryPage, String projectName) {
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
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
