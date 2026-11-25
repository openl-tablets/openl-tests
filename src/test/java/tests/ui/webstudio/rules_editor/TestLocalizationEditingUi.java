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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestLocalizationEditingUi extends BaseTest {

    private static final String MODULE = "Rules";
    private static final String DEFAULT = "Default";
    private static final String GERMAN = "German";
    private static final String LUXEMBOURG = "German (Luxembourg)";
    private static final String ARCHIVE = "LocalizationEditing.zip";

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("The languages stand in order, a regional language next to its parent, and own, inherited, missing and empty texts look different")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testLanguagesAndCellKinds() {
        LocalizationComponent localization = openLocalization(WorkflowService.loginCreateProjectFromZip(User.ADMIN, ARCHIVE));

        assertThat(localization.getColumnTitles())
                .as("The default language comes first, and German (Luxembourg) stands next to German")
                .containsExactly(DEFAULT, GERMAN, LUXEMBOURG);
        assertThat(localization.getKeys()).as("The keys are listed in the order the default file writes them")
                .containsExactly("greeting", "farewell", "title", "empty.text", "only key without separator");
        assertThat(localization.getCellText("greeting", GERMAN)).as("A text of its own is shown as written").isEqualTo("Hallo");
        assertThat(localization.getCellText("greeting", LUXEMBOURG)).as("A regional language inherits the text of its parent language")
                .isEqualTo("Hallo | Inherited from German");
        assertThat(localization.getCellText("farewell", GERMAN)).as("A language without the text inherits the default text")
                .isEqualTo("Goodbye | Inherited from Default");
        assertThat(localization.getCellText("empty.text", DEFAULT)).as("A key written with '=' and no text is a deliberately empty text")
                .isEqualTo("Empty text");
        assertThat(localization.getCellText("only key without separator", DEFAULT))
                .as("A line without '=' or ':' is a key without any text, shown as missing")
                .isEqualTo("Not found: the rule returns the key itself");
    }

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("A text is changed, taken back to the inherited one, a key is added and another removed; Undo and Redo walk the steps across the table and the key page, and Save writes only the changed lines, each keeping its separator")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testEditUndoRedoAndSave() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, ARCHIVE);
        LocalizationComponent localization = openLocalization(projectName);

        localization.editCell("farewell", DEFAULT, "See you");
        assertThat(localization.getCellText("farewell", GERMAN)).as("German inherits the changed default text at once")
                .isEqualTo("See you | Inherited from Default");

        localization.useInheritedText("greeting", GERMAN);
        assertThat(localization.getCellText("greeting", GERMAN)).as("Use the inherited text takes the German text back to the default one")
                .isEqualTo("Hello | Inherited from Default");
        assertThat(localization.getCellText("greeting", LUXEMBOURG)).as("German (Luxembourg) follows its parent to the default text")
                .isEqualTo("Hello | Inherited from Default");

        localization.undo();
        assertThat(localization.getCellText("greeting", GERMAN)).as("Undo brings the German text back").isEqualTo("Hallo");
        localization.redo();
        assertThat(localization.getCellText("greeting", GERMAN)).as("Redo takes it back to the inherited text again")
                .isEqualTo("Hello | Inherited from Default");

        localization.removeKey("empty.text");
        assertThat(localization.getKeys()).as("Remove Key takes the key out of the table")
                .containsExactly("greeting", "farewell", "title", "only key without separator");

        localization.addKeyInNewRow("new.key", "New text");
        localization.openKey("farewell");
        assertThat(localization.getKeyPageText(DEFAULT)).as("The key page keeps the text written in the table and not saved yet")
                .isEqualTo("See you");
        localization.undo().showAllMessages();
        assertThat(localization.waitForKeys(keys -> !keys.contains("new.key")))
                .as("Undo on the key page takes back the key added in the table")
                .isTrue();
        localization.redo();
        assertThat(localization.waitForKeys(keys -> keys.contains("new.key"))).as("Redo on the table adds the key again").isTrue();
        assertThat(localization.getKeys()).as("The table lists the kept and the added keys")
                .containsExactly("greeting", "farewell", "title", "only key without separator", "new.key");
        localization.save();

        ProjectDetailPage projectDetail = new RepositoryPage().openProjectsList().openProjectDetail(projectName);
        List<String> defaultFile = projectDetail.readFileText("i18n/message.properties").lines().filter(line -> !line.isBlank()).toList();
        assertThat(defaultFile)
                .as("Only the lines of the changed keys are written: the comment, the order of the keys and the separator of each line stay, the new key goes to the end")
                .hasSize(6)
                .startsWith("# Messages of the editing test", "greeting = Hello", "farewell:See you", "title=Title", "only key without separator");
        assertThat(defaultFile.getLast()).as("The new key is written at the end of the file").matches("new\\.key\\s*=\\s*New text");
        assertThat(projectDetail.readFileText("i18n/message_de.properties").lines().filter(line -> !line.isBlank()).toList())
                .as("Use the inherited text removes only the German line of greeting; the other German line stays as written")
                .containsExactly("title=Titel");
    }

    private LocalizationComponent openLocalization(String projectName) {
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        return editorPage.openLocalization();
    }
}
