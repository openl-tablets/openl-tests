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
import helpers.utils.EntityIdUtil;
import helpers.utils.ProjectLinkUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectLinkUi extends BaseTest {

    private static final String TEMPLATE = "Sample Project";

    @Test
    @TestCaseId("IPBQA-33051")
    @Description("EPBDS-10235: a project link /projects/:projectId is an address a reader pastes into a browser. "
            + "The project must be reachable by every spelling the server accepts - the id it issued, the URL-safe "
            + "Base64 of repository:projectName, and the bare project name - because a link written by hand cannot "
            + "know the canonical id. A closed project behind such a link must offer Open rather than dead-end.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testProjectLinkOpensTheProjectBySpellingTheServerAccepts() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE);
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList().openProjectDetail(projectName);

        String issuedId = URLDecoder.decode(EntityIdUtil.lastUrlSegment(DriverPool.getPage().url()),
                StandardCharsets.UTF_8);
        String repositoryId = ProjectLinkUtil.repositoryOf(issuedId);
        String rebuiltId = ProjectLinkUtil.encodeProjectId(repositoryId, projectName);
        assertThat(rebuiltId)
                .as("An id rebuilt from '%s:%s' must differ from the one the server issued, otherwise the second "
                        + "check below merely repeats the first", repositoryId, projectName)
                .isNotEqualTo(issuedId);

        assertThat(openedProjectPath(ProjectLinkUtil.projectLink(issuedId)))
                .as("A project link carrying the id the server issued must open %s", projectName)
                .isEqualTo(projectName);
        assertThat(openedProjectPath(ProjectLinkUtil.projectLink(rebuiltId)))
                .as("A project link whose id was rebuilt as the Base64 of '%s:%s' must open the same project",
                        repositoryId, projectName)
                .isEqualTo(projectName);
        assertThat(openedProjectPath(ProjectLinkUtil.projectLink(projectName)))
                .as("A project link naming the project by its bare name must open the same project")
                .isEqualTo(projectName);

        repositoryPage.openProjectsList();
        repositoryPage.closeProject(projectName);

        DriverPool.getPage().navigate(ProjectLinkUtil.projectLink(issuedId));
        ProjectDetailPage closedDetail = new ProjectDetailPage();
        assertThat(closedDetail.getStatus())
                .as("A project link into a closed project must still report the project, not an error screen")
                .containsIgnoringCase("closed");
        assertThat(closedDetail.isHeaderActionAvailable("Open"))
                .as("A closed project reached by a link must offer Open, so the link is not a dead end")
                .isTrue();
    }

    private String openedProjectPath(String link) {
        new RepositoryPage().openProjectsList();
        DriverPool.getPage().navigate(link);
        return new ProjectDetailPage().getOverviewPath();
    }
}
