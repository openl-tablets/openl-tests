package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.EditorRevisionsTabComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ResolveConflictsDialogComponent;
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

public class TestRenameProjectFromOldRevisionConflictUi extends BaseTest {

    private static final String TEMPLATE_NAME = "Tutorial 1 - Introduction to Decision Tables";

    @Test
    @TestCaseId("EPBDS-16269")
    @Description("Renaming a project, then renaming it again from an older revision must offer Resolve "
            + "Conflicts instead of refusing the save (regression guard for EPBDS-16269). A project is named "
            + "by its descriptor, so a rename is a write into rules.xml.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testRenameFromOldRevisionOffersConflictResolution() throws IOException {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE_NAME);
        String renamedOnce = projectName + "2";
        String renamedTwice = projectName + "3";
        EditorPage editorPage = new EditorPage();

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .updateFile("rules.xml", descriptorNamed(renamedOnce));
        repositoryPage.openProjectsList().saveProject(renamedOnce, "Renamed once");

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(renamedOnce);
        editorPage.getEditorToolbarPanelComponent().clickMore().clickRevisions();
        EditorRevisionsTabComponent revisionsTab = new EditorRevisionsTabComponent();
        revisionsTab.waitForTableToLoad();
        revisionsTab.openRevision(2);

        // The working copy now carries the descriptor of the revision it was opened at, so it is listed
        // under the name it was created with again.
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList().openProjectDetail(projectName)
                .updateFile("rules.xml", descriptorNamed(renamedTwice));
        repositoryPage.openProjectsList().saveProject(renamedTwice, "Renamed twice from an old revision");

        assertThat(new ResolveConflictsDialogComponent().isDialogVisible())
                .as("Resolve Conflicts should be offered when a rename from an old revision conflicts with HEAD")
                .isTrue();
    }

    /** A descriptor naming the project and nothing else, which is what a rename writes. */
    private static String descriptorNamed(String projectName) throws IOException {
        Path written = Files.createTempFile("rules-renamed", ".xml");
        Files.writeString(written, "<project>\n    <name>" + projectName + "</name>\n</project>\n",
                StandardCharsets.UTF_8);
        return written.toAbsolutePath().toString();
    }
}
