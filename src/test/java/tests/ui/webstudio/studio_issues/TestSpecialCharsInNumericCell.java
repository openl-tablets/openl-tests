package tests.ui.webstudio.studio_issues;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.core.ui.WebElement;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestSpecialCharsInNumericCell extends BaseTest {

    private static final String SPECIAL_CHARACTERS = "',;:=";
    private static final String SCORE_BEFORE = "0.7";
    private static final String SCORE_WRITTEN = "5.48";
    private static final String SCORE_KEPT = "1.23";
    private static final int SCORE_ROW = 4;
    private static final int SCORE_COLUMN = 2;
    private static final long CELL_TIMEOUT_MS = 10000;
    private static final long CELL_POLLING_MS = 250;

    @Test
    @TestCaseId("EPBDS-15348")
    @Description("BUG: Special characters in a table cell break editing; a decimal value cannot be entered until the page is refreshed. "
            + "Guards the React cell editor that replaced the editor the bug was filed against, where the characters are refused silently")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testSpecialCharsDoNotBlockDecimalInput() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Example 1 - Bank Rating");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Bank Rating");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "CapitalAdequacyScore");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();

        TableComponent table = editorPage.getCenterTable();
        assertThat(table.getCellText(SCORE_ROW, SCORE_COLUMN))
                .as("Cell [%d,%d] of CapitalAdequacyScore is the Double cell this test writes into",
                        SCORE_ROW, SCORE_COLUMN)
                .isEqualTo(SCORE_BEFORE);

        writeAfterSpecialCharacters(table, SCORE_WRITTEN);
        requireCellHolds(table, SCORE_WRITTEN);

        writeAfterSpecialCharacters(table, SCORE_KEPT);
        requireCellHolds(table, SCORE_KEPT);

        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        requireCellHolds(table, SCORE_KEPT);
    }

    private void writeAfterSpecialCharacters(TableComponent table, String value) {
        WebElement editor = table.openCellEditor(SCORE_ROW, SCORE_COLUMN);
        editor.clear();
        editor.fillSequentially(SPECIAL_CHARACTERS);
        assertThat(editor.getCurrentInputValue())
                .as("None of the characters %s may land in a cell that holds a number", SPECIAL_CHARACTERS)
                .isEmpty();

        editor.fillSequentially(value);
        assertThat(editor.getCurrentInputValue())
                .as("'%s' must be taken right after %s was typed, without the page being refreshed",
                        value, SPECIAL_CHARACTERS)
                .isEqualTo(value);
        editor.press("Enter");
    }

    private void requireCellHolds(TableComponent table, String value) {
        WaitUtil.requireCondition(() -> {
            try {
                return value.equals(table.getCellText(SCORE_ROW, SCORE_COLUMN));
            } catch (RuntimeException beingRedrawn) {
                return false;
            }
        }, CELL_TIMEOUT_MS, CELL_POLLING_MS,
                "Waiting for cell [" + SCORE_ROW + "," + SCORE_COLUMN + "] to hold " + value);
    }
}
