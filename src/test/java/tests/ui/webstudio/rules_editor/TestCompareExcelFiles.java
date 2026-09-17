package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
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
            + "which lists the sheets that differ and marks the cells that changed. Fails on EPBDS-16655: the "
            + "action opens the comparison of the project against its own revisions instead, and the screen "
            + "that compares two uploaded workbooks is left with no way into it.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16655")
    public void testCompareExcelFiles() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();
        // The action stands in the More menu of a module, so a module is opened to reach it.
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

        // What was uploaded can be taken back and uploaded again, and the comparison is offered either way.
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

        assertThat(compareDialog.isTreeItemPresent("Rules")).as("Rules sheet differs").isTrue();
        assertThat(compareDialog.isTreeItemPresent("Old Sheet")).as("Old Sheet differs").isTrue();
        assertThat(compareDialog.isTreeItemPresent("New test tab")).as("New test tab differs").isTrue();
        assertThat(compareDialog.isTreeItemPresent("Const"))
                .as("A sheet that reads the same on both sides is left out")
                .isFalse();

        compareDialog.openTreeNode("Rules");
        compareDialog.clickTreeNode("Spreadsheet SpreadsheetResult SpreadsheetTable (ByteValue a_byte)");
        assertThat(compareDialog.getHighlightedCellCount(1))
                .as("The differences of the table are marked in the first workbook")
                .isPositive();
        assertThat(compareDialog.getHighlightedCellCount(2))
                .as("The differences of the table are marked in the second workbook")
                .isPositive();
    }
}
