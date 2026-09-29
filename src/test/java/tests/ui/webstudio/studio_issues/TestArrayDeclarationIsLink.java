package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.core.ui.WebElement;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.SPREADSHEET;

public class TestArrayDeclarationIsLink extends BaseTest {

    private static final String RULE_HEADER = "SmartRules String MyRules(String code)";

    @Test
    @TestCaseId("EPBDS-11230")
    @Description("Verify that array declarations are displayed as links with proper styling")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testArrayDeclarationIsLink() {
        EditorPage editorPage = createProjectAndOpenTable("TestArrayDeclarationIsLink", DECISION, "DetermineStatusByCodeRule");

        List<WebElement> links = editorPage.createElementList(
                "xpath=//td//button[starts-with(@data-testid,'cell-usage-')][normalize-space()='Procedure']");
        assertThat(links.size()).as("Should find exactly 12 procedure links").isEqualTo(12);

        String plain = editorPage.createElementList("xpath=//td[not(.//button)][normalize-space()!='']")
                .get(0).getCssValue("color");
        links.forEach(link -> assertThat(link.getCssValue("color"))
                .as("A procedure should be drawn in the colour a link is drawn in, not as plain text")
                .isNotEqualTo(plain));
    }

    @Test
    @TestCaseId("EPBDS-12990")
    @Description("A rule called with an array in a Spreadsheet step is drawn as a link on the rule name and leads to that rule")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testRuleCalledWithArrayIsLink() {
        EditorPage editorPage = createProjectAndOpenTable("TestRuleCalledWithArrayIsLink", SPREADSHEET, "mySpr");
        TableComponent spreadsheet = editorPage.getCenterTable();

        assertThat(spreadsheet.getCellText(3, 2)).as("The step calls the rule with the array").startsWith("=MyRules(arr)");
        assertThat(spreadsheet.getCellLinkTexts(3, 2))
                .as("In '=MyRules(arr)' the name of the called rule, and only it, should be a link")
                .containsExactly("MyRules");

        spreadsheet.getCellLink(3, 2, "MyRules").click();
        assertThat(WaitUtil.waitForCondition(() -> RULE_HEADER.equals(editorPage.getCenterTable().getCellText(1, 1)),
                10000, 200, "Waiting for the rule the link names to open"))
                .as("Following the link should open the table of the rule MyRules")
                .isTrue();
    }

    private EditorPage createProjectAndOpenTable(String module, String folder, String table) {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, module + ".xlsx");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, module);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(folder)
                .selectItemInFolder(folder, table);
        WaitUtil.waitForCondition(() -> editorPage.getCenterTable().isVisible(), 5000, 100, "Waiting for table to be visible...");
        return editorPage;
    }
}
