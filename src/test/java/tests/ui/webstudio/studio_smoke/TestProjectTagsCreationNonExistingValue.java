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

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectTagsCreationNonExistingValue extends BaseTest {

    private static final String PROJECT_NAME = "Project5";
    private static final String ZIP_FILE_NAME = "TagsTestProject5.zip";

    private static final String TAG_TYPE_NAME = "Tag";
    private static final String TAG_TYPE_OPT = "TagOpt";
    private static final String TAG_TYPE_EXT = "TagExt";
    private static final String TAG_TYPE_OPT_EXT = "TagOptExt";

    @Test
    @TestCaseId({"IPBQA-32767", "EPBDS-17083"})
    @Description("Create project from zip with a tag value outside the type's own list: the value declared by "
            + "tags.properties is applied as it is, without the removed reconciliation dialogs, and the Overview warns "
            + "that the value is not allowed for the non-extensible type (EPBDS-16604)")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testNonExistingTagValueHandling() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        setupRequiredTagTypes(editorPage);
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT_NAME, ZIP_FILE_NAME, false);
        repositoryPage.fillCommitInfo();

        ProjectDetailPage projectDetail = repositoryPage.openProjectsList().openProjectDetail(PROJECT_NAME);
        assertThat(projectDetail.getTagValueForType(TAG_TYPE_NAME)).as("The tag declared by the zip should be applied").isEqualTo("Tag9");
        assertThat(projectDetail.getTagNotAllowedWarning(TAG_TYPE_NAME))
                .as("A value outside the list of a non-extensible tag type is warned about")
                .isEqualTo("'Tag9' is not allowed value for tag type 'Tag'.");
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
