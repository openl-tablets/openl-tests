package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.StringUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectWithoutDescriptorUi extends BaseTest {

    private static final String TEMPLATE = "Sample Project";

    @Test
    @TestCaseId("EPBDS-16638")
    @Description("The card of a project whose rules.xml was deleted opens and its Management tab lists the "
            + "roles of the project. Fails on EPBDS-16638: the card answers 404.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16638")
    public void testManagementTabOpensForAProjectWithoutDescriptor() {
        ProjectDetailPage card = projectWithoutDescriptor();
        assertThat(card.isManagementTabOffered())
                .as("The Management tab should be offered on the card of the project")
                .isTrue();
        card.openManagementTab();
        assertThat(card.isPageNotFound())
                .as("Opening the Management tab should read the roles, not answer 404")
                .isFalse();
        assertThat(card.whyTheManagementTabRefused())
                .as("The Management tab should read the roles of the project, not refuse them")
                .isEmpty();
        assertThat(card.managementTabListsRoles())
                .as("The Management tab should list the roles, or say there are none")
                .isTrue();
    }

    @Test
    @TestCaseId("EPBDS-16639")
    @Description("A project whose rules.xml was deleted is copied to a branch of its own. Fails on "
            + "EPBDS-16639: the copy is refused.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16639")
    public void testCopyToBranchWorksForAProjectWithoutDescriptor() {
        ProjectDetailPage card = projectWithoutDescriptor();
        String branch = StringUtil.generateUniqueName("branch");
        String refused = card.createBranchExpectingError(branch);
        assertThat(refused)
                .as("Copying the project to a branch should not be refused")
                .isEmpty();
        assertThat(card.isBranchPresent(branch))
                .as("The branch the copy was asked for should stand among the project's branches")
                .isTrue();
    }

    private ProjectDetailPage projectWithoutDescriptor() {
        String projectName = WorkflowService.loginCreateProjectWithoutDescriptor(User.ADMIN, TEMPLATE);
        return new RepositoryPage().openProjectsList().openProjectDetail(projectName);
    }

}
