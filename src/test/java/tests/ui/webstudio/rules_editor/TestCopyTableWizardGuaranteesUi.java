package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.CopyTableDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestCopyTableWizardGuaranteesUi extends BaseTest {

    private static final String MODULE = "Main";
    private static final String TABLE_FOLDER = DECISION;
    private static final String TABLE_NAME = "Hello";
    private static final String VERSION = "0.0.2";
    private static final String TUTORIAL_PROJECT = "Tutorial 5 - Introduction to Table Properties";
    private static final String TUTORIAL_MODULE = "Tutorial5 - Intro to Table Properties";
    private static final String DATED_TABLE = "CarPrice";
    private static final String DATED_TABLE_2009 =
            "CarPrice [effectiveDate=01/01/2009, expirationDate=01/01/2010 11:59:59 PM, startRequestDate=06/01/2008]";
    private static final String DATED_TABLE_SHEET = "Step2";
    private static final long SHEET_SETTLE_MS = 5000;

    @Test
    @TestCaseId("IPBQA-33026")
    @Description("The copy wizard guaranteed version uniqueness; the React modal must reject copying a table "
            + "as a version that already exists (regression guard for EPBDS-16388).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDuplicateVersionCopyIsRejected() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TABLE_FOLDER)
                .selectItemInFolder(TABLE_FOLDER, TABLE_NAME);

        editorPage.getEditorToolbarPanelComponent().copyTableAsNewVersion(VERSION);
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getEditorLeftRulesTreeComponent()
                .expandFolderInTree(TABLE_FOLDER)
                .selectItemInFolder(TABLE_FOLDER, TABLE_NAME);
        CopyTableDialogComponent copyDialog = editorPage.getEditorToolbarPanelComponent().clickCopy();
        copyDialog.setVersion(VERSION);

        assertThat(copyDialog.isCopyButtonEnabled())
                .as("Copying as the already existing version '%s' must be rejected (EPBDS-16388)", VERSION)
                .isFalse();
    }

    @Test
    @TestCaseId({"EPBDS-16450", "EPBDS-11978"})
    @Description("The copy dialog opened on a table that declares dates and sits on a later sheet of its module "
            + "shows the table's Effective Date and Expiration Date in their date fields (EPBDS-16450) and offers "
            + "the table's own sheet in Save To instead of the first sheet of the module (EPBDS-11978).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCopyDialogShowsSourceDatesAndOwnSheet() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TUTORIAL_PROJECT);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, TUTORIAL_MODULE);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(TABLE_FOLDER)
                .expandFolderInTree(DATED_TABLE)
                .selectItemInFolder(DATED_TABLE, DATED_TABLE_2009);
        editorPage.waitUntilSpinnerLoaded();

        CopyTableDialogComponent copyDialog = editorPage.getEditorToolbarPanelComponent().clickCopy();

        assertThat(copyDialog.getPropertyValue("Effective Date"))
                .as("The date picker must show the Effective Date the source table declares (EPBDS-16450)")
                .isEqualTo("01/01/2009");
        assertThat(copyDialog.getPropertyValue("Expiration Date"))
                .as("The date picker must show the Expiration Date the source table declares (EPBDS-16450)")
                .isEqualTo("01/01/2010");
        WaitUtil.waitForCondition(() -> DATED_TABLE_SHEET.equals(copyDialog.getSheet()), SHEET_SETTLE_MS, 250,
                "Waiting for Save To to settle on the sheet of the copied table");
        assertThat(copyDialog.getSheet())
                .as("Save To must default to the sheet of the copied table, not the first sheet of the module (EPBDS-11978)")
                .isEqualTo(DATED_TABLE_SHEET);
    }
}
