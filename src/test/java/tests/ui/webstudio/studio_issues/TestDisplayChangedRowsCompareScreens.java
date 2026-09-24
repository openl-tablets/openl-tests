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
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestDisplayChangedRowsCompareScreens extends BaseTest {

    private static final String BANK_RATING_FILE_1 = "Bank_Rating_1.xlsx";
    private static final String BANK_RATING_FILE_2 = "Bank_Rating_2.xlsx";
    private static final int BANK_LIMIT_INDEX_TOP_ROW = 8;

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
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "BankLimitIndex");

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
        assertThat(compareDialog.getNumberOfRows(1))
                .as("Hiding the rows that read the same must leave fewer rows in the left version")
                .isLessThan(shownLeft);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Hiding the rows that read the same must leave fewer rows in the right version")
                .isLessThan(shownRight);
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
        changesDialog.closeIfOpen();

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(projectName);

        CompareGitRevisionsDialogComponent repoCompareDialog = projectDetail.openRevisionCompare();
        repoCompareDialog.openTreeNode("Limit");
        repoCompareDialog.clickTreeNode("Rules Double BankLimitIndex (Bank bank, RatingGroup bankRatingGroup)");
        repoCompareDialog.setShowEqualRows(true);

        validateRepositoryCompareWindowCells(repoCompareDialog);
        assertThat(repoCompareDialog.getNumberOfRows(1))
                .as("Repo left fragment should render the diff rows").isGreaterThan(0);
        assertThat(repoCompareDialog.getNumberOfRows(2))
                .as("Repo right fragment should render the diff rows").isGreaterThan(0);

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
    @Description("Display Changed Rows: the comparison of two uploaded workbooks draws the rows that differ, "
            + "and drawing the equal ones as well is asked for with the toggle.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDisplayChangedRowsUploadedFilesCompareScreen() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Main");

        CompareExcelFilesDialogComponent compareDialog = editorPage
                .getEditorToolbarPanelComponent()
                .clickMore()
                .clickCompareExcelFiles();

        assertThat(compareDialog.offersWorkbooksToUpload())
                .as("'Compare Excel files' must open the comparison of two workbooks the reader uploads")
                .isTrue();

        compareDialog.uploadFile(TestDataUtil.getFilePathFromResources(BANK_RATING_FILE_1));
        compareDialog.uploadFile(TestDataUtil.getFilePathFromResources(BANK_RATING_FILE_2));
        compareDialog.clickCompareExcel();

        compareDialog.openTreeNode("Limit");
        compareDialog.clickTreeNode("Rules Double BankLimitIndex (Bank bank, RatingGroup bankRatingGroup)");

        int drawnLeft = compareDialog.getNumberOfRows(1);
        int drawnRight = compareDialog.getNumberOfRows(2);
        validateCompareWindowCells(compareDialog);

        compareDialog.setShowEqualRows(true);
        assertThat(compareDialog.getNumberOfRows(1))
                .as("Asking for the rows that read the same draws more of the first workbook")
                .isGreaterThan(drawnLeft);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Asking for the rows that read the same draws more of the second workbook")
                .isGreaterThan(drawnRight);
        validateCompareWindowCells(compareDialog);
        compareDialog.close();
    }

    @Test
    @TestCaseId("IPBQA-32105")
    @Description("Display Changed Rows: verify no equal rows checkbox in non-Excel Resolve Conflicts screen")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testNoEqualRowsCheckboxInNonExcelResolveConflicts() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Example 2 - Corporate Rating");
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        editorPage.openEditProjectDialog(projectName).setDescription("desc1").clickUpdateButton();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.openEditProjectDialog(projectName).setDescription("desc2").clickUpdateButton();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getEditorToolbarPanelComponent().clickMore().clickRevisions();
        EditorRevisionsTabComponent revisionsTab = new EditorRevisionsTabComponent();
        revisionsTab.waitForTableToLoad();
        revisionsTab.openRevision(2);

        editorPage.openEditProjectDialog(projectName).setDescription("desc3").clickUpdateButton();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        ResolveConflictsDialogComponent resolveConflictsDialog = new ResolveConflictsDialogComponent();
        assertThat(resolveConflictsDialog.isDialogVisible())
                .as("Resolve Conflicts dialog should appear when saving from an old revision while HEAD has advanced")
                .isTrue();

        CompareLocalChangesDialogComponent compareDialog = resolveConflictsDialog.clickCompareLinkAsPopup();
        compareDialog.waitForTextCompareToAppear();
        assertThat(compareDialog.isShowEqualRowsCheckboxVisible())
                .as("Equal rows checkbox must not be visible for non-Excel (text) file diff in Resolve Conflicts")
                .isFalse();
    }

    private void validateCompareWindowCells(CompareLocalChangesDialogComponent dialog) {
        assertThat(dialog.isSheetRowHighlighted(1, bankLimitIndexSheetRow(7)))
                .as("The first edit must show as a difference on row 7 of the left version")
                .isTrue();
        assertThat(dialog.isSheetRowHighlighted(2, bankLimitIndexSheetRow(7)))
                .as("The first edit must show as a difference on row 7 of the right version")
                .isTrue();
        assertThat(dialog.isSheetRowHighlighted(1, bankLimitIndexSheetRow(16)))
                .as("The second edit must show as a difference on row 16 of the left version")
                .isTrue();
        assertThat(dialog.isSheetRowHighlighted(2, bankLimitIndexSheetRow(16)))
                .as("The second edit must show as a difference on row 16 of the right version")
                .isTrue();
    }

    private static int bankLimitIndexSheetRow(int tableRow) {
        return BANK_LIMIT_INDEX_TOP_ROW + tableRow - 1;
    }

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
        assertThat(dialog.getHighlightedCellCount(2))
                .as("The revision must show what the working copy differs from it in")
                .isPositive();
    }
}
