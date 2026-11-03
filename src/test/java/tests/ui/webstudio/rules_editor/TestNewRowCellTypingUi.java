package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.core.ui.WebElement;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorTableActionsPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.function.ObjIntConsumer;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DATA;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TEST;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.VOCABULARY;
import static org.assertj.core.api.Assertions.assertThat;

public class TestNewRowCellTypingUi extends BaseTest {

    private static final String WORKBOOK = "NewRowCellTyping.xlsx";
    private static final String MODULE = "NewRowCellTyping";
    private static final String TYPED = "abc7";
    private static final String KEPT = "7";
    private static final String VOCABULARY_NOT_TYPED = "A cell of a new Vocabulary row must refuse letters";
    private static final String TYPED_PRICE = "abc1.5x";
    private static final String KEPT_PRICE = "1.5";
    private static final int PRICE_COLUMN = 2;
    private static final int SINCE_COLUMN = 3;
    private static final int PICKED_DAY = 15;
    private static final int DATA_LAST_ROW = 5;
    private static final ObjIntConsumer<TableComponent> NOTHING_MORE = (table, newRow) -> { };

    @Test
    @TestCaseId("EPBDS-16881")
    @Description("EPBDS-16750: a row added to a Data table gets cells of the types of its fields: the Integer and "
            + "the Double cells refuse letters and keep the number, the Date cell takes no typing and a date is picked "
            + "from the calendar; all of them are saved.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void newDataTableRowRefusesLetters() {
        TableComponent saved = typeIntoNewRow(DATA, "myData", DATA_LAST_ROW,
                "A cell of a new Data table row must refuse letters", this::fillPriceAndSinceOfNewRow);
        assertThat(saved.getCellText(DATA_LAST_ROW + 1, PRICE_COLUMN))
                .as("The decimal typed into the Double cell of the new row must be saved")
                .isEqualTo(KEPT_PRICE);
        assertThat(saved.getCellText(DATA_LAST_ROW + 1, SINCE_COLUMN))
                .as("The date picked for the Date cell of the new row must be saved as the dates of the other rows")
                .matches("\\d{2}/" + PICKED_DAY + "/\\d{4}");
    }

    private void fillPriceAndSinceOfNewRow(TableComponent table, int newRow) {
        WebElement price = table.openCellEditor(newRow, PRICE_COLUMN);
        price.clear();
        price.fillSequentially(TYPED_PRICE);
        assertThat(price.getCurrentInputValue())
                .as("The Double cell of a new Data table row must refuse letters and keep the decimal")
                .isEqualTo(KEPT_PRICE);
        price.press("Enter");

        WebElement since = table.openCellEditor(newRow, SINCE_COLUMN);
        assertThat(since.getAttribute("placeholder"))
                .as("The Date cell of a new Data table row must be a date picker, as in the other rows")
                .isEqualTo("Select date");
        since.fillSequentially(TYPED);
        assertThat(since.getCurrentInputValue())
                .as("The Date cell of a new Data table row must refuse what is not a date")
                .isEmpty();
        table.pickDayInOpenDateEditor(PICKED_DAY);
    }

    @Test
    @TestCaseId("EPBDS-16881")
    @Description("EPBDS-16750: a rule added to a SmartRules table gets cells of its Integer condition: letters are "
            + "refused, the digits are kept and saved.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void newRuleRowRefusesLetters() {
        typeIntoNewRow(DECISION, "Hello", 4, "A condition cell of a new rule must refuse letters", NOTHING_MORE);
    }

    @Test
    @TestCaseId("EPBDS-16881")
    @Description("EPBDS-16750: a case added to a Test table gets cells of the Integer parameter: letters are "
            + "refused, the digits are kept and saved.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void newTestCaseRowRefusesLetters() {
        typeIntoNewRow(TEST, "HelloTest", 5, "A parameter cell of a new test case must refuse letters", NOTHING_MORE);
    }

    @Test
    @TestCaseId("EPBDS-16881")
    @Description("EPBDS-16750: a case added to a Test table whose parameter is a Vocabulary gets a list of the "
            + "Vocabulary values, as the existing cases do, instead of a free text box.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void newTestCaseRowOffersVocabularyValues() {
        EditorPage editorPage = openTable(TEST, "HelloTest2");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        TableComponent table = editorPage.getCenterTable();
        int lastRow = table.getRowsCount();
        WebElement existing = table.openCellEditor(lastRow, 1);
        assertThat(existing.getAttribute("class"))
                .as("An existing case picks its Level from a list, which the new case must follow")
                .contains("ant-select");
        existing.press("Escape");

        table.clickCell(lastRow, 1);
        editorPage.getEditorTableActionsPanelComponent().clickInsertRowAfter();
        WebElement added = table.openCellEditor(lastRow + 1, 1);
        assertThat(added.getAttribute("class"))
                .as("The Level of a new case must be picked from the Vocabulary values, not typed")
                .contains("ant-select");
        added.press("Escape");
        table.editCell(lastRow + 1, 1, "3");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        editorPage.reloadPage();
        selectTable(editorPage, TEST, "HelloTest2");
        assertThat(editorPage.getCenterTable().getCellText(lastRow + 1, 1))
                .as("The Level picked for the new case must be saved")
                .isEqualTo("3");
    }

    @Test
    @TestCaseId("EPBDS-16881")
    @Description("EPBDS-16765: a value added to an Integer Vocabulary gets an Integer cell: letters are refused, "
            + "the digits are kept and saved.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void newVocabularyRowRefusesLetters() {
        typeIntoNewRow(VOCABULARY, "Level", 4, VOCABULARY_NOT_TYPED, NOTHING_MORE);
    }

    private TableComponent typeIntoNewRow(String folder, String tableName, int lastRow, String refusal,
                                          ObjIntConsumer<TableComponent> otherCellsOfNewRow) {
        EditorPage editorPage = openTable(folder, tableName);
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        TableComponent table = editorPage.getCenterTable();
        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();
        int rowsBefore = table.getRowsCount();
        assertThat(rowsBefore)
                .as("The last row of " + tableName + " is the one the new row is added after")
                .isEqualTo(lastRow);

        WebElement existing = table.openCellEditor(lastRow, 1);
        existing.clear();
        existing.fillSequentially(TYPED);
        assertThat(existing.getCurrentInputValue())
                .as("An existing cell of " + tableName + " refuses letters, which the new row must follow")
                .isEqualTo(KEPT);
        existing.press("Escape");

        table.clickCell(lastRow, 1);
        actions.clickInsertRowAfter();
        WebElement added = table.openCellEditor(lastRow + 1, 1);
        added.clear();
        added.fillSequentially(TYPED);
        assertThat(added.getCurrentInputValue())
                .as(refusal)
                .isEqualTo(KEPT);
        added.press("Enter");
        otherCellsOfNewRow.accept(table, lastRow + 1);
        actions.clickSaveChanges();

        editorPage.reloadPage();
        selectTable(editorPage, folder, tableName);
        table = editorPage.getCenterTable();
        assertThat(table.getRowsCount())
                .as("The new row must be saved")
                .isEqualTo(rowsBefore + 1);
        assertThat(table.getCellText(lastRow + 1, 1))
                .as("Only the digits typed into the new row must be saved")
                .isEqualTo(KEPT);
        return table;
    }

    private EditorPage openTable(String folder, String tableName) {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        selectTable(editorPage, folder, tableName);
        return editorPage;
    }

    private void selectTable(EditorPage editorPage, String folder, String tableName) {
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(folder)
                .selectItemInFolder(folder, tableName);
    }
}
