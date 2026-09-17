package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import helpers.utils.WaitUtil;
import configuration.driver.DriverPool;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectCreatedMessageUi extends BaseTest {

    private static final int MESSAGE_BUDGET_MS = 10000;

    @Test
    @TestCaseId("EPBDS-16652")
    @Description("Creating a project says so, as copying and deleting one do. Fails on EPBDS-16652: the "
            + "wizard closes and the new project opens without a word about it.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16652")
    public void testProjectCreationSaysSo() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        String projectName = StringUtil.generateUniqueName("CreatedMessage");
        repositoryPage.getCreateProjectLink().click();
        repositoryPage.getCreateNewProjectComponent().createProjectFromTemplate("Sample Project", projectName);
        repositoryPage.fillCommitInfo();
        boolean said = WaitUtil.waitForCondition(
                () -> repositoryPage.getAllMessages().stream().anyMatch(TestProjectCreatedMessageUi::readsAsCreated),
                MESSAGE_BUDGET_MS, 250, "Waiting for the studio to say that the project was created");

        assertThat(new RepositoryPage().openProjectsList().isProjectPresent(projectName))
                .as("The wizard should have created the project, or there is nothing to be told about")
                .isTrue();
        assertThat(said)
                .as("Creating a project should be confirmed the way copying and deleting one are")
                .isTrue();
    }

    private static boolean readsAsCreated(String message) {
        String said = message.toLowerCase();
        return said.contains("creat") && !said.contains("error") && !said.contains("fail");
    }
}
