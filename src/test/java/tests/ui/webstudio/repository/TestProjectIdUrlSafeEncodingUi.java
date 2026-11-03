package tests.ui.webstudio.repository;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.EntityIdUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectIdUrlSafeEncodingUi extends BaseTest {

    private static final String PROJECT = "Тестовый проект";
    private static final String MODULE = "Main";

    @Test
    @TestCaseId("IPBQA-33034")
    @Description("EPBDS-16402: a project with a non-ASCII name must work through create, project detail, Files, "
            + "Revisions, the editor bridge, a deep link and delete. Since EPBDS-16945 its id in the URL is the "
            + "opaque 13-character Crockford Base32 id, safe in a URL as it is and not built from the name.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testUrlSafeIdSurvivesFullProjectLifecycle() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, PROJECT, "Sample Project");

        ProjectDetailPage detail = repositoryPage.openProjectsList().openProjectDetail(PROJECT);
        String detailUrl = DriverPool.getPage().url();
        String idSegment = EntityIdUtil.lastUrlSegment(detailUrl);
        assertThat(idSegment)
                .as("The project id in the URL must be the 13 characters of Crockford Base32, not a name the "
                        + "URL has to escape")
                .matches(EntityIdUtil.PROJECT_ID_PATTERN);

        detail.openFilesTab();
        assertThat(detail.isFilePresent("Main.xlsx"))
                .as("The Files tab must list the template module for a project with a non-ASCII name")
                .isTrue();

        detail.openHistoryTab();
        assertThat(detail.getRevisionsCount())
                .as("The Revisions tab must load the history for a project with a non-ASCII name")
                .isGreaterThanOrEqualTo(1);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT, MODULE);
        assertThat(editorPage.getEditorLeftRulesTreeComponent().waitForTreeFoldersToLoad().getAllEndNodesNames())
                .as("The editor must open the module of the project with a non-ASCII name")
                .isNotEmpty();

        DriverPool.getPage().navigate(detailUrl);
        detail = new ProjectDetailPage();
        assertThat(detail.getStatus())
                .as("A deep link holding the id of a project with a non-ASCII name must open the project detail, not HTTP 400")
                .isNotBlank();

        repositoryPage.openProjectsList();
        repositoryPage.closeProject(PROJECT);
        repositoryPage.deleteProject(PROJECT)
                .enterDeletionComment("Removed by the non-ASCII project name regression test")
                .acknowledgePermanentDeletion()
                .clickDelete();
        repositoryPage.waitUntilSpinnerLoaded();
        assertThat(repositoryPage.isProjectPresent(PROJECT))
                .as("Delete must work for the project with a non-ASCII name")
                .isFalse();
    }
}
