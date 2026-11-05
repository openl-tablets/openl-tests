package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceWindow;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestTraceBusinessModeUi extends BaseTest {

    private static final String MODULE = "Main";
    private static final String TABLE_FOLDER = DECISION;
    private static final String TABLE_NAME = "Hello";
    private static final String SPREADSHEET = "GreetAll";
    private static final String STEP_NAME = "Greetings";
    private static final String STEP = "$" + STEP_NAME;
    private static final String SPREADSHEET_RESULT_INPUT = "{\"params\":{\"ratingDetails\":{\"Plan\":\"P1\","
            + "\"Coverages\":[{\"code\":\"C1\",\"premium\":3.0}],\"Total\":7.0}}}";
    private static final int CALLS = 150;
    private static final int HOURS_A_DAY = 24;

    @Test
    @TestCaseId({"IPBQA-33031", "EPBDS-17083"})
    @Description("EPBDS-16292: without the Advanced tracer switch the trace opens in the business view - no "
            + "debugger toolbar, the result-oriented tree names the fired rule, and the detailed toggle breaks "
            + "the decision table into its conditions; closing and relaunching works cleanly. A step calling a rule "
            + "150 times lists its calls 100 at a time and shows the rest on request (EPBDS-17009).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testBusinessTraceShowsFiredRuleAndConditions() {
        EditorPage editorPage = openHelloTable();

        ITraceWindow trace = editorPage.getEditorToolbarPanelComponent().clickTrace()
                .setParameterField("hour", "10")
                .clickTraceInsideMenuBusiness();

        assertThat(trace.isBusinessToggleShown(10000))
                .as("The business view must offer the detailed-trace toggle")
                .isTrue();
        assertThat(trace.isDebugToolbarShown(2000))
                .as("The business view must not show the step-debugger toolbar")
                .isFalse();
        assertThat(trace.getSimpleTreeText())
                .as("The business tree must show the traced result and the fired rule")
                .contains("Good Morning")
                .contains("Returned rule: [R10]");

        trace.toggleDetailedTrace();
        assertThat(trace.getSimpleTreeText())
                .as("The detailed toggle must break the decision table into its conditions")
                .contains("Condition: C1")
                .contains("Condition: C2");
        trace.close();

        ITraceWindow secondTrace = editorPage.getEditorToolbarPanelComponent().clickTrace()
                .setParameterField("hour", "23")
                .clickTraceInsideMenuBusiness();
        assertThat(secondTrace.getSimpleTreeText())
                .as("A relaunch after closing the trace must work and reflect the new input")
                .contains("Good Night");
        secondTrace.close();

        editorPage.getEditorToolbarPanelComponent().clickCreateTable();
        editorPage.getCreateTableDialogComponent().waitForDialogToAppear()
                .selectType("Spreadsheet")
                .setTechnicalName(SPREADSHEET)
                .setRow(1, STEP_NAME, helloCalledForEveryHour())
                .save();
        editorPage.waitUntilSpinnerLoaded();
        ITraceWindow manyCalls = editorPage.getEditorToolbarPanelComponent().clickTrace().clickTraceInsideMenuBusiness()
                .expandSimpleStep(STEP);
        assertThat(manyCalls.getSimpleCallsCount(STEP)).as("A step lists its calls 100 at a time").isEqualTo(100);
        assertThat(manyCalls.getSimpleMoreCallsText(STEP)).as("The calls not listed yet are counted")
                .isEqualTo("+50 more executions");
        manyCalls.showMoreSimpleCalls(STEP);
        assertThat(manyCalls.getSimpleCallsCount(STEP)).as("Show more lists every call of the step, the 150th included")
                .isEqualTo(CALLS);
        assertThat(manyCalls.getSimpleMoreCallsText(STEP)).as("Nothing is left to show").isEmpty();
        manyCalls.close();
    }

    @Test
    @TestCaseId({"EPBDS-16861", "EPBDS-16344"})
    @Description("EPBDS-16861: the business trace of a rule that calls itself until StackOverflowError opens the "
            + "executed call tree down to the failed call and shows StackOverflowError, as Run does, instead of "
            + "'is not valid JSON'. EPBDS-16344: tracing a test whose run fails marks the top call red and shows "
            + "the error text on it, so the failure is found without going deeper.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testBusinessTraceMarksFailedCallsAndShowsTheError() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, "TraceFailures.zip");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "BugReproducing");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_EXCEL_SHEET)
                .expandFolderInTree("calc1")
                .selectItemInFolder("calc1", "calc1Test");

        ITraceWindow failedTest = editorPage.getEditorToolbarPanelComponent().clickTrace()
                .clickTraceInsideMenuBusiness();
        failedTest.selectSimpleRow(0).waitForDetailsToContain("compile error");
        assertThat(failedTest.getSimpleTreeText()).as("The trace of the test must open its call tree").contains("calc3");
        assertThat(failedTest.isSimpleRowFailed(0))
                .as("The top call of the failed test must be marked as failed")
                .isTrue();
        assertThat(failedTest.getSimpleRowText(0))
                .as("The top call of the failed test must be labelled as an error")
                .endsWith("= ERROR");
        assertThat(failedTest.getDetailsText())
                .as("The error text must be shown on the top call, without going deeper")
                .contains("You are trying to run openl code with a compile error in it");
        failedTest.close();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Recursion");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_EXCEL_SHEET)
                .expandFolderInTree("Rules")
                .selectItemInFolder("Rules", "Recurse");

        editorPage.getEditorToolbarPanelComponent().clickRun().writeFieldValue("n", "1").clickRunInsideMenu();
        TestResultValidationComponent runResults = new TestResultValidationComponent();
        assertThat(runResults.getRunError())
                .as("Run of an endless recursion must report StackOverflowError")
                .contains("StackOverflowError");
        runResults.closeResults();

        ITraceWindow recursion = editorPage.getEditorToolbarPanelComponent().clickTrace()
                .setParameterField("n", "1")
                .clickTraceInsideMenuBusiness();
        recursion.selectLastSimpleRow().waitForDetailsToContain("StackOverflowError");
        assertThat(recursion.getSimpleTreeText())
                .as("The business trace of an endless recursion must show the executed calls down to the failed one")
                .contains("Recurse(Integer n) = ERROR");
        assertThat(recursion.getDetailsText())
                .as("The deepest call must report StackOverflowError, the same error Run shows")
                .contains("StackOverflowError");
        recursion.close();
    }

    @Test
    @TestCaseId("EPBDS-16460")
    @Description("EPBDS-16460: a rule taking the result of another spreadsheet as a parameter, traced with that "
            + "parameter given as JSON, keeps the populated input: the parameter shows its fields and the steps "
            + "calculate from the supplied values instead of an empty object.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testBusinessTraceKeepsPopulatedSpreadsheetResultInput() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, "SpreadsheetResultInput.zip");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "sprInput");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_EXCEL_SHEET)
                .expandFolderInTree("Rules")
                .selectItemInFolder("Rules", "PolicyOverrideCalculation");

        ITraceWindow trace = editorPage.getEditorToolbarPanelComponent().clickTrace()
                .selectJSONTrace(SPREADSHEET_RESULT_INPUT)
                .clickTraceInsideMenuBusiness();
        assertThat(trace.getSimpleTreeText())
                .as("The steps must calculate from the supplied spreadsheet result")
                .contains("$Doubled = 14")
                .contains("$PlanCode = P1");
        trace.selectSimpleRow(0).waitForDetailsToContain("ratingDetails").loadFirstDetailsValue()
                .waitForDetailsToContain("{3 fields}");
        assertThat(trace.getDetailsText().replaceAll("\\s+", ""))
                .as("The traced input must show the populated spreadsheet result, not an empty object")
                .contains("ratingDetailsSpreadsheetResultRatingDetails={3fields}");
        trace.close();
    }

    private static String helloCalledForEveryHour() {
        return "= Hello(new Integer[]{" + IntStream.range(0, CALLS).mapToObj(call -> String.valueOf(call % HOURS_A_DAY))
                .collect(Collectors.joining(",")) + "})";
    }

    private EditorPage openHelloTable() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TABLE_FOLDER)
                .selectItemInFolder(TABLE_FOLDER, TABLE_NAME);
        return editorPage;
    }
}
