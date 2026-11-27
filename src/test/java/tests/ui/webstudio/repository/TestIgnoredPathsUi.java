package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.SystemSettingsPageComponent;
import domain.ui.webstudio.components.common.SettingsMenuComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.projectdetail.ProjectHistoryTabComponent;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.GitContainerService;
import helpers.service.GitRemote;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import helpers.utils.WaitUtil;
import helpers.utils.ZipUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIgnoredPathsUi extends BaseTest {

    private static final String GIT_CONTAINER_ALIAS = "git-ignored-paths";
    private static final String REPO_NAME = "design";
    private static final String BRANCH = "master";
    private static final String PROJECT = "IgnoredPaths";
    private static final String FIXTURE = "TestIgnoredPathsUi";

    private static final String INITIAL = "Initial commit";
    private static final String PIPELINE_CHANGED = "Change the CI pipeline";
    private static final String POM_BUMPED = "Bump the version in pom.xml to 1.0.1";
    private static final String RULES_AND_POM_CHANGED = "Describe the project and bump the version to 1.0.2";
    private static final String POM_BUMPED_AGAIN = "Bump the version in pom.xml to 1.0.3";
    private static final String NO_FILE_CHANGED = "The revision changes no file of the project";

    private static final String DEFAULT_IGNORED_PATHS = "**/.git*/**, **/.svn/**, **/CVS/**, **/.cvsignore, "
            + "**/Jenkinsfile, **/.dockerignore, **/.mvn/**, **/mvnw*, **/target/**, **/.gradle/**, **/.bsp/**, "
            + "**/.bloop/**, **/.metals/**, **/node_modules/**, **/.idea/**, **/*.iml, **/.vscode/**, **/.settings/**, "
            + "**/.project, **/.classpath, **/*.log, **/*.tmp, **/*.swp, **/*~, **/~$*, **/.~lock.*, **/.DS_Store, "
            + "**/._*, **/__MACOSX/**, **/Thumbs.db, **/desktop.ini";
    private static final String BUILD_OUTPUT_ONLY = "**/target/**";

    private static final String POM = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openl.tests</groupId>
                <artifactId>ignored-paths</artifactId>
                <version>%s</version>
            </project>
            """;
    private static final String DESCRIBED_RULES_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <name>IgnoredPaths</name>
                <description>Changed outside OpenL Studio</description>
                <modules>
                    <module>
                        <name>Main</name>
                        <rules-root path="rules/*.xlsx"/>
                    </module>
                </modules>
            </project>
            """;
    private static final String CHANGED_PIPELINE = "name: build\non: [push, pull_request]\n";

    private GitContainerService gitContainer;
    private GitRemote gitRemote;
    private String headRevision;

    @Override
    protected void startAuxiliaryContainers() {
        gitContainer = new GitContainerService(GIT_CONTAINER_ALIAS, REPO_NAME, BRANCH,
                Path.of(TestDataUtil.getDirectoryPathFromResources(FIXTURE)))
                .withFile(PROJECT + "/target/Build.xlsx", "build output".getBytes(StandardCharsets.UTF_8));
        gitContainer.start();
        gitRemote = gitContainer.asRemote();
        gitContainer.commitUpdates(PIPELINE_CHANGED, Map.of(PROJECT + "/.github/workflows/build.yml", CHANGED_PIPELINE));
        gitContainer.commitUpdates(POM_BUMPED, Map.of(PROJECT + "/pom.xml", POM.formatted("1.0.1")));
        gitContainer.commitUpdates(RULES_AND_POM_CHANGED, Map.of(
                PROJECT + "/rules.xml", DESCRIBED_RULES_XML,
                PROJECT + "/pom.xml", POM.formatted("1.0.2")));
        headRevision = gitContainer.commitUpdates(POM_BUMPED_AGAIN, Map.of(PROJECT + "/pom.xml", POM.formatted("1.0.3")));
    }

    @Override
    protected void stopAuxiliaryContainers() {
        if (gitContainer != null) {
            gitContainer.stop();
            gitContainer = null;
        }
        gitRemote = null;
    }

    @Override
    protected Map<String, String> additionalContainerConfig() {
        return Map.of(
                "repository.design.uri", gitContainer.getInNetworkUrl(),
                "repository.design.login", gitRemote.login(),
                "repository.design.password", gitRemote.password()
        );
    }

    @Test
    @TestCaseId({"IPBQA-33078", "EPBDS-16755"})
    @Description("Ignored paths keep the build, VCS and CI files of a Git project out of reach: a commit that changes only "
            + "them or pom.xml is a technical revision the Revisions list hides unless it is the head of the branch, "
            + "which the project stands on, the projects tree lists none of "
            + "them even with hidden files shown, the export leaves them out, and the Ignored Paths system setting "
            + "decides which of them stay ignored")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_GIT)
    public void testIgnoredPaths() {
        RepositoryPage repositoryPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN))
                .getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage detail = repositoryPage.openProjectsList().openProjectDetail(PROJECT);
        String shownRevision = detail.getOverviewRevision();
        assertThat(shownRevision).as("The Overview tab shows the revision the project stands on").isNotBlank();
        assertThat(headRevision)
                .as("The project stands on the head of its branch, even when the head changes only pom.xml")
                .startsWith(shownRevision);

        ProjectHistoryTabComponent history = detail.getHistoryTab();
        assertShown(history::getShownRevisionDescriptions, List.of(POM_BUMPED_AGAIN, RULES_AND_POM_CHANGED, INITIAL),
                "The commits that change only ignored files or pom.xml are hidden by default, except the head of the branch");
        assertThat(history.getShownTechnicalRevisionDescriptions())
                .as("The head of the branch that changes only pom.xml is listed as a technical revision")
                .containsExactly(POM_BUMPED_AGAIN);
        history.showTechnicalRevisions(true);
        assertShown(history::getShownRevisionDescriptions,
                List.of(POM_BUMPED_AGAIN, RULES_AND_POM_CHANGED, POM_BUMPED, PIPELINE_CHANGED, INITIAL),
                "Technical revisions add the commits that change only ignored files or pom.xml");
        assertThat(history.getShownTechnicalRevisionDescriptions())
                .as("The commits that change only ignored files or pom.xml are marked technical")
                .containsExactly(POM_BUMPED_AGAIN, POM_BUMPED, PIPELINE_CHANGED);
        assertThat(history.isOpenOffered(INITIAL)).as("An earlier revision of the project can be opened").isTrue();
        assertThat(history.isOpenOffered(POM_BUMPED_AGAIN))
                .as("The technical head is the revision the project stands on and cannot be opened")
                .isFalse();
        assertThat(history.isOpenOffered(RULES_AND_POM_CHANGED))
                .as("The latest revision that changes the project is no longer the one it stands on and can be opened")
                .isTrue();
        assertThat(history.getChangesNote(PIPELINE_CHANGED))
                .as("A commit that changes only the CI pipeline changes no file of the project")
                .isEqualTo(NO_FILE_CHANGED);
        assertThat(history.getChangedFiles(POM_BUMPED)).as("A version bump changes only pom.xml")
                .containsExactly("Modified pom.xml");
        assertThat(history.getChangedFiles(RULES_AND_POM_CHANGED))
                .as("A mixed commit lists the project files it changed")
                .containsExactlyInAnyOrder("Modified pom.xml", "Modified rules.xml");
        assertThat(history.getChangedFiles(INITIAL))
                .as("The first revision lists the project files but none of the ignored ones")
                .containsExactlyInAnyOrder("Added Main.xlsx", "Added pom.xml", "Added rules.xml");
        history.showTechnicalRevisions(false);

        new SettingsMenuComponent().set(SettingsMenuComponent.Setting.SHOW_HIDDEN_FILES, true);
        repositoryPage.openProjectsList().openProject(PROJECT);
        ProjectDetailPage opened = repositoryPage.openProjectDetail(PROJECT);
        assertThat(opened.getListedFilePaths())
                .as("Even with hidden files shown, the projects tree lists none of the ignored files")
                .containsExactly("pom.xml", "rules.xml", "rules/Main.xlsx");
        assertThat(ZipUtil.listFiles(opened.openExportDialog().clickExportAndDownload()))
                .as("The export leaves out the ignored files and pom.xml")
                .containsExactlyInAnyOrder("rules.xml", "rules/Main.xlsx");
        repositoryPage.openProjectsList().closeProject(PROJECT);

        SystemSettingsPageComponent systemSettings = repositoryPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();
        assertThat(systemSettings.getIgnoredPaths())
                .as("Ignored Paths holds the default patterns")
                .isEqualTo(DEFAULT_IGNORED_PATHS);
        systemSettings.setIgnoredPaths(BUILD_OUTPUT_ONLY);
        systemSettings.applySettingsAndRelogin(User.ADMIN);

        RepositoryPage afterSetting = new RepositoryPage().waitUntilProjectListed(PROJECT).openProjectsList();
        afterSetting.openProject(PROJECT);
        new SettingsMenuComponent().set(SettingsMenuComponent.Setting.SHOW_HIDDEN_FILES, true);
        assertThat(afterSetting.openProjectDetail(PROJECT).getListedFilePaths())
                .as("Once only the build output is ignored, the CI pipeline and the Maven files are listed, and target is not")
                .containsExactly(".github/workflows/build.yml", ".mvn/maven.config", "pom.xml", "rules.xml",
                        "rules/Main.xlsx");
    }

    private static void assertShown(Supplier<List<String>> shown, List<String> expected, String because) {
        WaitUtil.waitForCondition(() -> shown.get().equals(expected), 10000, 300, "Waiting until " + because);
        assertThat(shown.get()).as(because).containsExactlyElementsOf(expected);
    }
}
