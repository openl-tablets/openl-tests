package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.DedicatedWorkflow;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerPool;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.GitContainerService;
import helpers.service.GitHubLfsService;
import helpers.service.GitRemote;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.LfsPointer;
import helpers.utils.WaitUtil;
import helpers.utils.ZipUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static org.assertj.core.api.Assertions.assertThat;

@DedicatedWorkflow(workflow = TestGitLfsGitHubStorage.WORKFLOW,
        reason = "Stores LFS objects in the GitHub LFS of this repository with the workflow token")
public final class TestGitLfsGitHubStorage extends BaseTest {

    private static final String GIT_CONTAINER_ALIAS = "git-container-lfs-github";
    private static final String FIXTURE_RESOURCE = "/git_daemon_repo";
    private static final String PROJECT_NAME = "Empty Project";
    private static final String MODULE_NAME = "Main";
    private static final String MODULE_PATH = PROJECT_NAME + "/Main.xlsx";
    private static final String TABLE_NAME = "Hello";
    private static final int FIRST_RULE_ROW = 6;
    private static final int GREETING_COLUMN = 4;
    private static final String SEEDED_GREETING = "Good Morning";
    static final String WORKFLOW = "openl-lfs.yml";
    private static final Pattern LFS_TRANSFER_FAILURE = Pattern.compile(
            "When trying to open a connection to (https?://[^?\\s\"]+)\\S* the server responded with an error code\\. rc=(\\d+)");

    private GitHubLfsService gitHubLfs;
    private GitContainerService gitContainer;
    private GitRemote gitRemote;
    private LfsPointer seeded;

    @Override
    protected void startAuxiliaryContainers() {
        String workflowRef = String.valueOf(System.getenv("GITHUB_WORKFLOW_REF"));
        if ("true".equals(System.getenv("GITHUB_ACTIONS")) && !workflowRef.contains("/" + WORKFLOW + "@")) {
            throw new IllegalStateException(getClass().getSimpleName() + " runs on GitHub Actions only in " + WORKFLOW);
        }
        gitHubLfs = new GitHubLfsService();
        seeded = gitHubLfs.upload(fixtureModule());
        gitContainer = new GitContainerService(GIT_CONTAINER_ALIAS)
                .withLfsTracking("*.xlsx")
                .withExternalLfs(gitHubLfs.lfsUrl())
                .withOwnerPassword(gitHubLfs.token())
                .withFile(MODULE_PATH, seeded.encode());
        gitContainer.start();
        gitRemote = gitContainer.asRemote();
    }

    @Override
    protected void stopAuxiliaryContainers() {
        if (gitContainer != null) {
            gitContainer.stop();
            gitContainer = null;
        }
        gitRemote = null;
        gitHubLfs = null;
    }

    @Override
    protected Map<String, String> additionalContainerConfig() {
        return Map.of(
                "repository.design.uri", gitContainer.getInNetworkUrl(),
                "repository.design.login", gitRemote.login(),
                "repository.design.password", gitRemote.password()
        );
    }

    @Test
    @TestCaseId("EPBDS-11591")
    @Description("Git LFS on GitHub - Studio opens a module whose LFS object is on GitHub and saves an edit of it back to GitHub LFS")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_GIT)
    public void testGitLfsGitHubStorage() {
        byte[] seededModule = gitContainer.readCommittedFile(MODULE_PATH);
        assertThat(LfsPointer.parse(seededModule))
                .as("Precondition: git should hold the pointer to the fixture module on GitHub LFS, but holds %s",
                        LfsPointer.describe(seededModule))
                .contains(seeded);
        String editedGreeting = SEEDED_GREETING + " " + UUID.randomUUID().toString().substring(0, 8);

        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        WaitUtil.requireCondition(() -> repositoryPage.getAllVisibleProjectsInTable().contains(PROJECT_NAME),
                30000, 1000, "Waiting for the project of the design repository to be listed");
        repositoryPage.openProject(PROJECT_NAME);

        EditorPage editor = new EditorPage();
        editor.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT_NAME, MODULE_NAME);
        editor.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, TABLE_NAME);

        assertThat(editor.getCenterTable().getCellText(FIRST_RULE_ROW, GREETING_COLUMN))
                .as("Studio should show the module content downloaded from GitHub LFS, not the pointer")
                .isEqualTo(SEEDED_GREETING);
        assertThat(editor.getProblemsPanelComponent().getAllErrors())
                .as("The module read from GitHub LFS should compile")
                .isEmpty();

        editor.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editor.getCenterTable().editCell(FIRST_RULE_ROW, GREETING_COLUMN, editedGreeting);
        editor.getEditorTableActionsPanelComponent().clickSaveChanges();
        editor.getEditorToolbarPanelComponent().clickSave();
        editor.getSaveChangesComponent().getSaveProjectDialog().waitForVisible().clickSubmit();

        WaitUtil.waitForCondition(() -> !Arrays.equals(gitContainer.readCommittedFile(MODULE_PATH), seededModule)
                        || !failuresShown(editor).isEmpty(),
                60000, 1000, "Waiting for the saved module to reach git or for Studio to report the save failed");
        assertThat(failuresShown(editor))
                .as("Studio should save the project with its module uploaded to GitHub LFS; Studio log says:%n%s",
                        lfsFailuresInStudioLog())
                .isEmpty();

        byte[] savedModule = gitContainer.readCommittedFile(MODULE_PATH);
        LfsPointer saved = LfsPointer.parse(savedModule)
                .orElseThrow(() -> new AssertionError("Studio should commit the saved module as an LFS pointer, but git holds "
                        + LfsPointer.describe(savedModule) + "; Studio log says: " + lfsFailuresInStudioLog()));
        assertThat(saved)
                .as("Saving the project should commit a pointer to a new LFS object; Studio log says: %s",
                        lfsFailuresInStudioLog())
                .isNotEqualTo(seeded);

        byte[] savedContent = gitHubLfs.download(saved);
        assertThat(LfsPointer.of(savedContent))
                .as("GitHub LFS should hold the object the committed pointer refers to")
                .isEqualTo(saved);
        assertThat(ZipUtil.readFileFromZip(savedContent, "xl/sharedStrings.xml"))
                .as("The module stored in GitHub LFS should contain the edited greeting")
                .contains(">" + editedGreeting + "<");
    }

    private static List<String> failuresShown(EditorPage editor) {
        return editor.getSaveChangesComponent().getAllMessagesFullText().stream()
                .filter(message -> message.contains("Failed"))
                .toList();
    }

    private static String lfsFailuresInStudioLog() {
        Matcher failure = LFS_TRANSFER_FAILURE.matcher(AppContainerPool.get().getAppContainer().getLogs());
        return failure.find()
                ? "LFS transfer to " + failure.group(1) + " failed with rc=" + failure.group(2)
                : "no LFS transfer failure";
    }

    private static byte[] fixtureModule() {
        try {
            Path fixture = Path.of(TestGitLfsGitHubStorage.class.getResource(FIXTURE_RESOURCE).toURI());
            return Files.readAllBytes(fixture.resolve(MODULE_PATH));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the fixture module " + MODULE_PATH, e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot resolve the fixture " + FIXTURE_RESOURCE, e);
        }
    }
}
