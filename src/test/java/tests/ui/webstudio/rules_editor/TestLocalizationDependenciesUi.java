package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.LocalizationComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.ProjectLinkUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestLocalizationDependenciesUi extends BaseTest {

    private static final String SHARED = "LocalizationShared";
    private static final String PARENT = "LocalizationParent";
    private static final String MODULE = "Rules";
    private static final long LINKS_DRAWN_MS = 20000;

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("A text the project lacks is taken from the project it depends on and named after it; the keys of the dependency get no rows of their own")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTextInheritedFromDependency() {
        LocalizationComponent localization = openParentModule().openLocalization();

        assertThat(localization.getKeys()).as("Only the keys the project writes have rows; shared.only stays in its project")
                .containsExactly("parent.key", "shared.text");
        assertThat(localization.getCellText("shared.text", "Default"))
                .as("The default text of shared.text comes from the dependency and names it")
                .isEqualTo("Shared text | Inherited from Default of \"" + SHARED + "\"");
        assertThat(localization.getCellText("shared.text", "French")).as("The French text is the project's own").isEqualTo("Texte commun");
    }

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("A msg key only the dependency defines opens in that project; a link to it in the project offers Open in that project, which opens it there, and Override Here, which adds it to the project")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testKeyOfDependency() {
        EditorPage editorPage = openParentModule();
        editorPage.getEditorLeftRulesTreeComponent().waitForTreeFoldersToLoad().selectVisibleLeafNode("ParentTexts");
        TableComponent table = editorPage.getCenterTable();
        WaitUtil.requireCondition(() -> table.getCellLinkTexts(3, 2).contains("shared.only"), LINKS_DRAWN_MS, 250,
                "Waiting for the key shared.only to be drawn as a link");
        String parentId = ProjectLinkUtil.projectIdOf(DriverPool.getPage().url());

        table.getCellLink(3, 2, "shared.only").click();
        LocalizationComponent localization = editorPage.getLocalizationComponent().waitForOpened();
        String sharedId = ProjectLinkUtil.projectIdOf(DriverPool.getPage().url());
        assertThat(sharedId).as("A key only the dependency defines should open in another project").isNotEqualTo(parentId);
        assertThat(localization.getKeyPageText("Default")).as("The key page shows the default text of the dependency").isEqualTo("Shared only");

        DriverPool.getPage().navigate(ProjectLinkUtil.localizationKeyLink(parentId, "shared.only"));
        assertThat(localization.getMissingKeyText()).as("The project itself does not write the key")
                .contains("written in the project \"" + SHARED + "\"");
        assertThat(localization.getOwnerButtonText()).as("The page offers to open the key in the dependency")
                .isEqualTo("Open in \"" + SHARED + "\"");
        localization.openInOwnerProject();
        localization.waitForOpened();
        assertThat(ProjectLinkUtil.projectIdOf(DriverPool.getPage().url())).as("Open in that project leads to the project of the msg link")
                .isEqualTo(sharedId);

        DriverPool.getPage().navigate(ProjectLinkUtil.localizationKeyLink(parentId, "shared.only"));
        assertThat(localization.overrideHere("Parent only")).as("Override Here offers the key with its name filled in")
                .isEqualTo("shared.only");
        assertThat(localization.showAllMessages().getKeys()).as("Override Here adds the key to the project")
                .contains("shared.only");
    }

    private EditorPage openParentModule() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, SHARED, SHARED + ".zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PARENT, PARENT + ".zip");
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PARENT, MODULE);
        return editorPage;
    }
}
