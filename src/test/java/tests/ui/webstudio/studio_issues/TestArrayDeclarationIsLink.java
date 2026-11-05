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

    private static final String CELL_USAGE_MODULE = "TestCellUsageLinks";
    private static final long TABLE_SHOWN_MS = 5000;
    private static final long LINKS_DRAWN_MS = 20000;
    private static final long LINK_OPENED_MS = 10000;
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

    @Test
    @TestCaseId({"EPBDS-16796", "EPBDS-12301", "EPBDS-16904"})
    @Description("EPBDS-16796: the called rule Hello, written for several states, is a link that opens the table choosing the version. "
            + "EPBDS-12301: a datatype named in a Spreadsheet header and in a step formula getValues(AlenaAlias) is a link that opens the datatype. "
            + "EPBDS-16904: the type of a Datatype field bound to a context property is a link that opens the type, and its default value is drawn as a value of the type")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testNamedTablesAreLinksThatOpenThem() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, CELL_USAGE_MODULE + ".xlsx");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, CELL_USAGE_MODULE);

        openTable(editorPage, "mySpr");
        assertLinkOpens(editorPage, 3, 2, "AlenaAlias", "Datatype AlenaAlias");
        openTable(editorPage, "mySpr");
        assertLinkOpens(editorPage, 1, 1, "AlenaAlias", "Datatype AlenaAlias");

        openTable(editorPage, "Policy");
        TableComponent policy = editorPage.getCenterTable();
        assertThat(policy.getCellText(2, 2)).as("The field bound to the context property").startsWith("situsState :");
        assertLinkOpens(editorPage, 2, 1, "State", "Datatype State");
        openTable(editorPage, "Policy");
        assertLinkOpens(editorPage, 3, 1, "State", "Datatype State");
        openTable(editorPage, "Policy");
        policy = editorPage.getCenterTable();
        assertThat(policy.getCell(2, 3).getCssValue("color"))
                .as("The default value of the field bound to a context property should be drawn as a value of its type, like the unbound State field")
                .isEqualTo(policy.getCell(3, 3).getCssValue("color"));

        openTable(editorPage, "CallHello");
        assertLinkOpens(editorPage, 2, 1, "Hello", "_Hello(");
    }

    private void openTable(EditorPage editorPage, String table) {
        editorPage.getEditorLeftRulesTreeComponent().waitForTreeFoldersToLoad().selectVisibleLeafNode(table);
        WaitUtil.requireCondition(() -> editorPage.getCenterTable().isVisible(), TABLE_SHOWN_MS, 100,
                "Waiting for the table " + table + " to be shown");
    }

    private void assertLinkOpens(EditorPage editorPage, int row, int column, String word, String openedHeader) {
        TableComponent table = editorPage.getCenterTable();
        assertThat(WaitUtil.waitForCondition(() -> table.getCellLinkTexts(row, column).contains(word),
                LINKS_DRAWN_MS, 250, "Waiting for '" + word + "' to be drawn as a link"))
                .as("'%s' in cell [%d,%d] should be a link to the table it names", word, row, column)
                .isTrue();
        table.getCellLink(row, column, word).click();
        assertThat(WaitUtil.waitForCondition(() -> editorPage.getCenterTable().getCellText(1, 1).contains(openedHeader),
                LINK_OPENED_MS, 200, "Waiting for the table the link names to open"))
                .as("Following the link '%s' should open the table '%s'", word, openedHeader)
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
