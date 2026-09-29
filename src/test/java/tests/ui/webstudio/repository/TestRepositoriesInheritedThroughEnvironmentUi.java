package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.RepositoriesPageComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRepositoriesInheritedThroughEnvironmentUi extends BaseTest {

    private static final String RATING = "openl-rating";
    private static final String POLICY_LIFE = "openl-policy-life";
    private static final String INHERITED_SAVE = "EPBDS- {project-name} saved";
    private static final String NOTHING_UNSAVED = "Leaving a repository whose settings were only looked at should not ask to discard unsaved changes";

    @Test
    @TestCaseId("IPBQA-32308")
    @Description("Design repositories declared in environment variables with _REF_ inherit the settings of the templates "
            + "they refer to, two levels deep, keep their own overrides, and Admin shows what they inherit")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_REF_REPOSITORIES_PARAMS)
    public void testRepositoriesInheritSettingsThroughEnvironmentRefs() {
        EditorPage editorPage = login();
        RepositoriesPageComponent repositories = editorPage.openUserMenu().navigateToAdministration().navigateToRepositoriesPage();

        SoftAssertions softly = new SoftAssertions();
        repositories.selectDesignRepositoryByName(RATING);
        softly.assertThat(repositories.getDesignRepositoryType()).as("Type of %s", RATING).isEqualTo("Git");
        softly.assertThat(repositories.getDesignRepositoryUrl()).as("URL of %s", RATING)
                .isEqualTo("/opt/openl/local/repositories/openl-rating");
        softly.assertThat(repositories.getSettingValue("branch"))
                .as("%s should inherit the branch of design.repo-git", RATING).isEqualTo("development");
        softly.assertThat(repositories.getProtectedBranches())
                .as("%s should inherit the protected branches of design.repo-git", RATING).isEqualTo("master, release-*");
        assertSecondLevelInherited(softly, repositories, RATING, INHERITED_SAVE);

        repositories.selectDesignRepositoryLeavingChanges(POLICY_LIFE);
        softly.assertThat(repositories.getSettingValue("branch"))
                .as("%s should keep its own branch over the inherited one", POLICY_LIFE).isEqualTo("main");
        softly.assertThat(repositories.getProtectedBranches())
                .as("%s should keep its own protected branches", POLICY_LIFE).isEqualTo("test-override");
        assertSecondLevelInherited(softly, repositories, POLICY_LIFE, "Overridden-");
        softly.assertAll();

        String project = StringUtil.generateUniqueName("RefProject");
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProjectFromTemplateWithSelectRepo(project, "Example 1 - Bank Rating", RATING);
        assertThat(repositoryPage.openProjectsList().openProjectDetail(project).getCurrentBranch())
                .as("A project created in %s should be on the branch it inherits", RATING)
                .isEqualTo("development");
    }

    @Test
    @TestCaseId("EPBDS-16779")
    @Description("Looking at a design repository configured by environment variables and moving to another one "
            + "does not ask to discard changes that were never made. Fails on EPBDS-16779: the unsaved changes "
            + "dialog is shown.")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_REF_REPOSITORIES_PARAMS)
    @KnownIssue(value = "EPBDS-16779", failsWith = NOTHING_UNSAVED)
    public void testLookingAtEnvironmentRepositoryLeavesNothingUnsaved() {
        RepositoriesPageComponent repositories = login().openUserMenu().navigateToAdministration().navigateToRepositoriesPage()
                .selectDesignRepositoryByName(RATING);
        repositories.clickDesignRepositoryTab(POLICY_LIFE);
        assertThat(repositories.isUnsavedChangesPromptShown()).as(NOTHING_UNSAVED).isFalse();
    }

    private EditorPage login() {
        return new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
    }

    private void assertSecondLevelInherited(SoftAssertions softly, RepositoriesPageComponent repositories, String repository,
                                            String saveComment) {
        softly.assertThat(repositories.getSettingValue("newBranchTemplate"))
                .as("%s should inherit the default branch name through design.repo-git and repo-default.design.new-branch", repository)
                .isEqualTo("EPBDS-{username}");
        softly.assertThat(repositories.isSettingChecked("useCustomComments"))
                .as("%s should inherit custom comments through design.repo-git and repo-default.design.comment-template", repository)
                .isTrue();
        softly.assertThat(repositories.getSettingValue("defaultCommentSave"))
                .as("Save comment of %s", repository)
                .isEqualTo(saveComment);
    }
}
