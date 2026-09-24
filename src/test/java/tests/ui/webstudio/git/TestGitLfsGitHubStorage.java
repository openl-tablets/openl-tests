package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.DedicatedWorkflow;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import helpers.service.LfsProvider;
import org.testng.annotations.Test;

@DedicatedWorkflow(workflow = GitLfsExternalStorageTest.WORKFLOW,
        reason = "Stores LFS objects in the GitHub LFS of this repository with the workflow token")
public final class TestGitLfsGitHubStorage extends GitLfsExternalStorageTest {

    @Override
    protected LfsProvider provider() {
        return LfsProvider.GITHUB;
    }

    @Test
    @TestCaseId("IPBQA-33053")
    @Description("Git LFS on GitHub - Studio opens a module whose LFS object is on GitHub and saves an edit of it back to GitHub LFS")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_GIT)
    public void testGitLfsGitHubStorage() {
        verifyStudioKeepsModuleInExternalLfs();
    }
}
