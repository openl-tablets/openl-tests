package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCreateProjectFromOpenApiFormValidation extends BaseTest {

    private static final String JSON_FILE = "openapi.json";
    private static final String INVALID_FILENAME_YAML = "dg!@#$%^&()_+{}.yaml";
    private static final String NON_OPENAPI_FILE = "non_openapi_file.xlsx";
    private static final String INVALID_JSON_FILE_1 = "openapi_inv1.json";
    private static final String INVALID_JSON_FILE_2 = "openapi_inv2.json";

    @Test
    @TestCaseId("IPBQA-30678")
    @Description("Create Project from OpenAPI refuses a bad project name, a bad module name or path, "
            + "duplicate module names or paths and a malformed specification. A module path with forbidden "
            + "characters is refused with the file name it carries "
            + "(EPBDS-15407); the other refusals are reported with one generic message. A file whose own name "
            + "carries forbidden characters is taken, because the specification is kept under the name its "
            + "format reads as")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateProjectFromOpenApiFormValidation() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.getCreateProjectLink().click();
        CreateNewProjectComponent openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(INVALID_FILENAME_YAML);
        String oddlyNamedSpecProject = "oddSpecName_" + System.currentTimeMillis();
        openApiComponent.setProjectName(oddlyNamedSpecProject);
        openApiComponent.clickCreate();
        repositoryPage.fillCommitInfo();
        assertThat(repositoryPage.openProjectsList().openProjectDetail(oddlyNamedSpecProject)
                        .isFilePresent("openapi.yaml"))
                .as("A specification whose file name carries forbidden characters is kept as openapi.yaml")
                .isTrue();

        repositoryPage.openProjectsList();
        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(NON_OPENAPI_FILE);
        String nonSpecProject = "nonOpenApi";
        openApiComponent.setProjectName(nonSpecProject);
        openApiComponent.clickCreate();
        repositoryPage.fillCommitInfo();
        assertThat(repositoryPage.openProjectsList().isProjectPresent(nonSpecProject))
                .as("6.4.0 no longer rejects a non-specification file at this step")
                .isTrue();

        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(JSON_FILE);
        openApiComponent.setProjectName("test%?>");
        openApiComponent.clickCreate(false);
        assertThat(openApiComponent.getError())
                .as("Creating with forbidden characters in the project name should be refused")
                .contains("Something went wrong");

        openApiComponent.cancelCreation();
        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(JSON_FILE);
        openApiComponent.setProjectName("bla");
        openApiComponent.setDataModuleName("Models?*/test");
        openApiComponent.setDataModulePath("rules/Models.xlsx");
        openApiComponent.clickCreate(false);
        assertThat(openApiComponent.getError())
                .as("Creating with forbidden characters in a module name should be refused")
                .contains("Failed to create the project");

        openApiComponent.cancelCreation();
        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(JSON_FILE);
        openApiComponent.setProjectName("bla");
        openApiComponent.setDataModulePath("rules/Models?*test.xlsx");
        openApiComponent.clickCreate(false);
        assertThat(openApiComponent.getError())
                .as("Creating with forbidden characters in a module path should be refused with the file name")
                .contains("Invalid name 'Models?*test.xlsx' of a project file or folder");

        openApiComponent.cancelCreation();
        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(JSON_FILE);
        openApiComponent.setProjectName("bla_" + System.currentTimeMillis());
        openApiComponent.setRulesModuleName("Models");
        openApiComponent.clickCreate(false);
        repositoryPage.fillCommitInfo();
        assertThat(openApiComponent.getError())
                .as("Creating with two identical module names should be refused")
                .contains("Failed to create the project");

        openApiComponent.cancelCreation();
        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(JSON_FILE);
        openApiComponent.setProjectName("bla2_" + System.currentTimeMillis());
        openApiComponent.setDataModulePath("rules/Algorithms.xlsx");
        openApiComponent.clickCreate(false);
        assertThat(openApiComponent.getError())
                .as("Creating with two identical module paths should be refused")
                .contains("Failed to create the project");

        openApiComponent.cancelCreation();
        String invalidProjectName = "InvalidJsonStructure_" + System.currentTimeMillis();
        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(INVALID_JSON_FILE_1);
        openApiComponent.setProjectName(invalidProjectName);
        openApiComponent.clickCreate(false);
        assertThat(openApiComponent.getError())
                .as("Creating from a malformed specification (file 1) should be refused")
                .contains("Failed to create the project");

        openApiComponent.cancelCreation();
        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(INVALID_JSON_FILE_2);
        openApiComponent.setProjectName(invalidProjectName);
        openApiComponent.clickCreate(false);
        assertThat(openApiComponent.getError())
                .as("Creating from a malformed specification (file 2) should be refused")
                .contains("Failed to create the project");

        openApiComponent.cancelCreation();
        String step29ProjectName = "bla29_" + System.currentTimeMillis();
        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        openApiComponent.uploadOpenApiSpec(JSON_FILE);
        openApiComponent.setProjectName(step29ProjectName);
        openApiComponent.setDataModuleName("bla1");
        openApiComponent.setRulesModuleName("kek2");
        openApiComponent.clickCreate();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();

        repositoryPage.openProjectsList();
        repositoryPage.getCreateProjectLink().click();
        openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);
        assertThat(openApiComponent.getDataModuleName()).isNotEqualTo("bla1");
        assertThat(openApiComponent.getDataModulePath()).isEqualTo("rules/Models.xlsx");
        assertThat(openApiComponent.getRulesModuleName()).isNotEqualTo("kek2");
        assertThat(openApiComponent.getRulesModulePath()).isEqualTo("rules/Algorithms.xlsx");
        repositoryPage.getCreateNewProjectComponent().cancelCreation();
    }
}
