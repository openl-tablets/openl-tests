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

    @Test
    @TestCaseId("EPBDS-16881")
    @Description("EPBDS-16750: a row added to a Data table gets cells of the Integer field: letters are refused, "
            + "the digits are kept and saved.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void newDataTableRowRefusesLetters() {
        typeIntoNewRow(DATA, "myData", 5, "A cell of a new Data table row must refuse letters");
    }

    @Test
    @TestCaseId("EPBDS-16881")
    @Description("EPBDS-16750: a rule added to a SmartRules table gets cells of its Integer condition: letters are "
            + "refused, the digits are kept and saved.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void newRuleRowRefusesLetters() {
        typeIntoNewRow(DECISION, "Hello", 4, "A condition cell of a new rule must refuse letters");
    }

    @Test
    @TestCaseId("EPBDS-16881")
    @Description("EPBDS-16750: a case added to a Test table gets cells of the Integer parameter: letters are "
            + "refused, the digits are kept and saved.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void newTestCaseRowRefusesLetters() {
        typeIntoNewRow(TEST, "HelloTest", 5, "A parameter cell of a new test case must refuse letters");
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
        typeIntoNewRow(VOCABULARY, "Level", 4, VOCABULARY_NOT_TYPED);
    }

    private void typeIntoNewRow(String folder, String tableName, int lastRow, String refusal) {
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
