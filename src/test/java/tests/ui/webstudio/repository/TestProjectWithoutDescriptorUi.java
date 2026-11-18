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
import helpers.utils.StringUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectWithoutDescriptorUi extends BaseTest {

    private static final String TEMPLATE = "Sample Project";
    private static final String CARD_REFUSED = "The card of the project should open, not say the project does not exist";

    @Test
    @TestCaseId("EPBDS-16638")
    @Description("With Detect projects by Excel files off, deleting the rules.xml of a project is refused with a "
            + "message that tells to edit it or to turn the setting on, the file stays, and the Management tab of "
            + "the project lists its roles (EPBDS-16638).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDeletingTheDescriptorIsRefusedAndManagementTabOpens() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE);
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage card = repositoryPage.openProjectsList().openProjectDetail(projectName);

        String refusal = card.deleteFileExpectingRefusal("rules.xml");
        assertThat(refusal)
                .contains("The project descriptor rules.xml cannot be deleted or moved")
                .contains("turn on Detect projects by Excel files");
        assertThat(card.isFilePresent("rules.xml"))
                .as("rules.xml must stay in the project")
                .isTrue();

        assertThat(card.isManagementTabOffered())
                .as("The Management tab should be offered on the card of the project")
                .isTrue();
        card.openManagementTab();
        assertThat(card.isPageNotFound())
                .as("Opening the Management tab should read the roles, not answer 404")
                .isFalse();
        assertThat(card.whyTheManagementTabRefused())
                .as("The Management tab should read the roles of the project, not refuse them")
                .isEmpty();
        assertThat(card.managementTabListsRoles())
                .as("The Management tab should list the roles, or say there are none")
                .isTrue();
    }

    @Test
    @TestCaseId("EPBDS-16639")
    @Description("A project saved without its rules.xml (Detect projects by Excel files turned on to delete it, "
            + "then off) is not copied to a new branch: the copy is refused with a message that tells to save the "
            + "project with its rules.xml first, and no branch is left (EPBDS-16639).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCopyToBranchIsRefusedForAProjectSavedWithoutDescriptor() {
        ProjectDetailPage card = projectWithoutDescriptor();
        String branch = StringUtil.generateUniqueName("branch");
        String refusal = card.createBranchExpectingError(branch);
        assertThat(refusal)
                .contains("The branch is not created")
                .contains("Save the project with its rules.xml first");
        card.cancelCopyDialog();
        assertThat(card.isBranchPresent(branch))
                .as("No branch may be left behind")
                .isFalse();
    }

    private ProjectDetailPage projectWithoutDescriptor() {
        String projectName = WorkflowService.loginCreateProjectWithoutDescriptor(User.ADMIN, TEMPLATE);
        ProjectDetailPage card = new RepositoryPage().openProjectsList().openProjectDetail(projectName);
        assertThat(card.isShownAsMissing()).as(CARD_REFUSED).isFalse();
        return card;
    }

}
