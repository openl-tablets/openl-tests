package tests.ui.webstudio.repository;

import com.microsoft.playwright.APIResponse;
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
import helpers.utils.EntityIdUtil;
import helpers.utils.ProjectLinkUtil;
import helpers.utils.TestDataUtil;
import helpers.utils.ZipUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectDownloadLinkUi extends BaseTest {

    private static final String TEMPLATE = "Sample Project";
    private static final String LOCAL_CHANGE_FILE = "TestFileAddDelete.rules.xls";
    private static final String UNKNOWN_REVISION = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";
    private static final int NOT_FOUND = 404;
    private static final String FOLLOW_LINK = "url => { const anchor = document.createElement('a');"
            + " anchor.href = url; document.body.appendChild(anchor); anchor.click(); anchor.remove(); }";

    @Test
    @TestCaseId("IPBQA-33051")
    @Description("EPBDS-10235: the project download link hands over the whole project as a ZIP. Without a version "
            + "it hands over the workspace copy, which carries the local changes of a project being edited; with "
            + "one it hands over the project as that revision left it. A revision the project has no record of must "
            + "be refused with HTTP 404 rather than answered with an empty or truncated archive.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDownloadLinkSeparatesTheWorkspaceCopyFromANamedRevision() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE);
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage detail = repositoryPage.openProjectsList().openProjectDetail(projectName);

        String projectId = URLDecoder.decode(EntityIdUtil.lastUrlSegment(DriverPool.getPage().url()),
                StandardCharsets.UTF_8);
        String revision = detail.getLatestRevisionId();
        assertThat(revision)
                .as("The project must report the revision it was created at, otherwise the version link is untestable")
                .isNotBlank();

        detail.uploadFile(TestDataUtil.getFilePathFromResources(LOCAL_CHANGE_FILE));
        assertThat(detail.isFilePresent(LOCAL_CHANGE_FILE))
                .as("The added file must be in the workspace copy before the archives are compared")
                .isTrue();

        List<String> workspaceCopy = downloadedEntries(ProjectLinkUtil.downloadLink(projectId));
        List<String> namedRevision = downloadedEntries(ProjectLinkUtil.downloadLink(projectId, revision));

        assertThat(namedRevision)
                .as("The revision archive must hold the project as revision %s left it, not be empty", revision)
                .isNotEmpty();
        assertThat(onlyIn(workspaceCopy, namedRevision))
                .as("The only entry the workspace copy holds that revision %s does not must be the file added "
                        + "after it: without a version the link hands over the workspace copy with its local "
                        + "changes, with one it hands over that revision", revision)
                .containsExactly(LOCAL_CHANGE_FILE);
        assertThat(onlyIn(namedRevision, workspaceCopy))
                .as("Adding a file must not take anything out of the project, so the revision archive holds no "
                        + "entry the workspace copy lacks")
                .isEmpty();

        APIResponse refused = DriverPool.getPage().request()
                .get(ProjectLinkUtil.downloadLink(projectId, UNKNOWN_REVISION));
        assertThat(refused.status())
                .as("A revision the project has no record of must be refused, not answered with an archive")
                .isEqualTo(NOT_FOUND);
    }

    private static List<String> onlyIn(List<String> entries, List<String> others) {
        return entries.stream().filter(entry -> !others.contains(entry)).toList();
    }

    private List<String> downloadedEntries(String link) {
        File archive = DownloadUtil.downloadFile(() -> DriverPool.getPage().evaluate(FOLLOW_LINK, link));
        try {
            assertThat(archive)
                    .as("The download link %s must hand over a file that exists", link)
                    .isNotNull()
                    .exists();
            assertThat(archive.length())
                    .as("The archive handed over by %s must not be empty", link)
                    .isGreaterThan(0);
            return ZipUtil.listFiles(archive);
        } finally {
            DownloadUtil.cleanupDownloadFile(archive);
        }
    }
}
