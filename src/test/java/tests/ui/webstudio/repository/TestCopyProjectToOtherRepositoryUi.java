package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.admincomponents.RepositoriesPageComponent;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.CopyProjectDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCopyProjectToOtherRepositoryUi extends BaseTest {

    private static final String SOURCE_PROJECT_TEMPLATE = "Example 1 - Bank Rating";
    private static final String COPIER = "copier";
    private static final String COPIER_PASSWORD = "copier";
    private static final String COPY_NAME = "CopiedByContributor";
    private static final String TARGET_REPOSITORY = "Design1";

    @Test
    @TestCaseId({"EPBDS-13668", "EPBDS-13866"})
    @Description("A user who may only view the source repository and create projects in a second, still empty design "
            + "repository copies a project into it: the copy appears in the second repository instead of failing "
            + "with 'Cannot copy the project: null' (EPBDS-13668; EPBDS-13866 is covered with these nearest current roles "
            + "and shows the same symptom).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCopyProjectIntoEmptyRepositoryAsViewerAndContributor() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, SOURCE_PROJECT_TEMPLATE);
        EditorPage editorPage = new EditorPage();
        RepositoriesPageComponent repositories = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToRepositoriesPage();
        repositories.addDesignRepository();
        repositories.applyChangesAndRelogin(User.ADMIN);

        UsersPageComponent users = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();
        users.clickAddUser()
                .setUsername(COPIER)
                .setPassword(COPIER_PASSWORD)
                .clickAddRoleBtn()
                .setRoleRepository(0, "Design")
                .setRole(0, "Viewer")
                .clickAddRoleBtn()
                .setRoleRepository(1, TARGET_REPOSITORY)
                .setRole(1, "Contributor")
                .saveUser();
        editorPage.openUserMenu().signOut();

        editorPage = new LoginService(DriverPool.getPage()).login(new UserData(COPIER, COPIER_PASSWORD));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        CopyProjectDialogComponent copyDialog = repositoryPage.clickCopyAction(projectName);
        copyDialog.setAsNewProject().setNewProjectName(COPY_NAME).selectRepository(TARGET_REPOSITORY).clickCopyButton();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();

        ProjectDetailPage detail = repositoryPage.openProjectsList().openProjectDetail(COPY_NAME);
        assertThat(detail.getOverviewRepository())
                .as("The copy must be created in the second design repository (EPBDS-13668, EPBDS-13866)")
                .isEqualTo(TARGET_REPOSITORY);
    }
}
