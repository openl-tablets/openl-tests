package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.ModuleSearchResultsComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSearchOnProjectLevel extends BaseTest {

    private static final int TICKET_TEXT_LENGTH = 33009;
    private static final String TICKET_SENTENCE = "In show dull give need so held. One order all scale sense her gay style wrote. ";

    @Test
    @TestCaseId({"IPBQA-32590", "EPBDS-14069"})
    @Description("Search of the cells of the tables on module and project level. Enter in the Tables search field of the "
            + "top bar searches every cell of the module and lists the tables found in place of the table; Enter again "
            + "searches the whole project, including its other modules, and a third time the projects it depends on. "
            + "EPBDS-14069: a text of 33,009 characters typed into the field ends in a message to shorten it, with no "
            + "Oops screen.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testSimpleSearchOnProjectLevel() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        String nameProjectSpreadsheetSalary = "SpreadsheetSalary";
        String nameProjectSearchingByTag = "SearchingByTag";
        String nameProjectExample1BankRating = "Example1BankRating";

        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectSpreadsheetSalary, "RulesEditor.TestSearchOnProjectLevel.SpreadsheetSalaryAndTags.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectSearchingByTag, "RulesEditor.TestSearchOnProjectLevel.SearchingByTag.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectExample1BankRating, "RulesEditor.TestSearchOnProjectLevel.Example1BankRating.zip");

        new EditorPage();
        editorPage = new EditorPage();
        ModuleSearchResultsComponent search = new ModuleSearchResultsComponent();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.searchModule("median");
        assertThat(search.getSummary())
                .as("The band of the results tells how many tables of the module contain the text")
                .contains("1 table contains “median”");
        assertThat(search.getFoundTableNames()).containsExactly("SalaryCalc");
        assertThat(search.getGroupNames())
                .as("In a search of the module the tables are listed by the sheet they are written on")
                .containsExactly("Sheet2");

        search.searchModule("step1");
        assertThat(search.getFoundTableNames())
                .as("A cell of the table holds the text, whatever the case of the letters")
                .containsExactly("SalaryInfo");

        search.searchModule("SalaryCalc");
        assertThat(search.getFoundTableNames())
                .as("A table is found by the cells that call it too")
                .containsExactlyInAnyOrder("SalaryCalc", "SalaryInfo");

        search.searchModule("Spreadsheet Double SalaryInfo ( String name, Double [] salary )");
        assertThat(search.getFoundTableNames())
                .as("The header of a table is searched like any other cell")
                .containsExactly("SalaryInfo");

        search.searchModule("resul");
        assertThat(search.getFoundTableNames()).containsExactly("SalaryCalc");

        search.searchModule("step1");
        assertThat(search.getFoundTableNames()).containsExactly("SalaryInfo");
        assertThat(search.isWiderSearchOffered())
                .as("The band offers pressing Enter again to search the whole project")
                .isTrue();
        search.searchWholeProject();
        assertThat(search.getFoundTableNames())
                .as("Enter again lists the tables of the other module of the project too")
                .containsExactlyInAnyOrder("SalaryInfo", "RulesName");
        assertThat(search.getGroupNames())
                .as("In a search of the project the tables are listed by their module")
                .containsExactlyInAnyOrder("spreadsheet salary", "tag search");

        search.searchModule("balance");
        assertThat(search.isNothingFoundShown())
                .as("No table of this module holds the text")
                .isTrue();
        search.searchWholeProject();
        assertThat(search.isNothingFoundShown())
                .as("No table of this project holds the text")
                .isTrue();
        search.searchDependenciesToo();
        assertThat(search.getFoundTableNames())
                .as("A third Enter lists the tables of the projects it depends on")
                .contains("BalanceDynamicIndexCalculation", "BalanceQualityIndexCalculation");
        assertThat(search.getGroupNames())
                .as("In a wider search the tables are listed by their project and module")
                .contains("Example1BankRating / Bank Rating");
        assertThat(search.isWiderSearchOffered())
                .as("There is nothing wider than the projects it depends on")
                .isFalse();

        search.closeResults();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectExample1BankRating);
        search.searchModule("MONEY");
        assertThat(search.getFoundTableNames())
                .containsExactlyInAnyOrder("BalanceQualityIndexCalculation", "NetMoneyMarketLiabilitiesScore");

        search.searchModule("Balance");
        assertThat(search.getFoundTableNames())
                .contains("BalanceDynamicIndexCalculation", "BalanceQualityIndexCalculation");

        String ticketText = TICKET_SENTENCE.repeat(TICKET_TEXT_LENGTH / TICKET_SENTENCE.length() + 1)
                .substring(0, TICKET_TEXT_LENGTH);
        search.searchLongText(ticketText);
        assertThat(search.isFailureShown())
                .as("A text of %d characters must end in the message to shorten it, not in an Oops screen (EPBDS-14069)",
                        TICKET_TEXT_LENGTH)
                .isTrue();
        assertThat(search.getFailureText())
                .contains("The text is too long for the server");

        search.closeResults();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSearchingByTag);
        search.searchModule("testingFirst");
        assertThat(search.getFoundTableNames()).containsExactly("RulesName");
        search.openTable("RulesName");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getSelectedItemText())
                .as("Opening a table from the results must open that very table")
                .isEqualTo("RulesName");

        search.searchModule("money");
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNothingFoundMessage())
                .as("The search should say that nothing matched")
                .contains("No cell holds this text");

        search.searchModule("''");
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNothingFoundMessage())
                .as("The search should say that nothing matched")
                .contains("No cell holds this text");

        search.searchModule(" alert(\"zzz\") ");
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNothingFoundMessage())
                .as("The search should say that nothing matched")
                .contains("No cell holds this text");

        search.closeResults();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.searchModule("balance");
        search.searchWholeProject();
        search.searchDependenciesToo();
        search.openTable("BalanceQualityIndexCalculation");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getSelectedItemText())
                .as("A table of a project the searched one depends on opens in its own module")
                .isEqualTo("BalanceQualityIndexCalculation");
        editorPage.getCenterTable().editCell(3, 1, "Step 1");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();
        assertThat(editorPage.getCenterTable().getCellText(3, 1))
                .as("The table opened from the results keeps the edit after the project is saved")
                .isEqualTo("Step 1");
    }

    @Test
    @TestCaseId({"IPBQA-32590", "EPBDS-16932", "EPBDS-1656", "EPBDS-5111"})
    @Description("Finding tables by a property value on project level. The extended search by table properties is "
            + "removed, so a property value is searched for in the cells, since a property is written in a cell of the "
            + "table that declares it. EPBDS-1656: the value of the Tags property finds the tables tagged with it. "
            + "EPBDS-5111: a date typed in the field finds the tables whose properties hold that day. EPBDS-16932: a "
            + "search matching only a utility table tells how many utility tables the type filter hides and Show them "
            + "lists them.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testAdvancedSearchOnProjectLevel() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        String nameProjectTechnicalDateTags = "TechnicalDateTags";

        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectTechnicalDateTags, "RulesEditor.TestSearchOnProjectLevel.TechnicalDateTags.zip");

        new EditorPage();
        editorPage = new EditorPage();
        ModuleSearchResultsComponent search = new ModuleSearchResultsComponent();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectTechnicalDateTags);
        search.searchModule("xxx");
        search.searchWholeProject();
        assertThat(search.getFoundTableNames())
                .as("A tag value searched for in the cells at project scope must find the table tagged with it and "
                        + "no table tagged with another value (EPBDS-1656)")
                .containsExactly("TaggedSecond");
        assertThat(search.isTableFound("TaggedFirst")).isFalse();

        search.searchModule("xx");
        search.searchWholeProject();
        assertThat(search.getFoundTableNames())
                .as("A tag value contained in the values of other tables finds those tables too (EPBDS-1656)")
                .containsExactlyInAnyOrder("TaggedFirst", "TaggedSecond");
        assertThat(search.isTableFound("DatedGreeting")).isFalse();

        search.searchModule("3/12/2014");
        search.searchWholeProject();
        assertThat(search.getFoundTableNames())
                .as("A date typed in the field must find the table whose property holds that day (EPBDS-5111)")
                .containsExactly("DatedGreeting");

        search.searchModule("Technical");
        search.searchWholeProject();
        assertThat(search.getHiddenByTypeFilterMessage())
                .as("The search must tell that a utility table matched but is hidden by the type filter (EPBDS-16932)")
                .contains("1 table hidden by the type filter");
        assertThat(search.getFoundTablesCount())
                .as("A utility table of the type Other must not be listed while the type filter hides it (EPBDS-16932)")
                .isZero();
        search.clickShowHiddenTables();
        assertThat(search.getFoundTablesCount()).isEqualTo(1);
        assertThat(search.getFoundLines())
                .as("Show them must list the technical table in the results (EPBDS-16932)")
                .anyMatch(line -> line.contains("Technical Notes Of Greetings"));
    }
}
