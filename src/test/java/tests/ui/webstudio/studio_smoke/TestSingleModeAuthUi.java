package tests.ui.webstudio.studio_smoke;

import configuration.annotations.Description;
import configuration.annotations.AppContainerConfig;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.common.UserSlidingRightMenuComponent;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorTableActionsPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.LoginPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.AppContainerFileService;
import helpers.utils.TestDataUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static configuration.appcontainer.AppContainerStartParameters.SINGLE_FILE_REPO_URI;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.SPREADSHEET;
import static org.assertj.core.api.Assertions.assertThat;

public class TestSingleModeAuthUi extends BaseTest {

    private static final String PROJECT = "InPlace";
    private static final String MODULE = "InPlaceRules";
    private static final String WORKBOOK = "InPlaceRules.xlsx";
    private static final String CHANGED_WORKBOOK = "InPlaceRules_ChangedOnDisk.xlsx";
    private static final String TABLE = "InPlaceCalc";
    private static final int TOTAL_ROW = 4;
    private static final int VALUE_COLUMN = 2;
    private static final int DISK_CHANGE_SHOWN_MS = 30000;
    private static final String EXTRA_MODULE = "InPlaceExtra";
    private static final String EXTRA_WORKBOOK = "InPlaceExtra.xlsx";
    private static final String RULES_WITH_EXTRA_MODULE = "rules_with_extra_module.xml";

