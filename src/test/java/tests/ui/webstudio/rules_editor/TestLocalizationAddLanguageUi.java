package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.LocalizationComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestLocalizationAddLanguageUi extends BaseTest {

    private static final String MODULE = "Rules";

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("Add Language adds a regional language as an empty column showing the inherited texts, and Save writes its file named as the engine opens it")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testAddRegionalLanguage() {
        String projectName = createProject();
        LocalizationComponent localization = openLocalization(projectName);

        localization.addLanguage("French (fr)", "Canada (CA)");
        assertThat(localization.getColumnTitles()).as("French (Canada) takes its place among the languages by name")
                .containsExactly("Default", "French (Canada)", "German");
        assertThat(localization.getColumnFiles()).as("The file of the new language is named with the language in lower case and the country in upper case")
                .containsExactly("message.properties", "message_fr_CA.properties", "message_de.properties");
        assertThat(localization.getCellText("greeting", "French (Canada)"))
                .as("The new column starts empty and shows the inherited text, so nothing has to be copied")
                .isEqualTo("Hello | Inherited from Default");
        localization.save();

        assertThat(new RepositoryPage().openProjectsList().openProjectDetail(projectName).isFileListedAt("i18n/message_fr_CA.properties"))
                .as("Save should write the file of the new language")
                .isTrue();
    }

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("A language the messages already have cannot be added again")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testExistingLanguageIsRefused() {
        LocalizationComponent localization = openLocalization(createProject());

        assertThat(localization.addLanguageExpectingRefusal("German (de)"))
                .as("German without a country is already a language of the messages")
                .isEqualTo("The messages already have this language");
        assertThat(localization.getColumnTitles()).as("No column is added").containsExactly("Default", "German");
    }

    private String createProject() {
        return WorkflowService.loginCreateProjectFromZip(User.ADMIN, "LocalizationAddLanguage.zip");
    }

    private LocalizationComponent openLocalization(String projectName) {
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        return editorPage.openLocalization();
    }
}
