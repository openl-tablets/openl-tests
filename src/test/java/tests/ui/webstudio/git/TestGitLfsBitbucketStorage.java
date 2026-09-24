package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.DedicatedWorkflow;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import helpers.service.LfsProvider;
import org.testng.annotations.Test;

@DedicatedWorkflow(workflow = GitLfsExternalStorageTest.WORKFLOW,
        reason = "Stores LFS objects in the Bitbucket repository of LFS_BITBUCKET_REPO with the LFS_BITBUCKET_TOKEN secret")
public final class TestGitLfsBitbucketStorage extends GitLfsExternalStorageTest {

    @Override
    protected LfsProvider provider() {
        return LfsProvider.BITBUCKET;
    }

    @Test
    @TestCaseId("IPBQA-33055")
    @Description("Git LFS on Bitbucket - Studio opens a module whose LFS object is on Bitbucket and saves an edit of it back to Bitbucket LFS")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_GIT)
    public void testGitLfsBitbucketStorage() {
        verifyStudioKeepsModuleInExternalLfs();
    }
}
