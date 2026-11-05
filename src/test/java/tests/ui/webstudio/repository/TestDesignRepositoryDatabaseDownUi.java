package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.RepositoriesPageComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.repositorytabcomponents.SaveProjectDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.DeployInfrastructureService;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.WaitUtil;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.HashMap;
import java.util.Map;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestDesignRepositoryDatabaseDownUi extends BaseTest {

    private static final Map<String, String> additionalContainerFiles = new HashMap<>();

    @Override
    protected Map<String, String> additionalContainerFiles() {
        return additionalContainerFiles;
    }

    private static final String DESIGN_REPOSITORY = "Design1";
    private static final String PROJECT_NAME = "DatabaseDownProject";
    private static final int SAVE_FAILURE_TIMEOUT_MS = 60_000;

    private DeployInfrastructureService deployInfra;

    @Override
    @BeforeMethod
    public void beforeMethod(ITestResult result) {
        additionalContainerFiles.clear();
        deployInfra = DeployInfrastructureService.builder().withPostgres().build();
        deployInfra.start();
        additionalContainerFiles.putAll(deployInfra.getFilesToCopy());
        super.beforeMethod(result);
    }

    @Override
    @AfterMethod
    public void afterMethod(ITestResult result) {
        super.afterMethod(result);
        if (deployInfra != null) {
            deployInfra.cleanup();
        }
    }

    @Test
    @TestCaseId("EPBDS-12207")
    @Description("EPBDS-12207: saving a project edited in the Editor while the database of its JDBC design repository is down "
            + "is refused with a message that names the repository that failed to store the project, not a generic failure")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testSaveWhileDesignRepositoryDatabaseIsDown() {
        PostgreSQLContainer<?> postgres = deployInfra.getPostgresContainer();
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));

        RepositoriesPageComponent repositories = editorPage.openUserMenu().navigateToAdministration().navigateToRepositoriesPage();
        repositories.addDesignRepository();
        repositories.setDesignRepositoryType("Database JDBC");
        String jdbcUrl = "jdbc:postgresql://postgres:5432/" + postgres.getDatabaseName();
        WaitUtil.requireCondition(() -> {
            repositories.setDesignRepositoryJdbcUrl(jdbcUrl);
            return jdbcUrl.equals(repositories.getDesignRepositoryUrl());
        }, 15_000, 500, "Waiting for the URL field to hold only the JDBC URL of the design repository");
        repositories.setSecureConnection(true);
        repositories.setDesignRepositoryLogin(postgres.getUsername());
        repositories.setDesignRepositoryPassword(postgres.getPassword());
        repositories.applyChangesAndRelogin(User.ADMIN);

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProjectFromTemplateWithSelectRepo(PROJECT_NAME, "Sample Project", DESIGN_REPOSITORY);

        postgres.stop();

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT_NAME, "Main");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "Hello");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(6, 2, "100");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        editorPage.getEditorToolbarPanelComponent().clickSave();
        SaveProjectDialogComponent saveDialog = new SaveProjectDialogComponent().waitForVisible();
        saveDialog.clickSubmit();

        assertThat(saveDialog.waitForFailureMessage(SAVE_FAILURE_TIMEOUT_MS))
                .as("EPBDS-12207: the refusal names the '%s' repository that failed to store the project", DESIGN_REPOSITORY)
                .startsWith("The project is not saved: the '" + DESIGN_REPOSITORY + "' repository failed to store it.");
    }
}
