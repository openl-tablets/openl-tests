package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.CompareExcelFilesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.CompareLocalChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorRevisionsTabComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.repositorytabcomponents.CompareGitRevisionsDialogComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ResolveConflictsDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.TestDataUtil;
import org.testng.SkipException;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDisplayChangedRowsCompareScreens extends BaseTest {

    private static final String BANK_RATING_FILE_1 = "Bank_Rating_1.xlsx";
    private static final String BANK_RATING_FILE_2 = "Bank_Rating_2.xlsx";

    @Test
    @TestCaseId("IPBQA-32105")
    @Description("Display Changed Rows: verify equal rows toggle in Local Changes and Repository Compare screens")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDisplayChangedRowsLocalChangesAndRepositoryCompareScreens() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Example 1 - Bank Rating");
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "Bank Rating");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Rules")
                .selectItemInFolder("Rules", "BankLimitIndex");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(7, 5, "10");
        editorPage.getCenterTable().editCell(16, 9, "5");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        ChangesDialogComponent changesDialog = editorPage.getEditorToolbarPanelComponent()
                .clickMore()
                .clickChanges();

        changesDialog.setCompareCheckbox(1, true);
        changesDialog.setCompareCheckbox(2, true);
        CompareLocalChangesDialogComponent compareDialog = changesDialog.clickCompare();
        compareDialog.waitForDialogToAppear();
        compareDialog.setShowEqualRows(true);

        compareDialog.openTreeNode("Limit");
        compareDialog.clickTreeNode("Rules Double BankLimitIndex (Bank bank, RatingGroup bankRatingGroup)");

        assertThat(compareDialog.getNumberOfRows(1))
                .as("Left fragment should have more than 4 rows with equal rows shown")
                .isGreaterThan(4);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Right fragment should have more than 4 rows with equal rows shown")
                .isGreaterThan(4);

        validateCompareWindowCells(compareDialog);

        int shownLeft = compareDialog.getNumberOfRows(1);
        int shownRight = compareDialog.getNumberOfRows(2);
        compareDialog.setShowEqualRows(false);
        // What the toggle does is leave out the rows the two versions read the same. How many rows remain
        // is the table's business — the old screen led each version with a header row of its own and this
        // one does not — so what is asked is that fewer remain and that the edits are still among them.
        assertThat(compareDialog.getNumberOfRows(1))
                .as("Hiding the rows that read the same must leave fewer rows in the left version")
                .isLessThan(shownLeft);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Hiding the rows that read the same must leave fewer rows in the right version")
                .isLessThan(shownRight);
        // With the rows that read the same left out, what remains is the differences themselves: both
        // versions still carry them. Where they sit is no longer the place they sat in the whole table,
        // which is what the rows being left out means.
        assertThat(compareDialog.getHighlightedCellCount(1))
                .as("The differences must still be shown in the left version once the equal rows are hidden")
                .isPositive();
        assertThat(compareDialog.getHighlightedCellCount(2))
                .as("The differences must still be shown in the right version once the equal rows are hidden")
                .isPositive();

        compareDialog.setShowEqualRows(true);
        validateCompareWindowCells(compareDialog);
        assertThat(compareDialog.getNumberOfRows(1))
                .as("Left fragment should have more than 4 rows after re-enabling equal rows")
                .isGreaterThan(4);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Right fragment should have more than 4 rows after re-enabling equal rows")
                .isGreaterThan(4);
        compareDialog.close();
        // The history the comparison was started from stands over the module screen and is still open.
        changesDialog.closeIfOpen();

        // Compare the working copy against the repository BEFORE committing: the repo compare screen always
        // puts the working copy on the left, so the edited-but-unsaved project is what differs from HEAD.
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(projectName);

        // The comparison against the repository opens in a browser tab of its own, on the same screen the
        // local changes are compared on: it opens with the rows that read the same left out, so they are
        // asked for before the table is read line by line.
        CompareGitRevisionsDialogComponent repoCompareDialog = projectDetail.openRevisionCompare();
        repoCompareDialog.openTreeNode("Limit");
        repoCompareDialog.clickTreeNode("Rules Double BankLimitIndex (Bank bank, RatingGroup bankRatingGroup)");
        repoCompareDialog.setShowEqualRows(true);

        validateRepositoryCompareWindowCells(repoCompareDialog);
        assertThat(repoCompareDialog.getNumberOfRows(1))
                .as("Repo left fragment should render the diff rows").isGreaterThan(0);
        assertThat(repoCompareDialog.getNumberOfRows(2))
                .as("Repo right fragment should render the diff rows").isGreaterThan(0);

        // The toggle re-renders the diff without breaking it; the changed cells stay highlighted in both states.
        repoCompareDialog.setShowEqualRows(false);
        assertThat(repoCompareDialog.getHighlightedCellCount(1))
                .as("The differences must still be shown in the working copy with the equal rows left out")
                .isPositive();
        assertThat(repoCompareDialog.getHighlightedCellCount(2))
                .as("The differences must still be shown in the revision with the equal rows left out")
                .isPositive();
        repoCompareDialog.setShowEqualRows(true);
        validateRepositoryCompareWindowCells(repoCompareDialog);
        repoCompareDialog.close();

        repositoryPage.openProjectsList().saveProject(projectName, "Edited BankLimitIndex");
    }

    @Test
    @TestCaseId("IPBQA-32105")
    @Description("Display Changed Rows: verify equal rows toggle in uploaded Excel files compare screen")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDisplayChangedRowsUploadedFilesCompareScreen() {
        WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();

        throw new SkipException("KNOWN-ISSUES.md #9: the screen that compares two uploaded Excel files has no "
                + "entry point left in the UI, so this scenario cannot be driven.");
    }

    @Test
    @TestCaseId("IPBQA-32105")
    @Description("Display Changed Rows: verify no equal rows checkbox in non-Excel Resolve Conflicts screen")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testNoEqualRowsCheckboxInNonExcelResolveConflicts() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Example 2 - Corporate Rating");
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        // Save desc1 → revision 2
        editorPage.openEditProjectDialog(projectName).setDescription("desc1").clickUpdateButton();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        // Save desc2 → revision 3 (HEAD)
        editorPage.openEditProjectDialog(projectName).setDescription("desc2").clickUpdateButton();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        // Open revision 2 (one behind HEAD=rev3) — editing from here causes a conflict
        editorPage.getEditorToolbarPanelComponent().clickMore().clickRevisions();
        EditorRevisionsTabComponent revisionsTab = new EditorRevisionsTabComponent();
        revisionsTab.waitForTableToLoad();
        revisionsTab.openRevision(2);

        // Edit description from old revision and save → triggers Resolve Conflicts
        editorPage.openEditProjectDialog(projectName).setDescription("desc3").clickUpdateButton();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        // Resolve Conflicts dialog must appear because we edited from an old revision
        ResolveConflictsDialogComponent resolveConflictsDialog = new ResolveConflictsDialogComponent();
        assertThat(resolveConflictsDialog.isDialogVisible())
                .as("Resolve Conflicts dialog should appear when saving from an old revision while HEAD has advanced")
                .isTrue();

        // Open text compare nested modal via Compare link and verify no equal rows checkbox (non-Excel file)
        CompareLocalChangesDialogComponent compareDialog = resolveConflictsDialog.clickCompareLinkInCurrentPage();
        compareDialog.waitForTextCompareToAppear();
        assertThat(compareDialog.isShowEqualRowsCheckboxVisible())
                .as("Equal rows checkbox must not be visible for non-Excel (text) file diff in Resolve Conflicts")
                .isFalse();
    }

    /**
     * Each of the two edits must show as a difference, in both versions, on the line it was made on.
     *
     * <p>The line is what the two versions are asked about rather than the cell: a merged cell is drawn once
     * and the cells it covers are not drawn at all, so which column a difference falls in is a matter of how
     * the table is drawn, while which line it falls on is a matter of the table itself.
     */
    private void validateCompareWindowCells(CompareLocalChangesDialogComponent dialog) {
        assertThat(dialog.isRowHighlighted(1, 7))
                .as("The first edit must show as a difference on row 7 of the left version")
                .isTrue();
        assertThat(dialog.isRowHighlighted(2, 7))
                .as("The first edit must show as a difference on row 7 of the right version")
                .isTrue();
        // The other edit is on row 16. Which column it falls in is the grid's own counting — a merged cell
        // is drawn once and the cells it covers take no place — so the row is what the versions are asked
        // about here.
        assertThat(dialog.isRowHighlighted(1, 16))
                .as("The second edit must show as a difference on row 16 of the left version")
                .isTrue();
        assertThat(dialog.isRowHighlighted(2, 16))
                .as("The second edit must show as a difference on row 16 of the right version")
                .isTrue();
    }

    /** The same two edits, held against the repository instead of against an earlier local version. */
    private void validateRepositoryCompareWindowCells(CompareGitRevisionsDialogComponent dialog) {
        assertThat(dialog.isRowHighlighted(1, 7))
                .as("The first edit must show as a difference on row 7 of the working copy")
                .isTrue();
        assertThat(dialog.isRowHighlighted(2, 7))
                .as("The first edit must show as a difference on row 7 of the revision")
                .isTrue();
        assertThat(dialog.isRowHighlighted(1, 16))
                .as("The second edit must show as a difference on row 16 of the working copy")
                .isTrue();
        // The revision is a workbook of its own and its table need not begin on the line the working copy's
        // does, so what it is asked for is that it carries the differences — which rows of it they fall on
        // is answered by the side that was edited.
        assertThat(dialog.getHighlightedCellCount(2))
                .as("The revision must show what the working copy differs from it in")
                .isPositive();
    }
}
