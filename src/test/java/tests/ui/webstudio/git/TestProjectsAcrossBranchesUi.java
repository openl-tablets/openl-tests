package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.GitContainerService;
import helpers.service.GitRemote;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectsAcrossBranchesUi extends BaseTest {

    private static final String GIT_CONTAINER_ALIAS = "git-projects-across-branches";
    private static final String REPO_NAME = "design";
    private static final String MASTER = "master";
    private static final String RELEASE = "release-1";
    private static final String TICKET = "EPBDS-1-fix";
    private static final String FIXTURE = "TestProjectsAcrossBranches";

    private static final String QUOTING = "Quoting";
    private static final String TVM = "TVM";
    private static final String LEGACY = "Legacy";
    private static final String TVM_FOLDER = "ref-impl/tvm-alg";

    private GitContainerService gitContainer;
    private GitRemote gitRemote;

    @Override
    protected void startAuxiliaryContainers() {
        gitContainer = new GitContainerService(GIT_CONTAINER_ALIAS, REPO_NAME, MASTER, fixture("master"));
        gitContainer.start();
        gitRemote = gitContainer.asRemote();
        gitContainer.addBranch(RELEASE, MASTER, fixture("release"));
        WaitUtil.sleep(2000, "Making the ticket branch tip newer than the release branch tip");
        gitContainer.addBranch(TICKET, RELEASE, fixture("ticket"));
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
    @TestCaseId({"EPBDS-16380", "EPBDS-16383", "EPBDS-16374"})
    @Description("Git design repository with folders and protected release-*: EPBDS-16383 two folders declaring the "
            + "same name stay two projects under that name, without a Name.2 suffix; EPBDS-16380 a project living "
            + "only outside the base branch is represented by the protected release branch, not by the newer ticket "
            + "branch, so the Branch filter lists only master and release-1; EPBDS-16374 a folder that collides "
            + "by name in the other branches is still one project, reports all its branches, opens, and has history "
            + "in a side branch")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_PROTECTED_NO_BYPASS_PARAMS)
    public void testProjectsOfAMappedRepositoryAcrossBranches() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        WaitUtil.requireCondition(() -> repositoryPage.getAllVisibleProjectsInTable().contains(TVM),
                15000, 500, "Waiting for the projects of the design repository to be listed");
        assertThat(repositoryPage.getAllVisibleProjectsInTable())
                .as("Two folders declaring the name Quoting stay two projects named Quoting, none is renamed to "
                        + "Quoting.2 (EPBDS-16383), and the TVM folder is one project although other branches "
                        + "name two folders alike (EPBDS-16374)")
                .containsExactlyInAnyOrder(QUOTING, QUOTING, TVM);

        assertThat(repositoryPage.getBranchFilterOptions())
                .as("The Branch filter offers the base branch and the protected release branch, not the newer "
                        + "ticket branch that also holds the projects living outside the base branch (EPBDS-16380)")
                .containsExactlyInAnyOrder(MASTER, RELEASE);

        repositoryPage.setBranchFilter(RELEASE, true);
        WaitUtil.requireCondition(() -> repositoryPage.getAllVisibleProjectsInTable().contains(LEGACY),
                15000, 500, "Waiting for the projects of the release-1 branch filter to be listed");
        assertThat(repositoryPage.getAllVisibleProjectsInTable())
                .as("The projects that live only outside the base branch are listed under the protected branch, "
                        + "and the folder that a branch renames keeps its name TVM (EPBDS-16380, EPBDS-16383)")
                .containsExactlyInAnyOrder(LEGACY, TVM);
        assertThat(repositoryPage.openProjectDetail(LEGACY).getCurrentBranch())
                .as("The project absent from the base branch is represented by the protected release branch, "
                        + "not by the ticket branch with the newest tip (EPBDS-16380)")
                .isEqualTo(RELEASE);
        repositoryPage.openProjectsList().setBranchFilter(RELEASE, false);
        WaitUtil.requireCondition(() -> repositoryPage.getAllVisibleProjectsInTable().contains(QUOTING),
                15000, 500, "Waiting for the unfiltered projects list to be restored");

        ProjectDetailPage tvm = repositoryPage.openProjectDetail(TVM);
        assertThat(tvm.getOverviewPath())
                .as("The TVM row of the base branch is the folder %s", TVM_FOLDER)
                .isEqualTo(TVM_FOLDER);
        for (String branch : List.of(MASTER, RELEASE, TICKET)) {
            assertThat(tvm.isBranchPresent(branch))
                    .as("The folder reports the branch %s that holds it, although the other branches name two folders "
                            + "alike (EPBDS-16374)", branch)
                    .isTrue();
        }

        tvm.clickHeaderAction("Open");
        WaitUtil.requireCondition(() -> "No Changes".equals(tvm.getStatus()),
                30000, 1000, "Waiting for the project of the TVM folder to be opened");
        assertThat(tvm.getStatus())
                .as("The project of the folder opens and its workspace copy stays (EPBDS-16374)")
                .isEqualTo("No Changes");

        tvm.switchBranch(TICKET);
        assertThat(tvm.getCurrentBranch())
                .as("The project is switched to the ticket branch")
                .isEqualTo(TICKET);
        assertThat(tvm.getRevisionDescriptions())
                .as("The history of the folder is read in a side branch where another folder claims the same name (EPBDS-16374)")
                .contains("Initial commit");
    }

    private static Path fixture(String branchFolder) {
        return Path.of(TestDataUtil.getDirectoryPathFromResources(FIXTURE)).resolve(branchFolder);
    }
}
