package tests.ui.webstudio.repository;

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
import helpers.service.WorkflowService;
import helpers.utils.DownloadUtil;
import helpers.utils.ProjectLinkUtil;
import helpers.utils.TestDataUtil;
import helpers.utils.ZipUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectDownloadLinkUi extends BaseTest {

    private static final String LOCAL_CHANGE_FILE = "TestFileAddDelete.rules.xls";
    private static final String UNKNOWN_REVISION = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";

    @Test
    @TestCaseId("IPBQA-33051")
    @Description("EPBDS-10235: the download link hands over the workspace copy with local changes, a named revision without them, and 404 for an unknown revision")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDownloadLinkSeparatesTheWorkspaceCopyFromANamedRevision() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage detail = repositoryPage.openProjectsList().openProjectDetail(projectName);
        String projectId = detail.getProjectId();
        String revision = detail.getLatestRevisionId();
        assertThat(revision).as("revision the project was created at").isNotBlank();

        detail.uploadFile(TestDataUtil.getFilePathFromResources(LOCAL_CHANGE_FILE));
        assertThat(detail.isFilePresent(LOCAL_CHANGE_FILE)).as("local change in the workspace").isTrue();

        List<String> revisionEntries = downloadedEntries(ProjectLinkUtil.downloadLink(projectId, revision));
        List<String> workspaceEntries = downloadedEntries(ProjectLinkUtil.downloadLink(projectId));
        assertThat(revisionEntries).as("archive of revision %s", revision).isNotEmpty();
        assertThat(workspaceEntries).as("workspace archive = revision %s + local change", revision)
                .containsExactlyInAnyOrderElementsOf(Stream.concat(revisionEntries.stream(), Stream.of(LOCAL_CHANGE_FILE)).toList());

        int refusedStatus = DriverPool.getPage().navigate(ProjectLinkUtil.downloadLink(projectId, UNKNOWN_REVISION)).status();
        assertThat(refusedStatus).as("download link to an unknown revision").isEqualTo(404);
    }

    private static List<String> downloadedEntries(String link) {
        File archive = DownloadUtil.downloadFromLink(link);
        try {
            return ZipUtil.listFiles(archive);
        } finally {
            DownloadUtil.cleanupDownloadFile(archive);
        }
    }
}
