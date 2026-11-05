package tests.ui.webstudio.git;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.api.ProjectBranchesMethod;
import domain.api.ProjectResourcesMethod;
import domain.api.ProjectsMethod;
import domain.api.RepositoryProjectsMethod;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ResolveConflictsDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMergeConflictsOver256FilesUi extends BaseTest {

    private static final String PROJECT = "ConflictsOver256";
    private static final String BRANCH = "ManyFiles";
    private static final String MASTER = "master";
    private static final String DESIGN_REPO = "design";
    private static final int FILES = 257;

    @Test
    @TestCaseId("EPBDS-16468")
    @Description("EPBDS-16468: Sync with 257 files changed differently on both branches: all 257 conflicts are offered, "
            + "a resolution is chosen for each and Save and Resolve completes the merge instead of failing with HTTP 500.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void resolveMoreThan256ConflictedFiles() {
        ProtectedBranchBypassFixture.configureAdminCommitIdentity();
        createProjectWithConflictsOnBothBranches();

        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectDetail(PROJECT).openMergeDialog(BRANCH).clickReceive();

        ResolveConflictsDialogComponent conflicts = repositoryPage.getResolveConflictsDialogComponent();
        conflicts.waitForConflictedFile(fileName(0));
        assertThat(conflicts.getConflictedFilesCount())
                .as("The Resolve Conflicts dialog offers every one of the files both branches changed")
                .isEqualTo(FILES);
        assertThat(conflicts.chooseTheirsForEveryFile())
                .as("A resolution is chosen for each of the conflicted files").isEqualTo(FILES);
        conflicts.clickSave();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();

        assertThat(repositoryPage.getMergeSummaryResult())
                .as("Save and Resolve of more than 256 resolutions completes: the Merge Summary reports the conflicts resolved")
                .isEqualTo("Conflicts resolved and \"" + BRANCH + "\" merged into \"" + MASTER + "\".");
    }

    private void createProjectWithConflictsOnBothBranches() {
        File archive = zipOfFiles("base");
        assertThat(new RepositoryProjectsMethod().uploadProject(DESIGN_REPO, PROJECT, archive).getStatusCode())
                .as("The project archive is uploaded").isLessThan(300);
        String projectId = ProtectedBranchBypassFixture.resolveProjectId(PROJECT);
        ProjectBranchesMethod branches = new ProjectBranchesMethod();
        assertThat(branches.createBranch(projectId, BRANCH).getStatusCode()).as("The branch is created").isLessThan(300);
        assertThat(branches.switchBranch(projectId, BRANCH).getStatusCode()).as("The project is switched to the branch").isLessThan(300);
        commitEveryFile(projectId, "branch");
        assertThat(branches.switchBranch(projectId, MASTER).getStatusCode()).as("The project is switched back to master").isLessThan(300);
        commitEveryFile(projectId, "master");
    }

    private void commitEveryFile(String projectId, String content) {
        ProjectResourcesMethod resources = new ProjectResourcesMethod();
        File file = writeFile(content);
        for (int i = 0; i < FILES; i++) {
            assertThat(resources.updateResource(projectId, fileName(i), file).getStatusCode())
                    .as("The change of %s is accepted", fileName(i)).isLessThan(300);
        }
        assertThat(new ProjectBranchesMethod().commit(projectId, "Change files: " + content).getStatusCode())
                .as("The changes are committed").isLessThan(300);
        assertThat(new ProjectsMethod().closeProject(projectId).getStatusCode())
                .as("The project is released").isLessThan(300);
    }

    private static String fileName(int index) {
        return String.format("f%03d.txt", index);
    }

    private static File writeFile(String content) {
        try {
            Path file = Files.createTempFile("conflict-", ".txt");
            Files.writeString(file, content + "\n");
            return file.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static File zipOfFiles(String content) {
        try {
            Path zip = Files.createTempFile("conflicts-over-256-", ".zip");
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
                out.putNextEntry(new ZipEntry("rules.xml"));
                out.write(("<project><name>" + PROJECT + "</name><modules/></project>\n").getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
                for (int i = 0; i < FILES; i++) {
                    out.putNextEntry(new ZipEntry(fileName(i)));
                    out.write((content + "\n").getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();
                }
            }
            return zip.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
