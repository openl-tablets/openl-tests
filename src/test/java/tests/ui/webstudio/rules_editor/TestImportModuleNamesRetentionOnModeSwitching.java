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
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestImportModuleNamesRetentionOnModeSwitching extends BaseTest {

    private static final String OPENAPI_FILE_1 = "openapi1.json";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Verify module names are retained when switching between Reconciliation and Tables Generation modes in Import OpenAPI dialog")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testImportModuleNamesRetentionOnModeSwitching() {
        String projectName = "TestModulesRetention_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        // Create project from openapi1.json with custom module names and custom paths
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

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        // Step 1.1: Open dialog, switch to Tables Generation mode. Creating a project from a specification
        // no longer writes what to generate into, so the project names no module to start from and the
        // reader says where the tables are to go (EPBDS-16415).
        ImportOpenApiDialogComponent importDialog = editorPage.openImportOpenApiDialog();
        importDialog.selectTablesGenerationMode();

        assertThat(importDialog.getRulesModuleName())
                .as("The project names no module for the rules until a generation is asked for")
                .isEmpty();
        assertThat(importDialog.getDataModuleName())
                .as("The project names no module for the data types until a generation is asked for")
                .isEmpty();

        // Change module names, then switch modes and verify names are retained
        importDialog.setRulesModuleName("Algorithms_test_1");
        importDialog.setDataModuleName("Models_test_1");
        importDialog.selectReconciliationMode();
        importDialog.selectTablesGenerationMode();

        assertThat(importDialog.getRulesModuleName())
                .as("Rules module name should be retained as 'Algorithms_test_1' after Reconciliation/Generation mode switch")
                .isEqualTo("Algorithms_test_1");
        assertThat(importDialog.getDataModuleName())
                .as("Data module name should be retained as 'Models_test_1' after Reconciliation/Generation mode switch")
                .isEqualTo("Models_test_1");

        importDialog.clickCancel();
    }
}
