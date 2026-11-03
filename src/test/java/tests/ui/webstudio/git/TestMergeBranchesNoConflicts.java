package tests.ui.webstudio.git;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.api.UsersMethod;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorTableActionsPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.repositorytabcomponents.SyncUpdatesDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.SPREADSHEET;

public class TestMergeBranchesNoConflicts extends BaseTest {

    private static final String PROJECT_NAME = "NoConflicts";
    private static final String BRANCH_NAME = "MyBranch";
    private static final String MASTER_BRANCH = "master";
    private static final String ARCHIVE = "TestMergeBranchesNoConflicts_SyncWorkbook.zip";
    private static final String BRANCH_STEP3_VALUE = "Branch3";
    private static final List<String> BRANCH_STEP = List.of("BranchStep", "B");
    private static final List<String> MASTER_STEP = List.of("MasterStep", "M");
    private static final int LAST_STEP_ROW = 5;

    @Test
    @TestCaseId("IPBQA-29455")
    @Description("Git - Merge branches without conflicts between master and MyBranch. Verifies EPBDS-8488: the "
            + "merge commit is authored by the WebStudio user's display name, not a .gitconfig identity, and the "
            + "first save on MyBranch after a module is deleted and another uploaded succeeds (regression guard for "
            + "EPBDS-16361). Both branches change MySpr1 of the same sheet: Sync combines the sheet without a conflict, "
            + "the steps both branches add at its end are all kept, those of the receiving branch first, and the Save "
            + "dialog, the Sync dialog and the Merge Summary show the files and the sheet the merge combines "
            + "(EPBDS-16883, EPBDS-16884, EPBDS-16962, EPBDS-16896).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testMergeBranchesNoConflicts() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT_NAME, ARCHIVE);

        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(PROJECT_NAME);
        assertThat(projectDetail.isBranchPresent(BRANCH_NAME)).as("MyBranch absent before create").isFalse();
        projectDetail.createBranch(BRANCH_NAME, true);
        assertThat(projectDetail.getCurrentBranch()).as("on MyBranch after create").isEqualTo(BRANCH_NAME);

        SyncUpdatesDialogComponent syncDialog = projectDetail.openMergeDialog(MASTER_BRANCH);
        assertThat(syncDialog.getHeader()).as("Sync dialog header").contains("Sync updates");
        assertThat(syncDialog.isReceiveEnabled()).as("Receive disabled initially").isFalse();
        assertThat(syncDialog.isSendEnabled()).as("Send disabled initially").isFalse();
        syncDialog.close();

        projectDetail.openFilesTab();
        projectDetail.deleteFile("Module4.xlsx");
        projectDetail.uploadFile(TestDataUtil.getFilePathFromResources("TestMergeBranchesNoConflicts_Module6.xlsx"));
        assertThat(repositoryPage.openProjectsList().saveProjectAndGetChangedFiles(PROJECT_NAME, "MyBranch: Module4 -> Module6"))
                .as("The Save dialog lists the files the save commits")
                .containsExactlyInAnyOrder("Deleted Module4.xlsx", "Added TestMergeBranchesNoConflicts_Module6.xlsx");

        editorPage = new EditorPage();
        EditorToolbarPanelComponent editorToolbar = editorPage.getEditorToolbarPanelComponent();
        editSpreadsheetCell(editorPage, "Module2", "MySpr2");
        openSpreadsheet(editorPage, "Module1", "MySpr1");
        editorToolbar.getEditTableBtn().click();
        editorPage.getCenterTable().editCell(LAST_STEP_ROW, 2, BRANCH_STEP3_VALUE);
        addStepAfterTheLast(editorPage, BRANCH_STEP);
        editorToolbar.clickSave();
        editorPage.getSaveChangesComponent().clickSave();

        editorToolbar.switchBranch(MASTER_BRANCH);
        editSpreadsheetCell(editorPage, "Module1", "MySpr1");
        editorToolbar.getEditTableBtn().click();
        addStepAfterTheLast(editorPage, MASTER_STEP);
        editorToolbar.clickSave();
        editorPage.getSaveChangesComponent().clickSave();

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        projectDetail = repositoryPage.openProjectDetail(PROJECT_NAME);
        assertThat(projectDetail.getCurrentBranch()).as("on master").isEqualTo(MASTER_BRANCH);
        projectDetail.openFilesTab();
        projectDetail.deleteFile("Module3.xlsx");
        projectDetail.uploadFile(TestDataUtil.getFilePathFromResources("TestMergeBranchesNoConflicts_Module5.xlsx"));
        repositoryPage.openProjectsList().saveProject(PROJECT_NAME, "master: Module3 -> Module5");

        SyncUpdatesDialogComponent receive = repositoryPage.openProjectDetail(PROJECT_NAME).openMergeDialog(BRANCH_NAME);
        assertThat(receive.getScopeProjectSummary())
                .as("The Sync dialog lists the current project with the number of files the merge brings in")
                .isEqualTo(PROJECT_NAME + " Current project 4 changed files");
        assertThat(receive.getFilesChangedInBoth())
                .as("The Sync dialog names the workbook both branches changed")
                .containsExactly(PROJECT_NAME + "/Module1.xlsx");
        receive.clickReceive();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();
        assertThat(repositoryPage.getMergeSummarySheets("Module1.xlsx"))
                .as("The Merge Summary tells that the sheet both branches changed was combined, not taken from one")
                .containsExactly("Sheet \"Sheet1\" combined from both branches");
        repositoryPage.closeMergeSummary();

        projectDetail = repositoryPage.openProjectsList().openProjectDetail(PROJECT_NAME);
        projectDetail.openFilesTab();
        assertPresent(projectDetail, "Module1.xlsx", "Module2.xlsx",
                "TestMergeBranchesNoConflicts_Module5.xlsx", "TestMergeBranchesNoConflicts_Module6.xlsx");
        assertAbsent(projectDetail, "Module3.xlsx", "Module4.xlsx");

        editorPage = new EditorPage();
        editorPage.reloadPage();
        assertSpreadsheetEdited(editorPage, "Module2", "MySpr2");
        editorPage.getEditorToolbarPanelComponent().getBreadcrumbsAllProjects().click();
        assertSpreadsheetEdited(editorPage, "Module1", "MySpr1");
        TableComponent merged = editorPage.getCenterTable();
        assertThat(List.of(merged.getRow(LAST_STEP_ROW).getValue(), merged.getRow(LAST_STEP_ROW + 1).getValue(),
                merged.getRow(LAST_STEP_ROW + 2).getValue()))
                .as("MySpr1 keeps the Step3 value of MyBranch and the steps both branches added, those of master first")
                .containsExactly(List.of("Step3", BRANCH_STEP3_VALUE), MASTER_STEP, BRANCH_STEP);
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorToolbar.switchBranch(BRANCH_NAME);
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        projectDetail = repositoryPage.openProjectDetail(PROJECT_NAME);
        assertThat(projectDetail.getCurrentBranch()).as("on MyBranch").isEqualTo(BRANCH_NAME);
        projectDetail.openFilesTab();
        assertPresent(projectDetail, "Module1.xlsx", "Module2.xlsx", "Module3.xlsx",
                "TestMergeBranchesNoConflicts_Module6.xlsx");
        assertAbsent(projectDetail, "TestMergeBranchesNoConflicts_Module5.xlsx", "Module4.xlsx");

        projectDetail.openMergeDialog(MASTER_BRANCH).clickReceive();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();

        projectDetail = repositoryPage.openProjectsList().openProjectDetail(PROJECT_NAME);
        projectDetail.openFilesTab();
        assertPresent(projectDetail, "Module1.xlsx", "Module2.xlsx",
                "TestMergeBranchesNoConflicts_Module5.xlsx", "TestMergeBranchesNoConflicts_Module6.xlsx");
        assertAbsent(projectDetail, "Module3.xlsx", "Module4.xlsx");

        String mergeComment = "Merge commit with ";
        List<String> revisionDescriptions = projectDetail.getRevisionDescriptions();
        List<String> revisionAuthors = projectDetail.getRevisionAuthors();
        assertThat(revisionDescriptions)
                .as("Revision history should include the merge, named after the commit merged in as Sync merged its "
                        + "workbook by itself, and the project creation")
                .anyMatch(d -> d.contains(mergeComment))
                .anyMatch(d -> d.contains("Format the workbooks merged from branch '" + BRANCH_NAME + "'"))
                .anyMatch(d -> d.contains("Project " + PROJECT_NAME + " is created"));

        String adminDisplayName = new UsersMethod().getProfileDisplayName(UserService.getUser(User.ADMIN));
        int mergeIndex = IntStream.range(0, revisionDescriptions.size())
                .filter(i -> revisionDescriptions.get(i).contains(mergeComment))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No merge revision found in: " + revisionDescriptions));
        assertThat(revisionAuthors.get(mergeIndex))
                .as("EPBDS-8488: the merge commit author must be the WebStudio user's display name, "
                        + "not a .gitconfig identity")
                .isEqualTo(adminDisplayName);
        assertThat(revisionAuthors)
                .as("Every revision should carry a non-empty author")
                .allMatch(a -> !a.isEmpty());
    }