    @Test
    @TestCaseId({"EPBDS-17081", "EPBDS-17103"})
    @Description("Single-user mode: opening the app auto-authenticates with no login form, and the "
            + "configured single user has admin access (Administration menu visible). With a file design repository "
            + "a project is edited in place: nothing to open, close or save, an edit lands in the file, a change of "
            + "the file on disk is told with Refresh, an edit over it is refused, and Refresh shows the file (EPBDS-16987). "
            + "The local history keeps the version from disk and restores it, and a workbook and rules.xml changed on disk "
            + "are told with Refresh.")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_SINGLE_FILE_REPO_PARAMS)
    public void testSingleModeAutoAuthentication() {
        DriverPool.getPage().navigate(DriverPool.getAppUrl());
        DriverPool.getPage().waitForLoadState();
        new LoginPage().completeProfileIfRequested();

        assertThat(new LoginPage().isLoginFormDisplayed(5000))
                .as("single mode auto-authenticates — no login form is shown")
                .isFalse();

        EditorPage editorPage = new EditorPage();
        UserSlidingRightMenuComponent userMenu = editorPage.openUserMenu();
        assertThat(userMenu.isAdministrationMenuItemVisible())
                .as("the single user has admin access (Administration menu visible)")
                .isTrue();
        userMenu.close();

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.EXCEL_FILES, PROJECT, WORKBOOK);
        assertThat(repositoryPage.getProjectActionLabels(PROJECT))
                .as("A project of a file repository is edited in place: there is nothing to open, close, save or sync")
                .contains("Export")
                .doesNotContain("Open", "Close", "Save", "Discard", "Sync");

        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();
        openTable(editorPage);
        assertThat(toolbar.getAllVisibleTopToolbarActions())
                .as("The editor offers nothing to open, save, close, sync or discard for a project edited in place")
                .contains("Export")
                .doesNotContain("Open", "Save", "Close", "Sync", "Discard");
        toolbar.getEditTableBtn().click();
        editorPage.getCenterTable().editCell(TOTAL_ROW, VALUE_COLUMN, "48");
        actions.clickSaveChanges();
        editorPage.reloadPage();
        openTable(editorPage);
        assertThat(editorPage.getCenterTable().getCellText(TOTAL_ROW, VALUE_COLUMN))
                .as("The edit is written into the file and stays after a reload")
                .isEqualTo("48");
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        assertThat(repositoryPage.openProjectDetail(PROJECT).getStatus())
                .as("A project edited in place is never shown as modified")
                .isEqualTo("No Changes");

        openTable(editorPage);
        assertThat(toolbar.getChangedOnDiskNotice(1000))
                .as("The edits of the editor itself are not taken for a change made outside OpenL Studio")
                .isEmpty();
        AppContainerFileService.copyFileToContainer(TestDataUtil.getFilePathFromResources(CHANGED_WORKBOOK),
                SINGLE_FILE_REPO_URI + "/" + PROJECT + "/rules/" + WORKBOOK);
        assertThat(toolbar.getChangedOnDiskNotice(DISK_CHANGE_SHOWN_MS))
                .as("The editor tells that the file changed on disk, as Excel would save it")
                .contains("The files of the project were changed outside OpenL Studio");

        toolbar.getEditTableBtn().click();
        editorPage.getCenterTable().editCell(TOTAL_ROW, VALUE_COLUMN, "60");
        assertThat(actions.clickSaveChangesAndReadRefusal())
                .as("An edit of the table read before the change on disk is refused instead of overwriting it")
                .contains(WORKBOOK + " changed outside OpenL Studio since it was read",
                        "Refresh the project and repeat the edit");
        actions.discardChanges();

        toolbar.clickProjectRefresh();
        WaitUtil.waitForCondition(() -> "36".equals(editorPage.getCenterTable().getCellText(TOTAL_ROW, VALUE_COLUMN)),
                DISK_CHANGE_SHOWN_MS, 500, "Waiting for Refresh to show the file changed on disk");
        assertThat(editorPage.getCenterTable().getCellText(TOTAL_ROW, VALUE_COLUMN))
                .as("Refresh shows the value of the file changed on disk")
                .isEqualTo("36");

        toolbar.getEditTableBtn().click();
        editorPage.getCenterTable().editCell(TOTAL_ROW, VALUE_COLUMN, "50");
        actions.clickSaveChanges();
        ChangesDialogComponent localChanges = toolbar.clickMore().clickChanges();
        assertThat(localChanges.isRowCurrent(1))
                .as("The edit made after Refresh is the current version of the module")
                .isTrue();
        localChanges.clickRestoreAtRow(2);
        localChanges.closeIfOpen();
        WaitUtil.waitForCondition(() -> "36".equals(editorPage.getCenterTable().getCellText(TOTAL_ROW, VALUE_COLUMN)),
                DISK_CHANGE_SHOWN_MS, 500, "Waiting for the restored version of the module");
        assertThat(editorPage.getCenterTable().getCellText(TOTAL_ROW, VALUE_COLUMN))
                .as("The local history kept the version another program saved before OpenL Studio wrote over it, "
                        + "and restoring it brings that version back")
                .isEqualTo("36");

        assertThat(toolbar.getChangedOnDiskNotice(1000))
                .as("A restore made in the editor is not taken for a change made outside OpenL Studio")
                .isEmpty();
        String projectFolder = SINGLE_FILE_REPO_URI + "/" + PROJECT;
        AppContainerFileService.copyFileToContainer(TestDataUtil.getFilePathFromResources(EXTRA_WORKBOOK),
                projectFolder + "/rules/" + EXTRA_WORKBOOK);
        AppContainerFileService.copyFileToContainer(TestDataUtil.getFilePathFromResources(RULES_WITH_EXTRA_MODULE),
                projectFolder + "/rules.xml");
        assertThat(toolbar.getChangedOnDiskNotice(DISK_CHANGE_SHOWN_MS))
                .as("The editor tells that rules.xml and the modules of the project changed on disk")
                .contains("The files of the project were changed outside OpenL Studio");
        toolbar.clickProjectRefresh();
        assertThat(editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(PROJECT))
                .as("After Refresh the workbook added on disk is a module of the project")
                .contains(MODULE, EXTRA_MODULE);
    }

    private void openTable(EditorPage editorPage) {
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT, MODULE);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(SPREADSHEET)
                .selectItemInFolder(SPREADSHEET, TABLE);
    }
}
