package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.SyncUpdatesDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

// EPBDS-16462: a long branch name widened the branch control past the 600px Sync updates dialog, so the
// label wrapped onto its own line and the dialog gained a horizontal scrollbar, and the full name was not
// available anywhere. The branch name below is the 45-character value the accompanying unit tests use.
// Requires an image containing a10fbcc440.
public class TestMergeDialogLongBranchLayoutUi extends BaseTest {

    private static final String PROJECT = "LongBranchLayout";
    private static final String ARCHIVE = "TestMergeBranchesNoConflicts_NoConflicts.zip";
    private static final String LONG_BRANCH = "Example3-AutoPolicyCalculation/openl/20260907";

    @Test
    @TestCaseId("EPBDS-16574")
    @Description("EPBDS-16462: a long branch name must not break the Sync updates dialog - the dialog must not "
            + "scroll sideways, the label must stay on one line with the field, and the full branch name must be "
            + "exposed as a title so the truncated text remains readable.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void longBranchNameKeepsTheSyncDialogIntact() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT, ARCHIVE);

        ProjectDetailPage detail = repositoryPage.openProjectDetail(PROJECT);
        detail.createBranch(LONG_BRANCH);

        SyncUpdatesDialogComponent sync = detail.openMergeDialog(LONG_BRANCH);

        // One tolerated pixel: a non-integer device pixel ratio rounds these apart on a dialog that fits.
        assertThat(sync.getBodyHorizontalOverflowPx())
                .as("The Sync updates dialog body must not scroll sideways: the branch control has to shrink "
                        + "into the 600px dialog instead of widening it")
                .isBetween(0L, 1L);
        assertThat(sync.getBranchFieldRowFlexWrap())
                .as("The form row holding the branch select must not wrap, so the label stays on one line "
                        + "with the field")
                .isEqualTo("nowrap");
        assertThat(sync.isFullBranchNameExposedAsTitle(LONG_BRANCH))
                .as("The truncated name must expose the full branch name as a title, otherwise a long name "
                        + "becomes unreadable once it is ellipsised")
                .isTrue();

        sync.close();
    }
}
