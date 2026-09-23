package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.EntityIdUtil;
import helpers.utils.ProjectLinkUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRuleLinkUi extends BaseTest {

    private static final String TEMPLATE = "Sample Project";
    private static final String MODULE = "Main";
    private static final String TABLE = "Hello";
    private static final String RAISED_CELL = "D7";
    private static final String RAISED_CELL_TEXT = "To";
    private static final String UNKNOWN_TABLE_ID = "00000000000000000000000000000000";
    private static final String TABLE_PARAM = "table=";
    private static final String CLOSED_NOTICE = "[data-testid=module-project-closed]";
    private static final String OPEN_PROJECT_BUTTON = CLOSED_NOTICE + " button >> nth=0";
    private static final String RELOAD_PROBE = "() => window.__ruleLinkProbe";
    private static final String SET_RELOAD_PROBE = "() => { window.__ruleLinkProbe = 'kept'; }";
    private static final String PROBE_VALUE = "kept";
    private static final String CELLS_OF_TABLE =
            "() => Array.from(document.querySelectorAll(\"[data-testid='module-table'] td\"))"
                    + ".map(cell => cell.textContent.trim())";
    private static final String OUTLINED_CELLS_OF_TABLE =
            "() => Array.from(document.querySelectorAll(\"[data-testid='module-table'] td\"))"
                    + ".filter(cell => getComputedStyle(cell).outlineStyle === 'solid')"
                    + ".map(cell => cell.textContent.trim())";
    private static final int COMPILE_TIMEOUT_MS = 90000;
    private static final int SETTLE_MS = 5000;
    private static final int POLL_MS = 500;

    @Test
    @TestCaseId("IPBQA-33051")
    @Description("EPBDS-10235: a rule link /projects/:projectId/modules/:moduleName?table=:tableId must open the "
            + "module on the table it names on a cold load, mark the cell errorCell names the way a reader arriving "
            + "from a compilation message needs, and fall back to a table the module holds when the table it names "
            + "has moved. Behind a closed project it must offer Open and then draw the named table without the "
            + "reader leaving the page.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testRuleLinkOpensTheTableItNamesEvenIntoAClosedProject() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getEditorLeftRulesTreeComponent().waitForTreeFoldersToLoad().selectVisibleLeafNode(TABLE);

        String moduleUrl = waitForTableInAddress();
        String projectId = URLDecoder.decode(EntityIdUtil.lastUrlSegment(moduleUrl.split("/modules/")[0]),
                StandardCharsets.UTF_8);
        String tableId = tableIdOf(moduleUrl);

        DriverPool.getPage().navigate(ProjectLinkUtil.ruleLink(projectId, MODULE, tableId));
        waitForTableDrawn();
        assertThat(selectedTableName())
                .as("A rule link must open the module on the table it names")
                .isEqualTo(TABLE);
        assertThat(cellTexts().stream().filter(RAISED_CELL_TEXT::equals).count())
                .as("Cell %s of this template's table must be the only one reading '%s', otherwise the check below "
                        + "cannot tell a wrongly marked cell from a changed template", RAISED_CELL, RAISED_CELL_TEXT)
                .isEqualTo(1);
        assertThat(anyCellGetsMarked())
                .as("No cell may be marked while the address names no errorCell, otherwise the marking below "
                        + "would prove nothing about the parameter")
                .isFalse();

        DriverPool.getPage().navigate(ProjectLinkUtil.ruleLink(projectId, MODULE, tableId, RAISED_CELL));
        waitForTableDrawn();
        assertThat(waitForMarkedCells())
                .as("errorCell=%s must mark exactly the cell a compilation message would name", RAISED_CELL)
                .containsExactly(RAISED_CELL_TEXT);

        DriverPool.getPage().navigate(ProjectLinkUtil.ruleLink(projectId, MODULE, UNKNOWN_TABLE_ID));
        waitForTableDrawn();
        assertThat(waitForTableInAddress())
                .as("A rule link naming a table the module no longer holds must have its address rewritten to the "
                        + "table actually shown, not left naming the missing one")
                .contains(TABLE_PARAM + tableId);
        assertThat(selectedTableName())
                .as("A rule link naming a table the module no longer holds must fall back to a table it does hold")
                .isEqualTo(TABLE);

        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList();
        repositoryPage.closeProject(projectName);

        DriverPool.getPage().navigate(ProjectLinkUtil.ruleLink(projectId, MODULE, tableId));
        WebElement closedNotice = new WebElement(DriverPool.getPage(), CLOSED_NOTICE, "moduleProjectClosed");
        assertThat(closedNotice.waitForVisible(COMPILE_TIMEOUT_MS).getText())
                .as("A rule link into a closed project must say the project has to be opened first")
                .contains("Open the project to work with its modules");
        assertThat(DriverPool.getPage().url())
                .as("The address must keep naming the table while the project is still closed")
                .contains(TABLE_PARAM + tableId);

        DriverPool.getPage().evaluate(SET_RELOAD_PROBE);
        new WebElement(DriverPool.getPage(), OPEN_PROJECT_BUTTON, "openProjectBtn").click();

        waitForTableDrawn();
        assertThat(selectedTableName())
                .as("Pressing Open must compile the module and draw the table the address names")
                .isEqualTo(TABLE);
        assertThat(DriverPool.getPage().evaluate(RELOAD_PROBE))
                .as("The table must appear in the page the reader was already on, without a page load")
                .isEqualTo(PROBE_VALUE);
    }

    private void waitForTableDrawn() {
        WaitUtil.requireCondition(() -> !quietly(CELLS_OF_TABLE).isEmpty(), COMPILE_TIMEOUT_MS, POLL_MS,
                "Waiting for the table the address names to be drawn");
    }

    private String waitForTableInAddress() {
        WaitUtil.requireCondition(() -> DriverPool.getPage().url().contains(TABLE_PARAM), COMPILE_TIMEOUT_MS, POLL_MS,
                "Waiting for the module to settle on a table and put it in the address");
        return DriverPool.getPage().url();
    }

    private boolean anyCellGetsMarked() {
        return WaitUtil.waitForCondition(() -> !quietly(OUTLINED_CELLS_OF_TABLE).isEmpty(), SETTLE_MS, POLL_MS,
                "Checking whether any cell of the table becomes marked");
    }

    private List<String> waitForMarkedCells() {
        WaitUtil.requireCondition(() -> !quietly(OUTLINED_CELLS_OF_TABLE).isEmpty(), COMPILE_TIMEOUT_MS, POLL_MS,
                "Waiting for the cell errorCell names to be marked");
        return evaluateTexts(OUTLINED_CELLS_OF_TABLE);
    }

    private String tableIdOf(String url) {
        int start = url.indexOf(TABLE_PARAM);
        if (start == -1) {
            throw new IllegalStateException("The address names no table: " + url);
        }
        String value = url.substring(start + TABLE_PARAM.length());
        int nextParam = value.indexOf('&');
        return nextParam == -1 ? value : value.substring(0, nextParam);
    }

    private String selectedTableName() {
        return new EditorPage().getEditorLeftRulesTreeComponent().getSelectedItemText();
    }

    private List<String> cellTexts() {
        return evaluateTexts(CELLS_OF_TABLE);
    }

    private List<String> quietly(String expression) {
        try {
            return evaluateTexts(expression);
        } catch (RuntimeException pageStillMoving) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> evaluateTexts(String expression) {
        return (List<String>) DriverPool.getPage().evaluate(expression);
    }
}
