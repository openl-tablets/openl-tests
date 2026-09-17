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

public class TestImportNewModulesWithPathEditingAndMixedScenarios extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String OPENAPI_FILE_1 = "openapi1.json";

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
        importDialog.setOpenApiFilePath(OPENAPI_FILE_1);
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Alg");
        importDialog.setDataModuleName(moduleName);
        importDialog.clickImportTablesGeneration();

        settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        assertThat(settingsDialog.getPlanLines())
                .as("The plan must name both modules and the workbook the project writes each into")
                .anySatisfy(line -> assertThat(line).contains("Services module: Alg", "rules/Alg.xlsx"))
                .anySatisfy(line -> assertThat(line).contains("Data types module: " + moduleName,
                        String.format("rules/%s.xlsx", moduleName)));

        // Where each module is written is the project's own answer now, named in the plan above; the
        // screen no longer offers it to be typed over. See KNOWN-ISSUES.md #10.
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
        settingsDialog.clickImportAndOverride();
        editorPage.waitUntilSpinnerLoaded();

        List<String> modules = editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName);
        assertThat(modules).as("Algorithms_test should still be present").contains("Algorithms_test");
        assertThat(modules).as("Models_test should still be present").contains("Models_test");
        assertThat(modules).as("Alg should be created").contains("Alg");
        assertThat(modules).as("Mod-123 should be created").contains(moduleName);

        assertThat(editorPage.getOpenApiMode()).isEqualTo("Tables generation");
        assertThat(editorPage.getOpenApiPropertyValue("File")).isEqualTo(OPENAPI_FILE_1);
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

        // One module is new and the other already stands, which the plan says a line each: what the plan
        // no longer says is carried by the button, which reads the same whatever it is about to do.
        assertThat(settingsDialog.getPlanLines())
                .as("Alg1 is written into a workbook of its own")
                .anySatisfy(line -> assertThat(line).contains("Services module: Alg1", "rules/Alg1.xlsx"));
        assertThat(settingsDialog.getPlanLines())
                .as("Mod-123 already exists, so the workbook it was written into is replaced")
                .contains(String.format("Data types module: %s — the workbook rules/%s.xlsx is replaced",
                        moduleName, moduleName));

        settingsDialog.clickCancel();
        importDialog.selectTablesGenerationMode();
        importDialog.setRulesModuleName("Alg");
        importDialog.setDataModuleName("Mod1");
        importDialog.clickImportTablesGeneration();

        settingsDialog = editorPage.getOpenApiModuleSettingsDialogComponent();
        settingsDialog.waitForVisible();

        assertThat(settingsDialog.getPlanLines())
                .as("Alg already exists, so the workbook the project wrote it into is replaced")
                .contains("Services module: Alg — the workbook rules/Alg.xlsx is replaced");
        assertThat(settingsDialog.getPlanLines())
                .as("Mod1 is written into a workbook of its own")
                .anySatisfy(line -> assertThat(line).contains("Data types module: Mod1", "rules/Mod1.xlsx"));

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

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
