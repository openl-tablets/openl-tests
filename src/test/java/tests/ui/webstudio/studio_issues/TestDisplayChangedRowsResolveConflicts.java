package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.CompareLocalChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static domain.ui.webstudio.components.editortabcomponents.CompareLocalChangesDialogComponent.COMBINED;
import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestDisplayChangedRowsResolveConflicts extends BaseTest {

    private static final String EPBDS_12417_FILE = "EPBDS-12417.xlsx";
    private static final int BENEFIT_PREMIUM_ROWS = 5;
    private static final int BENEFIT_PREMIUM_ROWS_AROUND_THE_EDIT = 3;

    @Test
    @TestCaseId("IPBQA-32105")
    @Description("Display Changed Rows: equal rows checkbox in Resolve Conflicts for project created from Excel file")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testEqualRowsCheckboxInResolveConflictsProjFromExcelFile() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, EPBDS_12417_FILE);
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "EPBDS-12417");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", "BenefitPremium");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(2, 2, "changedValue");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        ChangesDialogComponent changesDialog = editorPage.getEditorToolbarPanelComponent()
                .clickMore()
                .clickChanges();

        changesDialog.setCompareCheckbox(1, true);
        changesDialog.setCompareCheckbox(2, true);
        CompareLocalChangesDialogComponent compareDialog = changesDialog.clickCompare();
        compareDialog.waitForDialogToAppear();

        compareDialog.openTreeNode("Sheet1");
        compareDialog.clickTreeNode("Test BenefitPremium");

        compareDialog.setShowEqualRows(false);
        assertThat(compareDialog.getNumberOfRows(COMBINED))
                .as("The one table shows the header, the changed row and the equal row next to it")
                .isEqualTo(BENEFIT_PREMIUM_ROWS_AROUND_THE_EDIT);
        assertThat(compareDialog.getEqualRowsLeftOut(COMBINED))
                .as("The one table says how many equal rows it leaves out")
                .containsExactly("⋯ 2 equal rows");
        int paintedAsOne = compareDialog.getHighlightedCellCount(COMBINED);
        assertThat(paintedAsOne).as("The one table must carry what differs").isPositive();
        compareDialog.setShowEqualRows(true);
        assertThat(compareDialog.getNumberOfRows(COMBINED))
                .as("The one table shows every row once the equal ones are asked for")
                .isEqualTo(BENEFIT_PREMIUM_ROWS);
        assertThat(compareDialog.getEqualRowsLeftOut(COMBINED))
                .as("No equal row is left out once they are asked for")
                .isEmpty();
        assertThat(compareDialog.getHighlightedCellCount(COMBINED))
                .as("Showing the rows that read the same must add nothing to what differs in the one table")
                .isEqualTo(paintedAsOne);
        compareDialog.setShowEqualRows(false);
        compareDialog.showSideBySide();

        assertThat(compareDialog.getNumberOfRows(1))
                .as("Left fragment: the header, the changed row and the equal row next to it when equal rows hidden")
                .isEqualTo(BENEFIT_PREMIUM_ROWS_AROUND_THE_EDIT);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Right fragment: the header, the changed row and the equal row next to it when equal rows hidden")
                .isEqualTo(BENEFIT_PREMIUM_ROWS_AROUND_THE_EDIT);
        assertThat(compareDialog.getEqualRowsLeftOut(1))
                .as("Left fragment says how many equal rows it leaves out")
                .containsExactly("⋯ 2 equal rows");
        int paintedLeft = compareDialog.getHighlightedCellCount(1);
        int paintedRight = compareDialog.getHighlightedCellCount(2);
        assertThat(paintedLeft)
                .as("The one row left must carry what differs")
                .isPositive();
        assertThat(paintedRight)
                .as("The one row left must carry what differs")
                .isPositive();

        compareDialog.setShowEqualRows(true);
        assertThat(compareDialog.getNumberOfRows(1))
                .as("Left fragment shows every row when equal rows shown")
                .isEqualTo(BENEFIT_PREMIUM_ROWS);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Right fragment shows every row when equal rows shown")
                .isEqualTo(BENEFIT_PREMIUM_ROWS);
        assertThat(compareDialog.getHighlightedCellCount(1))
                .as("Showing the rows that read the same must add nothing to what differs")
                .isEqualTo(paintedLeft);
        assertThat(compareDialog.getHighlightedCellCount(2))
                .as("Showing the rows that read the same must add nothing to what differs")
                .isEqualTo(paintedRight);

        compareDialog.close();
    }

    @Test
    @TestCaseId("IPBQA-32105")
    @Description("Display Changed Rows: equal rows checkbox when a row is added to a table")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testEqualRowsCheckboxInAddingRowComparison() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Example 1 - Bank Rating");
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "Bank Rating");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "BankLimitIndex");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        TableComponent editedTable = editorPage.getCenterTable();
        int lastRow = editedTable.getRowsCount();
        editedTable.clickCell(lastRow, 1);
        editorPage.getEditorTableActionsPanelComponent().clickInsertRowAfter();
        editedTable.editCell(lastRow + 1, 1, "changedValue");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        ChangesDialogComponent changesDialog = editorPage.getEditorToolbarPanelComponent()
                .clickMore()
                .clickChanges();

        changesDialog.setCompareCheckbox(1, true);
        changesDialog.setCompareCheckbox(2, true);
        CompareLocalChangesDialogComponent compareDialog = changesDialog.clickCompare();
        compareDialog.waitForDialogToAppear();

        compareDialog.openTreeNode("Limit");
        compareDialog.clickTreeNode("Rules Double BankLimitIndex (Bank bank, RatingGroup bankRatingGroup)");

        compareDialog.setShowEqualRows(false);
        int rowsAsOne = compareDialog.getNumberOfRows(COMBINED);
        assertThat(compareDialog.getCellContent(COMBINED, rowsAsOne, 1))
                .as("The one table ends with the added row, carrying what was written")
                .isEqualTo("changedValue");
        assertThat(compareDialog.isRowHighlighted(COMBINED, rowsAsOne))
                .as("The added row should be marked as the difference in the one table")
                .isTrue();
        assertThat(compareDialog.isRowHighlighted(COMBINED, rowsAsOne - 1))
                .as("The row before the added one reads the same in the one table")
                .isFalse();
        compareDialog.showSideBySide();
        int rowsBefore = compareDialog.getNumberOfRows(1);
        int rowsAfterAdding = compareDialog.getNumberOfRows(2);
        assertThat(rowsAfterAdding - rowsBefore)
                .as("The version with the added row should hold one row more than the version before it")
                .isEqualTo(1);
        assertThat(compareDialog.getCellContent(2, rowsAfterAdding, 1))
                .as("The added row should be the last of the version it was added to, carrying what was written")
                .isEqualTo("changedValue");
        assertThat(compareDialog.isRowHighlighted(2, rowsAfterAdding))
                .as("The added row should be marked as the difference")
                .isTrue();
        assertThat(compareDialog.getCellContent(1, rowsBefore, 1))
                .as("The row before the added one should read the same on both sides")
                .isEqualTo(compareDialog.getCellContent(2, rowsBefore, 1));

        compareDialog.setShowEqualRows(true);
        assertThat(compareDialog.getNumberOfRows(1))
                .as("Left fragment should have more than 1 row when equal rows shown")
                .isGreaterThan(1);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Right fragment should have more than 1 row when equal rows shown")
                .isGreaterThan(1);
        assertThat(compareDialog.getCellContent(2, compareDialog.getNumberOfRows(2), 1))
                .as("New row value in right fragment still present")
                .isEqualTo("changedValue");

        compareDialog.close();
    }

    @Test
    @TestCaseId("IPBQA-32105")
    @Description("Display Changed Rows: equal rows checkbox when a table is deleted")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testEqualRowsCheckboxInTableDeletionComparison() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Example 1 - Bank Rating");
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "Bank Rating");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "BankLimitIndex");

        editorPage.getEditorToolbarPanelComponent().removeCurrentTable();
        editorPage.waitUntilSpinnerLoaded();

        ChangesDialogComponent changesDialog = editorPage.getEditorToolbarPanelComponent()
                .clickMore()
                .clickChanges();

        changesDialog.setCompareCheckbox(1, true);
        changesDialog.setCompareCheckbox(2, true);
        CompareLocalChangesDialogComponent compareDialog = changesDialog.clickCompare();
        compareDialog.waitForDialogToAppear();

        compareDialog.openTreeNode("Limit");
        compareDialog.clickTreeNode("Rules Double BankLimitIndex (Bank bank, RatingGroup bankRatingGroup)");

        compareDialog.setShowEqualRows(false);
        assertThat(compareDialog.getNumberOfRows(COMBINED))
                .as("The one table shows all 17 rows of the deleted table")
                .isEqualTo(17);
        assertThat(compareDialog.getHighlightedCellCount(COMBINED))
                .as("Every cell of the deleted table is marked as removed in the one table")
                .isEqualTo(compareDialog.getNumberOfCells(COMBINED));
        compareDialog.showSideBySide();
        assertThat(compareDialog.getNumberOfRows(1))
                .as("Left fragment (original): should have 17 rows")
                .isEqualTo(17);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Right fragment (after delete): should have 0 rows")
                .isEqualTo(0);

        compareDialog.setShowEqualRows(true);
        assertThat(compareDialog.getNumberOfRows(1))
                .as("Left fragment stays 17 rows (no equal rows to show since table is deleted)")
                .isEqualTo(17);
        assertThat(compareDialog.getNumberOfRows(2))
                .as("Right fragment stays 0 rows (table deleted)")
                .isEqualTo(0);

        compareDialog.close();
    }
}
