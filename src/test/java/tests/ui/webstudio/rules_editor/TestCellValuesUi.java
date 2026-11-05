package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.IRunMenu;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DATA;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TEST;
import static org.assertj.core.api.Assertions.assertThat;

public class TestCellValuesUi extends BaseTest {

    private static final String BIG_INTEGER_WORKBOOK = "BigIntegerValues.xlsx";
    private static final String BIG_INTEGER_MODULE = "BigIntegerValues";
    private static final String AS_WRITTEN_WORKBOOK = "CellValuesAsWritten.xlsx";
    private static final String AS_WRITTEN_MODULE = "CellValuesAsWritten";
    private static final String BEYOND_INT = "2147483648";

    @Test
    @TestCaseId({"EPBDS-8391", "EPBDS-8392"})
    @Description("EPBDS-8391: a number beyond the int range, 2147483648, is stored in a BigInteger field of a Data table "
            + "and in a Test table of a rule taking a BigInteger: the module compiles without errors and both tables show "
            + "2147483648. EPBDS-8392: the rule myBigInteger run with bigValue=2147483648 returns value2, the result of "
            + "the row holding that number.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void numberBeyondTheIntRangeIsStoredAndMatchedAsBigInteger() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, BIG_INTEGER_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, BIG_INTEGER_MODULE);
        editorPage.getProblemsPanelComponent().checkNoProblems();

        selectTable(editorPage, DATA, "holders");
        assertThat(editorPage.getCenterTable().getCellText(4, 1))
                .as("EPBDS-8391: the Data table must show the BigInteger field as written, with no parse error")
                .isEqualTo(BEYOND_INT);

        selectTable(editorPage, TEST, "myBigInteger_Test");
        TableComponent testTable = editorPage.getCenterTable();
        assertThat(testTable.getCellText(4, 1))
                .as("EPBDS-8391: the Test table must show the BigInteger argument as written, with no parse error")
                .isEqualTo(BEYOND_INT);
        editorPage.getProblemsPanelComponent().checkNoProblems();

        selectTable(editorPage, DECISION, "myBigInteger");
        IRunMenu run = editorPage.getEditorToolbarPanelComponent().clickRun();
        run.writeFieldValue("bigValue", BEYOND_INT);
        run.clickRunInsideMenu();
        assertThat(editorPage.getTestResultValidationComponent().getUnfoldedRunCellText("Result"))
                .as("EPBDS-8392: the row holding %s must be the one the rule runs, not the first or the last row", BEYOND_INT)
                .isEqualTo("\"value2\"");
    }

    @Test
    @TestCaseId({"EPBDS-16759", "EPBDS-16640"})
    @Description("EPBDS-16759: the Band table, whose Double cells the workbook stores as 1.1500000000000001 and "
            + "1.4500000000000002, shows 1.15 and 1.45 as Excel does. EPBDS-16640: a Double cell holding 1abc, a Date cell "
            + "holding 12/31/2024 and later and an Integer[] cell holding 1,2abc,3 show that text as written, beside the "
            + "message about each, not a number or a date read out of the front of it.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void tableShowsTheCellValuesAsTheWorkbookHoldsThem() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, AS_WRITTEN_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, AS_WRITTEN_MODULE);

        selectTable(editorPage, DECISION, "Band");
        TableComponent band = editorPage.getCenterTable();
        assertThat(band.getCellText(3, 1))
                .as("EPBDS-16759: 1.1500000000000001 of the workbook must be shown as Excel shows it")
                .isEqualTo("1.15");
        assertThat(band.getCellText(4, 1))
                .as("EPBDS-16759: 1.4500000000000002 of the workbook must be shown as Excel shows it")
                .isEqualTo("1.45");

        assertShownAsWritten(editorPage, "Score", 4, 2, "1abc", "Cannot parse cell value '1abc'");
        assertShownAsWritten(editorPage, "DueDate", 3, 2, "12/31/2024 and later",
                "Cannot parse cell value '12/31/2024 and later'");
        assertShownAsWritten(editorPage, "Codes", 3, 2, "1,2abc,3", "Cannot parse cell value '2abc'");
    }

    private void assertShownAsWritten(EditorPage editorPage, String tableName, int row, int column, String written,
                                      String message) {
        selectTable(editorPage, DECISION, tableName);
        assertThat(editorPage.getCenterTable().getCellText(row, column))
                .as("EPBDS-16640: the cell of %s must show the text %s as written, not a value read out of the front of it",
                        tableName, written)
                .isEqualTo(written);
        assertThat(editorPage.getProblemsPanelComponent().errorsSaying(message))
                .as("EPBDS-16640: the cell that shows %s is the one the engine refuses", written)
                .isNotEmpty();
    }

    private void selectTable(EditorPage editorPage, String folder, String table) {
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(folder)
                .selectItemInFolder(folder, table);
    }
}
