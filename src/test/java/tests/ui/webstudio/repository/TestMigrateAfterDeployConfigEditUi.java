package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
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

public class TestMigrateAfterDeployConfigEditUi extends BaseTest {

    @Test
    @TestCaseId("EPBDS-16657")
    @Description("The deploy configuration written through the studio is written in the form the studio asks "
            + "for, so it is not offered to be migrated afterwards. Fails on EPBDS-16657: Migrate is offered "
            + "again after every edit of the deploy configuration.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16657")
    public void testMigrateIsNotOfferedAgainAfterEditingTheDeployConfig() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage card = repositoryPage.openProjectsList().openProjectDetail(projectName);

        card.writeDeployConfigServiceName("firstService");
        if (card.isDeployConfigMigrateOffered()) {
            card.migrateDeployConfig();
        }
        assertThat(card.isDeployConfigMigrateOffered())
                .as("A deploy configuration just migrated should not be offered to be migrated again")
                .isFalse();

        card.writeDeployConfigServiceName("secondService");
        assertThat(card.isDeployConfigMigrateOffered())
                .as("Writing the deploy configuration through the studio should leave nothing to migrate")
                .isFalse();
    }
}
