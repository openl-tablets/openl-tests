package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.LocalizationComponent;
import domain.ui.webstudio.components.editortabcomponents.ProblemsPanelComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestLocalizationWarningsNavigationUi extends BaseTest {

    private static final String MODULE = "Rules";
    private static final String MISSING_KEY = "Key 'order.shiping' has no default text: a language without a text of its own returns the key.";
    private static final String EXTRA_VALUE = "'order.items' has 1 placeholder, but 2 values are passed.";
    private static final long NAVIGATED_MS = 15000;
    private static final long LINKS_DRAWN_MS = 20000;

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("Example 1 of the ticket: a misspelled key and an extra value give one warning each on their cells, and a click opens the cell")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testWarningsAboutMsgCalls() {
        String projectName = createProject();
        ProblemsPanelComponent problems = openModule(projectName).getProblemsPanelComponent();

        assertThat(problems.getAllWarnings()).as("The misspelled key and the extra value are each reported once")
                .containsOnlyOnce(MISSING_KEY, EXTRA_VALUE);

        problems.selectProblemByText(MISSING_KEY);
        assertNavigated("errorCell=C5", "The warning about the misspelled key should open the cell of the Shipping step");
        openModule(projectName).getProblemsPanelComponent().selectProblemByText(EXTRA_VALUE);
        assertNavigated("errorCell=C6", "The warning about the extra value should open the cell of the Items step");
    }

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("Example 2 of the ticket: a duplicate, different placeholders and untranslated keys are reported; a warning about a language opens the key page with the language marked, the untranslated keys open the filtered table")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testWarningsAboutLocalizationFiles() {
        String projectName = createProject();
        EditorPage editorPage = openModule(projectName);
        ProblemsPanelComponent problems = editorPage.getProblemsPanelComponent();

        assertThat(problems.getAllWarnings())
                .as("The files of Example 2 give the duplicate, the placeholder and the untranslated keys warnings")
                .anyMatch(warning -> warning.contains("order.title") && warning.contains("message_de.properties")
                        && warning.contains("Written twice, on lines 1 and 4: the last text wins."))
                .anyMatch(warning -> warning.contains("order.total") && warning.contains("message_fr.properties")
                        && warning.contains("Placeholders [1] differ from [0] of the default language."))
                .anyMatch(warning -> warning.contains("1 key is not translated into German"))
                .anyMatch(warning -> warning.contains("2 keys are not translated into French"));

        problems.selectProblemByText("Placeholders [1] differ from [0]");
        assertNavigated("key=order.total", "The placeholder warning should open the page of order.total");
        assertThat(DriverPool.getPage().url()).as("The address names the language of the warning").contains("lang=fr");
        LocalizationComponent localization = editorPage.getLocalizationComponent();
        String unmarked = localization.getKeyPageLanguageBackground("Default");
        assertThat(localization.getKeyPageLanguageBackground("German")).as("Default and German are not marked alike").isEqualTo(unmarked);
        assertThat(localization.getKeyPageLanguageBackground("French")).as("The key page marks French, the language of the warning").isNotEqualTo(unmarked);

        openModule(projectName).getProblemsPanelComponent().selectProblemByText("2 keys are not translated into French");
        assertThat(localization.waitForOpened().getKeys()).as("The untranslated keys warning opens the table filtered to the keys French does not translate")
                .containsExactly("order.items", "order.discount");
    }

    @Test
    @TestCaseId("EPBDS-16933")
    @Description("A key written inside msg is a link with the default text on hover, a missing key tells it is missing, and a text equal to a key is marked as a possible key")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testMessageKeysInCells() {
        String projectName = createProject();
        TableComponent summary = openTable(openModule(projectName), "OrderSummary", 3, 2, "order.title");
        assertThat(summary.getCellHintText(3, 2, "order.title")).as("The hover shows the default text of the key")
                .contains("order.title = Order summary");
        assertThat(summary.getCellHintText(4, 2, "order.shiping")).as("The hover tells that the default file has no such key")
                .contains("order.shiping is not in the default localization file");
        assertThat(summary.getCellLink(3, 2, "order.title").getCssValue("text-decoration-style"))
                .as("A key written inside msg is a link, not a dotted possible key").isNotEqualTo("dotted");
        summary.getCellLink(3, 2, "order.title").click();
        assertNavigated("key=order.title", "A click on the key should open its page");

        TableComponent choice = openTable(openModule(projectName), "Choice", 3, 2, "order.discount");
        assertThat(choice.getCellLink(3, 2, "order.discount").getCssValue("text-decoration-style"))
                .as("A text equal to a key is underlined with a dotted line").isEqualTo("dotted");
        assertThat(choice.getCellHintText(3, 2, "order.discount")).as("A text equal to a key is offered as a possible key")
                .contains("May be the localization key \"order.discount\"");
        choice.getCellLink(3, 2, "order.discount").click();
        assertNavigated("key=order.discount", "A possible key should lead to its page as well");
    }

    private String createProject() {
        return WorkflowService.loginCreateProjectFromZip(User.ADMIN, "LocalizationOrders.zip");
    }

    private EditorPage openModule(String projectName) {
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        return editorPage;
    }

    private TableComponent openTable(EditorPage editorPage, String tableName, int row, int column, String link) {
        editorPage.getEditorLeftRulesTreeComponent().waitForTreeFoldersToLoad().selectVisibleLeafNode(tableName);
        TableComponent table = editorPage.getCenterTable();
        WaitUtil.requireCondition(() -> table.getCellLinkTexts(row, column).contains(link), LINKS_DRAWN_MS, 250,
                "Waiting for '" + link + "' to be drawn as a link in " + tableName);
        return table;
    }

    private void assertNavigated(String addressPart, String expectation) {
        assertThat(WaitUtil.waitForCondition(() -> DriverPool.getPage().url().contains(addressPart), NAVIGATED_MS, 200,
                "Waiting for the address to contain " + addressPart))
                .as(expectation)
                .isTrue();
    }
}
