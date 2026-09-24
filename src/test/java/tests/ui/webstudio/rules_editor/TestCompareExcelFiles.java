package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.CompareExcelFilesDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCompareExcelFiles extends BaseTest {

    private static final String FILE_1 = "All_tables_type.xlsx";
    private static final String FILE_2 = "All_tables_type2.xlsx";

    @Test
    @TestCaseId("IPBQA-28380")
    @Description("Compare Excel files: the action opens the comparison of two workbooks a reader uploads, "
            + "which lists the sheets that differ and marks the cells that changed.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCompareExcelFiles() {
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

        compareDialog.uploadFile(TestDataUtil.getFilePathFromResources(FILE_1));
        compareDialog.uploadFile(TestDataUtil.getFilePathFromResources(FILE_2));
        assertThat(compareDialog.countPickedFiles())
                .as("Both workbooks should be taken for the comparison")
                .isEqualTo(2);

        compareDialog.clearPickedFiles();
        assertThat(compareDialog.countPickedFiles())
                .as("Clearing should leave no workbook picked")
                .isZero();
        compareDialog.uploadFile(TestDataUtil.getFilePathFromResources(FILE_1));
        compareDialog.uploadFile(TestDataUtil.getFilePathFromResources(FILE_2));
        assertThat(compareDialog.isCompareOffered())
                .as("The comparison should be offered once two workbooks are picked")
                .isTrue();
        assertThat(compareDialog.isCompareEnabled())
                .as("The comparison should be ready to be started")
                .isTrue();

        compareDialog.clickCompareExcel();
        compareDialog.setShowEqualRows(true);

        assertThat(compareDialog.isTreeItemPresent("Rules")).as("Rules sheet differs").isTrue();
        assertThat(compareDialog.isTreeItemPresent("Old Sheet")).as("Old Sheet differs").isTrue();
        assertThat(compareDialog.isTreeItemPresent("New test tab")).as("New test tab differs").isTrue();
        assertThat(compareDialog.isTreeItemPresent("Const"))
                .as("A sheet that reads the same on both sides is left out")
                .isFalse();

        compareDialog.openTreeNode("Rules");
        assertThat(compareDialog.isTreeItemPresent("Spreadsheet SpreadsheetResult SpreadsheetTable (ByteValue a_byte)")).isTrue();
        assertThat(compareDialog.isTreeItemPresent("SimpleRules int SimpleRulesTable(Boolean CondValue)")).isTrue();
        assertThat(compareDialog.isTreeItemPresent("SimpleRules BigDecimal SimpleRuleTable(integer e_var)")).isTrue();
        assertThat(compareDialog.isTreeItemPresent("Datatype Vocabulary1 <String[]>")).isTrue();
        assertThat(compareDialog.isTreeItemPresent("SmartLookup DoubleValue SmartLookup1(int intValue, String arg2)")).isTrue();
        assertThat(compareDialog.isTreeItemPresent("Conditions  testConditions")).isTrue();
        assertThat(compareDialog.isTreeItemPresent("Returns")).isTrue();

        verifyDifferenceInCells(compareDialog, "Spreadsheet SpreadsheetResult SpreadsheetTable (ByteValue a_byte)", 3, 3, "200", "201");
        verifyDifferenceInCells(compareDialog, "SimpleRules int SimpleRulesTable(Boolean CondValue)", 2, 1, "Val", "Val1");
        verifyDifferenceInCells(compareDialog, "SimpleRules BigDecimal SimpleRuleTable(integer e_var)", 4, 1, "2", "3");
        verifyDifferenceInCells(compareDialog, "Datatype Vocabulary1 <String[]>", 3, 1, "Bla3, Bla4", "Bla3, Bla4. Bla1");
        verifyDifferenceInCells(compareDialog, "SmartLookup DoubleValue SmartLookup1(int intValue, String arg2)", 7, 2, "0.05", "0.005");
        verifyDifferenceInCells(compareDialog, "Conditions  testConditions", 3, 2, "a==d.b", "a==c.b");
        verifyDifferenceInCells(compareDialog, "Conditions  testConditions", 4, 2, "Integer a", "Integer c");
        verifyDifferenceInCells(compareDialog, "Returns", 3, 2, "new Double[] {a, b, c, d}", "new Double[] {a, b, c}");

        compareDialog.openTreeNode("ColumnMatch <MATCH> String ColumnMatchTable(Long RandNumber)");
        compareDialog.clickTreeNode("SmartRules DoubleValue SmartRules1(String stringValue, int integerValue)");
        assertThat(compareDialog.isFirstFragmentPresent()).as("SmartRules1 stands in the first workbook").isTrue();
        assertThat(compareDialog.isSecondFragmentPresent()).as("SmartRules1 is not in the second").isFalse();
        compareDialog.clickTreeNode("Actions");
        assertThat(compareDialog.isFirstFragmentPresent()).as("Actions is not in the first workbook").isFalse();
        assertThat(compareDialog.isSecondFragmentPresent()).as("Actions stands in the second").isTrue();

        compareDialog.openTreeNode("Old Sheet");
        compareDialog.clickTreeNode("SimpleRules BigDecimal SimpleRuleTable2(integer e_var)");
        assertThat(compareDialog.isFirstFragmentPresent()).isTrue();
        assertThat(compareDialog.isSecondFragmentPresent()).isFalse();

        compareDialog.openTreeNode("New test tab");
        compareDialog.clickTreeNode("SmartRules String someRule(Integer a)");
        assertThat(compareDialog.isFirstFragmentPresent()).isFalse();
        assertThat(compareDialog.isSecondFragmentPresent()).isTrue();

        compareDialog.backToFilePicking();
        compareDialog.uploadFile(TestDataUtil.getFilePathFromResources(FILE_1));
        compareDialog.uploadFile(TestDataUtil.getFilePathFromResources(FILE_2));
        compareDialog.setShowEqualElements(true);
        compareDialog.clickCompareExcel();
        assertThat(compareDialog.isTreeItemPresent("Const"))
                .as("A sheet that reads the same is listed once it is asked for")
                .isTrue();
        compareDialog.openTreeNode("Const");
        compareDialog.clickTreeNode("Constants");
        assertThat(compareDialog.isFirstFragmentPresent()).isTrue();
        assertThat(compareDialog.isSecondFragmentPresent()).isTrue();

        compareDialog.close();
    }

    private void verifyDifferenceInCells(CompareExcelFilesDialogComponent dialog,
                                         String nodeName, int row, int col,
                                         String expectedValue1, String expectedValue2) {
        dialog.clickTreeNode(nodeName);
        assertThat(dialog.isFirstFragmentPresent()).as(nodeName + " should have first fragment").isTrue();
        assertThat(dialog.isSecondFragmentPresent()).as(nodeName + " should have second fragment").isTrue();
        assertThat(dialog.getCellContent(1, row, col))
                .as(nodeName + " cell[" + row + "," + col + "] in first file")
                .isEqualTo(expectedValue1);
        assertThat(dialog.getCellContent(2, row, col))
                .as(nodeName + " cell[" + row + "," + col + "] in second file")
                .isEqualTo(expectedValue2);
    }
}
