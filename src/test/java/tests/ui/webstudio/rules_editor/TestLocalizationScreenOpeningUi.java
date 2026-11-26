package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.LocalizationComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestLocalizationScreenOpeningUi extends BaseTest {

    private static final String MODULE = "Rules";

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("The module switcher of a module offers Localization below the modules, and it opens the table of every key against every language")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testModuleSwitcherOpensLocalization() {
        EditorPage editorPage = openModule("LocalizationOpening.zip");

        assertThat(editorPage.getEditorToolbarPanelComponent().getBreadcrumbs().getModuleSwitcherItems())
                .as("The module switcher should list the module, then Localization below it")
                .containsExactly(MODULE, LocalizationComponent.SWITCHER_ITEM);

        LocalizationComponent localization = editorPage.openLocalization();
        assertThat(localization.getColumnTitles()).as("The default language comes first, then German").containsExactly("Default", "German");
        assertThat(localization.getColumnFiles()).as("Each column names the file of its language")
                .containsExactly("message.properties", "message_de.properties");
        assertThat(localization.getKeys()).as("The table lists the key of the files").containsExactly("greeting");
        assertThat(localization.getCellText("greeting", "German")).as("The German text of the key").isEqualTo("Hallo");
    }

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("Open in Editor of the i18n folder and of a localization file on the Files tab opens the Localization screen")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testOpenInEditorFromFilesTab() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, "LocalizationOpening.zip");
        LocalizationComponent localization = new EditorPage().getLocalizationComponent();

        projectDetail(projectName).openFolderInLocalization("i18n");
        assertThat(localization.waitForOpened().getKeys()).as("Open in Editor of the i18n folder should show the keys of the project")
                .containsExactly("greeting");

        projectDetail(projectName).openFileInLocalization("i18n/message_de.properties");
        assertThat(localization.waitForOpened().getKeys()).as("Open in Editor of i18n/message_de.properties should show the keys of the project")
                .containsExactly("greeting");
    }

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("A project without localization files offers Create Default Language on the Localization screen, and Save writes i18n/message.properties")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateDefaultLanguage() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, "LocalizationWithoutMessages.zip");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);

        LocalizationComponent localization = editorPage.openLocalization().createDefaultLanguage();
        assertThat(localization.getColumnTitles()).as("Create Default Language adds the column of the default language").containsExactly("Default");
        assertThat(localization.getKeys()).as("The new default language has no keys yet").isEmpty();
        localization.save();

        assertThat(projectDetail(projectName).isFileListedAt("i18n/message.properties"))
                .as("Save should write the default localization file into the i18n folder")
                .isTrue();
    }

    private EditorPage openModule(String archive) {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, archive);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        return editorPage;
    }

    private ProjectDetailPage projectDetail(String projectName) {
        return new RepositoryPage().openProjectsList().openProjectDetail(projectName);
    }
}
