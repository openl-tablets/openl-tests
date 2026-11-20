package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.api.ProjectsMethod;
import domain.serviceclasses.constants.User;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.admincomponents.SystemSettingsPageComponent;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.components.common.ThemeMenuComponent;
import domain.ui.webstudio.components.editortabcomponents.CreateTableDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.service.WorkflowService;
import helpers.utils.ProjectLinkUtil;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestFormatOnSaveCompiledRolesUi extends BaseTest {

    private static final UserData RULES_AUTHOR = new UserData("format_author", "format_author");
    private static final String WORKBOOK = "FormatOnSaveRoles.xlsx";
    private static final String MODULE = "FormatOnSaveRoles";
    private static final String BIG_SHEET = "Big";
    private static final String MARKED_STEP_CELL = "2:0";

    private static final List<TableEdit> ROLE_CHANGING_EDITS = List.of(
            new TableEdit("Quote", new CellEdit(3, 1, "Doubled*")),
            new TableEdit("Across", new CellEdit(2, 3, "Tax*")),
            new TableEdit("RulesGrade", new CellEdit(2, 2, "RET1"), new CellEdit(3, 2, "l"),
                    new CellEdit(2, 3, "C2"), new CellEdit(3, 3, "level == r")));

    private static final List<TableEdit> BIG_TABLE_EDITS = List.of(
            new TableEdit("BigGrade", new CellEdit(1, 1, "SmartRules Result BigGrade(Integer score)")),
            new TableEdit("bigPeople", new CellEdit(2, 1, "_PK_")));

    private static final Map<String, Integer> BIG_TABLE_CELLS = Map.of("BigGrade", 3004, "bigPeople", 2005);

    private static final List<TableEdit> EDITS_KEEPING_THE_LOOK = List.of(
            new TableEdit("Twice", new CellEdit(2, 1, "return a * 3;")),
            new TableEdit("Constants", new CellEdit(2, 3, "2.5")),
            new TableEdit("Defaults", new CellEdit(3, 2, "Home")));

    @Test
    @TestCaseId({"IPBQA-33075", "EPBDS-17222"})
    @Description("EPBDS-17222: with Format tables on save, an edit in the table editor that changes how a table is "
            + "read gives the table at this save the look a Reformat gives: a step of a SpreadsheetResult marked *, "
            + "in a column or in a row, and a condition and a return of a Rules table changing places at one save.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void roleChangingEditTakesTheLookOfReformatAtThisSave() {
        EditorPage editorPage = openWorkbookAsNewAuthor(true);
        Looks looks = saveAndReformat(editorPage, ROLE_CHANGING_EDITS);

        for (TableEdit edit : ROLE_CHANGING_EDITS) {
            assertThat(bodyOf(looks.afterSave().get(edit.table())))
                    .as("The save of %s must change the look of the body to its new roles", edit.table())
                    .isNotEqualTo(bodyOf(looks.before().get(edit.table())));
            assertThat(looks.afterSave().get(edit.table()))
                    .as("%s must look after its first save as after a Reformat", edit.table())
                    .isEqualTo(looks.afterReformat().get(edit.table()));
        }

        new ThemeMenuComponent().setExcelFormatting(true);
        openTable(editorPage, "Quote");
        assertThat(editorPage.getCenterTable().getCell(3, 1).getCssValue("font-weight"))
                .as("With Excel Formatting on, the step Doubled* is drawn bold, as the Excel file holds it")
                .isEqualTo("700");
    }

    @Test
    @TestCaseId({"IPBQA-33075", "EPBDS-17222"})
    @Description("EPBDS-17222: tables of 1000 rows take the look of their new roles at the save that changes them: "
            + "a SmartRules table whose column turns from a condition into a return, and a Data table given keys, "
            + "look to their last row as after a Reformat.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void bigTablesTakeTheLookOfReformatToTheirLastRow() {
        EditorPage editorPage = openWorkbookAsNewAuthor(true);

        editorPage.getEditorLeftRulesTreeComponent().expandFolderInTree(BIG_SHEET);
        Looks looks = saveAndReformat(editorPage, BIG_TABLE_EDITS);

        for (TableEdit edit : BIG_TABLE_EDITS) {
            assertThat(looks.afterSave().get(edit.table()))
                    .as("%s must be read with all its 1000 rows", edit.table())
                    .hasSize(BIG_TABLE_CELLS.get(edit.table()));
            assertThat(lastRowOf(looks.afterSave().get(edit.table())))
                    .as("The save of %s must change the look of its last row to the new roles", edit.table())
                    .isNotEqualTo(lastRowOf(looks.before().get(edit.table())));
            assertThat(looks.afterSave().get(edit.table()))
                    .as("%s must look after its first save as after a Reformat", edit.table())
                    .isEqualTo(looks.afterReformat().get(edit.table()));
        }
    }

    @Test
    @TestCaseId({"IPBQA-33075", "EPBDS-17222"})
    @Description("EPBDS-17222: a Rules, SmartRules, SimpleRules, SmartLookup, SimpleLookup, Spreadsheet, Test or Data "
            + "table created in the Create Table window looks after its first save as after a Reformat.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void createdTableTakesTheLookOfReformatAtItsFirstSave() {
        EditorPage editorPage = openWorkbookAsNewAuthor(true);

        createInTheWindow(editorPage);
        List<String> created = List.of("NewRules", "NewSmartRules", "NewSimpleRules", "NewSmartLookup",
                "NewSimpleLookup", "NewSpreadsheet", "NewGradeTest", "newPeople");

        Map<String, Map<String, String>> looksAfterSave = looksOfTables(created);
        reformat(editorPage);
        Map<String, Map<String, String>> looksAfterReformat = looksOfTables(created);

        for (String table : created) {
            assertThat(looksAfterSave.get(table))
                    .as("The created %s must look after its first save as after a Reformat", table)
                    .isEqualTo(looksAfterReformat.get(table));
        }
    }

    @Test
    @TestCaseId({"IPBQA-33075", "EPBDS-17222"})
    @Description("EPBDS-17222: a table copied right after a save of its workbook takes the style of the table it is "
            + "copied from and the look a Reformat gives.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void copiedTableTakesTheLookOfReformat() {
        EditorPage editorPage = openWorkbookAsNewAuthor(true);

        saveEdit(editorPage, new TableEdit("cars", new CellEdit(4, 1, "A6")));
        openTable(editorPage, "SmartGrade");
        editorPage.getEditorToolbarPanelComponent().copyTableAsNew("SmartGradeCopy", null);
        editorPage.waitUntilSpinnerLoaded();

        Map<String, String> original = lookOf("SmartGrade");
        Map<String, String> copyAfterSave = lookOf("SmartGradeCopy");
        reformat(editorPage);

        assertThat(bodyOf(copyAfterSave))
                .as("The copy must take the style of the table it is copied from")
                .isEqualTo(bodyOf(original));
        assertThat(copyAfterSave)
                .as("The copy must look after its first save as after a Reformat")
                .isEqualTo(lookOf("SmartGradeCopy"));
    }

    @Test
    @TestCaseId({"IPBQA-33075", "EPBDS-17222"})
    @Description("EPBDS-17222 out of scope: an edit of a Method, Constants or Properties table changes no look in "
            + "the file, and the table still looks as after a Reformat.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void editOfMethodConstantsOrPropertiesKeepsTheLook() {
        EditorPage editorPage = openWorkbookAsNewAuthor(true);
        Looks looks = saveAndReformat(editorPage, EDITS_KEEPING_THE_LOOK);

        for (TableEdit edit : EDITS_KEEPING_THE_LOOK) {
            assertThat(looks.afterSave().get(edit.table()))
                    .as("The edit of %s must change no look", edit.table())
                    .isEqualTo(looks.before().get(edit.table()))
                    .isEqualTo(looks.afterReformat().get(edit.table()));
        }
    }

    @Test
    @TestCaseId({"IPBQA-33075", "EPBDS-17222"})
    @Description("EPBDS-17222: with Format tables on save turned off, the save of a step marked * leaves the look "
            + "of the table as it was, and only a Reformat makes the step bold.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void editKeepsItsLookWithFormatTablesOnSaveOff() {
        EditorPage editorPage = openWorkbookAsNewAuthor(false);
        TableEdit markedStep = new TableEdit("Quote", new CellEdit(3, 1, "Doubled*"));

        Map<String, String> lookBefore = lookOf(markedStep.table());
        saveEdit(editorPage, markedStep);
        Map<String, String> lookAfterSave = lookOf(markedStep.table());
        reformat(editorPage);
        Map<String, String> lookAfterReformat = lookOf(markedStep.table());

        assertThat(lookAfterSave)
                .as("With Format tables on save off, the save must leave the look of Quote as it was")
                .isEqualTo(lookBefore);
        assertThat(lookAfterSave.get(MARKED_STEP_CELL))
                .as("With Format tables on save off, the step Doubled* must stay plain after the save")
                .doesNotContain("bold=true");
        assertThat(lookAfterReformat.get(MARKED_STEP_CELL))
                .as("A Reformat must make the step Doubled* bold")
                .contains("bold=true");
    }

    private EditorPage openWorkbookAsNewAuthor(boolean formatTablesOnSave) {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        addRulesAuthor(editorPage);
        if (!formatTablesOnSave) {
            SystemSettingsPageComponent systemSettings = editorPage.openUserMenu()
                    .navigateToAdministration()
                    .navigateToSystemSettingsPage();
            systemSettings.setFormatTablesOnSave(false);
            systemSettings.applySettingsAndRelogin(User.ADMIN);
        }
        editorPage.openUserMenu().signOut();

        editorPage = new LoginService(DriverPool.getPage()).login(RULES_AUTHOR);
        String projectName = WorkflowService.createProjectFromExcelFile(WORKBOOK);
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getProblemsPanelComponent().checkNoProblems();
        reformat(editorPage);
        return editorPage;
    }

    private void addRulesAuthor(EditorPage editorPage) {
        UsersPageComponent users = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();
        users.clickAddUser()
                .setUsername(RULES_AUTHOR.getLogin())
                .setPassword(RULES_AUTHOR.getPassword())
                .saveUser();
        users.clickEditUser(RULES_AUTHOR.getLogin())
                .clickAddRoleBtn()
                .setRoleRepository(0, "Design")
                .setRole(0, "Contributor")
                .saveUser();
    }

    private void createInTheWindow(EditorPage editorPage) {
        createInTheWindow(editorPage, dialog -> dialog
                .selectType("Rules")
                .setSimpleRulesInitialParameters("NewRules", "Integer")
                .setCell(0, 1, "42"));
        for (String type : List.of("Smart Rules", "Simple Rules", "Smart Lookup", "Simple Lookup")) {
            createInTheWindow(editorPage, dialog -> dialog
                    .selectType(type)
                    .setSimpleRulesInitialParameters("New" + type.replace(" ", ""), "Integer")
                    .addSimpleRulesParameter("String", false, "a")
                    .setRow(0, "x", "1")
                    .setRow(1, "y", "2"));
        }
        createInTheWindow(editorPage, dialog -> dialog
                .selectType("Spreadsheet")
                .setTechnicalName("NewSpreadsheet"));
        createInTheWindow(editorPage, dialog -> dialog
                .selectType("Test")
                .selectTestedTable("GradeOf")
                .setTechnicalName("NewGradeTest")
                .toggleTransposed());
        createInTheWindow(editorPage, dialog -> dialog
                .selectType("Data")
                .setTechnicalName("newPeople")
                .selectDatatype("Person")
                .setRow(0, "Ann", "30"));
        editorPage.getProblemsPanelComponent().checkNoProblems();
    }

    private void createInTheWindow(EditorPage editorPage, UnaryOperator<CreateTableDialogComponent> fill) {
        editorPage.getEditorToolbarPanelComponent().clickCreateTable();
        fill.apply(editorPage.getCreateTableDialogComponent().waitForDialogToAppear()).save();
        editorPage.waitUntilSpinnerLoaded();
    }

    private void saveEdit(EditorPage editorPage, TableEdit edit) {
        openTable(editorPage, edit.table());
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        for (CellEdit cell : edit.cells()) {
            editorPage.getCenterTable().editCell(cell.row(), cell.column(), cell.value());
        }
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.waitUntilSpinnerLoaded();
    }

    private void openTable(EditorPage editorPage, String table) {
        editorPage.getEditorLeftRulesTreeComponent().waitForTreeFoldersToLoad().selectVisibleLeafNode(table);
        editorPage.waitUntilSpinnerLoaded();
    }

    private void reformat(EditorPage editorPage) {
        editorPage.getEditorToolbarPanelComponent().clickMore().reformatExcelFiles();
        editorPage.waitUntilSpinnerLoaded();
    }

    private Looks saveAndReformat(EditorPage editorPage, List<TableEdit> edits) {
        Map<String, Map<String, String>> before = looksOf(edits);
        edits.forEach(edit -> saveEdit(editorPage, edit));
        Map<String, Map<String, String>> afterSave = looksOf(edits);
        reformat(editorPage);
        return new Looks(before, afterSave, looksOf(edits));
    }

    private Map<String, Map<String, String>> looksOf(List<TableEdit> edits) {
        return looksOfTables(edits.stream().map(TableEdit::table).toList());
    }

    private Map<String, Map<String, String>> looksOfTables(List<String> tables) {
        Map<String, Map<String, String>> looks = new LinkedHashMap<>();
        tables.forEach(table -> looks.put(table, lookOf(table)));
        return looks;
    }

    private Map<String, String> lookOf(String tableName) {
        ProjectsMethod projects = new ProjectsMethod();
        String projectId = ProjectLinkUtil.projectIdOf(DriverPool.getPage().url());
        JsonPath found = projects.getProjectTables(projectId, tableName).jsonPath();
        List<Map<String, Object>> tables = found.getList("content");
        String tableId = tables.stream()
                .filter(table -> tableName.equals(table.get("name")))
                .map(table -> String.valueOf(table.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The project has no table " + tableName));
        JsonPath table = projects.getTableWithStyles(projectId, tableId).jsonPath();
        List<Map<String, Object>> styles = table.getList("styles");
        List<List<Map<String, Object>>> source = table.getList("source");
        Map<String, String> look = new LinkedHashMap<>();
        for (int row = 0; row < source.size(); row++) {
            for (int column = 0; column < source.get(row).size(); column++) {
                Map<String, Object> cell = source.get(row).get(column);
                if (!Boolean.TRUE.equals(cell.get("covered"))) {
                    look.put(row + ":" + column, styleOf(styles, cell));
                }
            }
        }
        return look;
    }

    private String styleOf(List<Map<String, Object>> styles, Map<String, Object> cell) {
        List<Map<String, Object>> runs = (List<Map<String, Object>>) cell.getOrDefault("runs", List.of());
        String runStyles = runs.stream()
                .map(run -> run.get("to") + "=" + styleAt(styles, run.get("style")))
                .collect(Collectors.joining(", "));
        return styleAt(styles, cell.get("style")) + " [" + runStyles + "]";
    }

    private String styleAt(List<Map<String, Object>> styles, Object index) {
        return index == null ? "{}" : String.valueOf(styles.get((Integer) index));
    }

    private Map<String, String> bodyOf(Map<String, String> look) {
        return rowsWhere(look, row -> row > 0);
    }

    private Map<String, String> lastRowOf(Map<String, String> look) {
        int lastRow = look.keySet().stream().mapToInt(this::rowOf).max().orElseThrow();
        return rowsWhere(look, row -> row == lastRow);
    }

    private Map<String, String> rowsWhere(Map<String, String> look, IntPredicate rowFilter) {
        return look.entrySet().stream()
                .filter(cell -> rowFilter.test(rowOf(cell.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private int rowOf(String cellKey) {
        return Integer.parseInt(cellKey.substring(0, cellKey.indexOf(':')));
    }

    private record CellEdit(int row, int column, String value) {
    }

    private record TableEdit(String table, CellEdit... cells) {
    }

    private record Looks(Map<String, Map<String, String>> before, Map<String, Map<String, String>> afterSave,
                         Map<String, Map<String, String>> afterReformat) {
    }
}
