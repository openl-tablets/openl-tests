package tests.ui.webstudio.studio_smoke;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.createnewproject.ZipArchiveComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.COLUMN_MATCH;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.CONSTANTS;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DATA;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DATATYPE;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.METHOD;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.RUN;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.SPREADSHEET;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TBASIC;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.TEST;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.VOCABULARY;

public class TestTableIcons extends BaseTest {

    private static final String PROJECT_NAME = "TestTableIcons";
    private static final String ZIP_FILE_NAME = "TestTableIcons.zip";
    private static final String MODULE_NAME = "All_tables_type";

    private static final List<String> CATALOG_NAMES = Arrays.asList(
            DECISION, SPREADSHEET, TBASIC, COLUMN_MATCH,
            DATA, RUN, TEST, DATATYPE, VOCABULARY, METHOD, CONSTANTS
    );

    private static final Map<String, String> TABLE_NAMES_AND_ICONS = new HashMap<>() {{
        put("SimpleLookupTable", "table");
        put("SimpleRulesTable", "table");
        put("SmartLookup1", "table");
        put("SmartRules1", "table");
        put("SpreadsheetTable", "layout");
        put("TBasicTable", "apartment");
        put("ColumnMatchTable", "column-width");
        put("DataTable1", "database");
        put("RunTable", "caret-right");
        put("Test1", "check-square");
        put("Datatype1", "block");
        put("Vocabulary1", "block");
        put("MethodTable", "function");
        put("Constants", "layout");
    }};

    @Test
    @TestCaseId("IPBQA-25719")
    @Description("Verify that each table type has the correct icon in the rules tree")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTableIcons() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, PROJECT_NAME, ZIP_FILE_NAME);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(PROJECT_NAME, MODULE_NAME);

        EditorPage finalEditorPage = editorPage;
        finalEditorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);
        CATALOG_NAMES.forEach(catalog ->
            finalEditorPage.getEditorLeftRulesTreeComponent()
                    .expandFolderInTree(catalog)
        );

        TABLE_NAMES_AND_ICONS.forEach((tableName, expectedIcon) -> assertThat(
                finalEditorPage.getEditorLeftRulesTreeComponent().getTableIconName(tableName))
                .as("Table '%s' should wear the '%s' glyph", tableName, expectedIcon)
                .isEqualTo(expectedIcon));
    }
}
