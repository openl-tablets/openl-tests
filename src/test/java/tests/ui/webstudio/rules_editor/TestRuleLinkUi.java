package tests.ui.webstudio.rules_editor;

import com.microsoft.playwright.Page;
import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.ModuleProjectClosedComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.ProjectLinkUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRuleLinkUi extends BaseTest {

    private static final String MODULE = "Main";
    private static final String TABLE = "Hello";
    private static final String RAISED_CELL = "D7";
    private static final String RAISED_CELL_TEXT = "To";
    private static final String UNKNOWN_TABLE_ID = "00000000000000000000000000000000";
    private static final int COMPILE_TIMEOUT_MS = 90000;
    private static final int SETTLE_MS = 5000;
    private static final int POLL_MS = 500;

    @Test
    @TestCaseId("IPBQA-33051")
    @Description("EPBDS-10235: a rule link opens the table it names, marks errorCell, falls back to a table the module holds, and opens a closed project in place")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testRuleLinkOpensTheTableItNamesEvenIntoAClosedProject() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getEditorLeftRulesTreeComponent().waitForTreeFoldersToLoad().selectVisibleLeafNode(TABLE);
        TableComponent table = editorPage.getCenterTable();
        Page page = DriverPool.getPage();
        String tableId = waitForTableIdInAddressOtherThan(UNKNOWN_TABLE_ID);
        String projectId = ProjectLinkUtil.projectIdOf(page.url());
        String ruleLink = ProjectLinkUtil.ruleLink(projectId, MODULE, tableId);

        page.navigate(ruleLink);
        waitForTableDrawn(table);
        assertThat(selectedTableName(editorPage)).as("table opened by the rule link").isEqualTo(TABLE);
        assertThat(table.getCellTexts()).as("cell %s is the only one reading '%s'", RAISED_CELL, RAISED_CELL_TEXT)
                .containsOnlyOnce(RAISED_CELL_TEXT);
        assertThat(WaitUtil.waitForCondition(() -> !table.getRaisedCellTexts().isEmpty(), SETTLE_MS, POLL_MS, "Checking that no cell gets marked"))
                .as("a cell is marked although the link names no errorCell")
                .isFalse();

        page.navigate(ProjectLinkUtil.ruleLink(projectId, MODULE, tableId, RAISED_CELL));
        WaitUtil.requireCondition(() -> !table.getRaisedCellTexts().isEmpty(), COMPILE_TIMEOUT_MS, POLL_MS, "Waiting for the errorCell mark");
        assertThat(table.getRaisedCellTexts()).as("cells marked by errorCell=%s", RAISED_CELL).containsExactly(RAISED_CELL_TEXT);

        page.navigate(ProjectLinkUtil.ruleLink(projectId, MODULE, UNKNOWN_TABLE_ID));
        waitForTableDrawn(table);
        assertThat(waitForTableIdInAddressOtherThan(UNKNOWN_TABLE_ID)).as("address rewritten to the table shown").isEqualTo(tableId);
        assertThat(selectedTableName(editorPage)).as("fallback table").isEqualTo(TABLE);

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList();
        repositoryPage.closeProject(projectName);
        page.navigate(ruleLink);
        ModuleProjectClosedComponent closedNotice = new ModuleProjectClosedComponent();
        assertThat(closedNotice.waitForMessage(COMPILE_TIMEOUT_MS)).as("notice behind a closed project")
                .isEqualTo("Open the project to work with its modules.");
        assertThat(ProjectLinkUtil.tableIdOf(page.url())).as("table in the address while closed").hasValue(tableId);

        page.evaluate("() => window.ruleLinkProbe = true");
        closedNotice.openProject();
        waitForTableDrawn(table);
        assertThat(selectedTableName(editorPage)).as("table drawn after Open").isEqualTo(TABLE);
        assertThat(page.evaluate("() => window.ruleLinkProbe === true")).as("same page, no reload, after Open").isEqualTo(true);
    }

    private static void waitForTableDrawn(TableComponent table) {
        WaitUtil.requireCondition(() -> !table.getCellTexts().isEmpty(), COMPILE_TIMEOUT_MS, POLL_MS, "Waiting for the table to be drawn");
    }

    private static String waitForTableIdInAddressOtherThan(String unexpectedTableId) {
        Page page = DriverPool.getPage();
        return WaitUtil.waitForResult(() -> ProjectLinkUtil.tableIdOf(page.url()).filter(id -> !id.equals(unexpectedTableId)),
                        COMPILE_TIMEOUT_MS, POLL_MS, "Waiting for the address to name the table shown")
                .orElseThrow(() -> new IllegalStateException("The address names no table other than " + unexpectedTableId + ": " + page.url()));
    }

    private static String selectedTableName(EditorPage editorPage) {
        return editorPage.getEditorLeftRulesTreeComponent().getSelectedItemText();
    }
}
