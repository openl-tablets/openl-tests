package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
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

    private static final String XML_COMMENT = "<!-- EPBDS-16697 -->";
    private static final String UNKNOWN_PUBLISHER = "MY PUBLISHER";

    @Test
    @TestCaseId("EPBDS-16657")
    @Description("The deploy configuration written through the studio is written in the form the studio asks "
            + "for, so Migrate is not offered afterwards, which is the fix of EPBDS-16657 this test guards.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
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

    @Test
    @TestCaseId({"EPBDS-16697", "EPBDS-16698"})
    @Description("EPBDS-16697: Edit unlocks the Configuration (XML) block of the deploy configuration and what is "
            + "typed there is kept by Save. EPBDS-16698: the Publishers field takes no word the engine does not "
            + "know, so the invalid \"MY PUBLISHER\" is refused and is not saved.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDeployConfigXmlIsEditableAndPublishersRefuseUnknownWords() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN,
                "Example 3 - Auto Policy Calculation");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage card = repositoryPage.openProjectsList().openProjectDetail(projectName);

        card.startEditingDeployConfig();
        assertThat(card.isDeployConfigXmlEditable())
                .as("The Configuration (XML) block should be editable once Edit is pressed")
                .isTrue();
        card.typeIntoDeployConfigXml(XML_COMMENT);

        card.typeDeployPublisherAndConfirm(UNKNOWN_PUBLISHER);
        assertThat(card.getDeployPublishers())
                .as("The Publishers field should refuse a word the engine does not know")
                .doesNotContain(UNKNOWN_PUBLISHER);
        card.saveDeployConfig();

        card.reloadPage();
        assertThat(card.getDeployConfigXml())
                .as("The text typed into Configuration (XML) should be kept by Save")
                .isEqualTo(XML_COMMENT);
        assertThat(card.getDeployPublishers())
                .as("The invalid publisher should not be saved")
                .doesNotContain(UNKNOWN_PUBLISHER);
    }
}
