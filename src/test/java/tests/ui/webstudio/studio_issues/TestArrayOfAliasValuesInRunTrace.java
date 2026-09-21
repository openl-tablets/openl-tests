package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import domain.ui.webstudio.components.editortabcomponents.toolbar.IRunMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceMenu;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestArrayOfAliasValuesInRunTrace extends BaseTest {

    private final List<String> tables = Arrays.asList("myRule2", "myRule3", "myRule5", "myRule_array", "myRule_x_array",
            "myRule_x", "myRule");

    @Test
    @TestCaseId("EPBDS-7796")
    @Description("BUG: Dropdown with alias values is empty in Run/Trace for array types")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16660")
    public void testArrayOfAliasValuesInRunTrace() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "TestArrayOfAliasValuesInRunTrace.xlsx");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "TestArrayOfAliasValuesInRunTrace");

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION);

        tables.forEach(tableName -> {
            editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder(DECISION, tableName);

            IRunMenu runMenu = editorPage.getEditorToolbarPanelComponent().clickRun();
            runMenu.clickCreateItem()
                    .clickAddElementToCollectionBtn("my")
                    .clickExpandCollection();
            
            assertThat(runMenu.offersTheFirstElementAsAList())
                    .as("The element of the array should be written by picking from the values of the alias "
                            + "datatype in the Run menu for table: " + tableName)
                    .isTrue();
            assertThat(runMenu.getAliasDropdownValues())
                    .as("Dropdown for alias values should contain expected values in Run menu for table: " + tableName)
                    .containsExactly("bla1", "bla2", "bla3");

            ITraceMenu traceMenu = editorPage.getEditorToolbarPanelComponent().clickTrace();
            assertThat(traceMenu.offersTheFirstElementAsAList())
                    .as("The element of the array should be written by picking from the values of the alias "
                            + "datatype in the Trace menu for table: " + tableName)
                    .isTrue();
            assertThat(traceMenu.getAliasDropdownValues())
                    .as("Dropdown for alias values should contain expected values in Trace menu for table: " + tableName)
                    .containsExactly("bla1", "bla2", "bla3");
        });
    }
}