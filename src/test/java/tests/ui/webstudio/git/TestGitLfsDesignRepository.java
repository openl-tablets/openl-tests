package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.DedicatedWorkflow;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.GitContainerService;
import helpers.service.GitRemote;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.LfsPointer;
import helpers.utils.WaitUtil;
import helpers.utils.ZipUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.Arrays;
import java.util.Map;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static org.assertj.core.api.Assertions.assertThat;

@DedicatedWorkflow(workflow = GitLfsExternalStorageTest.WORKFLOW,
        reason = "Git LFS tests run together in the LFS workflow")
public class TestGitLfsDesignRepository extends BaseTest {

    private static final String GIT_CONTAINER_ALIAS = "git-container-lfs";
    private static final String PROJECT_NAME = "Empty Project";
    private static final String MODULE_NAME = "Main";
    private static final String MODULE_PATH = PROJECT_NAME + "/Main.xlsx";
    private static final String TABLE_NAME = "Hello";
    private static final int FIRST_RULE_ROW = 6;
    private static final int GREETING_COLUMN = 4;
    private static final String SEEDED_GREETING = "Good Morning";
    private static final String EDITED_GREETING = "Good Morning from LFS";

    private GitContainerService gitContainer;
    private GitRemote gitRemote;

    @Override
    protected void startAuxiliaryContainers() {
        gitContainer = new GitContainerService(GIT_CONTAINER_ALIAS).withLfsTracking("*.xlsx");
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
    @Description("Git LFS - Studio opens a module stored in LFS and saves an edit of it back to LFS")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_GIT)
    public void testGitLfsDesignRepository() {
        byte[] seededModule = gitContainer.readCommittedFile(MODULE_PATH);
        LfsPointer seeded = lfsPointer(seededModule, "The fixture module should be seeded into git as an LFS pointer");

        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        WaitUtil.requireCondition(() -> repositoryPage.getAllVisibleProjectsInTable().contains(PROJECT_NAME),
                30000, 1000, "Waiting for the project of the design repository to be listed");
        repositoryPage.openProject(PROJECT_NAME);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT_NAME, MODULE_NAME);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, TABLE_NAME);

        assertThat(editorPage.getCenterTable().getCellText(FIRST_RULE_ROW, GREETING_COLUMN))
                .as("Studio should show the module content downloaded from LFS, not the pointer")
                .isEqualTo(SEEDED_GREETING);
        assertThat(editorPage.getProblemsPanelComponent().getAllErrors())
                .as("The module read from LFS should compile")
                .isEmpty();

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(FIRST_RULE_ROW, GREETING_COLUMN, EDITED_GREETING);
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        WaitUtil.waitForCondition(() -> !Arrays.equals(gitContainer.readCommittedFile(MODULE_PATH), seededModule),
                30000, 1000, "Waiting for the saved module to reach the git remote");
        LfsPointer saved = lfsPointer(gitContainer.readCommittedFile(MODULE_PATH),
                "Studio should commit the saved module as an LFS pointer");
        assertThat(saved.oid())
                .as("Saving the project should commit a pointer to a new LFS object")
                .isNotEqualTo(seeded.oid());

        byte[] savedModule = gitContainer.readLfsContent(MODULE_PATH);
        assertThat(LfsPointer.of(savedModule))
                .as("The LFS server should hold the object the committed pointer refers to")
                .isEqualTo(saved);
        assertThat(ZipUtil.readFileFromZip(savedModule, "xl/sharedStrings.xml"))
                .as("The module stored in LFS should contain the edited greeting")
                .contains(">" + EDITED_GREETING + "<");
    }

    private static LfsPointer lfsPointer(byte[] committed, String expectation) {
        return LfsPointer.parse(committed).orElseThrow(() -> new AssertionError(
                expectation + ", but git holds " + LfsPointer.describe(committed)));
    }
}
