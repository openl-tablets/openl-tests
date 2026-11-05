package tests.ui.webstudio.studio_issues;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.ProblemsPanelComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

public class TestCompileErrorNavigationUi extends BaseTest {

    private static final String MODULE = "CompileErrorsNavigation";
    private static final int SETTLE_MS = 30000;
    private static final int POLL_MS = 500;
    private static final String NOT_FOUND = "' is not found.";
    private static final String ALREADY_DEFINED = "is already defined.";
    private static final String SPANS_CONDITION = "The cell spans the condition column 'C2' and the horizontal condition column 'HC1'. "
            + "A title cell must not span both vertical and horizontal conditions.";
    private static final String INVALID_STRUCTURE = "Invalid table structure: data table body should contain key and value columns.";

    @Test
    @TestCaseId({"EPBDS-16492", "EPBDS-17002", "EPBDS-14031", "EPBDS-14040", "EPBDS-15361"})
    @Description("EPBDS-16492: an error of a Spreadsheet with one or two table properties is opened on the cell holding it, "
            + "not on the one after it. EPBDS-17002: an error in a Method body of several rows or lines is opened on the row "
            + "holding it and marks the piece. EPBDS-14031: a Spreadsheet whose properties cannot be read reports only "
            + "the structure error, not 'The element is null.'. EPBDS-14040: a Lookup title cell crossing the horizontal "
            + "condition says which condition it spans and opens on that cell. EPBDS-15361: a parameter declared twice "
            + "is reported once, at the header, not for every cell of the table.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCompileErrorsOpenTheCellsHoldingThem() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, MODULE + ".xlsx");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        ProblemsPanelComponent problems = editorPage.getProblemsPanelComponent();
        problems.assertCompilationCompletes();
        SoftAssertions soft = new SoftAssertions();

        List<String> allErrors = problems.getAllErrors();
        soft.assertThat(allErrors)
                .as("EPBDS-14031: only the structure error is shown for the Spreadsheet whose properties cannot be read, not 'The element is null.'")
                .contains(INVALID_STRUCTURE)
                .noneMatch(error -> error.contains("The element is null"));
        soft.assertThat(allErrors.stream().filter(error -> error.contains(ALREADY_DEFINED)).toList())
                .as("EPBDS-15361: a parameter declared twice is reported once per table, the Spreadsheet and the Rules table")
                .containsExactlyInAnyOrder("Parameter 'a' is already defined.", "Parameter 'hour' is already defined.");
        soft.assertThat(allErrors).as("EPBDS-14040: the Lookup title cell crossing the horizontal condition is explained, not 'Cannot convert table.'")
                .contains(SPANS_CONDITION)
                .noneMatch(error -> error.contains("Cannot convert table"));

        followError(soft, editorPage, "EPBDS-16492 one property", "Identifier 'BlaBla" + NOT_FOUND, "MySpr",
                "=BlaBla+100", List.of("Identifier 'BlaBla" + NOT_FOUND), List.of("BlaBla"));
        followError(soft, editorPage, "EPBDS-16492 two properties", "Identifier 'BlaBlaTwo" + NOT_FOUND, "MyTwoPropertiesSpr",
                "=BlaBlaTwo+100", List.of("Identifier 'BlaBlaTwo" + NOT_FOUND), List.of("BlaBlaTwo"));
        followError(soft, editorPage, "EPBDS-17002 body of several rows", "Method 'unknownFunction(", "calcTwo",
                "return unknownFunction(a);", List.of("Method 'unknownFunction(java.lang.Integer)" + NOT_FOUND),
                List.of("unknownFunction(a)"));
        followError(soft, editorPage, "EPBDS-17002 body of one line in the first row", "Method 'otherFunction(", "calcOne",
                "return otherFunction(a);", List.of("Method 'otherFunction(java.lang.Integer)" + NOT_FOUND),
                List.of("otherFunction(a)"));
        followError(soft, editorPage, "EPBDS-14031", INVALID_STRUCTURE, "unreadableProperties",
                "Spreadsheet SpreadsheetResult unreadableProperties()", List.of(INVALID_STRUCTURE), List.of());
        followError(soft, editorPage, "EPBDS-14040", SPANS_CONDITION, "CellToTableEdge",
                "test", List.of(SPANS_CONDITION), List.of("test"));
        followError(soft, editorPage, "EPBDS-15361 Spreadsheet", "Parameter 'a' is already defined.", "TestX",
                "Spreadsheet SpreadsheetResult TestX(String a, String a, String b, Integer c, Integer d)",
                List.of("Parameter 'a' is already defined."), List.of("String a"));
        followError(soft, editorPage, "EPBDS-15361 Rules", "Parameter 'hour' is already defined.", "Greeting",
                "Rules String Greeting(Integer hour, Integer hour)",
                List.of("Parameter 'hour' is already defined."), List.of("Integer hour"));

        soft.assertAll();
    }

    private void followError(SoftAssertions soft, EditorPage editorPage, String bug, String message, String table,
                             String errorCell, List<String> tableErrors, List<String> markedParts) {
        try {
            editorPage.getProblemsPanelComponent().selectProblemByText(message);
            WaitUtil.requireCondition(() -> table.equals(shownTable(editorPage)), SETTLE_MS, POLL_MS,
                    "Waiting for the table " + table + " to be opened by the error");
            WaitUtil.waitForCondition(() -> List.of(errorCell).equals(editorPage.getCenterTable().getRaisedCellTexts())
                            && markedParts.equals(editorPage.getEditorMainContentProblemsPanelComponent().getMarkedParts()),
                    SETTLE_MS, POLL_MS, "Waiting for the cell and the code of the error to be marked");
            soft.assertThat(shownTable(editorPage)).as(bug + ": the error opens the table it was raised in").isEqualTo(table);
            soft.assertThat(editorPage.getCenterTable().getRaisedCellTexts())
                    .as(bug + ": the cell marked by the error is the one holding it")
                    .containsExactly(errorCell);
            soft.assertThat(editorPage.getEditorMainContentProblemsPanelComponent().getErrorMessages())
                    .as(bug + ": the table shows its own errors, each once")
                    .isEqualTo(tableErrors);
            soft.assertThat(editorPage.getEditorMainContentProblemsPanelComponent().getMarkedParts())
                    .as(bug + ": the part of the code the error is about is marked")
                    .isEqualTo(markedParts);
        } catch (RuntimeException | AssertionError couldNotFollow) {
            soft.fail(bug + ": the error '" + message + "' could not be followed: " + couldNotFollow);
        }
    }

    private static String shownTable(EditorPage editorPage) {
        return editorPage.getEditorLeftRulesTreeComponent().getSelectedItemText();
    }
}
