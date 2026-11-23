package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceWindow;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTraceDebuggerHighlightUi extends BaseTest {

    private static final String WORKBOOK = "TraceHighlight.xlsx";
    private static final String MODULE = "TraceHighlight";
    private static final int FIRED_MS = 15000;

    @Test
    @TestCaseId({"EPBDS-9684", "EPBDS-5322", "EPBDS-10165"})
    @Description("EPBDS-9684: of two rules whose conditions hold, the trace names and highlights only the one that "
            + "returned. EPBDS-10165: the trace of a SmartLookup highlights the cells it matched and no others. "
            + "EPBDS-5322: it highlights the value found under a condition merged over several columns.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTraceHighlightsOnlyTheReturnedRuleAndTheMatchedLookupCells() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, WORKBOOK);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorLeftRulesTreeComponent().setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);

        editorPage.getEditorLeftRulesTreeComponent().selectLeafNamed("pick", 1);
        ITraceWindow business = editorPage.getEditorToolbarPanelComponent().clickTrace()
                .setParameterField("a", "2")
                .clickTraceInsideMenuBusiness();
        assertThat(business.getSimpleTreeText())
                .as("EPBDS-9684: both rules of pick hold for 2, but the trace must name only the first, which returned")
                .contains("Returned rule: [R1]")
                .doesNotContain("R2]");
        business.close();

        ITraceWindow pick = firedRuleOf(editorPage.getEditorToolbarPanelComponent().clickTrace().setParameterField("a", "2"), "7");
        assertThat(pick.getDecisionPanelText())
                .as("EPBDS-9684: the Decision panel must say that only R1 fired")
                .containsPattern("Fired: R1(?!,)");
        waitForHighlight(pick, "C14");
        assertThat(pick.getHighlightedTracedCells())
                .as("EPBDS-9684: the returned rule must be highlighted with its result and the result of the other rule not")
                .contains("B14", "C14")
                .doesNotContain("C15");
        pick.close();

        editorPage.getEditorLeftRulesTreeComponent().selectLeafNamed("price", 1);
        ITraceWindow price = firedRuleOf(editorPage.getEditorToolbarPanelComponent().clickTrace()
                .setParameterField("region", "USA")
                .setParameterField("brand", "Porche")
                .setParameterField("model", "911"), "100");
        waitForHighlight(price, "C5");
        assertThat(price.getHighlightedTracedCells())
                .as("EPBDS-10165, EPBDS-5322: the trace must highlight the matched conditions USA, Porche merged over three "
                        + "columns and 911, and the value 100 at their crossing, and no other cell")
                .containsExactlyInAnyOrder("B5", "C3", "C4", "C5");
        price.close();
    }

    private void waitForHighlight(ITraceWindow trace, String cell) {
        WaitUtil.waitForCondition(() -> trace.getHighlightedTracedCells().contains(cell), FIRED_MS, 250,
                "Waiting for the trace to highlight " + cell);
    }

    private ITraceWindow firedRuleOf(ITraceMenu menu, String result) {
        ITraceWindow trace = menu.clickTraceInsideMenu();
        trace.setBreakWhenRuleFires().resume();
        trace.waitForDecisionPanelToContain("Fired:", FIRED_MS);
        return trace.stepOver().waitForDetailsToContain("returnInteger=" + result);
    }
}
