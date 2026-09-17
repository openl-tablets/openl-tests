package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.pages.mainpages.DeploymentsHomePage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.DeployInfrastructureService;
import helpers.service.WorkflowService;
import helpers.utils.StringUtil;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDeployProjectWithoutDescriptorUi extends BaseTest {

    private static final String TEMPLATE = "Sample Project";
    private static final Map<String, String> additionalContainerFiles = new HashMap<>();

    private DeployInfrastructureService deployInfra;

    @Override
    protected Map<String, String> additionalContainerFiles() {
        return additionalContainerFiles;
    }

    @Override
    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(ITestResult result) {
        additionalContainerFiles.clear();
        deployInfra = DeployInfrastructureService.builder().withPostgres().build();
        deployInfra.start();
        additionalContainerFiles.putAll(deployInfra.getFilesToCopy());
        super.beforeMethod(result);
    }

    @Override
    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        super.afterMethod(result);
        if (deployInfra != null) {
            deployInfra.cleanup();
        }
    }

    @Test
    @TestCaseId("EPBDS-16641")
    @Description("A project whose rules.xml was deleted is deployed and stands among the deployments. Fails "
            + "on EPBDS-16641: nothing is deployed.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEPLOY_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16641")
    public void testDeployWorksForAProjectWithoutDescriptor() {
        String projectName = WorkflowService.loginCreateProjectWithoutDescriptor(User.ADMIN, TEMPLATE);
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

}
