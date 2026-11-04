package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorTableActionsPanelComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestLargeTableEditToolbarUi extends BaseTest {

    private static final String PROJECT_ZIP = "LargeTableAddColumnRow.zip";
    private static final String MODULE = "Main";
    private static final String SHEET = "Rules";

    @Test
    @TestCaseId("EPBDS-12542")
    @Description("EPBDS-12542: in tables longer than the screen the edit toolbar stays in view at the last row, "
            + "and a row added after the last one and a column inserted at the last one keep the values typed into "
            + "them after Save: LargeSpreadsheet (Step35 under Formula1, Formula 15 in the Step1 row), LargeLookup "
            + "(Text3 in the added rule, Text6 in the column values) and the transposed Data table "
            + "LogDatatypeNameTrans (obj5.field10 in the added field, Test in the obj1.field2 row).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void largeTablesEditedAtLastRowAndColumnKeepValues() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, PROJECT_ZIP);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);

        TableComponent table = openTable(editorPage, "LargeSpreadsheet");
        int step1 = table.getRowIndexHolding("Step1");
        editAtLastRowAndColumn(editorPage, table, "LargeSpreadsheet",
                step1, 1, 2, "Step35",
                step1, "Formula 15");

        table = openTable(editorPage, "LargeLookup");
        int header = table.getRowIndexHolding("a", "b", "Text1");
        editAtLastRowAndColumn(editorPage, table, "LargeLookup",
                header, 3, 1, "Text3",
                header + 1, "Text6");

        table = openTable(editorPage, "LogDatatypeNameTrans");
        editAtLastRowAndColumn(editorPage, table, "LogDatatypeNameTrans",
                table.getRowIndexHolding("obj1.field1"), 1, 1, "obj5.field10",
                table.getRowIndexHolding("obj1.field2"), "Test");
    }

    private TableComponent openTable(EditorPage editorPage, String tableName) {
        editorPage.getEditorLeftRulesTreeComponent()
                .expandFolderInTree(SHEET)
                .selectItemInFolder(SHEET, tableName);
        return editorPage.getCenterTable();
    }

    private void editAtLastRowAndColumn(EditorPage editorPage, TableComponent table, String tableName,
                                        int startRow, int startColumn, int newRowColumn, String newRowValue,
                                        int newColumnRow, String newColumnValue) {
        table.openCellEditor(startRow, startColumn).press("Escape");
        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();

        int lastRow = table.getRowsCount();
        double titleTop = table.getCellTop(1, 1);
        table.scrollToCell(lastRow, 1);
        assertThat(table.getCellTop(1, 1))
                .as(tableName + " must be longer than the screen, so reaching its last row scrolls the title away")
                .isLessThan(titleTop);
        assertThat(actions.isInView())
                .as("The edit toolbar of " + tableName + " must stay in view at the last row")
                .isTrue();
        assertThat(actions.areEditActionsShown())
                .as("Save, Undo, Redo and the row and column actions of " + tableName
                        + " must be shown at the last row")
                .isTrue();

        int cellsOfLastRow = table.getRow(lastRow).getCells().size();
        table.clickCell(lastRow, 1);
        actions.clickInsertRowAfter();
        table.editCell(lastRow + 1, newRowColumn, newRowValue);

        int lastColumn = table.getRow(newColumnRow).getCells().size();
        table.scrollToCell(newColumnRow, lastColumn);
        table.clickCell(newColumnRow, lastColumn);
        actions.clickInsertColumnBefore();
        table.editCell(newColumnRow, lastColumn, newColumnValue);
        actions.clickSaveChanges();

        editorPage.reloadPage();
        table = openTable(editorPage, tableName);
        assertThat(table.getRowsCount())
                .as("The row added after the last row of " + tableName + " must be saved")
                .isEqualTo(lastRow + 1);
        assertThat(table.getRow(lastRow + 1).getCells())
                .as("The added row of " + tableName + " must offer the cells of the last row and one of the new column")
                .hasSize(cellsOfLastRow + 1);
        assertThat(table.getCellText(lastRow + 1, newRowColumn))
                .as("The value typed into the added row of " + tableName + " must be saved")
                .isEqualTo(newRowValue);
        assertThat(table.getRow(newColumnRow).getCells())
                .as("The column inserted at the last column of " + tableName + " must be saved")
                .hasSize(lastColumn + 1);
        assertThat(table.getCellText(newColumnRow, lastColumn))
                .as("The value typed into the inserted column of " + tableName + " must be saved")
                .isEqualTo(newColumnValue);
    }
}
