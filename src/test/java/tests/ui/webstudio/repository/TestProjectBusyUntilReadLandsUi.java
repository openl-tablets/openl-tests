package tests.ui.webstudio.repository;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Route;
import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectBusyUntilReadLandsUi extends BaseTest {

    private static final String SIDE = "side";
    private static final String MASTER = "master";
    private static final Pattern PROJECT_READ = Pattern.compile(".*/rest/projects/[^/?]+\\?(.*&)?include=status.*");
    private static final int ECHO_WINDOW_MS = 3500;
    private static final int READ_TIMEOUT_MS = 20000;

    @Test
    @TestCaseId({"EPBDS-16389", "EPBDS-16392"})
    @Description("EPBDS-16389: while the read behind a branch switch or a Close on the project page is slow, the project "
            + "shows a busy state and its other actions are blocked until the page shows the result. EPBDS-16392: "
            + "the buttons of a Close unlock only once the page shows the new status, not before the second read "
            + "that the change ping starts. The read of the project is held with a route, its data is not altered.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void projectStaysBusyUntilTheReadBehindItsActionIsOnScreen() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage detail = repositoryPage.openProjectDetail(projectName);
        detail.createBranch(SIDE);
        assertThat(detail.getCurrentBranch()).as("Precondition: the project stands on " + MASTER).isEqualTo(MASTER);

        assertProjectActionsEnabled(detail, "nothing is running");
        try (HeldReads held = new HeldReads(DriverPool.getPage())) {
            detail.startSwitchBranch(SIDE);
            held.awaitFirst("the read that follows the branch switch");
            assertProjectBusy(detail, "the read behind a branch switch is slow");
            assertThat(detail.isBusyWithBranchSwitch())
                    .as("The branch switcher says the switch is running while the read behind it is slow")
                    .isTrue();
            assertThat(detail.getShownBranch())
                    .as("The page still shows " + MASTER + " while the read of " + SIDE + " is held")
                    .isEqualTo(MASTER);

            held.releaseAll();
            WaitUtil.requireCondition(() -> SIDE.equals(detail.getShownBranch()), READ_TIMEOUT_MS, 250,
                    "Waiting for the page to show branch " + SIDE);
            assertProjectUnlocked(detail, "the branch switch has landed");
        }
        detail.waitUntilAppIdle();

        try (HeldReads held = new HeldReads(DriverPool.getPage())) {
            detail.clickHeaderAction("Close");
            held.awaitFirst("the read that follows the Close");
            assertProjectBusy(detail, "the read behind a Close is slow");

            held.awaitCount(2, ECHO_WINDOW_MS);
            held.releaseFirst();
            WaitUtil.requireCondition(() -> !detail.isBusy(), READ_TIMEOUT_MS, 250,
                    "Waiting for the project to unlock after the first read of the Close");
            assertThat(detail.isHeaderActionEnabled("Open"))
                    .as("When the project unlocks the page shows it as closed and offers Open, "
                            + "not the status from before the Close")
                    .isTrue();
        }
    }

    private static void assertProjectBusy(ProjectDetailPage detail, String when) {
        assertThat(detail.isBusy())
                .as("The project shows a busy state while " + when)
                .isTrue();
        assertThat(detail.isHeaderActionEnabled("Sync"))
                .as("Sync is blocked while " + when)
                .isFalse();
        assertThat(detail.isHeaderActionEnabled("Copy"))
                .as("Copy is blocked while " + when)
                .isFalse();
        assertThat(detail.isHeaderActionEnabled("Delete"))
                .as("Delete is blocked while " + when)
                .isFalse();
    }

    private static void assertProjectUnlocked(ProjectDetailPage detail, String when) {
        WaitUtil.requireCondition(() -> !detail.isBusy(), READ_TIMEOUT_MS, 250,
                "Waiting for the busy state to end once " + when);
        assertProjectActionsEnabled(detail, when);
    }

    private static void assertProjectActionsEnabled(ProjectDetailPage detail, String when) {
        for (String action : List.of("Sync", "Copy", "Delete")) {
            assertThat(detail.isHeaderActionEnabled(action))
                    .as(action + " is offered and enabled while " + when)
                    .isTrue();
        }
    }

    private static final class HeldReads implements AutoCloseable {
        private final Page page;
        private final List<Route> held = new CopyOnWriteArrayList<>();
        private volatile boolean released;
        private final Consumer<Route> hold = route -> {
            if (released) {
                route.resume();
            } else {
                held.add(route);
            }
        };

        HeldReads(Page page) {
            this.page = page;
            page.route(PROJECT_READ, hold);
        }

        void awaitFirst(String what) {
            if (!awaitCount(1, READ_TIMEOUT_MS)) {
                throw new AssertionError("Waiting for " + what + " to be held timed out after " + READ_TIMEOUT_MS + "ms");
            }
        }

        boolean awaitCount(int count, int timeoutMs) {
            long end = System.currentTimeMillis() + timeoutMs;
            while (held.size() < count && System.currentTimeMillis() < end) {
                page.waitForTimeout(100);
            }
            return held.size() >= count;
        }

        void releaseFirst() {
            held.remove(0).resume();
        }

        void releaseAll() {
            released = true;
            while (!held.isEmpty()) {
                releaseFirst();
            }
        }

        @Override
        public void close() {
            try {
                page.unroute(PROJECT_READ, hold);
                releaseAll();
            } catch (PlaywrightException ignored) {
                held.clear();
            }
        }
    }
}
