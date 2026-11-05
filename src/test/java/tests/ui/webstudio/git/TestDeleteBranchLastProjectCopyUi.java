package tests.ui.webstudio.git;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.api.RepositoryProjectsMethod;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.SyncChangesDialogComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.DeleteBranchModalComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDeleteBranchLastProjectCopyUi extends BaseTest {

    private static final String SOLO_PROJECT = "SoloBranchProject";
    private static final String SOLO_BRANCH = "solo-branch";
    private static final String DEFAULT_BRANCH = "master";
    private static final String LAST_BRANCH_WARNING =
            "No other branch contains this project, so deleting the branch deletes the project.";
    private static final String UNSAFE_CONFIRM_LABEL = "I understand the consequences, delete this branch";

    @Test
    @TestCaseId({"IPBQA-33016", "EPBDS-16410", "EPBDS-16411"})
    @Description("EPBDS-16410 The Sync button is offered in the Editor for a project that lives only in a non-default branch; "
            + "EPBDS-16411 the Sync updates dialog of that project offers the default branch to merge with and lets the updates be sent to it. "
            + "EPBDS-16378 Deleting the only branch that holds a project must warn that the project goes with it, require "
            + "the explicit unsafe confirmation, leave everything intact on Cancel, and on confirm delete the "
            + "branch together with the project while other projects survive.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDeletingLastProjectBranchWarnsAndDeletesTheProject() {
        String anchorProject = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        assertThat(new RepositoryProjectsMethod()
                .createProjectFromTemplate("design", SOLO_PROJECT, "Sample Project", SOLO_BRANCH).getStatusCode())
                .as("Fixture: the solo project must be created directly in its own branch")
                .isEqualTo(200);

        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.reloadPage();
        ProjectDetailPage detail = repositoryPage.openProjectDetail(SOLO_PROJECT);
        assertThat(detail.getCurrentBranch())
                .as("Precondition: the solo project must sit on its own branch")
                .isEqualTo(SOLO_BRANCH);

        EditorPage editor = new EditorPage();
        editor.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(SOLO_PROJECT);
        assertThat(editor.getEditorToolbarPanelComponent().isSyncButtonVisible())
                .as("The Sync button must be offered in the Editor for a project that exists only in the non-default branch")
                .isTrue();
        editor.getEditorToolbarPanelComponent().clickSync();
        SyncChangesDialogComponent syncDialog = repositoryPage.getSyncChangesDialogComponent();
        syncDialog.waitForDialogToAppear();
        syncDialog.selectBranch(DEFAULT_BRANCH);
        assertThat(WaitUtil.waitForCondition(syncDialog::isExportButtonEnabled, 10000, 250,
                "Waiting for Send your updates to be enabled for " + DEFAULT_BRANCH))
                .as("The default branch '%s' must be offered in Sync updates and accept the updates of a project created in '%s'",
                        DEFAULT_BRANCH, SOLO_BRANCH)
                .isTrue();
        syncDialog.clickCancel();

        repositoryPage = editor.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        detail = repositoryPage.openProjectsList().openProjectDetail(SOLO_PROJECT);

        DeleteBranchModalComponent dialog = detail.openDeleteBranchDialog();
        assertThat(dialog.isLastBranchWarningShown())
                .as("The dialog must surface the last-branch warning block")
                .isTrue();
        assertThat(dialog.getBodyText())
                .as("The dialog must warn that no other branch holds this project")
                .contains(LAST_BRANCH_WARNING);
        assertThat(dialog.getConfirmButtonLabel())
                .as("Deleting the last project copy must require the explicit unsafe confirmation")
                .isEqualTo(UNSAFE_CONFIRM_LABEL);

        dialog.clickCancel();
        repositoryPage.openProjectsList();
        assertThat(repositoryPage.isProjectPresent(SOLO_PROJECT))
                .as("Cancel must leave the project untouched")
                .isTrue();

        detail = repositoryPage.openProjectDetail(SOLO_PROJECT);
        detail.openDeleteBranchDialog().clickDelete();
        detail.waitUntilSpinnerLoaded();

        repositoryPage.openProjectsList();
        assertThat(repositoryPage.isProjectPresent(SOLO_PROJECT))
                .as("Deleting the only branch of the project must delete the project itself")
                .isFalse();
        assertThat(repositoryPage.isProjectPresent(anchorProject))
                .as("Projects that also live on other branches must survive the branch deletion")
                .isTrue();
    }
}
