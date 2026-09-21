package tests.ui.webstudio.repository;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.repositorytabcomponents.DeployModalComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.DeployInfrastructureService;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import helpers.utils.WaitUtil;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestNewDeployPopup extends BaseTest {

    private static final int WS_PORT = 8080;
    private static final Map<String, String> additionalContainerFiles = new HashMap<>();

    @Override
    protected Map<String, String> additionalContainerFiles() {
        return additionalContainerFiles;
    }

    private DeployInfrastructureService deployInfra;

    @Override
    @BeforeMethod
    public void beforeMethod(ITestResult result) {
        additionalContainerFiles.clear();
        deployInfra = DeployInfrastructureService.builder()
                .withPostgres()
                .withWsContainer()
                .build();
        deployInfra.start();
        additionalContainerFiles.putAll(deployInfra.getFilesToCopy());
        super.beforeMethod(result);
    }

    @Override
    @AfterMethod
    public void afterMethod(ITestResult result) {
        super.afterMethod(result);
        deployInfra.cleanup();
    }

    @Test
    @TestCaseId("IPBQA-30049")
    @Description("Deploy lifecycle: deploy to production PostgreSQL, edit, redeploy, "
            + "deploy dependent projects, verify via WS REST")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEPLOY_STUDIO_PARAMS)
    public void testNewDeployPopup() {
        String nameProject = StringUtil.generateUniqueName("DeployTest");
        String deploymentName = StringUtil.generateUniqueName("Deploy");

        EditorPage editorPage = new LoginService(DriverPool.getPage())
                .login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE,
                nameProject, "Example 1 - Bank Rating");

        String projectInitialRevision = repositoryPage.openProjectDetail(nameProject).getLatestRevisionId();
        repositoryPage.openProjectsList();
        LOGGER.info("Step 1: Project '{}' created, initial revision: {}", nameProject, projectInitialRevision);

        DeployModalComponent deployModal = repositoryPage.clickDeploy(nameProject);
        deployModal.deployWithAllFields(null, deploymentName, "First deploy to production");
        assertThat(deployModal.isSuccessNotificationVisible())
                .as("Deploy should succeed with success notification")
                .isTrue();
        repositoryPage.closeAllMessages();
        LOGGER.info("Step 2: Project '{}' deployed to production as '{}'", nameProject, deploymentName);

        String nameDependentProject1 = "Tutorial 3 - More Advanced Decision and Data Tables";
        String nameDependentProject2 = "Tutorial 6 - Introduction to Spreadsheet Tables";
        String zipFile1 = "Tutorial 3 - More Advanced Decision and Data Tables.zip";
        String zipFile2 = "Tutorial 6 - Introduction to Spreadsheet Tables.zip";
        String deploymentNameComplex = StringUtil.generateUniqueName("ComplexDeploy");

        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameDependentProject1, zipFile1);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameDependentProject2, zipFile2);

        deployModal = repositoryPage.clickDeploy(nameDependentProject1);
        deployModal.deployWithAllFields(null, deploymentNameComplex, "Deploy dependent project");
        assertThat(deployModal.isSuccessNotificationVisible())
                .as("Deploy of dependent project should succeed")
                .isTrue();
        repositoryPage.closeAllMessages();
        LOGGER.info("Step 3: Dependent projects deployed as '{}'", deploymentNameComplex);

        editorPage = new EditorPage();
        editProjectCell(editorPage, nameProject, "1000");

        repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.saveProject(nameProject, "Edit CapitalDynamicScore to 1000");

        String projectUpdatedRevision = repositoryPage.openProjectDetail(nameProject).getLatestRevisionId();
        repositoryPage.openProjectsList();
        assertThat(projectUpdatedRevision)
                .as("Revision should change after edit")
                .isNotEqualTo(projectInitialRevision);
        LOGGER.info("Step 4: Project edited, new revision: {}", projectUpdatedRevision);

        deployModal = repositoryPage.clickDeploy(nameProject);
        deployModal.deployWithAllFields(null, deploymentName, "Redeploy with updated revision");
        assertThat(deployModal.isSuccessNotificationVisible())
                .as("Redeploy should succeed")
                .isTrue();
        repositoryPage.closeAllMessages();
        LOGGER.info("Step 5: Project redeployed with updated revision");

        editorPage = new EditorPage();
        editProjectCell(editorPage, nameProject, "2000");

        repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.saveProject(nameProject, "Second edit to 2000");

        if (repositoryPage.getResolveConflictsDialogComponent().isDialogVisible()) {
            repositoryPage.getResolveConflictsDialogComponent().resolveConflictUseYours();
            LOGGER.info("Step 6: Conflict resolved using 'Use Yours'");
        } else {
            LOGGER.info("Step 6: No conflict occurred (expected in new deploy flow)");
        }

        String projectSecondUpdatedRevision = repositoryPage.openProjectDetail(nameProject).getLatestRevisionId();
        repositoryPage.openProjectsList();
        assertThat(projectSecondUpdatedRevision)
                .as("Revision should change after second edit")
                .isNotEqualTo(projectUpdatedRevision);
        LOGGER.info("Step 6: Second edit done, revision: {}", projectSecondUpdatedRevision);

        deployModal = repositoryPage.clickDeploy(nameProject);
        deployModal.deployWithAllFields(null, deploymentName, "Deploy after second edit");
        assertThat(deployModal.isSuccessNotificationVisible())
                .as("Deploy after second edit should succeed")
                .isTrue();
        repositoryPage.closeAllMessages();
        LOGGER.info("Step 6: Deployed after second edit");

        String nameProjectTutorial2 = "Tutorial 2 - Introduction to Data Tables";
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE,
                nameProjectTutorial2, nameProjectTutorial2);

        deployModal = repositoryPage.clickDeploy(nameProjectTutorial2);
        deployModal.deployWithAllFields(null, nameProjectTutorial2, "Deploy Tutorial 2");
        assertThat(deployModal.isSuccessNotificationVisible())
                .as("Deploy of Tutorial 2 should succeed")
                .isTrue();
        repositoryPage.closeAllMessages();
        LOGGER.info("Step 7: Tutorial 2 deployed");

        boolean isDockerMode = DriverPool.getCurrentExecutionMode() == configuration.driver.ExecutionMode.PLAYWRIGHT_DOCKER;
        String wsBaseUrl = isDockerMode
                ? "http://wscontainer:" + WS_PORT
                : "http://localhost:" + deployInfra.getWsContainer().getMappedPort(WS_PORT);
        List<String> expectedProjects = List.of(nameProject, nameDependentProject1, nameDependentProject2, nameProjectTutorial2);
        final long wsServicesTimeoutMs = 30000;
        final long wsPollingIntervalMs = 3000;

        boolean allServicesAppeared = WaitUtil.waitForCondition(
                () -> {
                    try {
                        DriverPool.getPage().navigate(wsBaseUrl);
                        DriverPool.getPage().waitForSelector("xpath=//h3");
                        String pageContent = DriverPool.getPage().content();
                        List<String> missingProjects = expectedProjects.stream()
                                .filter(project -> !pageContent.contains(project))
                                .toList();
                        if (!missingProjects.isEmpty()) {
                            LOGGER.info("WS services not ready yet. Missing projects: {}", missingProjects);
                            return false;
                        }
                        return true;
                    } catch (Exception e) {
                        LOGGER.warn("Transient error polling WS admin page, will retry: {}", e.getMessage());
                        return false;
                    }
                },
                wsServicesTimeoutMs, wsPollingIntervalMs, "Waiting for all services to appear in WS");
        assertThat(allServicesAppeared)
                .as("All expected WS services should appear within %sms", wsServicesTimeoutMs)
                .isTrue();

        String finalPageContent = DriverPool.getPage().content();
        for (String project : expectedProjects) {
            assertThat(finalPageContent)
                    .as("WS admin UI should show service for project '%s'", project)
                    .contains(project);
        }
        LOGGER.info("Step 8: WebService verification completed — all services found in WS admin UI");
    }

    private void editProjectCell(EditorPage editorPage, String projectName, String value) {
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Bank Rating");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "CapitalDynamicScore");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(6, 2, value);
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        WaitUtil.sleep(1000, "Wait for table save");
    }
}
