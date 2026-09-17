package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.RightTableDetailsComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;


import static org.assertj.core.api.Assertions.assertThat;

public class TestAddProperty extends BaseTest {

    @Test
    @TestCaseId("EPBDS-6964")
    @Description("Exception occurs on adding property to the table with two columns - Playwright version")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testAddProperty() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "StudioIssues_TestAddProperty.xlsx");
        EditorPage editorPage = new EditorPage();
        
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "StudioIssues_TestAddProperty");
        
        // The table is reached through the sheet it is written on, which is what names the folder here. The
        // category view used to file a table declaring no category under its sheet too; it no longer does
        // (KNOWN-ISSUES.md #14), and what this test is about is the property, not the grouping.
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_EXCEL_SHEET)
                .expandFolderInTree("Rules")
                .selectItemInFolder("Rules", "SimpleCalc");
                
        // The property added is Category rather than Description: Description is one of the four the panel
        // can no longer offer (KNOWN-ISSUES.md #7). What this test guards is the write on a two-column
        // table, which any property drives.
        editorPage.getRightTableDetailsComponent()
                .addProperty(RightTableDetailsComponent.DropdownOptions.CATEGORY.getValue())
                .setProperty(RightTableDetailsComponent.DropdownOptions.CATEGORY.getValue(), "Category details")
                .clickSaveBtn();

        assertThat(editorPage.getRightTableDetailsComponent()
                .isPropertySet(RightTableDetailsComponent.DropdownOptions.CATEGORY.getValue(), "Category details"))
                .isTrue();
    }
}