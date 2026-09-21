package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.RightTableDetailsComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestVersioningByFolders extends BaseTest {

    private static final String MODULE_NAME = "TestModuleCategoryInheritedProperties";
    private static final String BASE_FOLDER = DECISION;
    private static final String VERSION_FOLDER = "MyRules1";
    private static final String VERSION_VALUE = "0.0.2";
    private static final String PROPERTY_NAME = "LOB";
    private static final String PROPERTY_TABLE_NAME = "lob";
    private static final String INHERITED_VALUE = "001";
    private static final String OVERRIDDEN_VALUE = "777";

    @Test
    @TestCaseId("IPBQA-30979")
    @Description("Versioning by folders: verify copied versions are grouped under a table folder, inherited "
            + "properties are preserved per version, and overriding a property in one version does not affect "
            + "the other version; Copy as New Version must deactivate the source table (regression guard for "
            + "EPBDS-16357).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testVersioningByFolders() {
        String projectName = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "TestModuleCategoryInheritedProperties.xlsx");
        EditorPage editorPage = new EditorPage();
        EditorLeftRulesTreeComponent rulesTree = editorPage.getEditorLeftRulesTreeComponent();
        RightTableDetailsComponent tableDetails = editorPage.getRightTableDetailsComponent();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE_NAME);

        rulesTree.setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(BASE_FOLDER)
                .selectItemInFolder(BASE_FOLDER, VERSION_FOLDER);
        verifyInheritedProperty(tableDetails, INHERITED_VALUE);

        editorPage.getEditorToolbarPanelComponent().copyTableAsNewVersion(VERSION_VALUE);
        editorPage.waitUntilSpinnerLoaded();
        editorPage.getProblemsPanelComponent().checkNoProblems();

        rulesTree.setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(BASE_FOLDER);

        assertThat(rulesTree.countLeavesNamed(VERSION_FOLDER))
                .as("Two versions of the table should stand in the rail after Copy as New Version")
                .isEqualTo(2);
        assertThat(rulesTree.countInactiveLeavesNamed(VERSION_FOLDER))
                .as("Copying as a new version must set the version copied from aside")
                .isEqualTo(1);

        rulesTree.selectLeafNamed(VERSION_FOLDER, 1);
        verifyInheritedProperty(tableDetails, INHERITED_VALUE);
        tableDetails.editTextProperty(PROPERTY_NAME, OVERRIDDEN_VALUE);
        tableDetails.clickSaveBtn();

        rulesTree.setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(BASE_FOLDER);
        List<String> versions = rulesTree.getAllEndNodesNames().stream()
                .filter(name -> name.startsWith(VERSION_FOLDER + " ["))
                .toList();

        assertThat(versions)
                .as("Both versions should still stand in the rail after saving an override")
                .hasSize(2);
        assertThat(String.join(" | ", versions))
                .as("Overriding a property in one version should not affect the other version")
                .contains(PROPERTY_TABLE_NAME + "=" + INHERITED_VALUE)
                .contains(PROPERTY_TABLE_NAME + "=" + OVERRIDDEN_VALUE);
    }

    private void verifyInheritedProperty(RightTableDetailsComponent tableDetails, String expectedValue) {
        assertThat(tableDetails.getPropertyValue(PROPERTY_NAME))
                .as("Inherited property value should be visible in table details")
                .contains(expectedValue);
        assertThat(tableDetails.isPropertyInherited(PROPERTY_NAME))
                .as("Inherited property should stay marked as inherited")
                .isTrue();
        assertThat(tableDetails.getInheritedPropertyTitle(PROPERTY_NAME))
                .as("Inherited property should keep saying where its value comes from")
                .isEqualTo("Inherited from the module properties table");
        assertThat(tableDetails.getGoToPropertiesTableArrow(PROPERTY_NAME).isVisible())
                .as("Inherited property should keep the arrow leading to the properties table it comes from")
                .isTrue();
    }
}
