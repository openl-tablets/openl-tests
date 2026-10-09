package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.IRunMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceMenu;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.METHOD;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.SPREADSHEET;
import static org.assertj.core.api.Assertions.assertThat;

public class TestRunFormInputUi extends BaseTest {

    private static final String EXAMPLE_3 = "Example 3 - Auto Policy Calculation";
    private static final String EXAMPLE_3_MODULE = "AutoPolicyCalculation";
    private static final String VEHICLE_PREMIUM = "DetermineVehiclePremium";
    private static final String RUNTIME_CONTEXT = "runtime-context";
    private static final String RUN_FORM_WORKBOOK = "RunFormInput.xlsx";
    private static final String RUN_FORM_MODULE = "RunFormInput";
    private static final String SHOW = "Show";
    private static final String TRACE_KEEPS_RUN_INPUT = "The Trace form must keep the input entered in the Run form";

    @Test
    @TestCaseId("EPBDS-16880")
    @Description("EPBDS-16730: the Runtime Context fields of the Run form offer their values by name, as Québec and "
            + "Hors Québec, not by the codes QC and HQ.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void runtimeContextValuesAreOfferedByName() {
        IRunMenu run = openRunWithRuntimeContext();
        assertThat(run.getFieldChoices("caRegion"))
                .as("caRegion must be picked from the names of its values")
                .containsExactly("Québec", "Hors Québec");
    }

    @Test
    @TestCaseId("EPBDS-16880")
    @Description("EPBDS-16731: the locale field of the Runtime Context is written as text or picked from a list, not edited as a JSON object.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void runtimeContextLocaleIsNotEditedAsJson() {
        IRunMenu run = openRunWithRuntimeContext();
        assertThat(run.isFieldEditedAsTextOrList("locale"))
                .as("locale must be written as text or picked from a list, not edited as a JSON object")
                .isTrue();
    }

    @Test
    @TestCaseId("EPBDS-16880")
    @Description("EPBDS-16732: the results of a run show the Runtime Context the table was run with.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void runResultsShowTheRuntimeContext() {
        EditorPage editorPage = openTableWithRuntimeContext();
        IRunMenu run = editorPage.getEditorToolbarPanelComponent().clickRun();
        run.unfoldParameter(RUNTIME_CONTEXT).writeFieldValue("lob", "Auto");
        run.clickRunInsideMenu();

        TestResultValidationComponent results = editorPage.getTestResultValidationComponent();
        assertThat(results.getRunColumnHeadings())
                .as("The results must have a column for the Runtime Context next to the parameters")
                .anyMatch(heading -> heading.equalsIgnoreCase("Runtime Context"));
        assertThat(results.getUnfoldedRunCellText("Runtime Context"))
                .as("The Runtime Context column must show the line of business the run was asked for")
                .contains("lob")
                .contains("Auto");
    }

    @Test
    @TestCaseId("EPBDS-16880")
    @Description("EPBDS-16726: a field that has a datatype default and is cleared with the cross is run as null, "
            + "as the form shows it, not with its default.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void clearedFieldWithDefaultIsRunAsNull() {
        EditorPage editorPage = openWorkbookTable(METHOD, SHOW);
        IRunMenu run = editorPage.getEditorToolbarPanelComponent().clickRun();
        run.unfoldParameter("p");
        assertThat(run.getFieldValue("grade"))
                .as("The form creates p with the defaults of Policy")
                .isEqualTo("\"A\"");
        run.clearFieldValue("grade").clearFieldValue("vip");
        run.clickRunInsideMenu();

        assertThat(editorPage.getTestResultValidationComponent().getUnfoldedRunCellText("Result"))
                .as("The rule must receive null for the cleared fields and the defaults for the others")
                .contains("grade=null|age=30|vip=null|rate=0.5");
    }

