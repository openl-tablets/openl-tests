package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.RepositoriesPageComponent;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.CopyProjectDialogComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ProjectDeleteConfirmModalComponent;
import domain.ui.webstudio.components.repositorytabcomponents.SaveProjectDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.service.WorkflowService;
import helpers.utils.StringUtil;
import helpers.utils.TestDataUtil;
import helpers.utils.WaitUtil;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.function.Supplier;

public class TestDesignRepositoryCommentsUi extends BaseTest {

    private static final String DESIGN = "Design";
    private static final String TEMPLATE = "Example 1 - Bank Rating";
    private static final String UPLOAD_FILE = "TestFileAddDelete.rules.xls";
    private static final String PATTERN = "commentValidationPattern";
    private static final String HINT = "invalidCommentMessage";
    private static final String CUSTOMIZE_COMMENTS = "useCustomComments";
    private static final String DEFAULT_HINT = "Invalid comment: Comment doesn't match validation pattern";
    private static final String TICKET_PATTERN = "EPBDS-\\d+ .+";
    private static final String TICKET_HINT = "Start the comment with the ticket number";
    private static final String MATCHING_COMMENT = "EPBDS-17254 upload a rules file";
    private static final String NON_MATCHING_COMMENT = "just a comment";
    private static final int SETTLE_TIMEOUT_MS = 10000;

    @Test
    @TestCaseId({"IPBQA-33076", "EPBDS-17254"})
    @Description("The Comments section of a design repository keeps only User message pattern and Invalid user message "
            + "hint: Customize comments, which showed the User message templates, is gone, the pattern is empty and "
            + "the hint has its default text")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCommentsSectionKeepsOnlyPatternAndHint() {
        RepositoriesPageComponent repositories = login().openUserMenu().navigateToAdministration()
                .navigateToRepositoriesPage().selectDesignRepositoryByName(DESIGN);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(repositories.getSettingValue(PATTERN))
                .as("User message pattern is shown and empty by default").isEmpty();
        softly.assertThat(repositories.getSettingValue(HINT))
                .as("Invalid user message hint is shown with its default text").isEqualTo(DEFAULT_HINT);
        softly.assertThat(repositories.isSettingShown(CUSTOMIZE_COMMENTS))
                .as("Customize comments is no longer a setting of the Comments section").isFalse();
        softly.assertAll();
    }

    @Test
    @TestCaseId({"IPBQA-33076", "EPBDS-17254"})
    @Description("The Create, Save and Copy dialogs suggest the standard comments that name the project: "
            + "Project <name> is created., Project <name> is saved. and Copied from: <name>.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDialogsSuggestStandardComments() {
        String project = StringUtil.generateUniqueName("CommentsProject");
        RepositoryPage repositoryPage = login().getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        SoftAssertions softly = new SoftAssertions();
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, project, TEMPLATE, false);
        assertSuggested(softly, repositoryPage.getCreateNewProjectComponent()::getComment,
                "Project " + project + " is created.", "The Create dialog suggests the comment of a new project");
        repositoryPage.completeTemplateProjectCreation(project);

        SaveProjectDialogComponent saveDialog = openSaveDialogOnLocalChange(repositoryPage, project);
        assertSuggested(softly, saveDialog::getComment,
                "Project " + project + " is saved.", "The Save dialog suggests the comment of a saved project");
        saveDialog.submit();
        repositoryPage.waitUntilSpinnerLoaded();

