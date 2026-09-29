package tests.ui.webstudio.repository;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.api.GetWsServicesMethod;
import domain.api.GetWsServicesMethod.WsService;
import domain.api.ServiceOpenApiMethod;
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
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
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
    private static final List<String> SERVICE_TEMPLATES = List.of(
            "Sample Project",
            "Example 1 - Bank Rating",
            "Example 2 - Corporate Rating",
            "Example 3 - Auto Policy Calculation",
            "Tutorial 1 - Introduction to Decision Tables",
            "Tutorial 2 - Introduction to Data Tables",
            "Tutorial 3 - More Advanced Decision and Data Tables",
            "Tutorial 4 - Introduction to Column Match Tables",
            "Tutorial 5 - Introduction to TBasic Tables",
            "Tutorial 6 - Introduction to Spreadsheet Tables",
            "Tutorial 7 - Introduction to Table Properties",
            "Tutorial 8 - Introduction to Smart Rules and Smart Lookup Tables");
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

    @Test
    @TestCaseId("EPBDS-13928")
    @Description("Every built-in template with rules, deployed from Studio, is served by Rule Services "
            + "and answers its OpenAPI in JSON and in YAML")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEPLOY_STUDIO_PARAMS)
    public void testDeployedTemplatesServeTheirOpenApi() {
        EditorPage editorPage = new LoginService(DriverPool.getPage())
                .login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        for (String template : SERVICE_TEMPLATES) {
            repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, template, template);
            DeployModalComponent deployModal = repositoryPage.clickDeploy(template);
            deployModal.deployWithAllFields(null, template, "Deploy " + template);
            assertThat(deployModal.isSuccessNotificationVisible())
                    .as("Deploying '%s' should succeed", template)
                    .isTrue();
            repositoryPage.closeAllMessages();
        }

        GetWsServicesMethod wsServices = new GetWsServicesMethod(deployInfra.getWsContainer(), WS_PORT);
        Map<String, WsService> served = new HashMap<>();
        WaitUtil.waitForCondition(() -> {
            try {
                wsServices.getServices().forEach(service -> served.put(service.deploymentName(), service));
            } catch (RuntimeException notReady) {
                LOGGER.warn("Rule Services did not list its services yet, will retry: {}", notReady.getMessage());
            }
            return served.keySet().containsAll(SERVICE_TEMPLATES);
        }, 90000, 3000, "Waiting for Rule Services to serve every deployed template");
        assertThat(served.keySet())
                .as("Rule Services should serve a deployment of every template")
                .containsAll(SERVICE_TEMPLATES);

        SoftAssertions softly = new SoftAssertions();
        for (String template : SERVICE_TEMPLATES) {
            WsService service = served.get(template);
            softly.assertThat(service.status())
                    .as("The service of '%s' should be deployed", template)
                    .isEqualTo("DEPLOYED");
            Response json = new ServiceOpenApiMethod(deployInfra.getWsContainer(), WS_PORT, service.restfulUrl(), "json").get();
            softly.assertThat(json.statusCode()).as("openapi.json of '%s' should be answered", template).isEqualTo(200);
            softly.assertThat(json.asString()).as("openapi.json of '%s' should not be an error", template)
                    .doesNotContain("NullPointerException");
            softly.assertThat(json.statusCode() == 200 ? json.jsonPath().getMap("paths") : Map.of())
                    .as("openapi.json of '%s' should describe the service paths", template)
                    .isNotEmpty();
            Response yaml = new ServiceOpenApiMethod(deployInfra.getWsContainer(), WS_PORT, service.restfulUrl(), "yaml").get();
            softly.assertThat(yaml.statusCode()).as("openapi.yaml of '%s' should be answered", template).isEqualTo(200);
            softly.assertThat(yaml.asString())
                    .as("openapi.yaml of '%s' should describe the service paths", template)
                    .startsWith("openapi:")
                    .containsPattern("(?m)^paths:\\R\\s+/");
        }
        softly.assertAll();
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
