package tests.ui.webstudio.git;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.projectdetail.ProjectOverviewTabComponent;
import domain.ui.webstudio.components.repositorytabcomponents.OpenProjectDialogComponent;
import domain.ui.webstudio.components.repositorytabcomponents.SyncUpdatesDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectDependenciesAcrossBranchesUi extends BaseTest {

    private static final String CONSUMER = "Consumer";
    private static final String PROVIDER = "Provider";
    private static final String CONSUMER_ARCHIVE = "DependenciesAcrossBranches_Consumer.zip";
    private static final String PROVIDER_ARCHIVE = "DependenciesAcrossBranches_Provider.zip";
    private static final String SIDE = "side";
    private static final String MASTER = "master";
    private static final String FOLLOW = "follow";
    private static final String DEVELOPMENT = "development";
    private static final String OPENL_RATING = "openl-rating";
    private static final String DESIGN = "Design";
    private static final String IN_EDITING = "In Editing";
    private static final String NO_CHANGES = "No Changes";
    private static final String DEPENDS_ON = "Depends on";
    private static final String USED_BY = "Used by";
    private static final String NOT_FOUND = "Not Found";
    private static final Pattern REPOSITORY_BRANCHES = Pattern.compile(".*/projects/[^/]+/branches\\?scope=repository");

    @Test
    @TestCaseId({"EPBDS-16382", "EPBDS-16448"})
    @Description("EPBDS-16382: a dependency is looked up only in the branch of the project, and opening the project "
            + "names the dependency its branch lacks. EPBDS-16448: the Sync updates branch list spins, not No data, "
            + "while the branches are still being read.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void dependenciesStayInsideTheBranchOfTheProject() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, CONSUMER, CONSUMER_ARCHIVE);
        repositoryPage.openProjectDetail(CONSUMER).createBranch(SIDE, true);
        repositoryPage.openProjectsList().createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROVIDER, PROVIDER_ARCHIVE);

        ProjectDetailPage consumer = repositoryPage.openProjectsList().openProjectDetail(CONSUMER);
        assertThat(consumer.getOverviewTab().getDependencyRowText(DEPENDS_ON, PROVIDER))
                .as("Consumer on branch " + SIDE + " does not hold Provider, so Provider is Not Found instead of "
                        + "being taken from another branch")
                .contains(NOT_FOUND);

        consumer.switchBranch(MASTER);
        assertThat(consumer.getOverviewTab().getDependencyRowText(DEPENDS_ON, PROVIDER))
                .as("Consumer on branch " + MASTER + " holds Provider, so Provider is found")
                .doesNotContain(NOT_FOUND);
        consumer.switchBranch(SIDE);

        ProjectOverviewTabComponent provider = repositoryPage.openProjectsList().openProjectDetail(PROVIDER).getOverviewTab();
        assertThat(provider.getDependencyNames(USED_BY))
                .as("Provider is listed as used by Consumer")
                .contains(CONSUMER);

        repositoryPage.openProjectsList().closeProject(CONSUMER);
        repositoryPage.openProjectDetail(CONSUMER).clickHeaderAction("Open");
        OpenProjectDialogComponent openConsumer = new OpenProjectDialogComponent().waitForVisible();
        assertThat(openConsumer.getBranchWarning())
                .as("Opening Consumer on " + SIDE + " names Provider, which that branch does not hold")
                .contains(PROVIDER)
                .contains(NOT_FOUND);
        openConsumer.cancel();

        assertSyncBranchListSpinsWhileBranchesAreRead(repositoryPage.openProjectsList().openProjectDetail(PROVIDER));
    }

    @Test
    @TestCaseId("EPBDS-8462")
    @Description("EPBDS-8462: opening a project whose dependency of the same repository is on another branch offers, "
            + "checked by default, to open the dependency on the branch of the project, and opens it there.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void openingAProjectTakesItsDependenciesToItsBranch() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, CONSUMER, CONSUMER_ARCHIVE);
        repositoryPage.openProjectsList().createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROVIDER, PROVIDER_ARCHIVE);
        assertThat(repositoryPage.openProjectsList().openProjectDetail(PROVIDER).createBranch(SIDE).getCurrentBranch())
                .as("Provider stays on " + MASTER + " after " + SIDE + " is created")
                .isEqualTo(MASTER);

        repositoryPage.openProjectsList().closeProject(CONSUMER);
        ProjectDetailPage consumer = repositoryPage.openProjectDetail(CONSUMER).switchBranch(SIDE);
        assertThat(consumer.getCurrentBranch())
                .as("The closed Consumer is on " + SIDE)
                .isEqualTo(SIDE);
        consumer.clickHeaderAction("Open");
        OpenProjectDialogComponent openConsumer = new OpenProjectDialogComponent().waitForVisible();
        assertThat(openConsumer.isSwitchDependenciesShown())
                .as("Opening Consumer on " + SIDE + " offers to open Provider, which is on " + MASTER + ", on " + SIDE)
                .isTrue();
        assertThat(openConsumer.isSwitchDependenciesChecked())
                .as("The offer to open Provider on " + SIDE + " is checked by default")
                .isTrue();
        openConsumer.open();
        assertThat(consumer.waitForStatus(NO_CHANGES))
                .as("Consumer is opened")
                .isEqualTo(NO_CHANGES);

        ProjectDetailPage provider = repositoryPage.openProjectsList().openProjectDetail(PROVIDER);
        assertThat(provider.getCurrentBranch())
                .as("Provider is on " + SIDE + ", the branch of Consumer")
                .isEqualTo(SIDE);
        assertThat(provider.waitForStatus(NO_CHANGES))
                .as("Provider is opened")
                .isEqualTo(NO_CHANGES);
    }

    @Test
    @TestCaseId("EPBDS-8462")
    @Description("EPBDS-8462: copying an open project with unsaved changes into a new branch asks to discard them, "
            + "and the project takes its dependency of the same repository to the new branch.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void copyingAnOpenProjectIntoANewBranchTakesItsDependencies() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, CONSUMER, CONSUMER_ARCHIVE);
        repositoryPage.openProjectsList().createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROVIDER, PROVIDER_ARCHIVE);

        ProjectDetailPage consumer = repositoryPage.openProjectsList().openProjectDetail(CONSUMER)
                .editOverviewDescriptionAndSave("Changed before the copy");
        assertThat(consumer.waitForStatus(IN_EDITING))
                .as("Consumer has unsaved changes before it is copied into a new branch")
                .isEqualTo(IN_EDITING);

        consumer.copyIntoNewBranchDiscardingChanges(FOLLOW);
        assertThat(consumer.waitForStatus(NO_CHANGES))
                .as("Moving Consumer onto the new branch " + FOLLOW + " discards its unsaved changes")
                .isEqualTo(NO_CHANGES);
        assertThat(repositoryPage.openProjectsList().openProjectDetail(CONSUMER).getOverviewTab()
                .getDependencyRowText(DEPENDS_ON, PROVIDER))
                .as("Provider of the same repository moves to " + FOLLOW + " together with Consumer")
                .contains(FOLLOW);
        assertThat(repositoryPage.openProjectsList().openProjectDetail(PROVIDER).getCurrentBranch())
                .as("Provider itself is on " + FOLLOW)
                .isEqualTo(FOLLOW);
    }

    @Test
    @TestCaseId("EPBDS-8462")
    @Description("EPBDS-8462: a dependency of another design repository keeps its branch when the project moves to "
            + "a new branch, and opening the project does not offer to move it.")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_REF_REPOSITORIES_PARAMS)
    public void dependencyOfAnotherRepositoryKeepsItsBranch() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProjectFromZipWithSelectRepo(PROVIDER, PROVIDER_ARCHIVE, OPENL_RATING);
        repositoryPage.openProjectsList().createProjectFromZipWithSelectRepo(CONSUMER, CONSUMER_ARCHIVE, DESIGN);

        repositoryPage.openProjectsList().openProjectDetail(CONSUMER).createBranch(SIDE, true);
        assertThat(repositoryPage.openProjectsList().openProjectDetail(CONSUMER).getOverviewTab()
                .getDependencyRowText(DEPENDS_ON, PROVIDER))
                .as("Provider of " + OPENL_RATING + " is found and stays on " + DEVELOPMENT + " while Consumer moves to "
                        + SIDE)
                .contains(DEVELOPMENT)
                .doesNotContain(SIDE, NOT_FOUND);
        assertThat(repositoryPage.openProjectsList().openProjectDetail(PROVIDER).getCurrentBranch())
                .as("Provider itself is still on " + DEVELOPMENT)
                .isEqualTo(DEVELOPMENT);

        repositoryPage.openProjectsList().closeProject(CONSUMER);
        repositoryPage.openProjectDetail(CONSUMER).clickHeaderAction("Open");
        OpenProjectDialogComponent openConsumer = new OpenProjectDialogComponent().waitForVisible();
        assertThat(openConsumer.isSwitchDependenciesShown())
                .as("Opening Consumer on " + SIDE + " does not offer to move Provider of another repository")
                .isFalse();
        openConsumer.cancel();
    }

    private void assertSyncBranchListSpinsWhileBranchesAreRead(ProjectDetailPage detail) {
        Page page = DriverPool.getPage();
        AtomicReference<Route> held = new AtomicReference<>();
        AtomicBoolean taken = new AtomicBoolean();
        Consumer<Route> holdFirst = route -> {
            if (taken.compareAndSet(false, true)) {
                held.set(route);
            } else {
                route.resume();
            }
        };
        page.route(REPOSITORY_BRANCHES, holdFirst);
        try {
            detail.clickHeaderAction("Sync");
            page.waitForCondition(() -> held.get() != null);

            SyncUpdatesDialogComponent sync = new SyncUpdatesDialogComponent().openTargetBranchList();
            WaitUtil.waitForCondition(sync::isTargetBranchListSpinning, 5000, 250,
                    "Waiting for the branch list of the Sync updates dialog to show it is reading");
            assertThat(sync.isTargetBranchListSpinning())
                    .as("While the branches are still being read the list spins")
                    .isTrue();
            assertThat(sync.isTargetBranchListEmptyStateShown())
                    .as("While the branches are still being read the list does not claim there is no data")
                    .isFalse();

            held.getAndSet(null).resume();
            WaitUtil.waitForCondition(() -> sync.getOfferedTargetBranches().contains(SIDE), 15000, 250,
                    "Waiting for the branch list of the Sync updates dialog to offer " + SIDE);
            assertThat(sync.getOfferedTargetBranches())
                    .as("Once the branches have been read the list offers them")
                    .contains(SIDE);
            sync.close();
        } finally {
            Route pending = held.getAndSet(null);
            if (pending != null) {
                pending.resume();
            }
            page.unroute(REPOSITORY_BRANCHES, holdFirst);
        }
    }
}
