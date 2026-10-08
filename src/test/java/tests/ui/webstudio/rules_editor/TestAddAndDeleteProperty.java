package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.RightTableDetailsComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestAddAndDeleteProperty extends BaseTest {

    private static final String PROJECT_NAME = "TestAddDeleteEditProperties";
    private static final String EXCEL_FILE = "TestAddDeleteEditProperties.xlsx";
    private static final DateTimeFormatter ENTERED_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final String EDITED_TABLE = "MyRules2";
    private static final String KEEPS_TABLE_OPEN = "After a property is saved the Rules Editor should keep " + EDITED_TABLE + " open";
    private static final int SAVE_SETTLE_MS = 10000;
    private static final int LEAVE_WINDOW_MS = 2000;
    private static final String PICKED_BY_NAME = "of the values picked by their names";

    @Test
    @TestCaseId("IPBQA-25857")
    @Description("Rules Editor - Add and delete properties in table details. Fails on EPBDS-16871: the save that moves "
            + "MyRules2 below MyRules1 may open MyRules1 instead of the saved table; and on EPBDS-16872: a list property "
            + "added after the first one is stored with the names of its values instead of their codes.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue(value = "EPBDS-16871", failsWith = KEEPS_TABLE_OPEN)
    @KnownIssue(value = "EPBDS-16872", failsWith = PICKED_BY_NAME)
    public void testAddAndDeleteProperty() {
        EditorPage editorPage = loginAndCreateProject();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(PROJECT_NAME, PROJECT_NAME);
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, EDITED_TABLE);

        addAndCheckProperty(editorPage, "Category", "category", "MyCategory");

        addAndCheckProperty(editorPage, "Description", "description", "TestDescription");
        addAndCheckProperty(editorPage, "Tags", "tags", "Tag1,Tag2");
        addAndCheckProperty(editorPage, "Effective Date", "effectiveDate", "05/14/2024");
        addAndCheckProperty(editorPage, "Expiration Date", "expirationDate", "05/16/2024");
        addAndCheckProperty(editorPage, "Start Request Date", "startRequestDate", "05/14/2024");
        addAndCheckProperty(editorPage, "End Request Date", "endRequestDate", "04/13/2024");
        addAndCheckProperty(editorPage, "LOB", "lob", "007");
        addAndCheckProperty(editorPage, "Nature", "nature", "TestNature1");
        addAndCheckProperty(editorPage, "ID", "id", "test2");
        addAndCheckProperty(editorPage, "Build Phase", "buildPhase", "Property2");

        addAndCheckCheckboxProperty(editorPage, "Canada Region", "caRegions", new Choice("Québec", "QC"));
        addAndCheckCheckboxProperty(editorPage, "Canada Province", "caProvinces",
                new Choice("Territoires du Nord-Ouest", "NT"), new Choice("Yukon", "YT"));
        addAndCheckCheckboxProperty(editorPage, "Countries", "country", new Choice("Belarus", "BY"));
        addAndCheckCheckboxProperty(editorPage, "Currency", "currency", new Choice("Yemen, Rials", "YER"));
        addAndCheckCheckboxProperty(editorPage, "Language", "lang", new Choice("Spanish", "SPA"));
        addAndCheckCheckboxProperty(editorPage, "US Region", "usregion", new Choice("Northeast", "NE"));
        addAndCheckCheckboxProperty(editorPage, "US States", "state", new Choice("Washington", "WA"), new Choice("West Virginia", "WV"));

        addAndCheckBooleanProperty(editorPage, "Cacheable", "cacheable", false);

        addAndCheckDropdownProperty(editorPage, "Origin", "origin", "Deviation");
        addAndCheckDropdownProperty(editorPage, "Recalculate", "recalculate", "Analyze");
        addAndCheckDropdownProperty(editorPage, "Validate DT", "validateDT", "Off");
        addAndCheckDropdownProperty(editorPage, "Empty Result Processing", "emptyResultProcessing", "Return");

        editorPage.getEditorLeftRulesTreeComponent()
                .selectItemInFolder(DECISION, "MyRules1");

        deletePropertyAndCheck(editorPage, "Category", "category");
        deletePropertyAndCheck(editorPage, "Description", "description");
        deletePropertyAndCheck(editorPage, "Tags", "tags");
        deletePropertyAndCheck(editorPage, "Effective Date", "effectiveDate");
        deletePropertyAndCheck(editorPage, "Expiration Date", "expirationDate");
        deletePropertyAndCheck(editorPage, "Start Request Date", "startRequestDate");
        deletePropertyAndCheck(editorPage, "End Request Date", "endRequestDate");
        deletePropertyAndCheck(editorPage, "Canada Region", "caRegions");
        deletePropertyAndCheck(editorPage, "Canada Province", "caProvinces");
        deletePropertyAndCheck(editorPage, "Countries", "country");
        deletePropertyAndCheck(editorPage, "Region", "region");
        deletePropertyAndCheck(editorPage, "Currency", "currency");
        deletePropertyAndCheck(editorPage, "Language", "lang");
        deletePropertyAndCheck(editorPage, "LOB", "lob");
        deletePropertyAndCheck(editorPage, "Origin", "origin");
        deletePropertyAndCheck(editorPage, "US Region", "usregion");
        deletePropertyAndCheck(editorPage, "US States", "state");
        deletePropertyAndCheck(editorPage, "ID", "id");
        deletePropertyAndCheck(editorPage, "Build Phase", "buildPhase");
        deletePropertyAndCheck(editorPage, "Validate DT", "validateDT");
        deletePropertyAndCheck(editorPage, "Cacheable", "cacheable");
        deletePropertyAndCheck(editorPage, "Recalculate", "recalculate");
        deletePropertyAndCheck(editorPage, "Nature", "nature");
        deletePropertyAndCheck(editorPage, "Empty Result Processing", "emptyResultProcessing");
    }

    private EditorPage loginAndCreateProject() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.createProject(CreateNewProjectComponent.TabName.EXCEL_FILES, PROJECT_NAME, EXCEL_FILE);

        return new EditorPage();
    }

    private void editAndCheckProperty(EditorPage editorPage, String propertyName, String propertyTableName, String newValue) {
        WaitUtil.sleep(300, "Waiting before editing property");
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();

        if (propertyName.contains("Date")) {
            tableDetails.editDateProperty(propertyName, newValue);
            tableDetails.clickSaveBtn();
            newValue = LocalDate.parse(newValue, ENTERED_DATE).toString();
        } else {
            tableDetails.editTextProperty(propertyName, newValue);
            tableDetails.clickSaveBtn();
        }

        assertThat(awaitSavedPropertyValue(editorPage, propertyTableName, newValue))
                .as("Property '%s' should have value '%s'", propertyTableName, newValue)
                .isEqualTo(newValue);
    }

    private void editAndCheckCheckboxProperty(EditorPage editorPage, String propertyName, String propertyTableName, Choice... choices) {
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();
        tableDetails.editCheckboxProperty(propertyName, Arrays.stream(choices).map(Choice::label).toArray(String[]::new));
        tableDetails.clickSaveBtn();

        String expectedValue = Arrays.stream(choices).map(Choice::code).collect(Collectors.joining(","));
        assertThat(awaitSavedPropertyValue(editorPage, propertyTableName, expectedValue))
                .as("Property '%s' should hold the codes '%s' " + PICKED_BY_NAME, propertyTableName, expectedValue)
                .isEqualTo(expectedValue);
    }

    private void editAndCheckBooleanProperty(EditorPage editorPage, String propertyName, String propertyTableName, boolean value) {
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();
        tableDetails.editBooleanProperty(propertyName, value);
        tableDetails.clickSaveBtn();

        assertThat(awaitSavedPropertyValue(editorPage, propertyTableName, String.valueOf(value)))
                .as("Property '%s' should have value '%s'", propertyTableName, value)
                .isEqualTo(String.valueOf(value));
    }

    private void editAndCheckDropdownProperty(EditorPage editorPage, String propertyName, String propertyTableName, String value) {
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();
        tableDetails.editDropdownProperty(propertyName, value);
        tableDetails.clickSaveBtn();

        assertThat(awaitSavedPropertyValue(editorPage, propertyTableName, value))
                .as("Property '%s' should have value '%s'", propertyTableName, value)
                .isEqualToIgnoringCase(value);
    }

    private String awaitSavedPropertyValue(EditorPage editorPage, String propertyTableName, String expectedValue) {
        TableComponent table = editorPage.getCenterTable();
        WaitUtil.waitForCondition(() -> expectedValue.equalsIgnoreCase(shownPropertyValue(table, propertyTableName)),
                SAVE_SETTLE_MS, 250, "Waiting for property '" + propertyTableName + "' to show the saved value");
        assertTableStaysOpen(editorPage.getEditorLeftRulesTreeComponent());
        return shownPropertyValue(table, propertyTableName);
    }

    private String shownPropertyValue(TableComponent table, String propertyTableName) {
        return table.isPropertyPresent(propertyTableName) ? table.getPropertyValue(propertyTableName) : "";
    }

    private void assertTableStaysOpen(EditorLeftRulesTreeComponent rulesTree) {
        WaitUtil.waitForCondition(() -> {
            String selected = rulesTree.getSelectedItemText();
            return !selected.isEmpty() && !EDITED_TABLE.equals(selected);
        }, LEAVE_WINDOW_MS, 250,
                "Watching whether the Rules Editor leaves " + EDITED_TABLE + " after the save");
        WaitUtil.waitForCondition(() -> !rulesTree.getSelectedItemText().isEmpty(), SAVE_SETTLE_MS, 250,
                "Waiting for a table to be selected in the tree");
        assertThat(rulesTree.getSelectedItemText())
                .as(KEEPS_TABLE_OPEN)
                .isEqualTo(EDITED_TABLE);
    }

    private void addAndCheckProperty(EditorPage editorPage, String propertyName, String propertyTableName, String value) {
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();
        tableDetails.addProperty(propertyName);
        editAndCheckProperty(editorPage, propertyName, propertyTableName, value);
    }

    private void addAndCheckCheckboxProperty(EditorPage editorPage, String propertyName, String propertyTableName, Choice... choices) {
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();
        tableDetails.addProperty(propertyName);
        editAndCheckCheckboxProperty(editorPage, propertyName, propertyTableName, choices);
    }


    private void addAndCheckBooleanProperty(EditorPage editorPage, String propertyName, String propertyTableName, boolean value) {
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();
        tableDetails.addProperty(propertyName);
        editAndCheckBooleanProperty(editorPage, propertyName, propertyTableName, value);
    }

    private void addAndCheckDropdownProperty(EditorPage editorPage, String propertyName, String propertyTableName, String value) {
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();
        tableDetails.addProperty(propertyName);
        editAndCheckDropdownProperty(editorPage, propertyName, propertyTableName, value);
    }

    private void deletePropertyAndCheck(EditorPage editorPage, String propertyName, String propertyTableName) {
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();
        tableDetails.deleteProperty(propertyName);
        WaitUtil.sleep(300, "Waiting after deleting property");
        tableDetails.clickSaveBtn();

        assertThat(editorPage.getCenterTable().isPropertyPresent(propertyTableName))
                .as("Property '%s' should not be present", propertyTableName)
                .isFalse();
    }

    private record Choice(String label, String code) {
    }
}