    private void editSpreadsheetCell(EditorPage editorPage, String module, String spreadsheet) {
        openSpreadsheet(editorPage, module, spreadsheet);
        editorPage.getCenterTable().editCell(3, 1, "Step1*");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
    }

    private void openSpreadsheet(EditorPage editorPage, String module, String spreadsheet) {
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT_NAME, module);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(SPREADSHEET)
                .selectItemInFolder(SPREADSHEET, spreadsheet);
    }

    private void addStepAfterTheLast(EditorPage editorPage, List<String> step) {
        TableComponent table = editorPage.getCenterTable();
        EditorTableActionsPanelComponent actions = editorPage.getEditorTableActionsPanelComponent();
        table.clickCell(LAST_STEP_ROW, 1);
        actions.clickInsertRowAfter();
        table.editCell(LAST_STEP_ROW + 1, 1, step.get(0));
        table.editCell(LAST_STEP_ROW + 1, 2, step.get(1));
        actions.clickSaveChanges();
    }

    private void assertSpreadsheetEdited(EditorPage editorPage, String module, String spreadsheet) {
        openSpreadsheet(editorPage, module, spreadsheet);
        assertThat(editorPage.getCenterTable().getCellText(3, 1))
                .as("%s cell should carry the edited text", spreadsheet)
                .contains("Step1", "*");
    }

    private void assertPresent(ProjectDetailPage detail, String... files) {
        for (String file : files) {
            assertThat(detail.isFilePresent(file)).as("%s should be present", file).isTrue();
        }
    }

    private void assertAbsent(ProjectDetailPage detail, String... files) {
        for (String file : files) {
            assertThat(detail.isFilePresent(file)).as("%s should be absent", file).isFalse();
        }
    }
}
