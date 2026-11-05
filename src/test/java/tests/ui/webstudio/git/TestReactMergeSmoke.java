package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.SyncUpdatesDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestReactMergeSmoke extends BaseTest {

    private static final String BRANCH = "MyBranch";
    private static final String MASTER = "master";
    private static final String ADDED_MODULE = "TestNavigationToTable.xlsx";
    private static final String SENT_MODULE = "TestMergeBranchesNoConflicts_Module5.xlsx";

    @Test
    @TestCaseId("EPBDS-16420")
    @Description("Receive merges MyBranch into master. EPBDS-16420: a project that is closed is merged by Send your "
            + "updates too, instead of the merge being refused with 'The project is not opened': master gets a new "
            + "module, the project is closed, and its updates are sent to MyBranch.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void receiveMergesBranchIntoMaster() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(projectName);
        projectDetail.createBranch(BRANCH, true);
        projectDetail.uploadFile(TestDataUtil.getFilePathFromResources(ADDED_MODULE));
        repositoryPage.openProjectsList().saveProject(projectName, "MyBranch: add module");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        editorPage.getEditorToolbarPanelComponent().switchBranch(MASTER);

        repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        projectDetail = repositoryPage.openProjectDetail(projectName);
        assertThat(projectDetail.getCurrentBranch()).as("on master before merge").isEqualTo(MASTER);
        SyncUpdatesDialogComponent syncDialog = projectDetail.openMergeDialog(BRANCH);
        assertThat(syncDialog.isReceiveEnabled())
                .as("Receive their updates should be enabled when MyBranch is ahead of master").isTrue();
        syncDialog.clickReceive();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();
        projectDetail = repositoryPage.openProjectsList().openProjectDetail(projectName);
        projectDetail.openFilesTab();
        assertThat(projectDetail.isFilePresent(ADDED_MODULE))
                .as("the module merged from MyBranch should now be present on master").isTrue();

        projectDetail.uploadFile(TestDataUtil.getFilePathFromResources(SENT_MODULE));
        repositoryPage.openProjectsList().saveProject(projectName, "master: add module to send");
        repositoryPage.closeProject(projectName);

        projectDetail = repositoryPage.openProjectDetail(projectName);
        assertThat(projectDetail.getStatus()).as("the project is closed before its updates are sent")
                .containsIgnoringCase("closed");
        SyncUpdatesDialogComponent sendDialog = projectDetail.openMergeDialog(BRANCH);
        assertThat(sendDialog.isSendEnabled())
                .as("Send your updates should be enabled when master is ahead of MyBranch").isTrue();
        sendDialog.clickSend();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();
        assertThat(repositoryPage.getMergeSummaryResult())
                .as("a closed project is merged, not refused with 'The project is not opened'")
                .contains("Merged \"" + MASTER + "\" into \"" + BRANCH + "\"");
        repositoryPage.closeMergeSummary();

        projectDetail = repositoryPage.openProjectsList().openProjectDetail(projectName);
        projectDetail.switchBranch(BRANCH);
        projectDetail.openFilesTab();
        assertThat(projectDetail.isFilePresent(SENT_MODULE))
                .as("the module sent from master should now be present on MyBranch").isTrue();
    }
}
