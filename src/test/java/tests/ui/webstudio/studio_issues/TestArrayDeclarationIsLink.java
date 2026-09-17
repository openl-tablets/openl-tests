package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.core.ui.WebElement;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestArrayDeclarationIsLink extends BaseTest {

    @Test
    @TestCaseId("EPBDS-11230")
    @Description("Verify that array declarations are displayed as links with proper styling")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testArrayDeclarationIsLink() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "TestArrayDeclarationIsLink.xlsx");

        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "TestArrayDeclarationIsLink");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Rules")
                .selectItemInFolder("Rules", "DetermineStatusByCodeRule");

        // A usage named in a cell is something to press, drawn in the colour a link is drawn in and
        // underlined under the pointer rather than at rest.
        WaitUtil.waitForCondition(() -> editorPage.getCenterTable().isVisible(), 5000, 100, "Waiting for table to be visible...");
        List<WebElement> links = editorPage.createElementList(
                "xpath=//td//button[starts-with(@data-testid,'cell-usage-')][normalize-space()='Procedure']");
        assertThat(links.size()).as("Should find exactly 12 procedure links").isEqualTo(12);

        links.forEach(link -> assertThat(link.getCssValue("cursor"))
                .as("A procedure should read as something the reader can press")
                .isEqualTo("pointer"));
    }
}