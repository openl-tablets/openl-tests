package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.RepositoriesPageComponent;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.DeployModalComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.DeployInfrastructureService;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestS3RepositoriesUi extends BaseTest {

    private static final String REGION = "US East (N. Virginia)";
    private static final String S3_DESIGN_REPOSITORY = "Design1";
    private static final String TEMPLATE = "Example 1 - Bank Rating";
    private static final String REGION_LIST = "The Region name list of an AWS S3 repository should offer the AWS regions";

    private final DeployInfrastructureService s3 = DeployInfrastructureService.builder().withS3Mock().build();

    @Override
    protected void startAuxiliaryContainers() {
        s3.start();
    }

    @Override
    protected void stopAuxiliaryContainers() {
        s3.cleanup();
    }

    @Test
    @TestCaseId("IPBQA-32111")
    @Description("An S3-compatible storage reached by its service endpoint works as a design repository: "
            + "its settings are kept, and a project is created, saved, copied and deleted in the bucket")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testS3DesignRepositoryKeepsProjects() {
        String bucket = s3.createBucket();
        EditorPage editorPage = login();
        addS3DesignRepository(editorPage, bucket, null);

        RepositoriesPageComponent repositories = editorPage.openUserMenu().navigateToAdministration().navigateToRepositoriesPage()
                .selectDesignRepositoryByName(S3_DESIGN_REPOSITORY);
        assertThat(repositories.getDesignRepositoryType()).as("Type of the saved repository").isEqualTo("AWS S3");
        assertThat(repositories.getSettingValue("serviceEndpoint")).as("Service endpoint of the saved repository")
                .isEqualTo(s3.getS3MockInNetworkEndpoint());
        assertThat(repositories.getSettingValue("bucketName")).as("Bucket name of the saved repository").isEqualTo(bucket);

        String project = StringUtil.generateUniqueName("S3Project");
        String created = "Project " + project + " is created.";
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProjectFromTemplateWithSelectRepo(project, TEMPLATE, S3_DESIGN_REPOSITORY);
        ProjectDetailPage detail = repositoryPage.openProjectsList().openProjectDetail(project);
        assertThat(detail.getOverviewRepository()).as("Repository of the created project").isEqualTo(S3_DESIGN_REPOSITORY);
        assertThat(detail.getRevisionDescriptions()).as("Revisions of the created project").containsExactly(created);
        String createdVersion = storedVersion(project);

        detail.editOverviewDescriptionAndSave("Kept in S3");
        repositoryPage.openProjectsList().saveProject(project, "Description kept in S3");
        assertThat(repositoryPage.openProjectsList().openProjectDetail(project).getRevisionDescriptions())
                .as("Saving the project should add a revision")
                .containsExactly("Description kept in S3", created);
        assertThat(storedVersion(project))
                .as("Saving the project should write a new version of it into the bucket")
                .isNotEqualTo(createdVersion);

        String copy = project + "Copy";
        repositoryPage.openProjectsList().copyProject(project, copy);
        assertThat(repositoryPage.openProjectsList().openProjectDetail(copy).getRevisionDescriptions())
                .as("The copy should say where it was copied from")
                .contains("Copied from: " + project + ".");
        assertThat(s3.snapshotObjects()).as("The copy should be written into the bucket").containsKey(key(copy));

        repositoryPage.openProjectsList().deleteProject(copy)
                .enterDeletionComment("Not needed")
                .acknowledgePermanentDeletion()
                .clickDelete();
        assertThat(WaitUtil.waitForCondition(() -> !repositoryPage.openProjectsList().isProjectPresent(copy),
                10000, 500, "Waiting for the deleted copy to leave the projects list"))
                .as("The deleted copy should leave the projects list").isTrue();
        assertThat(s3.snapshotObjects())
                .as("The deleted copy should leave the bucket, the original should stay")
                .doesNotContainKey(key(copy))
                .containsKey(key(project));
    }

    @Test
    @TestCaseId("IPBQA-32111")
    @Description("An S3-compatible storage reached by its service endpoint works as the first deployment repository: "
            + "it is configured in Admin and a project deployed from Studio lands in the bucket")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testS3DeploymentRepositoryReceivesDeployments() {
        String bucket = s3.createBucket();
        EditorPage editorPage = login();
        RepositoriesPageComponent repositories = editorPage.openUserMenu().navigateToAdministration().navigateToRepositoriesPage();
        repositories.addDeploymentRepository();
        repositories.setDesignRepositoryType("AWS S3");
        assertThat(repositories.getRegionOptions()).as(REGION_LIST).isNotEmpty();
        repositories.setS3Connection(s3.getS3MockInNetworkEndpoint(), bucket, REGION, s3.getS3AccessKey(), s3.getS3SecretKey())
                .applyChangesAndRelogin(User.ADMIN);

        String project = StringUtil.generateUniqueName("S3Deploy");
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, project, TEMPLATE);
        DeployModalComponent deployModal = repositoryPage.clickDeploy(project);
        deployModal.deployWithAllFields("Deployment", project, "Deploy to S3");
        assertThat(deployModal.isSuccessNotificationVisible()).as("Deploying to the S3 repository should succeed").isTrue();
        assertThat(s3.snapshotObjects().keySet())
                .as("The deployment should be written into the bucket")
                .anyMatch(stored -> stored.startsWith("deploy/" + project + "/"));
    }

    @Test
    @TestCaseId("IPBQA-32504")
    @Description("The SSE algorithm of an S3 design repository is kept in Admin and requests server-side encryption "
            + "for a project written into the bucket")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testS3SseAlgorithmEncryptsStoredProjects() {
        String bucket = s3.createBucket();
        EditorPage editorPage = login();
        addS3DesignRepository(editorPage, bucket, "AES256");

        RepositoriesPageComponent repositories = editorPage.openUserMenu().navigateToAdministration().navigateToRepositoriesPage()
                .selectDesignRepositoryByName(S3_DESIGN_REPOSITORY);
        assertThat(repositories.getSseAlgorithm()).as("SSE algorithm of the saved repository").isEqualTo("AES256");

        String project = StringUtil.generateUniqueName("S3Sse");
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProjectFromTemplateWithSelectRepo(project, TEMPLATE, S3_DESIGN_REPOSITORY);
        var stored = s3.snapshotObjects();
        assertThat(stored).as("The project should be written into the bucket").containsKey(key(project));
        assertThat(stored.get(key(project)).serverSideEncryption())
                .as("A project stored in an S3 repository with SSE algorithm AES256 should be written with server-side encryption AES256")
                .isEqualTo("AES256");
    }

    private EditorPage login() {
        return new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
    }

    private void addS3DesignRepository(EditorPage editorPage, String bucket, String sseAlgorithm) {
        RepositoriesPageComponent repositories = editorPage.openUserMenu().navigateToAdministration().navigateToRepositoriesPage();
        repositories.addDesignRepository(S3_DESIGN_REPOSITORY);
        repositories.setDesignRepositoryType("AWS S3");
        assertThat(repositories.getRegionOptions()).as(REGION_LIST).isNotEmpty();
        repositories.setS3Connection(s3.getS3MockInNetworkEndpoint(), bucket, REGION, s3.getS3AccessKey(), s3.getS3SecretKey());
        if (sseAlgorithm != null) {
            repositories.setSseAlgorithm(sseAlgorithm);
        }
        repositories.applyChangesAndRelogin(User.ADMIN);
    }

    private String storedVersion(String project) {
        var stored = s3.snapshotObjects();
        assertThat(stored).as("The project should be written into the bucket").containsKey(key(project));
        return stored.get(key(project)).versionId();
    }

    private static String key(String project) {
        return "DESIGN/rules/" + project;
    }
}
