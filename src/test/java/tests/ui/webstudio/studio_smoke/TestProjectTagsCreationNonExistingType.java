package tests.ui.webstudio.studio_smoke;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.TagsPageComponent;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.AdminPage;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectTagsCreationNonExistingType extends BaseTest {

    private static final String PROJECT_NAME = "Project6";
    private static final String ZIP_FILE_NAME = "TagsTestProject6.zip";

    private static final String TAG_TYPE_NAME = "Tag";
    private static final String TAG_TYPE_OPT = "TagOpt";
    private static final String TAG_TYPE_EXT = "TagExt";
    private static final String TAG_TYPE_OPT_EXT = "TagOptExt";
    private static final String TAG_TYPE_UNKNOWN = "TagNonExisging";

    @Test
    @TestCaseId({"IPBQA-32767", "EPBDS-17083"})
    @Description("Create project from zip with a non-existing tag type: the tags declared by tags.properties "
            + "are applied as they are, including a type that the Tags settings do not know, and the Overview warns "
            + "about the mandatory tag type the project lacks (EPBDS-16604)")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testNonExistingTagTypeHandling() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        setupRequiredTagTypes(editorPage);
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT_NAME, ZIP_FILE_NAME, false);
        repositoryPage.fillCommitInfo();

        ProjectDetailPage projectDetail = repositoryPage.openProjectsList().openProjectDetail(PROJECT_NAME);
        assertThat(projectDetail.getTagValueForType(TAG_TYPE_EXT)).as("An extensible type should take the zip's value").isEqualTo("TagExt3");
        assertThat(projectDetail.getTagValueForType(TAG_TYPE_UNKNOWN)).as("A type missing from the Tags settings should still be shown as the zip declares it").isEqualTo("Tag3");
        assertThat(projectDetail.getMandatoryTagsWarning())
                .as("The Overview warns that the project lacks the mandatory tag type Tag")
                .isEqualTo("Tag type 'Tag' is mandatory.");
    }

    @Test
    @TestCaseId({"EPBDS-15684", "EPBDS-16754"})
    @Description("EPBDS-15684: the Overview of a project edits its tags and keeps them after Save. "
            + "EPBDS-16754: a click on the Tag or Value field of the tags editor lists every configured tag type or "
            + "value, a field that already holds text included, and typing narrows the list ignoring case, and "
            + "reopening the field shows the whole list again. The administration screen listing the tags found in "
            + "projects is not covered.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testEditTagsOnOverview() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        setupRequiredTagTypes(editorPage);
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT_NAME, ZIP_FILE_NAME, false);
        repositoryPage.fillCommitInfo();

        ProjectDetailPage projectDetail = repositoryPage.openProjectsList().openProjectDetail(PROJECT_NAME);
        projectDetail.startEditingTags();

        assertThat(projectDetail.getTagKeySuggestions(0))
                .as("A Tag field that already holds a tag type lists every configured tag type (EPBDS-16754)")
                .containsExactlyInAnyOrder(TAG_TYPE_NAME, TAG_TYPE_OPT, TAG_TYPE_EXT, TAG_TYPE_OPT_EXT);
        projectDetail.closeTagSuggestions(0);

        int newRow = projectDetail.addTagRow();
        assertThat(projectDetail.typeTagKey(newRow, "OPT"))
                .as("Typing narrows the tag types to those containing the text, ignoring case (EPBDS-16754)")
                .containsExactlyInAnyOrder(TAG_TYPE_OPT, TAG_TYPE_OPT_EXT);
        projectDetail.closeTagSuggestions(newRow);
        assertThat(projectDetail.getTagKeySuggestions(newRow))
                .as("Reopening the Tag field after typing shows every tag type again (EPBDS-16754)")
                .containsExactlyInAnyOrder(TAG_TYPE_NAME, TAG_TYPE_OPT, TAG_TYPE_EXT, TAG_TYPE_OPT_EXT);
        projectDetail.pickTagSuggestion(TAG_TYPE_OPT);

        assertThat(projectDetail.getTagValueSuggestions(newRow))
                .as("The Value field lists every value of the tag type named in its row (EPBDS-16754)")
                .containsExactlyInAnyOrder("TagOpt1", "TagOpt2");
        projectDetail.pickTagSuggestion("TagOpt1");
        assertThat(projectDetail.typeTagValue(newRow, "OPT2"))
                .as("Typing narrows the values to those containing the text, ignoring case (EPBDS-16754)")
                .containsExactly("TagOpt2");
        projectDetail.pickTagSuggestion("TagOpt2");
        assertThat(projectDetail.getTagValueSuggestions(newRow))
                .as("A Value field that already holds a value lists every value of its tag type (EPBDS-16754)")
                .containsExactlyInAnyOrder("TagOpt1", "TagOpt2");
        projectDetail.pickTagSuggestion("TagOpt2");

        projectDetail.saveTags();
        assertThat(projectDetail.getTagValueForType(TAG_TYPE_OPT))
                .as("The tag added in the Overview editor is shown with its value after Save (EPBDS-15684)")
                .isEqualTo("TagOpt2");
        assertThat(projectDetail.getTagValueForType(TAG_TYPE_EXT))
                .as("The tags the project already had stay after the editor saves (EPBDS-15684)")
                .isEqualTo("TagExt3");
    }

    @Test
    @TestCaseId("EPBDS-16952")
    @Description("A tag type carried only by a project of a branch the tree does not show is not offered by the "
            + "Group projects levels of the projects tree.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testGroupProjectsDoesNotOfferTagTypesOfHiddenBranches() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT_NAME, ZIP_FILE_NAME, false);
        repositoryPage.fillCommitInfo();

        ProjectDetailPage projectDetail = repositoryPage.openProjectsList().openProjectDetail(PROJECT_NAME);
        assertThat(repositoryPage.getGroupingLevelOptions())
                .as("While the project is shown, its tag types are offered for grouping")
                .contains(TAG_TYPE_EXT, TAG_TYPE_UNKNOWN);

        projectDetail.createBranch("hiddenBranch");
        repositoryPage.openProjectsList().deleteProject(PROJECT_NAME)
                .enterDeletionComment("Only the project of the other branch is left")
                .acknowledgePermanentDeletion()
                .clickDelete();
        repositoryPage.reloadPage();
        assertThat(repositoryPage.getBranchFilterOptions())
                .as("The projects list is loaded and still knows the branch of the project the tree does not show")
                .contains("hiddenBranch");

        List<String> offered = repositoryPage.getGroupingLevelOptions();
        assertThat(offered)
                .as("The tag types of a project that only a branch the tree does not show holds are not offered for grouping (EPBDS-16952)")
                .doesNotContain(TAG_TYPE_EXT, TAG_TYPE_UNKNOWN);
    }

    private void setupRequiredTagTypes(EditorPage editorPage) {
        AdminPage adminPage = editorPage.openUserMenu().navigateToAdministration();
        TagsPageComponent tagsPageComponent = adminPage.navigateToTagsPage();

        tagsPageComponent.addNewTagType(TAG_TYPE_NAME)
                .addTag(TAG_TYPE_NAME, "Tag1")
                .addTag(TAG_TYPE_NAME, "Tag2")
                .setExtensible(TAG_TYPE_NAME, false)
                .setRequired(TAG_TYPE_NAME, true);

        tagsPageComponent.addNewTagType(TAG_TYPE_OPT)
                .addTag(TAG_TYPE_OPT, "TagOpt1")
                .addTag(TAG_TYPE_OPT, "TagOpt2")
                .setExtensible(TAG_TYPE_OPT, false)
                .setRequired(TAG_TYPE_OPT, false);

        tagsPageComponent.addNewTagType(TAG_TYPE_EXT)
                .addTag(TAG_TYPE_EXT, "TagExt1")
                .addTag(TAG_TYPE_EXT, "TagExt2")
                .setExtensible(TAG_TYPE_EXT, true)
                .setRequired(TAG_TYPE_EXT, true);

        tagsPageComponent.addNewTagType(TAG_TYPE_OPT_EXT)
                .addTag(TAG_TYPE_OPT_EXT, "TagOptExt1")
                .addTag(TAG_TYPE_OPT_EXT, "TagOptExt2")
                .setExtensible(TAG_TYPE_OPT_EXT, true)
                .setRequired(TAG_TYPE_OPT_EXT, false);

        tagsPageComponent.saveTemplates();
    }

}