        CopyProjectDialogComponent copyDialog = repositoryPage.openProjectsList().clickCopyAction(project).setAsNewProject();
        assertSuggested(softly, copyDialog::getComment,
                "Copied from: " + project + ".", "The Copy dialog suggests the comment that names the copied project");
        copyDialog.clickCancelButton();
        softly.assertAll();
    }

    @Test
    @TestCaseId({"IPBQA-33076", "EPBDS-17254", "EPBDS-17301"})
    @Description("A User message pattern set in Administration applies without Customize comments: the Save dialog "
            + "shows the Invalid user message hint and keeps Save unavailable for the suggested and any other "
            + "non-matching comment, and saves a matching one. EPBDS-17301: the Delete dialog shows the hint for the "
            + "comment as it is now, not as it was one keystroke before")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testPatternChecksCommentsWithoutCustomizeComments() {
        String project = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE);
        EditorPage editorPage = new EditorPage();
        editorPage.openUserMenu().navigateToAdministration().navigateToRepositoriesPage()
                .selectDesignRepositoryByName(DESIGN)
                .setSettingValue(PATTERN, TICKET_PATTERN)
                .setSettingValue(HINT, TICKET_HINT)
                .applyChangesAndRelogin(User.ADMIN);
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        SaveProjectDialogComponent saveDialog = openSaveDialogOnLocalChange(repositoryPage, project);
        SoftAssertions softly = new SoftAssertions();
        assertSuggested(softly, saveDialog::getComment,
                "Project " + project + " is saved.", "The Save dialog still suggests the standard comment");
        assertCommentChecked(softly, "Save", saveDialog::getCommentError, saveDialog::isSubmitEnabled,
                TICKET_HINT, false, "the suggested comment");
        saveDialog.setComment(NON_MATCHING_COMMENT);
        assertCommentChecked(softly, "Save", saveDialog::getCommentError, saveDialog::isSubmitEnabled,
                TICKET_HINT, false, "a typed non-matching comment");
        saveDialog.setComment(MATCHING_COMMENT);
        assertCommentChecked(softly, "Save", saveDialog::getCommentError, saveDialog::isSubmitEnabled,
                "", true, "a matching comment");
        softly.assertAll();

        saveDialog.submit();
        repositoryPage.waitUntilSpinnerLoaded();
        SoftAssertions deletion = new SoftAssertions();
        deletion.assertThat(repositoryPage.openProjectsList().openProjectDetail(project).getRevisionDescriptions())
                .as("The saved revision keeps the matching comment")
                .first().isEqualTo(MATCHING_COMMENT);

        ProjectDeleteConfirmModalComponent deleteDialog = repositoryPage.openProjectsList().deleteProject(project)
                .acknowledgePermanentDeletion();
        deleteDialog.enterDeletionComment(NON_MATCHING_COMMENT);
        assertCommentChecked(deletion, "Delete", deleteDialog::getCommentError, deleteDialog::isDeleteEnabled,
                TICKET_HINT, false, "a non-matching comment");
        deleteDialog.enterDeletionComment(MATCHING_COMMENT);
        assertCommentChecked(deletion, "Delete", deleteDialog::getCommentError, deleteDialog::isDeleteEnabled,
                "", true, "a matching comment");
        deleteDialog.enterDeletionComment(NON_MATCHING_COMMENT);
        assertCommentChecked(deletion, "Delete", deleteDialog::getCommentError, deleteDialog::isDeleteEnabled,
                TICKET_HINT, false, "a comment made non-matching again");
        deleteDialog.clickCancel();
        deletion.assertAll();
    }

    private EditorPage login() {
        return new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
    }

    private SaveProjectDialogComponent openSaveDialogOnLocalChange(RepositoryPage repositoryPage, String project) {
        repositoryPage.openProjectsList().openProjectDetail(project)
                .uploadFile(TestDataUtil.getFilePathFromResources(UPLOAD_FILE));
        return repositoryPage.openProjectsList().openSaveDialog(project);
    }

    private static void assertSuggested(SoftAssertions softly, Supplier<String> comment, String expected, String because) {
        WaitUtil.waitForCondition(() -> expected.equals(comment.get()), SETTLE_TIMEOUT_MS, 250, "Waiting until " + because);
        softly.assertThat(comment.get()).as(because).isEqualTo(expected);
    }

    private static void assertCommentChecked(SoftAssertions softly, String action, Supplier<String> hint,
                                             Supplier<Boolean> actionEnabled, String expectedHint,
                                             boolean actionAvailable, String comment) {
        WaitUtil.waitForCondition(() -> expectedHint.equals(hint.get()) && actionEnabled.get() == actionAvailable,
                SETTLE_TIMEOUT_MS, 250, "Waiting for the " + action + " dialog to check " + comment);
        softly.assertThat(hint.get())
                .as("%s dialog: Invalid user message hint shown for %s", action, comment).isEqualTo(expectedHint);
        softly.assertThat(actionEnabled.get())
                .as("%s dialog: %s is available for %s", action, action, comment).isEqualTo(actionAvailable);
    }
}
