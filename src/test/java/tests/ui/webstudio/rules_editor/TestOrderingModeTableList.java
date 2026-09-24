package tests.ui.webstudio.rules_editor;

import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOrderingModeTableList extends BaseTest {

    private static final String SHEET_1 = "Sheet1";
    private static final String A_SHEET = "Asheet";
    private static final String MOVED_TABLE = "_MyRules2";
    private static final List<String> SHEET_1_AFTER_MOVE = List.of("MyRules1", "MyRules1", MOVED_TABLE);
    private static final long TREE_SETTLE_TIMEOUT_MS = 30000;
    private static final long TREE_SETTLE_POLL_MS = 500;

    @Test
    @TestCaseId("IPBQA-32507")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTableListOrdering() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "sortingtesting.xlsx");
        EditorPage editorPage = new EditorPage();
        EditorLeftRulesTreeComponent rulesTree = editorPage.getEditorLeftRulesTreeComponent();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "sortingtesting");

        assertThat(rulesTree.getViewFilterValue())
                .containsIgnoringCase("Excel Sheet");

        rulesTree.setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_EXCEL_SHEET);
        assertThat(rulesTree.getFoldersVisible())
                .as("The sheets should be listed in the order the workbook holds them")
                .containsExactly(SHEET_1, A_SHEET, "すsupersheet");
        assertThat(rulesTree.getTablesOfFolder(SHEET_1))
                .as("The tables of Sheet1 should be listed top to bottom as the sheet holds them")
                .containsExactly(MOVED_TABLE, "MyRules1", "MyRules1");
        assertThat(rulesTree.getTablesOfFolder(A_SHEET))
                .as("The tables of Asheet should be listed top to bottom as the sheet holds them")
                .containsExactly("тест123", "はsomeRules", "_someRules", "étudiantomeRules", "トsomeRules");

        insertRowIntoMovedTableAndSave(editorPage, rulesTree);

        assertThat(tablesOfFolderOnceSettled(rulesTree, SHEET_1, SHEET_1_AFTER_MOVE))
                .as("_MyRules2 outgrew its place and was written below the other tables, so Sheet1 should list it last")
                .containsExactlyElementsOf(SHEET_1_AFTER_MOVE);

        rulesTree.selectItemInFolder(SHEET_1, MOVED_TABLE);
        assertThat(editorPage.getCenterTable().getCellText(5, 1))
                .as("The row inserted into _MyRules2 should have been saved")
                .isEqualTo("1");

        editorPage.getEditorToolbarPanelComponent().clickCreateTable();
        editorPage.getCreateTableDialogComponent()
                .selectType("Datatype Table")
                .clickNext()
                .setTechnicalName("NewDatatype")
                .addParameter("", "textField")
                .setCategorySelection(A_SHEET)
                .save();

        List<String> aSheetWithNewTable = List.of("тест123", "はsomeRules", "_someRules", "étudiantomeRules", "トsomeRules", "NewDatatype");
        assertThat(tablesOfFolderOnceSettled(rulesTree, A_SHEET, aSheetWithNewTable))
                .as("The Datatype created in Asheet should be listed after the tables written above it")
                .containsExactlyElementsOf(aSheetWithNewTable);

        rulesTree.selectItemInFolder(A_SHEET, "はsomeRules");
        editorPage.getEditorToolbarPanelComponent().removeCurrentTable();

        List<String> aSheetAfterRemoval = List.of("тест123", "_someRules", "étudiantomeRules", "トsomeRules", "NewDatatype");
        assertThat(tablesOfFolderOnceSettled(rulesTree, A_SHEET, aSheetAfterRemoval))
                .as("The removed table should be gone from Asheet and the others should keep their order")
                .doesNotContain("はsomeRules")
                .containsExactlyElementsOf(aSheetAfterRemoval);
    }

    @Test
    @TestCaseId("IPBQA-32507")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testSavedTableStaysOpenAfterItMoves() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "sortingtesting.xlsx");
        EditorPage editorPage = new EditorPage();
        EditorLeftRulesTreeComponent rulesTree = editorPage.getEditorLeftRulesTreeComponent();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "sortingtesting");
        rulesTree.setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_EXCEL_SHEET);

        insertRowIntoMovedTableAndSave(editorPage, rulesTree);

        WaitUtil.requireCondition(() -> folderLists(rulesTree, SHEET_1, SHEET_1_AFTER_MOVE),
                TREE_SETTLE_TIMEOUT_MS, TREE_SETTLE_POLL_MS,
                "Waiting for Sheet1 to list the saved _MyRules2 below the other tables");
        WaitUtil.waitForCondition(() -> !rulesTree.getSelectedItemText().isEmpty(),
                TREE_SETTLE_TIMEOUT_MS, TREE_SETTLE_POLL_MS, "Waiting for a table to be selected in the tree");

        assertThat(rulesTree.getSelectedItemText())
                .as("After Save the Rules Editor should keep the saved _MyRules2 open, although saving moved it "
                        + "below the other tables and gave it a new id (OpenL Studio 6.4.0 keeps it open)")
                .isEqualTo(MOVED_TABLE);
        assertThat(editorPage.getCenterTable().getCellText(1, 1))
                .as("The table shown after Save should be the saved _MyRules2")
                .contains(MOVED_TABLE);
    }

    @Test
    @TestCaseId("IPBQA-32507")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTableListOrdering2() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "sortingtesting1.xlsx");
        EditorPage editorPage = new EditorPage();
        EditorLeftRulesTreeComponent rulesTree = editorPage.getEditorLeftRulesTreeComponent();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "sortingtesting1");
        rulesTree.setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_EXCEL_SHEET);

        assertThat(rulesTree.getTablesOfFolder(SHEET_1))
                .as("Utility tables are hidden by default, so Sheet1 should list only its OpenL tables, top to bottom")
                .doesNotContain("Test123")
                .containsExactly("_MyRules", "MyRules", "MyRules", "Atable");

        rulesTree.showUtilityTables();

        assertThat(rulesTree.getTablesOfFolder(SHEET_1))
                .as("With utility tables shown, Sheet1 should list them among its tables top to bottom")
                .containsExactly("_MyRules", "Test123", "MyRules", "MyRules", "Test123", "Atable");
    }

    private void insertRowIntoMovedTableAndSave(EditorPage editorPage, EditorLeftRulesTreeComponent rulesTree) {
        rulesTree.selectItemInFolder(SHEET_1, MOVED_TABLE);
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().clickCell(4, 2);
        editorPage.getEditorTableActionsPanelComponent().clickInsertRowAfter();
        editorPage.getCenterTable().editCell(5, 1, "1");
        editorPage.getCenterTable().editCell(5, 2, "1");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
    }

    private List<String> tablesOfFolderOnceSettled(EditorLeftRulesTreeComponent rulesTree, String folderName,
                                                   List<String> expected) {
        WaitUtil.waitForCondition(() -> folderLists(rulesTree, folderName, expected),
                TREE_SETTLE_TIMEOUT_MS, TREE_SETTLE_POLL_MS,
                "Waiting for folder '" + folderName + "' to list " + expected);
        return rulesTree.getTablesOfFolder(folderName);
    }

    private boolean folderLists(EditorLeftRulesTreeComponent rulesTree, String folderName, List<String> expected) {
        try {
            return expected.equals(rulesTree.getTablesOfFolder(folderName));
        } catch (RuntimeException treeIsBeingRebuilt) {
            return false;
        }
    }
}
