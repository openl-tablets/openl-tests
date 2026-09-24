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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectLinkUi extends BaseTest {

    @Test
    @TestCaseId("IPBQA-33051")
    @Description("EPBDS-10235: a project link opens the project by the issued id, by a rebuilt id and by the bare name, and offers Open for a closed project")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testProjectLinkOpensTheProjectBySpellingTheServerAccepts() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        String issuedId = repositoryPage.openProjectsList().openProjectDetail(projectName).getProjectId();
        String rebuiltId = EntityIdUtil.encodeProjectId(EntityIdUtil.repositoryOf(issuedId), projectName);
        assertThat(rebuiltId).as("rebuilt id must differ from the issued one").isNotEqualTo(issuedId);

        for (String spelling : List.of(issuedId, rebuiltId, projectName)) {
            DriverPool.getPage().navigate(ProjectLinkUtil.projectLink(spelling));
            assertThat(new ProjectDetailPage().getOverviewPath()).as("project link by '%s'", spelling).isEqualTo(projectName);
        }

        repositoryPage.openProjectsList();
        repositoryPage.closeProject(projectName);
        DriverPool.getPage().navigate(ProjectLinkUtil.projectLink(issuedId));
        ProjectDetailPage closedDetail = new ProjectDetailPage();
        assertThat(closedDetail.getStatus()).as("status of a closed project reached by link").containsIgnoringCase("closed");
        assertThat(closedDetail.isHeaderActionAvailable("Open")).as("closed project offers Open").isTrue();
    }
}
