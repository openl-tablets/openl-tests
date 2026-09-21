package tests.ui.webstudio.git;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.SPREADSHEET;

public class TestMergeBranchesWithConflicts extends BaseTest {

    private static final String PROJECT_NAME = "MergeConflicts";
    private static final String BRANCH_1 = "Branch1";
    private static final String BRANCH_2 = "Branch2";
    private static final String MASTER_BRANCH = "master";
    private static final String MODULE_NAME = "Module1";
    private static final String TABLE_NAME = "MySpr1";
    private static final String BRANCH_1_VALUE = "Branch1Value";
    private static final String BRANCH_2_VALUE = "Branch2Value";

    @Test
    @TestCaseId("IPBQA-32850")
    @Description("Git - Merge branches with conflicts: Export with Use Yours resolution")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testMergeBranchesWithConflictsExportUseYours() {
        EditorPage editorPage = createProjectWithConflictingBranches();
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(PROJECT_NAME);
        projectDetail.openMergeDialog(BRANCH_1).clickSend();
        repositoryPage.getResolveConflictsDialogComponent().waitForDialogToAppear();
        assertThat(repositoryPage.getResolveConflictsDialogComponent().isDialogVisible())
                .as("Resolve Conflicts dialog should appear on a conflicting export").isTrue();
        repositoryPage.getResolveConflictsDialogComponent().resolveConflictUseYours();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();

        assertThat(readSpreadsheetCell(editorPage, BRANCH_1))
                .as("Branch1 should hold Branch2's version after 'Use yours'").contains(BRANCH_2_VALUE);
        assertThat(readSpreadsheetCell(editorPage, BRANCH_2))
                .as("Branch2 keeps its own version").contains(BRANCH_2_VALUE);
    }

    @Test
    @TestCaseId("IPBQA-32851")
    @Description("Git - Merge branches with conflicts: Import with Use Theirs resolution")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testMergeBranchesWithConflictsImportUseTheirs() {
        EditorPage editorPage = createProjectWithConflictingBranches();
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(PROJECT_NAME);
        projectDetail.openMergeDialog(BRANCH_1).clickReceive();
        repositoryPage.getResolveConflictsDialogComponent().waitForDialogToAppear();
        assertThat(repositoryPage.getResolveConflictsDialogComponent().isDialogVisible())
                .as("Resolve Conflicts dialog should appear on a conflicting import").isTrue();
        repositoryPage.getResolveConflictsDialogComponent().resolveConflictUseTheirs();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();

        assertThat(readSpreadsheetCell(editorPage, BRANCH_2))
                .as("Branch2 should hold Branch1's version after 'Use theirs'").contains(BRANCH_1_VALUE);
        assertThat(readSpreadsheetCell(editorPage, BRANCH_1))
                .as("Branch1 keeps its own version").contains(BRANCH_1_VALUE);
    }

    private EditorPage createProjectWithConflictingBranches() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT_NAME,
                "TestMergeBranchesNoConflicts_NoConflicts.zip");

        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(PROJECT_NAME);
        projectDetail.createBranch(BRANCH_1, true);
        editSpreadsheetCell(editorPage, BRANCH_1_VALUE);

        new EditorPage();
        editorPage.getEditorToolbarPanelComponent().switchBranch(MASTER_BRANCH);
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        projectDetail = repositoryPage.openProjectDetail(PROJECT_NAME);
        projectDetail.createBranch(BRANCH_2, true);
        editSpreadsheetCell(editorPage, BRANCH_2_VALUE);
        return editorPage;
    }

    private void editSpreadsheetCell(EditorPage editorPage, String value) {
        EditorPage edit = new EditorPage();
        edit.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT_NAME, MODULE_NAME);
        edit.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(SPREADSHEET)
                .selectItemInFolder(SPREADSHEET, TABLE_NAME);
        edit.getCenterTable().editCell(3, 1, value);
        edit.getEditorTableActionsPanelComponent().clickSaveChanges();
        edit.getEditorToolbarPanelComponent().clickSave();
        edit.getSaveChangesComponent().clickSave();
    }

    private String readSpreadsheetCell(EditorPage editorPage, String branch) {
        RepositoryPage repo = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        if (repo.isProjectActionAvailable(PROJECT_NAME, "Open")) {
            repo.openProject(PROJECT_NAME);
            repo.waitUntilSpinnerLoaded();
        }
        EditorPage edit = new EditorPage();
        edit.reloadPage();
        edit.getEditorLeftProjectModuleSelectorComponent().selectProject(PROJECT_NAME);
        edit.getEditorToolbarPanelComponent().switchBranch(branch);
        edit.reloadPage();
        edit.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT_NAME, MODULE_NAME);
        edit.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(SPREADSHEET)
                .selectItemInFolder(SPREADSHEET, TABLE_NAME);
        return edit.getCenterTable().getCellText(3, 1);
    }
}
