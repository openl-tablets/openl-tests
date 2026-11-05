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
