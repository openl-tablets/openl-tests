package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.CompareLocalChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.EditProjectDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ResolveConflictsDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.SPREADSHEET;
import static org.assertj.core.api.Assertions.assertThat;

public class TestReactMergeConflictSmoke extends BaseTest {

    private static final String PROJECT = "ConflictSmoke";
    private static final String BRANCH = "MyBranch";
    private static final String MASTER = "master";
    private static final String ARCHIVE = "TestMergeBranchesNoConflicts_SyncWorkbook.zip";
    private static final String WORKBOOK = "Module1.xlsx";
    private static final String DESCRIPTOR = "rules.xml";
    private static final String TABLE_NODE = "Spreadsheet SpreadsheetResult MySpr1";
    private static final String THEIR_STEP1 = "TheirValue";
    private static final String YOUR_STEP1 = "YourValue";
    private static final String THEIR_STEP2 = "Their2";
    private static final String YOUR_STEP3 = "Your3";
    private static final String THEIR_DESCRIPTION = "Their description";
    private static final String YOUR_DESCRIPTION = "Your description";
    private static final int STEP2_ROW = 4;
    private static final int STEP3_ROW = 5;
    private static final int TABLE = 1;
    private static final String STEP1_CELL = "C7";

    @Test
    @TestCaseId("EPBDS-17078")
    @Description("Sync with conflicts in a workbook and in rules.xml: Compare File Versions opens on Their Changes "
            + "(EPBDS-16637), rules.xml is merged line by line, and the workbook table by table: the cells only one "
            + "branch changed are merged by themselves, Take Yours, Take Base and Take Theirs stand above the table "
            + "(EPBDS-17026) and the conflicting cell takes their value (EPBDS-16885, EPBDS-16964).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void resolveConflictsTableByTableAndLineByLine() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT, ARCHIVE);

        ProjectDetailPage detail = repositoryPage.openProjectDetail(PROJECT);
        detail.createBranch(BRANCH, true);
        detail.editOverviewDescriptionAndSave(THEIR_DESCRIPTION);
        editorPage = new EditorPage();
        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        editMySpr1(editorPage, THEIR_STEP1, STEP2_ROW, THEIR_STEP2);
        toolbar.clickSave();
        editorPage.getSaveChangesComponent().clickSave();

        toolbar.switchBranch(MASTER);
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectDetail(PROJECT).editOverviewDescriptionAndSave(YOUR_DESCRIPTION);
        editorPage = new EditorPage();
        editMySpr1(editorPage, YOUR_STEP1, STEP3_ROW, YOUR_STEP3);
        toolbar.clickSave();
        editorPage.getSaveChangesComponent().clickSave();

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectDetail(PROJECT).openMergeDialog(BRANCH).clickReceive();
        ResolveConflictsDialogComponent conflicts = repositoryPage.getResolveConflictsDialogComponent();
        conflicts.waitForConflictedFile(WORKBOOK);
        conflicts.waitForConflictedFile(DESCRIPTOR);

        CompareLocalChangesDialogComponent compare = conflicts.openCompareAsPopup(WORKBOOK);
        compare.clickTreeNode(TABLE_NODE);
        assertThat(compare.isDifferenceShown(2, THEIR_STEP1))
                .as("Compare File Versions opens on Their Changes, showing the Step1 MyBranch wrote").isTrue();
        assertThat(compare.isDifferenceShown(2, THEIR_STEP2))
                .as("Compare File Versions opens on Their Changes, showing the Step2 value MyBranch wrote").isTrue();
        compare.close();

        conflicts.chooseResolution(DESCRIPTOR, "Merge line by line");
        conflicts.takeTheirsInTextMerge();
        assertThat(conflicts.getTextMergeProgress()).as("The only line conflict of rules.xml is resolved")
                .isEqualTo("1 of 1 conflicts resolved");
        assertThat(conflicts.getTextMergeResult(1)).as("The result takes the description of MyBranch")
                .contains(THEIR_DESCRIPTION).doesNotContain(YOUR_DESCRIPTION);
        conflicts.applyTextMerge();

        conflicts.chooseResolution(WORKBOOK, "Merge table by table");
        assertThat(conflicts.getWorkbookTableActions(TABLE))
                .as("The choices for the whole table stand above it, not inside a drop-down")
                .containsExactlyInAnyOrder("Take Yours", "Take Base", "Take Theirs");
        assertThat(conflicts.isWorkbookTableStatusShown(TABLE, "1 conflict left"))
                .as("Only Step1, changed differently by both branches, is left to decide").isTrue();
        conflicts.takeTheirsForCell(TABLE, STEP1_CELL);
        assertThat(conflicts.isWorkbookTableStatusShown(TABLE, "All Conflicts Resolved"))
                .as("Taking their Step1 resolves the table").isTrue();
        assertThat(conflicts.getWorkbookMergedTable(TABLE))
                .as("The merged table holds their Step1, the Step2 of MyBranch and the Step3 of master")
                .contains(THEIR_STEP1, THEIR_STEP2, YOUR_STEP3).doesNotContain(YOUR_STEP1);
        conflicts.applyWorkbookMerge();
        conflicts.clickSave();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();
        assertThat(repositoryPage.getMergeSummaryResult())
                .as("The Merge Summary reports the conflicts resolved and MyBranch merged into master")
                .isEqualTo("Conflicts resolved and \"" + BRANCH + "\" merged into \"" + MASTER + "\".");
        repositoryPage.closeMergeSummary();

        editorPage = new EditorPage();
        editorPage.reloadPage();
        openMySpr1(editorPage);
        TableComponent table = editorPage.getCenterTable();
        assertThat(List.of(table.getRow(3).getValue(), table.getRow(STEP2_ROW).getValue(), table.getRow(STEP3_ROW).getValue()))
                .as("master keeps the resolved table after Save and Resolve")
                .containsExactly(List.of(THEIR_STEP1, "MySpr1"), List.of("Step2", THEIR_STEP2), List.of("Step3", YOUR_STEP3));
        EditProjectDialogComponent project = editorPage.openEditProjectDialog(PROJECT);
        assertThat(project.getDescription())
                .as("rules.xml of master keeps the description taken from MyBranch line by line")
                .isEqualTo(THEIR_DESCRIPTION);
        project.clickCancelButton();
    }

    private void editMySpr1(EditorPage editorPage, String step1, int valueRow, String value) {
        openMySpr1(editorPage);
        editorPage.getCenterTable().editCell(3, 1, step1);
        editorPage.getCenterTable().editCell(valueRow, 2, value);
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
    }

    private void openMySpr1(EditorPage editorPage) {
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT, "Module1");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(SPREADSHEET)
                .selectItemInFolder(SPREADSHEET, "MySpr1");
    }
}
