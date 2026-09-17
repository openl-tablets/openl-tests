package tests.ui.webstudio.repository;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectDeleteUnsavedEditUi extends BaseTest {

    @Test
    @TestCaseId("EPBDS-16229")
    @Description("Deleting a project must succeed even when the name in its descriptor was changed and the "
            + "project was not saved: a project is named by its descriptor, so writing another name into "
            + "rules.xml renames the working copy while the repository still holds the old one.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDeleteProjectWithUnsavedRulesEditSucceeds() throws IOException {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        String renamedName = projectName + "Renamed";
        EditorPage editorPage = new EditorPage();

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .updateFile("rules.xml", descriptorNamed(renamedName));

        // The projects list keys a row by its working-copy name, so after the unsaved rename the row — and
        // its Delete action — is found under the new name, not the one it was created with.
        repositoryPage = repositoryPage.openProjectsList();
        assertThat(repositoryPage.isProjectPresent(renamedName))
                .as("The working copy is listed under the name its descriptor now carries")
                .isTrue();

        repositoryPage.deleteProject(renamedName)
                .enterDeletionComment("Removed by automated regression test")
                .acknowledgePermanentDeletion()
                .attemptDelete();
        repositoryPage.reloadPage();

        assertThat(repositoryPage.isProjectPresent(renamedName))
                .as("A project with an unsaved rules.xml edit must delete successfully, not error out")
                .isFalse();
    }

    /** A descriptor naming the project and nothing else, which is what a rename writes. */
    private static String descriptorNamed(String projectName) throws IOException {
        Path written = Files.createTempFile("rules-renamed", ".xml");
        Files.writeString(written, "<project>\n    <name>" + projectName + "</name>\n</project>\n",
                StandardCharsets.UTF_8);
        return written.toAbsolutePath().toString();
    }
}
