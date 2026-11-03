package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.CreateTableDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorTableActionsPanelComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCreateTableRangeConditionUi extends BaseTest {

    private static final String MODULE = "Main";
    private static final String TABLE_NAME = "QaRangeRules";
    private static final String RANGE = "18-30";
    private static final String RETYPED_RANGE = "18-25";
    private static final String SPREADSHEET = "CalcProduct";
    private static final String PRODUCT = "= product($F1:$F4)";
    private static final int RANGE_COLUMN = 1;
    private static final int STEP_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;

    @Test
    @TestCaseId({"IPBQA-33024", "EPBDS-17082"})
    @Description("EPBDS-16359: a rules condition column typed Integer must accept the range 18-30 and write it "
            + "as the range OpenL matches by, not as a mangled number like 181. A range retyped in the editor keeps "
            + "its notation (EPBDS-16978); in a Spreadsheet an OpenL formula typed into a cell stays the text typed, "
            + "Excel Formula Editor is offered (EPBDS-17048), and a part of a step name is marked in the error "
            + "colour and keeps the mark after a reload (EPBDS-16961).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testIntegerConditionHoldsRange() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);

        editorPage.getEditorToolbarPanelComponent().clickCreateTable();
        CreateTableDialogComponent dialog = editorPage.getCreateTableDialogComponent().waitForDialogToAppear();
        dialog.selectType("Simple Rules")
                .setSimpleRulesInitialParameters(TABLE_NAME, "Boolean")
                .addSimpleRulesParameter("Integer", false, "age")
                .setCell(0, 0, RANGE)
                .save();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getProblemsPanelComponent().checkNoProblems();
        assertThat(renderedTableText(editorPage.getCenterTable()))
                .as("The created condition cell must hold the range '%s', not a mangled number", RANGE)
                .contains(RANGE)
                .doesNotContain("181");

        TableComponent table = editorPage.getCenterTable();
        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();
        int rangeRow = table.getRowIndexHolding(RANGE);
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        assertThat(table.getEditorsOffered(rangeRow, RANGE_COLUMN)).as("The range cell can be typed as text")
                .contains("Text Editor");
        table.editCellIn(rangeRow, RANGE_COLUMN, "Text Editor", RETYPED_RANGE);
        actions.clickSaveChanges();
        editorPage.reloadPage();
        assertThat(editorPage.getCenterTable().getCellText(rangeRow, RANGE_COLUMN))
                .as("A range retyped in the editor keeps the notation it was typed in, not [18..25]")
                .isEqualTo(RETYPED_RANGE);

        editorPage.getEditorToolbarPanelComponent().clickCreateTable();
        editorPage.getCreateTableDialogComponent().waitForDialogToAppear()
                .selectType("Spreadsheet")
                .setTechnicalName(SPREADSHEET)
                .setRow(1, "F1", "= 2")
                .setRow(2, "F2", "= 3")
                .setRow(3, "F3", "= 4")
                .setRow(4, "F4", "= 5")
                .setRow(5, "Result", "= 1")
                .save();
        editorPage.waitUntilSpinnerLoaded();
        table = editorPage.getCenterTable();
        int resultRow = table.getRowIndexHolding("Result");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        assertThat(table.getEditorsOffered(resultRow, VALUE_COLUMN))
                .as("The cell editor can be switched to the Excel Formula Editor")
                .contains("Excel Formula Editor");
        table.editCell(resultRow, VALUE_COLUMN, PRODUCT);
        table.openCellEditor(resultRow, STEP_COLUMN);
        table.markFirstLetters(3, "error");
        assertThat(table.getMarksInEditor()).as("The selected letters take the Error mark").containsExactly("error: Res");
        table.clearMarkOfFirstLetters(3);
        assertThat(table.getMarksInEditor()).as("Clear Mark takes the mark off the selected letters").isEmpty();
        assertThat(table.getOpenEditorText()).as("Clear Mark keeps the text of the step").isEqualTo("Result");
        table.markFirstLetters(3, "error");
        table.keepEditedText();
        actions.clickSaveChanges();
        editorPage.reloadPage();
        table = editorPage.getCenterTable();
        assertThat(table.getCellText(resultRow, VALUE_COLUMN))
                .as("An OpenL formula typed into a cell stays the text typed, not an Excel formula that shows 0")
                .isEqualTo(PRODUCT);
        assertThat(table.getBoldParts(resultRow, STEP_COLUMN))
                .as("The marked part of the step name stays bold after a reload, the rest of it does not")
                .containsExactly("Res");
        assertThat(table.getBoldPartColours(resultRow, STEP_COLUMN))
                .as("The marked part keeps the error colour, a red")
                .hasSize(1)
                .allMatch(TestCreateTableRangeConditionUi::isRed);
        editorPage.getProblemsPanelComponent().checkNoProblems();
    }

    private String renderedTableText(TableComponent table) {
        StringBuilder text = new StringBuilder();
        for (int row = 1; row <= 6; row++) {
            text.append(rowTextOrEmpty(table, row)).append('\n');
        }
        return text.toString();
    }

    private String rowTextOrEmpty(TableComponent table, int row) {
        try {
            return String.join(" | ", table.getRow(row).getValue());
        } catch (RuntimeException absentRow) {
            return "";
        }
    }

    private static boolean isRed(String colour) {
        Matcher rgb = Pattern.compile("rgba?\\((\\d+), (\\d+), (\\d+)").matcher(colour);
        return rgb.find() && Integer.parseInt(rgb.group(1)) > 150 && Integer.parseInt(rgb.group(2)) < 100
                && Integer.parseInt(rgb.group(3)) < 100;
    }
}
