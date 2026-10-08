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
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestImportOpenApiModuleNamesValidation extends BaseTest {

    private static final String OPENAPI_FILE = "openapi2.json";
    private static final String TEMPLATE_NAME = "Example 1 - Bank Rating";
    private static final String NAMES_SAME = "Module names cannot be the same.";

    @Test
    @TestCaseId("IPBQA-31035")
    @Description("Import OpenAPI Tables Generation mode: one name for the rules and the data types module, letter case aside, "
            + "is refused under both fields where it is entered and cannot be saved; two names can")
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
        assertThat(importDialog.getModuleNameErrors())
                .as("Both module fields should refuse one name for the rules and the data types")
                .containsExactly(NAMES_SAME, NAMES_SAME);
        assertThat(importDialog.isSaveEnabled()).as("Settings naming one module twice should not be saved").isFalse();

        importDialog.setDataModuleName("samemodule");
        assertThat(importDialog.getModuleNameErrors())
                .as("Names differing only in letter case should be refused as one name")
                .containsExactly(NAMES_SAME, NAMES_SAME);
        assertThat(importDialog.isSaveEnabled()).as("Settings naming one module twice should not be saved").isFalse();

        importDialog.setDataModuleName("SameModuleTypes");
        assertThat(importDialog.getModuleNameErrors()).as("Two module names should not be refused").isEmpty();
        assertThat(importDialog.isSaveEnabled()).as("Settings naming two modules should be saved").isTrue();
    }

    private void uploadFileToProject(RepositoryPage repositoryPage, String projectName, String fileName) {
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(fileName), fileName);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + fileName);
    }
}
