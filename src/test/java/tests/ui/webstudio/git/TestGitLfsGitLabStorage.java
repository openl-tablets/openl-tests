package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.DedicatedWorkflow;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import helpers.service.LfsProvider;
import org.testng.annotations.Test;

@DedicatedWorkflow(workflow = GitLfsExternalStorageTest.WORKFLOW,
        reason = "Stores LFS objects in the GitLab repository of LFS_GITLAB_REPO with the LFS_GITLAB_TOKEN secret")
public final class TestGitLfsGitLabStorage extends GitLfsExternalStorageTest {

    @Override
    protected LfsProvider provider() {
        return LfsProvider.GITLAB;
    }

    @Test
    @TestCaseId("IPBQA-33054")
    @Description("Git LFS on GitLab - Studio opens a module whose LFS object is on GitLab and saves an edit of it back to GitLab LFS")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_GIT)
    public void testGitLfsGitLabStorage() {
        verifyStudioKeepsModuleInExternalLfs();
    }
}
