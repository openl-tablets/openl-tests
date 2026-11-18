package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.SyncChangesDialogComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSyncRemovesOpenModuleUi extends BaseTest {

    private static final String PROJECT_NAME = "SyncRemovesModule";
    private static final String BRANCH_NAME = "MyBranch";
    private static final String MASTER_BRANCH = "master";
    private static final String REMOVED_MODULE = "Module4";
    private static final String ARCHIVE = "TestMergeBranchesNoConflicts_SyncWorkbook.zip";
    private static final int NOTICE_WAIT_MS = 5000;
    private static final int URL_CHANGED_MS = 5000;

    @Test
    @TestCaseId("EPBDS-12358")
    @Description("EPBDS-12358: when the updates received in the editor through Sync remove the module that is open, "
            + "the reader lands on the page of the project with its own header and a message that the module is no "
            + "longer in the project, not on a dead screen of the deleted module.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testSyncRemovingOpenModuleOpensProjectPage() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT_NAME, ARCHIVE);

        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(PROJECT_NAME);
        projectDetail.createBranch(BRANCH_NAME, true);
        projectDetail.openFilesTab();
        projectDetail.deleteFile(REMOVED_MODULE + ".xlsx");
        repositoryPage.openProjectsList().saveProject(PROJECT_NAME, BRANCH_NAME + ": " + REMOVED_MODULE + " removed");
        repositoryPage.openProjectsList().openProjectDetail(PROJECT_NAME).switchBranch(MASTER_BRANCH);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT_NAME, REMOVED_MODULE);
        assertThat(DriverPool.getPage().url())
                .as("Precondition: the module that the other branch removed is open in the editor on master")
                .contains("/modules/" + REMOVED_MODULE);

        editorPage.getEditorToolbarPanelComponent().clickSync();
        SyncChangesDialogComponent syncDialog = repositoryPage.getSyncChangesDialogComponent();
        syncDialog.waitForDialogToAppear();
        syncDialog.selectBranch(BRANCH_NAME);
        syncDialog.clickImportTheirChanges();

        boolean noticeShown = WaitUtil.waitForCondition(() -> syncDialog.getAllMessagesFullText().stream()
                        .anyMatch(text -> text.contains("Module \"" + REMOVED_MODULE + "\" is no longer in the project.")),
                NOTICE_WAIT_MS, 100, "Waiting for the message that the open module is gone");
        assertThat(noticeShown)
                .as("The reader is told that the open module is no longer in the project (EPBDS-12358)")
                .isTrue();
        WaitUtil.waitForCondition(() -> DriverPool.getPage().url().matches(".*/projects/[^/?#]+/?"),
                URL_CHANGED_MS, 100, "Waiting for the project page address");
        assertThat(DriverPool.getPage().url())
                .as("The reader lands on the page of the project, not on the address of the removed module (EPBDS-12358)")
                .matches(".*/projects/[^/?#]+/?");

        repositoryPage.closeMergeSummary();
        ProjectDetailPage projectPage = new ProjectDetailPage();
        assertThat(projectPage.isHeaderActionAvailable("Sync"))
                .as("The project page shows its own header with the Sync action (EPBDS-12358)")
                .isTrue();
        assertThat(projectPage.isHeaderActionAvailable("Copy"))
                .as("The project page shows its own header with the Copy action (EPBDS-12358)")
                .isTrue();
        assertThat(projectPage.isFilePresent(REMOVED_MODULE + ".xlsx"))
                .as("The removed module is gone from the project's files")
                .isFalse();
    }
}
