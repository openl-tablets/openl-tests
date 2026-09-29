package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.projectdetail.ProjectHistoryTabComponent;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRevisionsTechnicalAndSearchUi extends BaseTest {

    private static final String BRANCH = "Branch";
    private static final String MASTER = "master";

    @Test
    @TestCaseId("IPBQA-32483")
    @Description("Revisions of a Git project hide the commits that do not change it until technical revisions are shown, "
            + "draw them as technical without Open, and search finds revisions by comment and revision id")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTechnicalRevisionsAndSearch() {
        RepositoryPage repositoryPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN))
                .getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        String project = StringUtil.generateUniqueName("Revisions");
        String other = StringUtil.generateUniqueName("Neighbour");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, project, "Example 1 - Bank Rating");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, other, "Example 2 - Corporate Rating");
        String created = "Project " + project + " is created.";
        String otherCreated = "Project " + other + " is created.";

        ProjectHistoryTabComponent history = repositoryPage.openProjectsList().openProjectDetail(project).getHistoryTab();
        assertThat(history.isSearchOffered()).as("The Revisions tab of a Git project should offer a search").isTrue();
        assertThat(history.isTechnicalRevisionsSwitchOffered()).as("The Revisions tab should offer technical revisions").isTrue();
        assertShown(history::getShownRevisionDescriptions, List.of(created),
                "Only the revisions of the project itself are listed by default");

        history.showTechnicalRevisions(true);
        assertShown(history::getShownRevisionDescriptions, List.of(otherCreated, created),
                "Technical revisions add the commit of the neighbour project");
        assertThat(history.getShownTechnicalRevisionDescriptions())
                .as("The commit of the neighbour project is marked technical")
                .containsExactly(otherCreated);
        history.showTechnicalRevisions(false);

        String saved = "Revision to find";
        ProjectDetailPage detail = repositoryPage.openProjectsList().openProjectDetail(project);
        detail.editOverviewDescriptionAndSave("Changed for the revisions");
        repositoryPage.openProjectsList().saveProject(project, saved);
        history = repositoryPage.openProjectsList().openProjectDetail(project).getHistoryTab();
        assertShown(history::getShownRevisionDescriptions, List.of(saved, created), "A save adds a revision on top");
        assertThat(history.isOpenOffered(created)).as("An earlier revision of the project can be opened").isTrue();
        String createdId = history.getRevisionId(created);

        history.search("to find");
        assertShown(history::getShownRevisionDescriptions, List.of(saved), "The search finds a revision by its comment");
        history.search(createdId.substring(0, 7));
        assertShown(history::getShownRevisionDescriptions, List.of(created), "The search finds a revision by its short id");
        history.search(other);
        assertShown(history::getShownRevisionDescriptions, List.of(), "The neighbour commit is technical and hidden");
        assertThat(history.getEmptyStateText()).as("A search that finds nothing says so").isEqualTo("No revisions match");
        history.showTechnicalRevisions(true);
        assertShown(history::getShownRevisionDescriptions, List.of(otherCreated),
                "With technical revisions the search finds the neighbour commit");
        assertThat(history.isOpenOffered(otherCreated))
                .as("A technical revision that is not the current one still cannot be opened")
                .isFalse();
        history.search("");
        history.showTechnicalRevisions(false);

        String onBranch = "Change on the branch";
        detail = repositoryPage.openProjectsList().openProjectDetail(project).createBranch(BRANCH, true);
        detail.editOverviewDescriptionAndSave("Changed on the branch");
        repositoryPage.openProjectsList().saveProject(project, onBranch);
        detail = repositoryPage.openProjectsList().openProjectDetail(project).switchBranch(MASTER);
        detail.openMergeDialog(BRANCH).clickReceive();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();

        ProjectHistoryTabComponent merged = repositoryPage.openProjectsList().openProjectDetail(project).getHistoryTab();
        assertShown(merged::getShownRevisionDescriptions, List.of(onBranch, saved, created),
                "After the merge master lists the revision made on the branch");
        merged.showTechnicalRevisions(true);
        assertThat(WaitUtil.waitForCondition(() -> merged.getShownRevisionDescriptions().size() > 4, 10000, 300,
                "Waiting for the technical revisions to be listed"))
                .as("Technical revisions add the merge and the neighbour commit on master")
                .isTrue();
        assertThat(merged.getShownTechnicalRevisionDescriptions())
                .as("The merge commit and the neighbour commit are technical revisions of the project")
                .hasSize(2)
                .contains(otherCreated)
                .anyMatch(description -> description.contains(BRANCH));
    }

    private static void assertShown(Supplier<List<String>> shown, List<String> expected, String because) {
        WaitUtil.waitForCondition(() -> shown.get().equals(expected), 10000, 300, "Waiting until " + because);
        assertThat(shown.get()).as(because).containsExactlyElementsOf(expected);
    }
}
