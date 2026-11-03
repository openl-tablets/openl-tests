package tests.ui.webstudio.repository;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.AuditLogPageComponent;
import domain.ui.webstudio.components.admincomponents.AuditLogPageComponent.AuditEvent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ProjectDeleteConfirmModalComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectDeleteClosedProjectNameUi extends BaseTest {

    @Test
    @TestCaseId({"EPBDS-16231", "EPBDS-17079"})
    @Description("EPBDS-16231: the Confirm Delete dialog for a CLOSED project shows the plain project name, not the name with a hash appended. "
            + "Once deleted, the audit log names the project by the path it was created at (EPBDS-17029) and its old link "
            + "opens as a link to no project (EPBDS-16946).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testConfirmDeleteShowsCleanNameForClosedProject() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.openProjectDetail(projectName);
        String projectLink = DriverPool.getPage().url();
        repositoryPage.openProjectsList().closeProject(projectName);
        repositoryPage.waitUntilSpinnerLoaded();

        assertThat(repositoryPage.isProjectActionAvailable(projectName, "Open"))
                .as("Precondition: the project must be in Closed state before the Confirm Delete check")
                .isTrue();
        ProjectDeleteConfirmModalComponent deleteModal = repositoryPage.deleteProject(projectName);

        assertThat(deleteModal.getMessage())
                .as("Confirm Delete dialog for a closed project must not append a hash to the project name")
                .doesNotContainPattern(":[0-9a-f]{16,}");
        deleteModal.enterDeletionComment("Deleted to check the audit log")
                .acknowledgePermanentDeletion()
                .clickDelete();

        AuditLogPageComponent auditLog = repositoryPage.openUserMenu().navigateToAdministration().navigateToAuditLogPage();
        List<AuditEvent> events = auditLog.waitForEvents(shown -> shown.stream()
                .anyMatch(event -> event.action().equals("Project deleted")));
        AuditEvent created = events.stream().filter(event -> event.action().equals("Project created")
                        && event.object().contains(projectName))
                .findFirst().orElseThrow(() -> new AssertionError("No Project created event in " + events));
        assertThat(events).as("Project deleted names the project by the path it was created at: %s", events)
                .anyMatch(event -> event.action().equals("Project deleted") && event.object().equals(created.object()));

        DriverPool.getPage().navigate(projectLink);
        assertThat(new ProjectDetailPage().isShownAsMissing())
                .as("The old link of the deleted project opens as a link to no project")
                .isTrue();
    }
}
