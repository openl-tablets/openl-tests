package tests.ui.webstudio.studio_issues;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.editortabcomponents.toolbar.IRunMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceMenu;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestArrayOfAliasValuesInRunTrace extends BaseTest {

    private static final String EXCEL_FILE = "TestArrayOfAliasValuesInRunTrace.xlsx";
    private static final String MODULE_NAME = "TestArrayOfAliasValuesInRunTrace";
    private static final String COLLECTION = "my";
    private static final String FIRST_ELEMENT = "my[0]";
    private static final String TABLE_FOR_SHARED_INPUT = "myRule2";
    private static final List<String> ALIAS_VALUES = List.of("bla1", "bla2", "bla3");
    private static final List<String> TABLES = List.of("myRule2", "myRule3", "myRule5", "myRule_array",
            "myRule_x_array", "myRule_x", "myRule");

    @Test
    @TestCaseId("EPBDS-7796")
    @Description("BUG: Dropdown with alias values is empty in Run/Trace for array types. An element added to an "
            + "array of the alias datatype myType is offered as a list of bla1, bla2, bla3 in Run and in Trace "
            + "(EPBDS-16660).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testArrayOfAliasValuesInRunTrace() {
        EditorPage editorPage = openDecisionTables();

        TABLES.forEach(tableName -> {
            editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder(DECISION, tableName);

            IRunMenu runMenu = editorPage.getEditorToolbarPanelComponent().clickRun();
            runMenu.clickCreateItem()
                    .clickAddElementToCollectionBtn(COLLECTION)
                    .clickExpandCollection();
            assertThat(runMenu.offersTheFirstElementAsAList())
                    .as("The element of the array should be picked from the values of the alias datatype "
                            + "in the Run menu for table: " + tableName)
                    .isTrue();
            assertThat(runMenu.getAliasDropdownValues())
                    .as("Dropdown for alias values should contain expected values in Run menu for table: " + tableName)
                    .containsExactlyElementsOf(ALIAS_VALUES);

            ITraceMenu traceMenu = editorPage.getEditorToolbarPanelComponent().clickTrace();
            traceMenu.clickCreateItem()
                    .clickAddElementToCollectionBtn(COLLECTION)
                    .clickExpandCollection();
            assertThat(traceMenu.offersTheFirstElementAsAList())
                    .as("The element of the array should be picked from the values of the alias datatype "
                            + "in the Trace menu for table: " + tableName)
                    .isTrue();
            assertThat(traceMenu.getAliasDropdownValues())
                    .as("Dropdown for alias values should contain expected values in Trace menu for table: " + tableName)
                    .containsExactlyElementsOf(ALIAS_VALUES);
        });
    }

    @Test
    @TestCaseId("EPBDS-7796")
    @Description("The Trace menu keeps the element added in the Run menu to an array of the alias datatype and "
            + "offers the values of the alias for it, as the input form Run and Trace shared in 6.4.0 did.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16742")
    public void testTraceKeepsArrayElementAddedInRun() {
        EditorPage editorPage = openDecisionTables();
        editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder(DECISION, TABLE_FOR_SHARED_INPUT);

        IRunMenu runMenu = editorPage.getEditorToolbarPanelComponent().clickRun();
        runMenu.clickCreateItem()
                .clickAddElementToCollectionBtn(COLLECTION)
                .clickExpandCollection();
        assertThat(runMenu.getElementsOf(COLLECTION))
                .as("The Run menu should hold the element just added to '" + COLLECTION + "' of table: "
                        + TABLE_FOR_SHARED_INPUT)
                .containsExactly(FIRST_ELEMENT);

        ITraceMenu traceMenu = editorPage.getEditorToolbarPanelComponent().clickTrace();
        traceMenu.clickExpandCollection();
        assertThat(traceMenu.getElementsOf(COLLECTION))
                .as("The Trace menu should keep the element added to '" + COLLECTION + "' in the Run menu of table: "
                        + TABLE_FOR_SHARED_INPUT + " (6.4.0: Run and Trace share one input form)")
                .containsExactly(FIRST_ELEMENT);
        assertThat(traceMenu.offersTheFirstElementAsAList())
                .as("The element kept from the Run menu should be picked from the values of the alias datatype "
                        + "in the Trace menu for table: " + TABLE_FOR_SHARED_INPUT)
                .isTrue();
        assertThat(traceMenu.getAliasDropdownValues())
                .as("Dropdown for alias values of the element kept from the Run menu should contain expected values "
                        + "in Trace menu for table: " + TABLE_FOR_SHARED_INPUT)
                .containsExactlyElementsOf(ALIAS_VALUES);
    }

    private EditorPage openDecisionTables() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, EXCEL_FILE);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE_NAME);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION);
        return editorPage;
    }
}
