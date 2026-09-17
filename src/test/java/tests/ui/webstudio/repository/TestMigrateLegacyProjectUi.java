package tests.ui.webstudio.repository;

import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
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
    @Description("EPBDS-16327 Migrate of a legacy descriptor: a project whose rules.xml is written the old "
            + "way offers Migrate instead of Edit; the migration rewrites the descriptor into its minimal "
            + "modern form, keeps the module it declares, stops offering the migration, and lands as "
            + "uncommitted workspace edits.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testMigrateRewritesLegacyDescriptorAndKeepsModules() {
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

        assertThat(detail.getOverviewModuleNames())
                .as("The workbook must still be declared as a module after the migration")
                .anyMatch(name -> name.contains("LegacyOld"));
        assertThat(detail.isOverviewEditOffered())
                .as("Edit must replace Migrate once the descriptor is modern")
                .isTrue();
        assertThat(detail.isFilePresent("LegacyOld.xls"))
                .as("A descriptor rewrite must leave the workbook where rules.xml declares it")
                .isTrue();
        assertThat(detail.isOverviewMigrateOffered())
                .as("Migrate must stop being offered once rules.xml is in its minimal modern form")
                .isFalse();
        assertThat(detail.getStatus())
                .as("The migration must land as uncommitted workspace changes the user can save or revert")
                .contains("In Editing");
    }

    @Test
    @TestCaseId("IPBQA-33027")
    @Description("EPBDS-16327 Migrate of a project that declares nothing: with no rules.xml the workbook in "
            + "the root is what there is to move, so the migration moves it under rules/ and writes the "
            + "descriptor that names it.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16666")
    public void testMigrateMovesRootWorkbookOfAProjectWithoutDescriptor() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, "MigrateXlsProject.zip");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage detail = repositoryPage.openProjectDetail(projectName);
        detail.deleteFile("rules.xml");

        detail.openOverviewTab();
        assertThat(detail.isOverviewMigrateOffered())
                .as("Precondition: a project declaring nothing, with a workbook in its root, must offer Migrate")
                .isTrue();

        detail.migrateOverviewDescriptor();

        assertThat(detail.isFolderPresent("rules"))
                .as("The migration must move the root workbook under the rules folder")
                .isTrue();
        assertThat(detail.isFilePresent("rules.xml"))
                .as("The migration must write the descriptor that names the moved workbook")
                .isTrue();
        assertThat(detail.getOverviewModuleNames())
                .as("The moved workbook must still be a module of the project")
                .anyMatch(name -> name.contains("LegacyOld"));
    }
}
