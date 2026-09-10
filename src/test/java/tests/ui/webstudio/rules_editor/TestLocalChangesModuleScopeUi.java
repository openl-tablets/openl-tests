package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

// EPBDS-16528: Local Changes read and restored the history of the project's first module instead of the
// selected one, because changes.xhtml rendered data-module-name from an EL variable that page never
// defines. Needs a project with several modules: with one module "first" and "selected" coincide, which is
// why TestLocalChangesCoreMechanics on the single-module Sample Project could not catch it.
// Requires an image containing 48b1f6e688.
public class TestLocalChangesModuleScopeUi extends BaseTest {

    private static final String PROJECT = "LocalChangesModuleScope";
    private static final String ARCHIVE = "TestMergeBranchesNoConflicts_NoConflicts.zip";
    private static final String SPREADSHEET_FOLDER = "Spreadsheet";
    private static final int CELL_ROW = 3;
    private static final int CELL_COLUMN = 2;

    private boolean moduleOpened;

    @Test
    @TestCaseId("EPBDS-16569")
    @Description("EPBDS-16528: Local Changes shows the history of the selected module in a multi-module project. "
            + "One module gets a single edit, another gets three, a third is left untouched, and each must report "
            + "its own history. The untouched module must stay empty even though its siblings have history.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void localChangesAreScopedToTheSelectedModule() {
        EditorPage editorPage = createMultiModuleProject();

        editSpreadsheet(editorPage, "Module1", "MySpr1", "Module1-edit-1");

        editSpreadsheet(editorPage, "Module2", "MySpr2", "Module2-edit-1");
        editSpreadsheet(editorPage, "Module2", "MySpr2", "Module2-edit-2");
        editSpreadsheet(editorPage, "Module2", "MySpr2", "Module2-edit-3");

        ChangesDialogComponent changes = openLocalChanges(editorPage, "Module2", "MySpr2");
        assertThat(changes.getChangesCount())
                .as("Module2 received three edits, so its own history must report three changes")
                .isEqualTo(3);
        assertThat(changes.getRowCount())
                .as("Three changes are shown as four rows: three history entries plus the current version")
                .isEqualTo(4);
        assertThat(changes.getRenderedModuleName())
                .as("The Local Changes island must carry the selected module name, otherwise the API is asked "
                        + "for the project without a module and the server falls back to the first module")
                .isEqualTo("Module2");

        changes = openLocalChanges(editorPage, "Module1", "MySpr1");
        assertThat(changes.getChangesCount())
                .as("Module1 received a single edit, so its history must not include Module2's three edits")
                .isEqualTo(1);
        assertThat(changes.getRowCount())
                .as("One change is shown as two rows: the history entry plus the current version")
                .isEqualTo(2);
        assertThat(changes.getRenderedModuleName())
                .as("Switching modules must re-render the island with the newly selected module")
                .isEqualTo("Module1");

        changes = openLocalChanges(editorPage, "Module3", "MySpr3");
        assertThat(changes.getNoChangesMessage())
                .as("Module3 was never edited, so it must report no history at all - on the defect it showed "
                        + "the history belonging to whichever module the resolver returned first")
                .isEqualTo("No changes in history");
        assertThat(changes.getRenderedModuleName())
                .as("The island must carry Module3 even though Module3 has no history of its own")
                .isEqualTo("Module3");
    }

    @Test
    @TestCaseId("EPBDS-16569")
    @Description("EPBDS-16528: Restore from Local Changes rolls back only the selected module. Two modules are "
            + "edited, the restore is performed on one of them, and the other module's table must keep its edit - "
            + "on the defect the restore overwrote the first module's file, losing that module's work.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void restoreRollsBackOnlyTheSelectedModule() {
        EditorPage editorPage = createMultiModuleProject();

        String module2Original = readSpreadsheetCell(editorPage, "Module2", "MySpr2");

        editSpreadsheet(editorPage, "Module1", "MySpr1", "Module1-keep-me");
        editSpreadsheet(editorPage, "Module2", "MySpr2", "Module2-roll-me-back");

        ChangesDialogComponent changes = openLocalChanges(editorPage, "Module2", "MySpr2");
        assertThat(changes.getRenderedModuleName())
                .as("Restore must be issued for the selected module")
                .isEqualTo("Module2");
        changes.clickRestoreAtRow(2);

        assertThat(readSpreadsheetCell(editorPage, "Module2", "MySpr2"))
                .as("Module2 was restored to the version before its edit, so the cell must hold its original value")
                .isEqualTo(module2Original);
        assertThat(readSpreadsheetCell(editorPage, "Module1", "MySpr1"))
                .as("Module1 was not restored, so its edit must survive - on the defect the restore was applied "
                        + "to the first module and this edit was lost")
                .isEqualTo("Module1-keep-me");
    }

    private EditorPage createMultiModuleProject() {
        moduleOpened = false;
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT, ARCHIVE);
        return repositoryPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.EDITOR);
    }

    // Before any module is open the breadcrumbs are absent and only the project tree can select a module;
    // afterwards the tree is replaced by the rules tree and the breadcrumb dropdown is the way across.
    // The choice is tracked rather than probed, because the breadcrumb re-renders during the post-save
    // recompile and a visibility probe would intermittently take the wrong branch.
    private void selectModule(EditorPage editorPage, String moduleName) {
        if (moduleOpened) {
            editorPage.getEditorToolbarPanelComponent().selectBreadcrumbModule(PROJECT, moduleName);
        } else {
            editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT, moduleName);
            moduleOpened = true;
        }
    }

    private void openSpreadsheet(EditorPage editorPage, String moduleName, String tableName) {
        selectModule(editorPage, moduleName);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(SPREADSHEET_FOLDER)
                .selectItemInFolder(SPREADSHEET_FOLDER, tableName);
        editorPage.waitUntilSpinnerLoaded();
    }

    private void editSpreadsheet(EditorPage editorPage, String moduleName, String tableName, String value) {
        openSpreadsheet(editorPage, moduleName, tableName);
        editorPage.getCenterTable().editCell(CELL_ROW, CELL_COLUMN, value);
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.waitUntilSpinnerLoaded();
    }

    private String readSpreadsheetCell(EditorPage editorPage, String moduleName, String tableName) {
        openSpreadsheet(editorPage, moduleName, tableName);
        return editorPage.getCenterTable().getCellText(CELL_ROW, CELL_COLUMN);
    }

    private ChangesDialogComponent openLocalChanges(EditorPage editorPage, String moduleName, String tableName) {
        openSpreadsheet(editorPage, moduleName, tableName);
        return editorPage.getEditorToolbarPanelComponent().clickMore().clickChanges();
    }
}
