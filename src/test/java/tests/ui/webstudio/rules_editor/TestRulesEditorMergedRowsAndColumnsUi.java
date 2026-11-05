package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.SettingsMenuComponent;
import domain.ui.webstudio.components.common.SettingsMenuComponent.Setting;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorTableActionsPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.RightTableDetailsComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DATA;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DATATYPE;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestRulesEditorMergedRowsAndColumnsUi extends BaseTest {

    private static final String TUTORIAL_1 = "Tutorial 1 - Introduction to Decision Tables";
    private static final String TUTORIAL_1_MODULE = "Tutorial1 - Intro to Decision Tables";
    private static final String TUTORIAL_3 = "Tutorial 3 - More Advanced Decision and Data Tables";
    private static final String TUTORIAL_3_MODULE = "Tutorial3 - Advanced Decision and Data Tables";
    private static final String ADDRESSES = "addresses";
    private static final String EXAMPLE_3 = "Example 3 - Auto Policy Calculation";
    private static final String EXAMPLE_3_MODULE = "AutoPolicyCalculation";
    private static final String LONG_GROUP_WORKBOOK = "LongMergedGroup.xlsx";
    private static final String SHIFTED_WORKBOOK = "ShiftedCellEditor.xlsx";

    @Test
    @TestCaseId("EPBDS-16879")
    @Description("EPBDS-16716: Insert Row After on a rule of a vertically merged group. The merge grows over the new "
            + "row, the new row offers its own cells under # Rule, Marital Status and Premium Increase, and each typed "
            + "value is saved in the column it was typed under.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void insertedRowInsideMergedGroupKeepsValuesInTheirColumns() {
        EditorPage editorPage = openTable(TUTORIAL_1, TUTORIAL_1_MODULE, "DriverPremium2");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        TableComponent table = editorPage.getCenterTable();
        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();

        assertThat(table.getRow(6).getValue())
                .as("Rule R1 is the first row of the Young Driver group")
                .containsExactly("R1", "Young Driver", "Married", "700");
        table.clickCell(6, 3);
        actions.clickInsertRowAfter();
        assertThat(table.getRow(7).getCells())
                .as("The new row lies inside the Young Driver merge, so it must offer exactly the cells of "
                        + "# Rule, Marital Status and Premium Increase")
                .hasSize(3);

        table.editCell(7, 1, "R1b");
        table.editCell(7, 2, "Widowed");
        table.editCell(7, 3, "999");
        actions.clickSaveChanges();

        reloadTable(editorPage, DECISION, "DriverPremium2");
        table = editorPage.getCenterTable();
        assertThat(table.getRow(7).getValue())
                .as("After reload each value of the new rule must stand in the column it was typed under")
                .containsExactly("R1b", "Widowed", "999");
        assertThat(table.getRow(8).getValue())
                .as("Rule R2 must stay intact after the insert")
                .containsExactly("R2", "Single", "720");
    }

    @Test
    @TestCaseId("EPBDS-16879")
    @Description("EPBDS-16717: with Show Header off a cell edit is saved into the cell that was edited, not into the "
            + "cell the hidden header rows shift it to.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void cellEditWithHeaderHiddenIsSavedIntoTheEditedCell() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TUTORIAL_1);
        EditorPage editorPage = new EditorPage();
        new SettingsMenuComponent().set(Setting.SHOW_HEADER, false);

        editorPage = new EditorPage();
        selectTable(editorPage, projectName, TUTORIAL_1_MODULE, "DriverPremium1");
        TableComponent table = editorPage.getCenterTable();
        int lastRule = table.getRowsCount();
        assertThat(table.getRow(lastRule).getValue())
                .as("With Show Header off the last row drawn is the Standard Driver rule")
                .containsExactly("Standard Driver", "", "500");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        table.editCell(lastRule, 3, "555");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        reloadTable(editorPage, DECISION, "DriverPremium1");
        table = editorPage.getCenterTable();
        assertThat(table.getRow(table.getRowsCount()).getValue())
                .as("The premium typed into the Standard Driver rule must be saved there")
                .containsExactly("Standard Driver", "", "555");
        assertThat(table.getCellTexts())
                .as("No other rule may change: Young Driver / Married keeps 700 and the table keeps compiling")
                .contains("700", "720", "300", "350")
                .doesNotContain("500");
    }

    @Test
    @TestCaseId("EPBDS-16879")
    @Description("EPBDS-16718: Insert Row After on the first rule of a group merged in the first column adds one row "
            + "inside the group, and Save writes it instead of refusing the table with HTTP 400.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void insertAfterFirstRuleOfGroupMergedInFirstColumnIsSaved() {
        EditorPage editorPage = openTable(EXAMPLE_3, EXAMPLE_3_MODULE, "DriverPremium");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        TableComponent table = editorPage.getCenterTable();
        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();

        assertThat(table.getRow(3).getValue())
                .as("The first Young Driver rule opens the group merged in the Driver Age column")
                .containsExactly("Young Driver", "Married", "CA,NY,VA", "700");
        int rulesBefore = table.getRowsCount();
        table.clickCell(3, 2);
        actions.clickInsertRowAfter();
        table.editCell(4, 3, "999");
        actions.clickSaveChanges();

        reloadTable(editorPage, DECISION, "DriverPremium");
        table = editorPage.getCenterTable();
        assertThat(table.getRowsCount())
                .as("Exactly one rule must be added")
                .isEqualTo(rulesBefore + 1);
        assertThat(table.getRow(4).getValue())
                .as("The new rule must stand right after the first Young Driver rule, inside the group")
                .containsExactly("", "", "999");
        assertThat(table.getRow(5).getValue())
                .as("The rule that followed must stay intact")
                .containsExactly("Single", "CA,NY,VA", "720");
    }

    @Test
    @TestCaseId("EPBDS-16879")
    @Description("EPBDS-16720: saving the table properties panel keeps the cell edits still waiting on screen and "
            + "writes them together with the property.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void savingPropertiesKeepsPendingCellEdits() {
        EditorPage editorPage = openTable(TUTORIAL_1, TUTORIAL_1_MODULE, "DriverPremium1");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        TableComponent table = editorPage.getCenterTable();
        int youngMarried = table.getRowIndexHolding("Young Driver", "Married");
        table.editCell(youngMarried, 3, "777");
        assertThat(table.getCellText(youngMarried, 3))
                .as("The edit waits on screen before the properties are saved")
                .isEqualTo("777");

        RightTableDetailsComponent details = new RightTableDetailsComponent();
        details.addProperty("Category").setProperty("Category", "Premiums").clickSaveBtn();

        reloadTable(editorPage, DECISION, "DriverPremium1");
        table = editorPage.getCenterTable();
        assertThat(table.getCellTexts())
                .as("The premium edited before the properties were saved must be written, not dropped")
                .contains("777")
                .doesNotContain("700");
        assertThat(new RightTableDetailsComponent().isPropertySet("Category", "Premiums"))
                .as("The property must be written as well")
                .isTrue();
    }

    @Test
    @TestCaseId("EPBDS-16879")
    @Description("EPBDS-16721: Remove Column on the first column of a decision table is saved instead of being "
            + "refused with 'must be greater than or equal to 1'.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void removingFirstColumnIsSaved() {
        EditorPage editorPage = openTable(TUTORIAL_1, TUTORIAL_1_MODULE, "DriverPremium1");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        TableComponent table = editorPage.getCenterTable();
        table.clickCell(table.getRowIndexHolding("Senior Driver", "Married"), 1);
        editorPage.getEditorTableActionsPanelComponent().clickRemoveColumn();
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        reloadTable(editorPage, DECISION, "DriverPremium1");
        List<String> cells = editorPage.getCenterTable().getCellTexts();
        assertThat(cells)
                .as("The Driver Age column must be gone after reload")
                .doesNotContain("Driver Age", "Young Driver", "Senior Driver", "Standard Driver");
        assertThat(cells)
                .as("The other columns must stay")
                .contains("Marital Status", "Premium Increase", "700", "500");
    }

    @Test
    @TestCaseId("EPBDS-16879")
    @Description("EPBDS-16722: a vertical merge that crosses the 120-row window of the editor stays one cell, and "
            + "Remove Row on it removes every rule it spans.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void removeRowOnMergeAcrossRowWindowRemovesWholeGroup() {
        EditorPage editorPage = openWorkbookTable(LONG_GROUP_WORKBOOK, DECISION, "Grp");
        TableComponent table = showAllRows(editorPage);
        assertThat(table.getRowsCount())
                .as("The whole table must be drawn: the title, the header and 145 rules")
                .isEqualTo(147);
        int groupRow = table.getRowIndexHolding("M", "1", "1001");
        assertThat(groupRow)
                .as("The M group must start before row 120 and end after it, crossing the window edge")
                .isLessThan(120);

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        table.clickCell(groupRow, 1);
        editorPage.getEditorTableActionsPanelComponent().clickRemoveRow();
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        reloadTable(editorPage, DECISION, "Grp");
        table = showAllRows(editorPage);
        assertThat(table.getRowsCount())
                .as("Removing the merged M cell must remove all 12 rules it spans")
                .isEqualTo(135);
        assertThat(table.getCellTexts())
                .as("No rule of the M group may stay in the table")
                .doesNotContain("M", "1001", "1005", "1006", "1012")
                .contains("A112", "112", "Z1", "2001");
    }

    @Test
    @TestCaseId("EPBDS-16879")
    @Description("EPBDS-16723: after an unsaved Insert Row, Remove Row or Insert Column each shifted cell opens with "
            + "the editor of its own field: the Double rate and the BigDecimal limit of a Datatype keep their decimal "
            + "point after Insert Row and after Remove Row, the String default of the new field takes text, and the "
            + "Double price of a Data table keeps its decimal point after Insert Column.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void shiftedCellsOpenWithTheEditorOfTheirOwnField() {
        EditorPage editorPage = openWorkbookTable(SHIFTED_WORKBOOK, DATATYPE, "Policy");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        TableComponent table = editorPage.getCenterTable();
        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();

        table.clickCell(table.getRowIndexHolding("String", "name", "x"), 2);
        actions.clickInsertRowAfter();
        int newRow = table.getRowIndexHolding("String", "name", "x") + 1;
        table.editCell(newRow, 1, "String");
        table.editCell(newRow, 2, "note");
        WebElement noteDefault = table.openCellEditor(newRow, 3);
        noteDefault.clear();
        noteDefault.fillSequentially("memo");
        assertThat(noteDefault.getCurrentInputValue())
                .as("The default of the new String field must take text")
                .isEqualTo("memo");
        noteDefault.press("Enter");

        typeDecimal(table, table.getRowIndexHolding("Double", "rate"), 3, "0.5", "0.75",
                "The Double rate default shifted by the insert");
        typeDecimal(table, table.getRowIndexHolding("BigDecimal", "limit"), 3, "1000.5", "2500.75",
                "The BigDecimal limit default shifted by the insert");
        actions.clickSaveChanges();

        reloadTable(editorPage, DATATYPE, "Policy");
        table = editorPage.getCenterTable();
        assertThat(table.getRow(table.getRowIndexHolding("Double", "rate")).getValue())
                .as("rate must be saved as 0.75, not as 75")
                .containsExactly("Double", "rate", "0.75");
        assertThat(table.getRow(table.getRowIndexHolding("BigDecimal", "limit")).getValue())
                .as("limit must be saved as 2500.75, not as 250075")
                .containsExactly("BigDecimal", "limit", "2500.75");
        assertThat(table.getRow(table.getRowIndexHolding("String", "note")).getValue())
                .as("The new field must be saved as typed")
                .containsExactly("String", "note", "memo");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        table = editorPage.getCenterTable();
        table.clickCell(table.getRowIndexHolding("Integer", "count"), 2);
        actions.clickRemoveRow();
        typeDecimal(table, table.getRowIndexHolding("BigDecimal", "limit"), 3, "2500.75", "3000.25",
                "The BigDecimal limit default shifted into the place of the removed Integer field");
        actions.clickSaveChanges();

        reloadTable(editorPage, DATATYPE, "Policy");
        table = editorPage.getCenterTable();
        assertThat(table.getColumn(2))
                .as("Only the removed field must be gone")
                .contains("name", "note", "rate", "limit", "flag")
                .doesNotContain("count");
        assertThat(table.getRow(table.getRowIndexHolding("BigDecimal", "limit")).getValue())
                .as("limit must be saved as 3000.25 after Remove Row, not as 300025")
                .containsExactly("BigDecimal", "limit", "3000.25");

        editorPage.getEditorLeftRulesTreeComponent()
                .expandFolderInTree(DATA)
                .selectItemInFolder(DATA, "lines");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        table = editorPage.getCenterTable();
        int lineRow = table.getRowIndexHolding("12.5", "2");
        table.clickCell(lineRow, 1);
        actions.clickInsertColumnBefore();
        typeDecimal(table, lineRow, 2, "12.5", "12.75",
                "The Double price shifted into the place of the Integer qty by Insert Column");
        actions.clickSaveChanges();

        reloadTable(editorPage, DATA, "lines");
        assertThat(editorPage.getCenterTable().getRow(lineRow).getValue())
                .as("price must be saved as 12.75 after Insert Column, not as 1275")
                .containsExactly("", "12.75", "2");
    }

    @Test
    @TestCaseId({"EPBDS-16762", "EPBDS-16774"})
    @Description("EPBDS-16762: in the Data table addresses of Tutorial 3, a column inserted before city and a row "
            + "inserted after the last row, with a value only where they cross, are saved instead of being refused "
            + "with 'A row or column cannot be entirely empty'. EPBDS-16774: after Remove Row and Remove Column the "
            + "table keeps the keyboard focus and the cell that takes the place of the removed line is picked, so "
            + "the next lines are removed without picking a cell again.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void crossingLinesAreSavedAndRemovingLinesKeepsTheNextCellPicked() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TUTORIAL_3);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, TUTORIAL_3_MODULE);
        reloadTable(editorPage, DATA, ADDRESSES);
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        TableComponent table = editorPage.getCenterTable();
        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();

        int fieldsRow = table.getRowIndexHolding("street1", "street2", "city");
        int lastRow = table.getRowsCount();
        int fieldCells = table.getRow(fieldsRow).getCells().size();
        table.clickCell(fieldsRow, 3);
        actions.clickInsertColumnBefore();
        table.clickCell(lastRow, 1);
        actions.clickInsertRowAfter();
        table.editCell(lastRow + 1, 3, "Apt 5");
        actions.clickSaveChanges();

        reloadTable(editorPage, DATA, ADDRESSES);
        table = editorPage.getCenterTable();
        assertThat(table.getRowsCount())
                .as("EPBDS-16762: the row inserted after the last row must be saved")
                .isEqualTo(lastRow + 1);
        assertThat(table.getRow(fieldsRow).getCells())
                .as("EPBDS-16762: the column inserted before city must be saved")
                .hasSize(fieldCells + 1);
        assertThat(table.getCellText(lastRow + 1, 3))
                .as("EPBDS-16762: the value standing where the new row and the new column cross must be saved")
                .isEqualTo("Apt 5");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        table = editorPage.getCenterTable();
        table.clickCell(table.getRowIndexHolding("1600 Pennsylvania Avenue NW"), 1);
        actions.clickRemoveRow();
        table.waitForPickedCell("1085 Summit Dr");
        assertThat(table.getPickedCellTexts())
                .as("EPBDS-16774: after Remove Row the row that took the place of the removed one must be picked")
                .containsExactly("1085 Summit Dr");
        assertThat(table.hasKeyboardFocus())
                .as("EPBDS-16774: after Remove Row the keyboard focus must stay in the table")
                .isTrue();

        for (int step = 0; step < 3; step++) {
            table.pressKeyOnPage("ArrowRight");
        }
        table.waitForPickedCell("Beverly Hills");
        assertThat(table.getPickedCellTexts())
                .as("EPBDS-16774: the arrow keys must move the picked cell right after Remove Row")
                .containsExactly("Beverly Hills");

        actions.clickRemoveColumn();
        table.waitForPickedCell("CA");
        assertThat(table.getPickedCellTexts())
                .as("EPBDS-16774: after Remove Column the column that took the place of the removed one must be picked")
                .containsExactly("CA");
        assertThat(table.hasKeyboardFocus())
                .as("EPBDS-16774: after Remove Column the keyboard focus must stay in the table")
                .isTrue();

        actions.clickRemoveColumn();
        table.waitForPickedCell("90210");
        assertThat(table.getPickedCellTexts())
                .as("EPBDS-16774: the next column must be removed without picking a cell again, and the next one picked")
                .containsExactly("90210");
        assertThat(table.getCellTexts())
                .as("EPBDS-16774: the removed row and both removed columns must be gone")
                .doesNotContain("Beverly Hills", "CA", "1600 Pennsylvania Avenue NW");
    }

    private void typeDecimal(TableComponent table, int row, int column, String shown, String typed, String what) {
        WebElement editor = table.openCellEditor(row, column);
        assertThat(editor.getCurrentInputValue())
                .as(what + " must open showing its own value")
                .isEqualTo(shown);
        editor.clear();
        editor.fillSequentially(typed);
        assertThat(editor.getCurrentInputValue())
                .as(what + " must take the decimal point")
                .isEqualTo(typed);
        editor.press("Enter");
    }

    private EditorPage openTable(String template, String module, String tableName) {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, template);
        EditorPage editorPage = new EditorPage();
        selectTable(editorPage, projectName, module, tableName);
        return editorPage;
    }

    private EditorPage openWorkbookTable(String workbook, String folder, String tableName) {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, workbook);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, workbook.replace(".xlsx", ""));
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(folder)
                .selectItemInFolder(folder, tableName);
        return editorPage;
    }

    private TableComponent showAllRows(EditorPage editorPage) {
        TableComponent table = editorPage.getCenterTable();
        WebElement showMoreRows = new WebElement(DriverPool.getPage(),
                "xpath=//button[@data-testid='module-table-more']", "showMoreRowsBtn");
        while (showMoreRows.isVisible(2000)) {
            int drawn = table.getRowsCount();
            showMoreRows.click();
            WaitUtil.requireCondition(() -> table.getRowsCount() > drawn, 30000, 250,
                    "Waiting for the next rows of the table to be drawn");
        }
        return table;
    }

    private void reloadTable(EditorPage editorPage, String folder, String tableName) {
        editorPage.reloadPage();
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(folder)
                .selectItemInFolder(folder, tableName);
    }

    private void selectTable(EditorPage editorPage, String projectName, String module, String tableName) {
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, module);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, tableName);
    }
}
