package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ResolveConflictsDialogComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ResolveConflictsDialogComponent.ConflictSide;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

// EPBDS-16462: on a modify/delete conflict the dialog offered all three Download links for every file,
// because file existence was taken from the first conflicted file and stored per revision rather than per
// file, so following the link for the missing side opened a 404. Covered here only through the UI on a real
// git repository; the per-side rendering is already asserted in ConflictResolutionStep.test.tsx and the API
// contract in ITEST task_EPBDS-16462.
// Requires an image containing a10fbcc440.
public class TestMergeConflictDeletedFileUi extends BaseTest {

    private static final String PROJECT = "DeletedFileConflict";
    private static final String ARCHIVE = "TestMergeBranchesNoConflicts_NoConflicts.zip";
    private static final String SIDE_BRANCH = "SideBranch";
    private static final String MASTER = "master";
    private static final String CONFLICTED_MODULE = "Module1";
    private static final String CONFLICTED_FILE = "Module1.xlsx";
    private static final String CONFLICTED_TABLE = "MySpr1";

    @Test
    @TestCaseId("EPBDS-16574")
    @Description("EPBDS-16462: when a file is modified in one branch and deleted in the other, the Resolve "
            + "Conflicts dialog must offer Download only for the sides that still hold the file, must show the "
            + "missing side as a plain label rather than a link, and must keep Compare available.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void deletedSideOffersNoDownloadLink() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT, ARCHIVE);

        ProjectDetailPage detail = repositoryPage.openProjectDetail(PROJECT);
        detail.createBranch(SIDE_BRANCH, true);

        editorPage = repositoryPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.EDITOR);
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT, CONFLICTED_MODULE);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Spreadsheet")
                .selectItemInFolder("Spreadsheet", CONFLICTED_TABLE);
        editorPage.getCenterTable().editCell(3, 1, "ChangedInSideBranch");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        toolbar.clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        toolbar.switchBranch(MASTER);
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        detail = repositoryPage.openProjectDetail(PROJECT);
        detail.deleteFile(CONFLICTED_FILE);
        assertThat(detail.isFilePresent(CONFLICTED_FILE))
                .as("The file must be gone from master before the merge, otherwise there is no modify/delete conflict")
                .isFalse();

        // Deleting a file only changes the workspace copy. Until the project is saved it stays in editing
        // state, and merging is refused with "the project is not in a valid state for merging".
        editorPage = repositoryPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.EDITOR);
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(PROJECT);
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        detail = repositoryPage.openProjectDetail(PROJECT);
        detail.openMergeDialog(SIDE_BRANCH).clickReceive();
        ResolveConflictsDialogComponent conflicts = repositoryPage.getResolveConflictsDialogComponent();
        conflicts.waitForDialogToAppear();

        assertThat(conflicts.isDownloadOffered(CONFLICTED_FILE, ConflictSide.YOURS))
                .as("The file was deleted in master, so no Download link may be offered for your version - "
                        + "following it is what used to open a 404")
                .isFalse();
        assertThat(conflicts.isDeletedStatusShown(CONFLICTED_FILE, ConflictSide.YOURS))
                .as("The missing side must be reported explicitly instead of silently offering nothing")
                .isTrue();
        assertThat(conflicts.isDeletedStatusRenderedAsButton(CONFLICTED_FILE, ConflictSide.YOURS))
                .as("The deleted status must be a plain label, not a clickable control")
                .isFalse();

        assertThat(conflicts.isDownloadOffered(CONFLICTED_FILE, ConflictSide.THEIRS))
                .as("The side branch still holds the file, so its Download link must be offered")
                .isTrue();
        assertThat(conflicts.isDownloadOffered(CONFLICTED_FILE, ConflictSide.BASE))
                .as("The base revision predates the deletion, so its Download link must be offered")
                .isTrue();
        assertThat(conflicts.isCompareOffered(CONFLICTED_FILE))
                .as("A deleted file must still be comparable")
                .isTrue();

        conflicts.resolveConflictUseTheirs();
        repositoryPage.waitUntilSpinnerLoaded();
        assertThat(detail.isFilePresent(CONFLICTED_FILE))
                .as("Resolving with their version must bring the file back into master")
                .isTrue();
    }
}
