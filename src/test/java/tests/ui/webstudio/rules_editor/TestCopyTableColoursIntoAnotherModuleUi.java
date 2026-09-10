package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

// EPBDS-16416: a cell colour can be stored as an index into the workbook's own palette, and copying a table
// into another module carried the numbers rather than the colours, so the copy was painted in whatever those
// indices mean in the default Excel palette. Bank Rating is the only bundled project with a custom
// indexedColors block, which is why the substitution is visible at all.
//
// The unit tests that came with the fix cover the style writer; this covers the round trip through Studio.
//
// DISABLED: the fix (57781051b6) is the head of openl-tablets main and is not in any published image - it is
// three commits newer than 6.5.0-ef74952e66f5, the newest tag in ghcr. The test has therefore never been
// executed in either direction. Enable it, run it against an image that contains the fix and against one
// that does not, and only then treat it as a regression test.
public class TestCopyTableColoursIntoAnotherModuleUi extends BaseTest {

    private static final String TEMPLATE = "Example 1 - Bank Rating";
    private static final String SOURCE_MODULE = "Bank Rating";
    private static final String TARGET_MODULE = "Limits";
    private static final String TABLE = "BankLimitIndex";
    private static final String COPY = "BankLimitIndexCopy";
    private static final String TABLE_FOLDER = "Rules";
    private static final String WILDCARD_RULES_XML = "BankRatingWildcardRules.xml";

    private static final String HEADER_BACKGROUND = "rgb(221, 217, 195)";
    private static final String HEADER_FONT = "rgb(74, 69, 42)";

    @Test(enabled = false)
    @TestCaseId("EPBDS-16570")
    @KnownIssue("EPBDS-16416")
    @Description("EPBDS-16416: a table copied into another module must keep the colours it shows, not the "
            + "colours its palette indices mean in the default Excel palette. Bank Rating stores its colours "
            + "as indices into a custom palette, so a copy into a new workbook is where the defect appears.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void copyIntoAnotherModuleKeepsTheColoursTheTableShows() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE);
        EditorPage editorPage = new EditorPage();

        // Without a wildcard rules.xml every xlsx resolves as its own module, so the copy would not see the
        // datatypes declared in Bank Rating.xlsx and would fail to compile whatever the fix does.
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectDetail(projectName)
                .uploadFileAs(TestDataUtil.getFilePathFromResources(WILDCARD_RULES_XML), "rules.xml");
        editorPage = repositoryPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.EDITOR);

        openTable(editorPage, projectName, SOURCE_MODULE, TABLE);
        String sourceHeaderBackground = cellBackground(editorPage.getCenterTable());
        String sourceHeaderFont = cellFontColour(editorPage.getCenterTable());
        assertThat(sourceHeaderBackground)
                .as("Precondition: the source header must carry the custom palette colour, otherwise the "
                        + "comparison below proves nothing")
                .isEqualTo(HEADER_BACKGROUND);
        assertThat(sourceHeaderFont)
                .as("Precondition: the source header font must carry the custom palette colour")
                .isEqualTo(HEADER_FONT);

        editorPage.getEditorToolbarPanelComponent().clickCopy()
                .waitForDialogToAppear()
                .selectCopyAs("New Table")
                .setName(COPY)
                .typeNewModule(TARGET_MODULE)
                .clickCopy();
        editorPage.waitUntilSpinnerLoaded();

        openTable(editorPage, projectName, TARGET_MODULE, COPY);
        assertThat(cellBackground(editorPage.getCenterTable()))
                .as("The copy must show the colour the source shows, not green - which is what index 17 means "
                        + "in the default Excel palette")
                .isEqualTo(sourceHeaderBackground);
        assertThat(cellFontColour(editorPage.getCenterTable()))
                .as("The copy's header font must keep its colour, not turn dark red - which is what index 16 "
                        + "means in the default Excel palette")
                .isEqualTo(sourceHeaderFont);

        openTable(editorPage, projectName, SOURCE_MODULE, TABLE);
        assertThat(cellBackground(editorPage.getCenterTable()))
                .as("Copying must not repaint the source table")
                .isEqualTo(sourceHeaderBackground);
    }

    private void openTable(EditorPage editorPage, String projectName, String moduleName, String tableName) {
        editorPage.getEditorToolbarPanelComponent().selectBreadcrumbModule(projectName, moduleName);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TABLE_FOLDER)
                .selectItemInFolder(TABLE_FOLDER, tableName);
        editorPage.waitUntilSpinnerLoaded();
    }

    private String cellBackground(TableComponent table) {
        return String.valueOf(table.getRow(1).getCells().getFirst().getLocator()
                .evaluate("cell => getComputedStyle(cell).backgroundColor"));
    }

    private String cellFontColour(TableComponent table) {
        return String.valueOf(table.getRow(1).getCells().getFirst().getLocator()
                .evaluate("cell => getComputedStyle(cell).color"));
    }
}
