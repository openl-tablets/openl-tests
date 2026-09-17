package tests.ui.webstudio.rules_editor;

import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOrderingModeTableList extends BaseTest {

    @Test
    @TestCaseId("IPBQA-32507")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTableListOrdering() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "sortingtesting.xlsx");
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "sortingtesting");

        assertThat(editorPage.getEditorLeftRulesTreeComponent().getViewFilterValue())
                .containsIgnoringCase("Excel Sheet");

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_EXCEL_SHEET);
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getFoldersVisible())
                .containsExactlyInAnyOrder("Sheet1", "Asheet", "すsupersheet");

        editorPage.getEditorLeftRulesTreeComponent().expandFolderInTree("Sheet1");
        editorPage.getEditorLeftRulesTreeComponent().expandFolderInTree("Asheet");
        List<String> nodesNames = editorPage.getEditorLeftRulesTreeComponent().getAllEndNodesNames();
        assertThat(nodesNames).contains("_MyRules2");
        assertThat(nodesNames).filteredOn("MyRules1"::equals).hasSize(2);
        assertThat(nodesNames).contains("тест123", "はsomeRules", "_someRules", "étudiantomeRules", "トsomeRules");

        editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder("Sheet1", "_MyRules2");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().clickCell(4, 2);
        editorPage.getEditorTableActionsPanelComponent().clickInsertRowAfter();
        editorPage.getCenterTable().editCell(5, 1, "1");
        editorPage.getCenterTable().editCell(5, 2, "1");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();

        assertThat(editorPage.getCenterTable().getCellText(5, 1)).isEqualTo("1");
        nodesNames = editorPage.getEditorLeftRulesTreeComponent().getAllEndNodesNames();
        assertThat(nodesNames).contains("_MyRules2");
        assertThat(nodesNames).filteredOn("MyRules1"::equals).hasSize(2);

        editorPage.getEditorToolbarPanelComponent().clickCreateTable();
        editorPage.getCreateTableDialogComponent()
                .selectType("Datatype Table")
                .clickNext()
                .setTechnicalName("NewDatatype")
                .addParameter("", "textField")
                .setCategorySelection("Asheet")
                .save();

        nodesNames = editorPage.getEditorLeftRulesTreeComponent().getAllEndNodesNames();
        assertThat(nodesNames).contains("тест123", "はsomeRules", "_someRules", "étudiantomeRules", "トsomeRules", "NewDatatype");

        editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder("Asheet", "はsomeRules");
        editorPage.getEditorToolbarPanelComponent().removeCurrentTable();
        editorPage.getEditorLeftRulesTreeComponent().expandFolderInTree("Asheet");
        nodesNames = editorPage.getEditorLeftRulesTreeComponent().getAllEndNodesNames();
        assertThat(nodesNames)
                .contains("тест123", "_someRules", "étudiantomeRules", "トsomeRules", "NewDatatype")
                .doesNotContain("はsomeRules");
    }

    @Test
    @TestCaseId("IPBQA-32507")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16665")
    public void testTableListOrdering2() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "sortingtesting1.xlsx");
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "sortingtesting1");

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_EXCEL_SHEET);
        editorPage.getEditorLeftRulesTreeComponent().expandFolderInTree("Sheet1");

        List<String> nodesNames = editorPage.getEditorLeftRulesTreeComponent().getAllEndNodesNames();
        assertThat(nodesNames).contains("_MyRules", "Atable");
        assertThat(nodesNames).filteredOn("MyRules"::equals).hasSize(2);

        assertThat(nodesNames)
                .as("The utility tables of the sheet should be reachable from the module screen, which is what "
                        + "the 'Hide Utility Tables' filter of the old tree turned on and off")
                .contains("Test123");
    }
}
