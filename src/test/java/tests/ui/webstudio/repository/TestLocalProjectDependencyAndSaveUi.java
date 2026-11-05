package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerPool;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.projectdetail.ProjectOverviewTabComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.AppContainerFileService;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import org.testcontainers.containers.Container;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestLocalProjectDependencyAndSaveUi extends BaseTest {

    private static final String SOURCE_FOLDER = "example1";
    private static final String TARGET_FOLDER = "example2";
    private static final String TARGET_NAME = "Example 2 - Corporate Rating";
    private static final String WORKSPACE = AppContainerFileService.USER_WORKSPACE_BASE + "/"
            + UserService.getUser(User.ADMIN).getLogin();
    private static final String DEPENDS_ON = "Depends on";
    private static final String USED_BY = "Used by";

    @Test
    @TestCaseId({"EPBDS-16518", "EPBDS-16519"})
    @Description("EPBDS-16518: the dependency of a Local project whose folder differs from its name opens its page. "
            + "EPBDS-16519: a Local project does not offer Save on its page or in the Projects list.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void localProjectDependencyOpensAndSaveIsNotOffered() throws IOException, InterruptedException {
        putLocalProjectInWorkspace(SOURCE_FOLDER, "LocalProjectSource_rules.xml");
        putLocalProjectInWorkspace(TARGET_FOLDER, "LocalProjectTarget_rules.xml");

        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        ProjectDetailPage source = repositoryPage.openProjectDetail(SOURCE_FOLDER);
        assertThat(source.getStatus())
                .as("The project is Local: it exists only in the workspace")
                .isEqualTo("Local");
        assertThat(source.isHeaderActionAvailable("Delete"))
                .as("The page of the project has loaded its actions")
                .isTrue();
        assertThat(source.isHeaderActionAvailable("Save"))
                .as("A Local project is not linked to a Design repository, so its page does not offer Save")
                .isFalse();
        assertThat(repositoryPage.openProjectsList().getProjectActionLabels(SOURCE_FOLDER))
                .as("The row of a Local project offers its actions but not Save")
                .contains("Delete")
                .doesNotContain("Save");

        ProjectOverviewTabComponent overview = repositoryPage.openProjectDetail(SOURCE_FOLDER).getOverviewTab();
        assertThat(overview.getDependencyRowText(DEPENDS_ON, TARGET_NAME))
                .as("The dependency is found by the name rules.xml gives it, not reported as Not Found")
                .doesNotContain("Not Found");
        overview.openDependency(TARGET_NAME);

        ProjectDetailPage target = new ProjectDetailPage();
        assertThat(target.isPageNotFound())
                .as("The dependency opens its own page instead of Project 'example2' is not found")
                .isFalse();
        assertThat(target.getOverviewTab().getDependencyNames(USED_BY))
                .as("The page of the dependency lists the project it was opened from as using it")
                .containsExactly(SOURCE_FOLDER);
    }

    private void putLocalProjectInWorkspace(String folder, String descriptor) throws IOException, InterruptedException {
        Container.ExecResult made = AppContainerPool.get().getAppContainer().execInContainer("mkdir", "-p",
                WORKSPACE + "/" + folder, WORKSPACE + "/.metainfo");
        assertThat(made.getExitCode())
                .as("The workspace folders of the Local project are made: " + made.getStderr())
                .isZero();
        AppContainerFileService.copyFileToContainer(TestDataUtil.getFilePathFromResources(descriptor),
                WORKSPACE + "/" + folder + "/rules.xml");
        AppContainerFileService.copyFileToContainer(TestDataUtil.getFilePathFromResources("LocalProjectRegistry.properties"),
                WORKSPACE + "/.metainfo/" + folder + ".properties");
    }
}
