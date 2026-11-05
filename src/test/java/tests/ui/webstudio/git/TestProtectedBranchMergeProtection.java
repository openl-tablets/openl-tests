package tests.ui.webstudio.git;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.api.AuthenticationSettingsMethod;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.RepositoriesPageComponent;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.SyncChangesDialogComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.AdminPage;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.utils.TestDataUtil;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProtectedBranchMergeProtection extends BaseTest {

    private static final String PROJECT_NAME = "ProtectedBranchProject";
    private static final String BRANCH_NAME = "MyBranch";
    private static final String MASTER_BRANCH = "master";
    private static final String NEW_PROJECT_NAME = "ProjectInProtectedMaster";

    @Test
    @TestCaseId({"EPBDS-15753", "EPBDS-16558"})
    @Description("Git - Cannot merge into protected branch: UI shows server 409 error in merge dialog; "
            + "EPBDS-16558: creating a project in the protected branch is refused in the Create project dialog "
            + "with the protected-branch message, not a full-page 403")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCannotMergeIntoProtectedBranch() {
        new AuthenticationSettingsMethod().setAllowBypassProtectedBranches(false);

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT_NAME, "TestMergeBranchesNoConflicts_NoConflicts.zip");

        repositoryPage.openProjectDetail(PROJECT_NAME).createBranch(BRANCH_NAME, true);

        repositoryPage.openProjectsList().openProjectDetail(PROJECT_NAME).openFilesTab()
                .uploadFile(TestDataUtil.getFilePathFromResources("TestMergeBranchesNoConflicts_Module6.xlsx"));
        repositoryPage.openProjectsList().saveProject(PROJECT_NAME, BRANCH_NAME + ": add Module6");

        AdminPage adminPage = editorPage.openUserMenu().navigateToAdministration();
        RepositoriesPageComponent repositories = adminPage.navigateToRepositoriesPage();
        repositories.clickDesignRepositoriesTab()
                .setProtectedBranches(MASTER_BRANCH)
                .applyChangesAndRelogin(User.ADMIN);

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        EditorPage editor = new EditorPage();
        editor.getEditorLeftProjectModuleSelectorComponent().selectProject(PROJECT_NAME);
        assertThat(editor.getEditorToolbarPanelComponent().isSyncButtonVisible())
                .as("Sync button should be visible after branching")
                .isTrue();

        editor.getEditorToolbarPanelComponent().clickSync();
        SyncChangesDialogComponent syncDialog = repositoryPage.getSyncChangesDialogComponent();
        syncDialog.waitForDialogToAppear();
        syncDialog.selectBranch(MASTER_BRANCH);

        assertThat(syncDialog.hasBlockedMessageContaining("protected"))
                .as("The dialog should say the target branch '%s' is protected", MASTER_BRANCH)
                .isTrue();
        assertThat(syncDialog.getBlockedMessages())
                .as("Wording of the protected-branch note")
                .anyMatch(msg -> msg.contains("is protected and you may not merge into it"));
        assertThat(syncDialog.isExportButtonEnabled())
                .as("Send (export) button must be disabled for protected target branch")
                .isFalse();

        syncDialog.clickCancel();

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.getCreateProjectLink().click();
        CreateNewProjectComponent createDialog = repositoryPage.getCreateNewProjectComponent();
        createDialog.createProjectFromTemplate("Sample Project", NEW_PROJECT_NAME, false);
        assertThat(createDialog.submitExpectingRefusal())
                .as("EPBDS-16558: the Create project dialog stays open and names the protected branch '%s'", MASTER_BRANCH)
                .isEqualTo("The branch '" + MASTER_BRANCH + "' is protected, and you cannot bypass its protection.");
        createDialog.cancelCreation();
    }
}
