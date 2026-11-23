package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.CopyTableDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCopyTableVersioningUi extends BaseTest {

    private static final String MODULE_NAME = "TestMethodTable";
    private static final String TABLE_FOLDER = "Method";
    private static final String TABLE_NAME = "getGreetings";
    private static final String NEXT_VERSION = "0.0.2";
    private static final String MALFORMED_VERSION = "v2";
    private static final String BUSINESS_DIMENSION = "lob";
    private static final String BUSINESS_DIMENSION_VALUE = "test";
    private static final String KINDS_MODULE = "TableKinds";
    private static final String MERGED_TITLE_TABLE = "Somelogic";
    private static final String MERGED_TITLE_COPY = "SomelogicCopy";

    @Test
    @TestCaseId("EPBDS-16357")
    @Description("Copying a table as a new version must deactivate the source table and leave the module "
            + "compilable (regression guard for EPBDS-16357).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCopyAsNewVersionKeepsModuleCompilable() {
        EditorPage editorPage = openTable();

        editorPage.getEditorToolbarPanelComponent().copyTableAsNewVersion(NEXT_VERSION);
        editorPage.waitUntilSpinnerLoaded();

        assertThat(editorPage.getProblemsPanelComponent().getAllErrors())
                .as("A new version of a table must not leave two active tables behind")
                .isEmpty();
    }

    @Test
    @TestCaseId("IPBQA-31319")
    @Description("Copying a table with a business dimension property set keeps the module compilable: the "
            + "dimension tells the two tables apart, so no deactivation is needed.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCopyAsBusinessDimensionVersionKeepsModuleCompilable() {
        EditorPage editorPage = openTable();

        editorPage.getEditorToolbarPanelComponent().copyTableAsBusinessDimension(BUSINESS_DIMENSION, BUSINESS_DIMENSION_VALUE);
        editorPage.waitUntilSpinnerLoaded();

        assertThat(editorPage.getProblemsPanelComponent().getAllErrors())
                .as("A copy carrying its own business dimension must compile alongside the original")
                .isEmpty();
    }

    @Test
    @TestCaseId({"EPBDS-17262", "EPBDS-7390"})
    @Description("Copying Conditions, Actions, Returns and Constants tables creates the copy under the new name "
            + "instead of failing with 'The table name cannot be found in its header' (EPBDS-17262), and copying a "
            + "table whose title is merged over two rows keeps the title in one cell (EPBDS-7390).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCopyOfDefinitionTablesAndMergedTitle() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "TableKinds.xlsx");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, KINDS_MODULE);
        editorPage.getEditorLeftRulesTreeComponent().setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);

        copyDefinition(editorPage, TableTypeFolders.CONDITIONS, "MyConditions");
        copyDefinition(editorPage, TableTypeFolders.ACTIONS, "MyActions");
        copyDefinition(editorPage, TableTypeFolders.RETURNS, "MyReturns");
        copyDefinition(editorPage, TableTypeFolders.CONSTANTS, "MyConstants");

        String originalTitle = spreadsheetTitle(MERGED_TITLE_TABLE);
        String copyTitle = spreadsheetTitle(MERGED_TITLE_COPY);
        selectTable(editorPage, TableTypeFolders.SPREADSHEET, MERGED_TITLE_TABLE, originalTitle);
        List<String> originalBody = withoutTitle(editorPage.getCenterTable().getCellTexts(), originalTitle);
        editorPage.getEditorToolbarPanelComponent().copyTableAsNew(MERGED_TITLE_COPY, null);
        editorPage.waitUntilSpinnerLoaded();
        selectTable(editorPage, TableTypeFolders.SPREADSHEET, MERGED_TITLE_COPY, copyTitle);
        List<String> copyCells = editorPage.getCenterTable().getCellTexts();

        assertThat(copyCells)
                .as("The title of the copy must be written in one cell, and the original title must not be repeated (EPBDS-7390)")
                .containsOnlyOnce(copyTitle)
                .doesNotContain(originalTitle);
        assertThat(withoutTitle(copyCells, copyTitle))
                .as("Apart from its title the copy must hold the cells of the original in the same order (EPBDS-7390)")
                .containsExactlyElementsOf(originalBody);
    }

    private void copyDefinition(EditorPage editorPage, String folder, String tableName) {
        String copyName = tableName + "Copy";
        selectTable(editorPage, folder, tableName, folder + " " + tableName);
        editorPage.getEditorToolbarPanelComponent().copyTableAsNew(copyName, null);
        editorPage.waitUntilSpinnerLoaded();
        selectTable(editorPage, folder, copyName, folder + " " + copyName);
    }

    private String spreadsheetTitle(String tableName) {
        return "Spreadsheet SpreadsheetResult " + tableName + "()";
    }

    private List<String> withoutTitle(List<String> cells, String title) {
        return cells.stream().filter(cell -> !cell.equals(title)).toList();
    }

    private void selectTable(EditorPage editorPage, String folder, String tableName, String header) {
        editorPage.getEditorLeftRulesTreeComponent()
                .expandFolderInTree(folder)
                .selectItemInFolder(folder, tableName);
        editorPage.waitUntilSpinnerLoaded();
        assertThat(WaitUtil.waitForCondition(() -> header.equals(editorPage.getCenterTable().getCellText(1, 1)),
                10000, 200, "Waiting for the table '" + header + "' to open"))
                .as("The table '%s' must open with its header '%s'", tableName, header)
                .isTrue();
    }

    private EditorPage openTable() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "TestMethodTable.xlsx");
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE_NAME);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TABLE_FOLDER)
                .selectItemInFolder(TABLE_FOLDER, TABLE_NAME);
        editorPage.getProblemsPanelComponent().checkNoProblems();
        return editorPage;
    }
}
