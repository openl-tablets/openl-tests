package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.DeploymentsHomePage;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.StringUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A project whose descriptor was deleted while the studio does not take a folder holding an Excel file for a
 * project by itself. The workbooks are still there and the project is still opened, so everything a reader
 * does with it must go on working.
 */
public class TestProjectWithoutDescriptorUi extends BaseTest {

    private static final String TEMPLATE = "Sample Project";

    @Test
    @TestCaseId("EPBDS-16638")
    @Description("The Management tab of a project whose rules.xml was deleted still lists the roles of the "
            + "project. EPBDS-16638 reports a 404 there; it does not reproduce on the build this suite runs "
            + "against, so this guards the behaviour rather than the defect.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testManagementTabOpensForAProjectWithoutDescriptor() {
        ProjectDetailPage card = projectWithoutDescriptor();
        assertThat(card.openManagementAndSeeRoles())
                .as("The Management tab should list the roles of the project")
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
    }

    @Test
    @TestCaseId("EPBDS-16641")
    @Description("A project whose rules.xml was deleted is deployed and stands among the deployments. Fails "
            + "on EPBDS-16641: nothing is deployed.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEPLOY_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16641")
    public void testDeployWorksForAProjectWithoutDescriptor() {
        String projectName = projectWithoutDescriptorNamed();
        String deployment = StringUtil.generateUniqueName("Deploy");

        RepositoryPage repositoryPage = new RepositoryPage().openProjectsList();
        assertThat(repositoryPage.isDeployAvailable(projectName))
                .as("The project should still be offered to be deployed")
                .isTrue();
        repositoryPage.clickDeploy(projectName)
                .deployWithAllFields(null, deployment, "Deploy of a project without a descriptor");

        assertThat(new DeploymentsHomePage().open().waitForLoaded().getVisibleDeploymentNames())
                .as("The deployment should stand among the deployments")
                .contains(deployment);
    }

    /** Creates the project, deletes its descriptor and keeps that deletion, which is where the scenarios start. */
    private ProjectDetailPage projectWithoutDescriptor() {
        String projectName = projectWithoutDescriptorNamed();
        return new RepositoryPage().openProjectsList().openProjectDetail(projectName);
    }

    private String projectWithoutDescriptorNamed() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE);
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList().openProjectDetail(projectName).deleteFile("rules.xml");
        repositoryPage.openProjectsList().saveProject(projectName, "Descriptor deleted");
        return projectName;
    }
}
