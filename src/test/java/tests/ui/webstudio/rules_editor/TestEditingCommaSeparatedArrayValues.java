package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.MultiselectArrayEditorComponent;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorTableActionsPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestEditingCommaSeparatedArrayValues extends BaseTest {

    private static final String DDL_PROJECT_FILE = "projEditCommaSeparatedArr.xlsx";
    private static final String DDL_MODULE = "projEditCommaSeparatedArr";

    private static final String NULL_ELEM_PROJECT_FILE = "NullElemTest.xlsx";
    private static final String NULL_ELEM_MODULE = "NullElemTest";

    @Test
    @TestCaseId("EPBDS-13232")
    @Description("Editing a comma-separated array with an empty element ('1,,2,3') shows a server failure message but does not break the project")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testVerifyNoInfiniteLoading() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, NULL_ELEM_PROJECT_FILE);
        EditorPage editorPage = new EditorPage();
        EditorLeftRulesTreeComponent rulesTree = editorPage.getEditorLeftRulesTreeComponent();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, NULL_ELEM_MODULE);
        rulesTree.setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", "removeNullsStringTest");

        TableComponent table = editorPage.getCenterTable();
        table.editCell(4, 2, "1,,2,3");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        boolean failureMessageVisible = editorPage.getAllMessages().stream()
                .anyMatch(msg -> msg.contains("Sorry! Server failed to apply your changes!"));
        assertThat(failureMessageVisible)
                .as("'Sorry! Server failed to apply your changes!' message should NOT be visible — empty elements must be silently allowed")
                .isFalse();

        domain.ui.webstudio.pages.mainpages.RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(domain.ui.webstudio.components.common.TabSwitcherComponent.TabName.REPOSITORY);
        assertThat(repositoryPage.isProjectPresent(projectName))
                .as("Project '%s' should still be present in the repository", projectName)
                .isTrue();
    }

    @Test
    @TestCaseId("IPBQA-25824")
    @Description("Editing comma-separated array values via the multiselect popup: chosen/non-chosen values, Select All / Deselect All, save and re-open, switch to Formula Editor and back, multiple table types (Decision/Spreadsheet/TBasic/Method)")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testEditingCommaSeparatedArrayValues() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, DDL_PROJECT_FILE);
        EditorPage editorPage = new EditorPage();
        EditorLeftRulesTreeComponent rulesTree = editorPage.getEditorLeftRulesTreeComponent();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, DDL_MODULE);
        rulesTree.setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);

        rulesTree.expandFolderInTree("Data").selectItemInFolder("Data", "DataDDL");
        TableComponent table = editorPage.getCenterTable();
        String cellContent = table.getCellText(4, 1);

        MultiselectArrayEditorComponent multiselect = editorPage.getMultiselectArrayEditorComponent();
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        table.doubleClickCell(4, 1);
        multiselect.verifyChosenValues(Collections.singletonList(cellContent));
        multiselect.verifyNonChosenValues("0.001", "500", "1");

        multiselect.clickActionButton("Done");
        verifyEditTableCellContent(table, 4, 1, cellContent);

        table.doubleClickCell(4, 1);
        multiselect.setAllValuesChosen(true);
        multiselect.verifyChosenValues(Arrays.asList("0.001", "-333", "500", "1"));

        verifyValuesAfterDoneAndAfterSave(editorPage, "0.001,-333,500,1", 4, 1);

        table.doubleClickCell(4, 1);
        multiselect.setAllValuesChosen(false);
        multiselect.verifyNonChosenValues("0.001", "-333", "500", "1");

        verifyValuesAfterDoneAndAfterSave(editorPage, " ", 4, 1);

        table.doubleClickCell(4, 1);
        multiselect.selectValues("1");
        verifyValuesAfterDoneAndAfterSave(editorPage, "1", 4, 1);

        table.doubleClickCell(4, 1);
        multiselect.clearAllValues();
        multiselect.selectValues(cellContent);
        multiselect.clickActionButton("Done");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        verifyEditTableCellContent(table, 4, 1, cellContent);
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        chooseTableAndVerifyCell(editorPage, table, multiselect, DECISION, "SimpleLookupTable",
                Arrays.asList("Alaska", "Connecticut", "District of Columbia", "Delaware", "Georgia", "Wyoming"));
        multiselect.selectValues("Florida");
        verifyValuesAfterDoneAndAfterSave(editorPage, "AK,CT,DC,DE,GA,WY,FL", 2, 3);

        chooseTableAndVerifyCell(editorPage, table, multiselect, DECISION, "SimpleRulesTable",
                Arrays.asList("Alaska", "Alabama"));
        multiselect.selectValues("Florida");
        verifyValuesAfterDoneAndAfterSave(editorPage, "AL,AK,FL", 2, 3);

        chooseTableAndVerifyCell(editorPage, table, multiselect, DECISION, "SmartLookup1",
                Collections.singletonList("Americas"));
        multiselect.selectValues("European Union");
        verifyValuesAfterDoneAndAfterSave(editorPage, "NCSA,EU", 2, 3);

        chooseTableAndVerifyCell(editorPage, table, multiselect, DECISION, "SmartRules1",
                Collections.singletonList("Île-du-Prince-Édouard"));
        multiselect.selectValues("Ontario");
        verifyValuesAfterDoneAndAfterSave(editorPage, "PE,ON", 2, 3);

        chooseTableAndVerifyCell(editorPage, table, multiselect, "Spreadsheet", "SpreadsheetTable",
                Arrays.asList("Russia, Rubles", "Saudi Arabia, Riyals"));
        multiselect.selectValues("Philippines, Pesos");
        verifyValuesAfterDoneAndAfterSave(editorPage, "RUB,SAR,PHP", 2, 3);

        chooseTableAndVerifyCell(editorPage, table, multiselect, "TBasic", "TBasicTable",
                Arrays.asList("Alabama", "Utah"));
        multiselect.selectValues("Colorado");
        verifyValuesAfterDoneAndAfterSave(editorPage, "AL,UT,CO", 2, 3);

        chooseTableAndVerifyCell(editorPage, table, multiselect, "Method", "MethodTable",
                Arrays.asList("Americas", "European Union"));
        multiselect.selectValues("Asia Pacific; Japan");
        verifyValuesAfterDoneAndAfterSave(editorPage, "NCSA,EU,APJ", 2, 3);
    }

    private void chooseTableAndVerifyCell(EditorPage editorPage,
                                          TableComponent table,
                                          MultiselectArrayEditorComponent multiselect,
                                          String folderName,
                                          String tableName,
                                          List<String> expectedChosenValues) {
        editorPage.getEditorLeftRulesTreeComponent()
                .expandFolderInTree(folderName)
                .selectItemInFolder(folderName, tableName);
        table.doubleClickCell(2, 3);
        multiselect.verifyChosenValues(expectedChosenValues);
    }

    private void verifyValuesAfterDoneAndAfterSave(EditorPage editorPage, String expectedValue, int row, int column) {
        TableComponent table = editorPage.getCenterTable();
        editorPage.getMultiselectArrayEditorComponent().clickActionButton("Done");
        int anchorRow = row == 1 ? 2 : 1;
        int anchorCol = column == 1 ? 2 : 1;
        table.clickCell(anchorRow, anchorCol);
        verifyEditTableCellContent(table, row, column, expectedValue);

        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();
        actions.clickSaveChanges();
        verifyEditTableCellContent(table, row, column, expectedValue);
    }

    private void verifyEditTableCellContent(TableComponent table, int row, int column, String expectedValue) {
        long deadline = System.currentTimeMillis() + 10_000;
        String actual = "";
        String expectedNormalized = normalize(expectedValue);
        while (System.currentTimeMillis() < deadline) {
            actual = table.getCellText(row, column);
            if (expectedNormalized.equals(normalize(actual))) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError(String.format(
                "Cell [%d,%d] content mismatch. Expected: '%s', actual: '%s'",
                row, column, expectedValue, actual));
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace(' ', ' ').trim();
    }
}
