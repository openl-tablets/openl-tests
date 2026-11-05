package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.CreateTableDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCreateTableTypesUi extends BaseTest {

    private static final String MODULE = "Main";
    private static final String TITLED_FIXTURE = "TitledDatatype.xlsx";
    private static final String TITLED_MODULE = "TitledDatatype";

    @Test
    @TestCaseId("IPBQA-33020")
    @Description("EPBDS-16313 The Create Table modal must create a decision Rules table that compiles and renders in the module.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateRulesTableViaModal() {
        EditorPage editorPage = openEditor();

        openCreateTableDialog(editorPage)
                .selectType("Rules")
                .setSimpleRulesInitialParameters("QaDecisionRules", "Integer")
                .setCell(0, 1, "42")
                .save();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getProblemsPanelComponent().checkNoProblems();
        assertThat(editorPage.getCenterTable().getCellText(1, 1))
                .as("The created decision table header must carry the technical name and the Integer result type")
                .contains("QaDecisionRules")
                .contains("Integer");
    }

    @Test
    @TestCaseId("IPBQA-33021")
    @Description("EPBDS-16313 The Create Table modal must create a Spreadsheet table that compiles and renders in the module.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateSpreadsheetTableViaModal() {
        EditorPage editorPage = openEditor();

        openCreateTableDialog(editorPage)
                .selectType("Spreadsheet")
                .setTechnicalName("QaSpreadsheet")
                .save();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getProblemsPanelComponent().checkNoProblems();
        assertThat(editorPage.getCenterTable().getCellText(1, 1))
                .as("The created spreadsheet header must carry the technical name")
                .contains("QaSpreadsheet");
    }

    @Test
    @TestCaseId("IPBQA-33022")
    @Description("The Create Table modal must create a transposed Test table for the template's Hello table "
            + "(the transposed orientation is the EPBDS-6912 addition).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateTransposedTestTableViaModal() {
        EditorPage editorPage = openEditor();

        openCreateTableDialog(editorPage)
                .selectType("Test")
                .setTechnicalName("QaHelloTest")
                .toggleTransposed()
                .save();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getProblemsPanelComponent().checkNoProblems();
        assertThat(editorPage.getCenterTable().getCellText(1, 1))
                .as("The created test table header must reference the tested Hello table")
                .contains("Test Hello");
    }

    @Test
    @TestCaseId("EPBDS-16417")
    @Description("EPBDS-16417 The Create Table modal must let Create be pressed for a Smart Lookup and a Simple Lookup "
            + "that take a single argument, and create a table that compiles.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateSingleArgumentLookupTablesViaModal() {
        EditorPage editorPage = openEditor();

        for (String type : List.of("Simple Lookup", "Smart Lookup")) {
            String tableName = "Qa" + type.replace(" ", "");
            CreateTableDialogComponent dialog = openCreateTableDialog(editorPage)
                    .selectType(type)
                    .setSimpleRulesInitialParameters(tableName, "Integer")
                    .addSimpleRulesParameter("String", false, "a")
                    .setRow(0, "x", "1")
                    .setRow(1, "y", "2");

            assertThat(dialog.isCreateButtonEnabled())
                    .as("Create must be enabled for a %s of one argument", type)
                    .isTrue();
            dialog.save();
            editorPage.waitUntilSpinnerLoaded();

            editorPage.getProblemsPanelComponent().checkNoProblems();
            assertThat(editorPage.getCenterTable().getCellText(1, 1))
                    .as("The created %s must carry its name, the Integer result type and its single argument", type)
                    .contains(tableName)
                    .contains("Integer")
                    .contains("String a");
        }
    }

    @Test
    @TestCaseId("EPBDS-16418")
    @Description("EPBDS-16418 A Data table created through the Create Table modal for a datatype that titles its columns "
            + "(Type, Name) must have one column per field and no extra Name column.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateDataTableForTitledDatatypeHasNoExtraColumn() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, TITLED_FIXTURE);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, TITLED_MODULE);

        openCreateTableDialog(editorPage)
                .selectType("Data")
                .setTechnicalName("NewDataTable")
                .selectDatatype("Address")
                .save();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getProblemsPanelComponent().checkNoProblems();
        assertThat(editorPage.getCenterTable().getCellText(1, 1))
                .as("The created Data table must be for the Address datatype")
                .contains("Address")
                .contains("NewDataTable");
        assertThat(editorPage.getCenterTable().getColumn(1))
                .as("The only column of the Data table must be the city field, not a phantom Name field")
                .contains("city")
                .doesNotContain("Name", "name");
        assertThat(editorPage.getCenterTable().getColumn(2))
                .as("The Data table must have no second column: the datatype has one field")
                .isEmpty();
    }

    private EditorPage openEditor() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        return editorPage;
    }

    private CreateTableDialogComponent openCreateTableDialog(EditorPage editorPage) {
        editorPage.getEditorToolbarPanelComponent().clickCreateTable();
        return editorPage.getCreateTableDialogComponent().waitForDialogToAppear();
    }
}