    @Test
    @TestCaseId("EPBDS-16880")
    @Description("EPBDS-16864: JSON pasted as the value of the only parameter, its fields at the top level, fills "
            + "that parameter on the Form tab, comes back under params and the parameter name on the JSON tab, and "
            + "runs with the pasted values.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void pastedParameterJsonFillsTheParameter() {
        EditorPage editorPage = openWorkbookTable(METHOD, SHOW);
        IRunMenu run = editorPage.getEditorToolbarPanelComponent().clickRun();
        run.showJsonInput().pasteJsonInput("{\"grade\": \"B\", \"age\": 41, \"vip\": false, \"rate\": 0.25}");

        run.showFormInput();
        assertThat(run.getFieldValue("p"))
                .as("The Form tab must read the pasted JSON into p instead of leaving p empty")
                .isNotEmpty()
                .isNotEqualTo("null");
        run.unfoldParameter("p");
        assertThat(run.getFieldValue("grade"))
                .as("The Form tab must show the pasted grade inside p")
                .isEqualTo("\"B\"");
        assertThat(run.getFieldValue("age"))
                .as("The Form tab must show the pasted age inside p")
                .isEqualTo("41");

        String json = run.showJsonInput().getJsonInput().replaceAll("\\s", "");
        assertThat(json)
                .as("Back on the JSON tab the pasted fields must sit under params and p")
                .contains("\"params\":{\"p\":{")
                .contains("\"grade\":\"B\"");
        run.clickRunInsideMenu();

        assertThat(editorPage.getTestResultValidationComponent().getUnfoldedRunCellText("Result"))
                .as("The run must use the pasted values")
                .contains("grade=B|age=41|vip=false|rate=0.25");
    }

    @Test
    @TestCaseId("EPBDS-16880")
    @Description("EPBDS-16742: the Trace form opens with the input entered in the Run form, as the one input form "
            + "Run and Trace shared in 6.4.0 did. Fails on EPBDS-16742: Trace opens with the defaults of Policy.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue(value = "EPBDS-16742", failsWith = TRACE_KEEPS_RUN_INPUT)
    public void traceFormKeepsTheInputEnteredInRun() {
        EditorPage editorPage = openWorkbookTable(METHOD, SHOW);
        IRunMenu run = editorPage.getEditorToolbarPanelComponent().clickRun();
        run.unfoldParameter("p").writeFieldValue("grade", "B").writeFieldValue("age", "41");
        assertThat(run.getFieldValue("grade"))
                .as("The Run form takes the grade written into it")
                .isEqualTo("\"B\"");

        ITraceMenu trace = editorPage.getEditorToolbarPanelComponent().clickTrace();
        trace.unfoldParameter("p");
        assertThat(List.of(trace.getFieldValue("grade"), trace.getFieldValue("age")))
                .as(TRACE_KEEPS_RUN_INPUT)
                .containsExactly("\"B\"", "41");
    }

    @Test
    @TestCaseId("EPBDS-16880")
    @Description("EPBDS-16608: a project whose deploy configuration does not provide the Runtime Context is not "
            + "offered a Runtime Context in the Run form.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void runtimeContextIsNotOfferedWhenTheProjectDoesNotProvideIt() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN,
                "Tutorial 3 - More Advanced Decision and Data Tables");
        EditorPage editorPage = new EditorPage();
        selectTable(editorPage, projectName, "Tutorial3 - Advanced Decision and Data Tables", DECISION, "AmPmTo24");
        IRunMenu run = editorPage.getEditorToolbarPanelComponent().clickRun();
        assertThat(run.offersParameter("ampmHr"))
                .as("The Run form lists the parameters of AmPmTo24")
                .isTrue();
        assertThat(run.offersParameter(RUNTIME_CONTEXT))
                .as("The Runtime Context must not be offered while the project does not provide it")
                .isFalse();
    }

    private IRunMenu openRunWithRuntimeContext() {
        IRunMenu run = openTableWithRuntimeContext().getEditorToolbarPanelComponent().clickRun();
        return run.unfoldParameter(RUNTIME_CONTEXT);
    }

    private EditorPage openTableWithRuntimeContext() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, EXAMPLE_3);
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList().openProjectDetail(projectName).setProvideRuntimeContext(true);

        EditorPage editorPage = new EditorPage();
        selectTable(editorPage, projectName, EXAMPLE_3_MODULE, SPREADSHEET, VEHICLE_PREMIUM);
        assertThat(editorPage.getEditorToolbarPanelComponent().clickRun().offersParameter(RUNTIME_CONTEXT))
                .as("The project provides the Runtime Context, so the Run form must offer it")
                .isTrue();
        return editorPage;
    }

    private EditorPage openWorkbookTable(String folder, String tableName) {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, RUN_FORM_WORKBOOK);
        EditorPage editorPage = new EditorPage();
        selectTable(editorPage, projectName, RUN_FORM_MODULE, folder, tableName);
        return editorPage;
    }

    private void selectTable(EditorPage editorPage, String projectName, String module, String folder, String table) {
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, module);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(folder)
                .selectItemInFolder(folder, table);
    }
}
