package tests.ui.webstudio.studio_smoke;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.SystemSettingsPageComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.GitContainerService;
import helpers.service.GitRemote;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectDiscoveryByExcelFiles extends BaseTest {

    private static final String GIT_CONTAINER_ALIAS = "git-excel-discovery";
    private static final String FIXTURE_RESOURCE = "/excel_discovery_repo";
    private static final String REPO_NAME = "design";
    private static final String BRANCH = "master";

    private static final String DESCRIPTOR_PROJECT = "DescriptorProject";
    private static final String NESTED_DESCRIPTOR_PROJECT = "NestedDescriptorProject";
    private static final String NESTED_UNDER_EXCEL = "NestedUnderExcel";
    private static final String EXCEL_ONLY_PROJECT = "ExcelOnlyProject";
    private static final String EXCEL_FOLDER = "ExcelFolder";

    private static final List<String> DISCOVERED_BY_DESCRIPTOR_ONLY =
            List.of(DESCRIPTOR_PROJECT, NESTED_DESCRIPTOR_PROJECT, NESTED_UNDER_EXCEL);

    private static final List<String> DISCOVERED_WITH_EXCEL_FILES =
            List.of(DESCRIPTOR_PROJECT, EXCEL_FOLDER, EXCEL_ONLY_PROJECT, NESTED_DESCRIPTOR_PROJECT);

    private GitContainerService gitContainer;
    private GitRemote gitRemote;

    @Override
    protected void startAuxiliaryContainers() {
        gitContainer = new GitContainerService(GIT_CONTAINER_ALIAS, REPO_NAME, BRANCH, FIXTURE_RESOURCE);
        gitContainer.start();
        gitRemote = gitContainer.asRemote();
    }

    @Override
    protected void stopAuxiliaryContainers() {
        if (gitContainer != null) {
            gitContainer.stop();
            gitContainer = null;
        }
        gitRemote = null;
    }

    @Override
    protected Map<String, String> additionalContainerConfig() {
        return Map.of(
                "repository.design.uri", gitContainer.getInNetworkUrl(),
                "repository.design.login", gitRemote.login(),
                "repository.design.password", gitRemote.password()
        );
    }

    @Test
    @TestCaseId("IPBQA-33049")
    @Description("Projects tab hides a folder that only holds an Excel file in its root until Detect projects by Excel files is enabled")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_GIT)
    public void testProjectDiscoveryByExcelFiles() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        assertThat(repositoryPage.getAllVisibleProjectsInTable())
                .as("By default only descriptor projects are discovered, and discovery still descends into the folders it skips")
                .containsExactlyInAnyOrderElementsOf(DISCOVERED_BY_DESCRIPTOR_ONLY);

        SystemSettingsPageComponent systemSettings = repositoryPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();

        assertThat(systemSettings.isDetectProjectsByExcelFilesEnabled())
                .as("Detect projects by Excel files is cleared by default")
                .isFalse();

        systemSettings.setDetectProjectsByExcelFiles(true);
        systemSettings.applySettingsAndRelogin(User.ADMIN);

        RepositoryPage repositoryAfterRestart = new RepositoryPage();
        WaitUtil.waitForCondition(
                () -> repositoryAfterRestart.openProjectsList()
                        .getAllVisibleProjectsInTable()
                        .contains(EXCEL_ONLY_PROJECT),
                60000, 2000,
                "Waiting for the design repository to be re-read after the setting was applied");

        assertThat(repositoryAfterRestart.getAllVisibleProjectsInTable())
                .as("Once the setting is on, Excel-only folders become projects and the project nested below one of them is no longer reached")
                .containsExactlyInAnyOrderElementsOf(DISCOVERED_WITH_EXCEL_FILES);
    }
}
