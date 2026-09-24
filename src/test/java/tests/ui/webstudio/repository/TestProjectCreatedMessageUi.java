package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
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
    @Description("Creating a project says so, as copying and deleting one do.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testProjectCreationSaysSo() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        String projectName = StringUtil.generateUniqueName("CreationNotice");
        repositoryPage.getCreateProjectLink().click();
        CreateNewProjectComponent wizard = repositoryPage.getCreateNewProjectComponent();
        wizard.createProjectFromTemplate("Sample Project", projectName, false);
        wizard.clickCreate();
        String createdTitle = "Project \"" + projectName + "\" created";
        boolean said = WaitUtil.waitForCondition(
                () -> wizard.getAllMessagesFullText().stream().anyMatch(message -> message.contains(createdTitle)),
                MESSAGE_BUDGET_MS, 250, "Waiting for the studio to say that the project was created");
        repositoryPage.fillCommitInfo();

        assertThat(new RepositoryPage().openProjectsList().isProjectPresent(projectName))
                .as("The wizard should have created the project, or there is nothing to be told about")
                .isTrue();
        assertThat(said)
                .as("Creating a project should be confirmed the way copying and deleting one are")
                .isTrue();
    }
}
