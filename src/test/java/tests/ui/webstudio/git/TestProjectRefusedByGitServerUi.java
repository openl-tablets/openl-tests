package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.GitContainerService;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectRefusedByGitServerUi extends BaseTest {

    private static final String GIT_CONTAINER_ALIAS = "git-container-refusing";
    private static final String REFUSING_REPOSITORY = "ReadOnly";

    private GitContainerService gitContainer;

    @Override
    protected void startAuxiliaryContainers() {
        gitContainer = new GitContainerService(GIT_CONTAINER_ALIAS).withArchivedRepository();
        gitContainer.start();
    }

    @Override
    protected void stopAuxiliaryContainers() {
        if (gitContainer != null) {
            gitContainer.stop();
            gitContainer = null;
        }
    }

    @Override
    protected Map<String, String> additionalContainerConfig() {
        return Map.of(
                "design-repository-configs", "design,readonly",
                "repository.readonly.name", REFUSING_REPOSITORY,
                "repository.readonly.$ref", "repo-git",
                "repository.readonly.uri", gitContainer.getInNetworkUrl(),
                "repository.readonly.local-repository-path", "${openl.home}/repositories/readonly",
                "repository.readonly.branch", "master",
                "repository.readonly.login", gitContainer.asRemote().login(),
                "repository.readonly.password", gitContainer.asRemote().password()
        );
    }

    @Test
    @TestCaseId({"EPBDS-17268", "EPBDS-8571"})
    @Description("A project created in an archived Git repository, whose server refuses the push, is not created and the error names the repository "
            + "and says the Git server refused the changes for the user (EPBDS-8571, the Create flow only: Delete, Save, Copy and Deploy are not covered); "
            + "the Repository field of the Create dialog keeps the repository the user picked when the repositories are read again after the failed Create, "
            + "and the next Create goes to that repository (EPBDS-17268)")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateInRefusingRepositoryKeepsRepositoryAndExplainsRefusal() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, StringUtil.generateUniqueName("Refused"), "Sample Project", false);
        CreateNewProjectComponent dialog = repositoryPage.getCreateNewProjectComponent();
        dialog.selectRepository(REFUSING_REPOSITORY);

        String refusal = dialog.submitExpectingRefusal();
        assertThat(refusal)
                .as("The error names the repository and tells the Git server refused the write (EPBDS-8571)")
                .contains("The project is not created in the '" + REFUSING_REPOSITORY + "' repository")
                .contains("The Git server refused the changes. It does not let the user '" + gitContainer.asRemote().login()
                        + "' write to the repository: check the password and the access of the user.");

        String otherProject = StringUtil.generateUniqueName("OtherSession");
        DriverPool.BrowserSession otherSession = DriverPool.openAnotherSession();
        try {
            DriverPool.actIn(otherSession, () -> {
                EditorPage otherEditor = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
                otherEditor.getTabSwitcherComponent().<RepositoryPage>selectTab(TabSwitcherComponent.TabName.REPOSITORY)
                        .createProject(CreateNewProjectComponent.TabName.TEMPLATE, otherProject, "Sample Project");
            });
        } finally {
            otherSession.close();
        }
        assertThat(WaitUtil.waitForCondition(() -> repositoryPage.getAllVisibleProjectsInTable().contains(otherProject),
                30000, 500, "Waiting for the project of the other session to be read into the list behind the dialog"))
                .as("The repositories are read again behind the open dialog")
                .isTrue();
        assertThat(dialog.getRepositorySelectValue())
                .as("The Repository field keeps the repository the user picked after the repositories are read again (EPBDS-17268)")
                .isEqualTo(REFUSING_REPOSITORY);
        assertThat(dialog.submitExpectingRefusalOfThisCreate())
                .as("The next Create goes to the repository the user picked (EPBDS-17268)")
                .contains("The project is not created in the '" + REFUSING_REPOSITORY + "' repository");
    }
}
