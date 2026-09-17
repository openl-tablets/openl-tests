package tests.ui.webstudio.repository;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMigrateLegacyProjectUi extends BaseTest {

    @Test
    @TestCaseId("IPBQA-33027")
    @Description("EPBDS-16327 Migrate happy path: a project whose workbook lies in its root, so Overview "
            + "offers Migrate instead of Edit; after the migration the workbook lives under rules/, stays "
            + "matched as a module, Edit replaces Migrate, and the change lands as uncommitted workspace edits.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testMigrateMovesRootWorkbookAndKeepsModules() {
        // The templates the product ships now carry the standard layout and a descriptor of their own, so a
        // project with a workbook in its root — which is what there is to move — comes from an archive.
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, "MigrateXlsProject.zip");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage detail = repositoryPage.openProjectDetail(projectName);

        assertThat(detail.isOverviewMigrateOffered())
                .as("Precondition: a project with a workbook in its root must offer Migrate")
                .isTrue();
        assertThat(detail.getOverviewModuleNames())
                .as("Precondition: the module must be declared before the migration")
                .anyMatch(name -> name.contains("LegacyOld"));

        detail.openOverviewTab();
        detail.migrateOverviewDescriptor();

        assertThat(detail.getOverviewMatchedModuleNames())
                .as("The workbook must still be matched as a module after the migration")
                .anyMatch(name -> name.contains("LegacyOld"));
        assertThat(detail.isOverviewEditOffered())
                .as("Edit must replace Migrate once the descriptor is modern")
                .isTrue();
        assertThat(detail.isFolderPresent("rules"))
                .as("The migration must move the root workbook under the rules folder")
                .isTrue();
        assertThat(detail.getStatus())
                .as("The migration must land as uncommitted workspace changes the user can save or revert")
                .contains("In Editing");
    }
}
