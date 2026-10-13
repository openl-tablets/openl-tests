package tests.ui.webstudio.studio_smoke;

import configuration.annotations.KnownIssue;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.SystemSettingsPageComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestAdminSystemSettings extends BaseTest {

    private static final String THREAD_COUNT_REFUSED = "Number of threads must be positive integer";
    private static final List<String> INVALID_THREAD_COUNTS = List.of("aaa", "#%", "1.1", "-5");

    @Test
    @TestCaseId("IPBQA-30651")
    @Description("System Settings - Test Dispatching Validation, Verify on Edit, and Thread Number validation. "
            + "Fails on EPBDS-16902: a refused Apply names the internal class ApiHttpError in its notification "
            + "instead of Error.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue(value = "EPBDS-16902", failsWith = "ApiHttpError: Invalid date pattern")
    @KnownIssue(value = "EPBDS-16902", failsWith = "ApiHttpError: Invalid time pattern")
    @KnownIssue(value = "EPBDS-16902", failsWith = "ApiHttpError: Cannot be empty.")
    public void testSystemSettings() {
        String projectNameForVerification = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();

        SystemSettingsPageComponent systemSettings = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();
        systemSettings.setVerifyOnEdit(true);
        systemSettings.applySettingsAndRelogin(User.ADMIN);

        new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectNameForVerification, "Main");

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "Hello");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(6, 2, "1000", true);
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        systemSettings = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();
        systemSettings.setVerifyOnEdit(false);
        systemSettings.applySettingsAndRelogin(User.ADMIN);

        new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectNameForVerification, "Main");

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "Hello");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(4, 2, "Integer aaa", true);
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        editorPage.getProblemsPanelComponent().checkNoProblems();
        assertThat(editorPage.getEditorToolbarPanelComponent().isVerifyButtonPresent()).as("Verify button should be present when Verify on Edit is disabled").isTrue();
        editorPage.getEditorToolbarPanelComponent().clickVerify();
        WaitUtil.waitForCondition(() -> editorPage.getProblemsPanelComponent().getErrorsCount() == 1, 5000, 100, "Waiting for errors ti be listed...");
        assertThat(editorPage.getProblemsPanelComponent().getErrorsCount()).as("Should have 1 error after clicking Verify button").isEqualTo(1);

        systemSettings = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();
        systemSettings.setDispatchingValidation(true);
        systemSettings.clickApplyButton();

        String projectNameForDispatch = WorkflowService.loginCreateProjectFromExcelFile(User.ADMIN, "TestSystemSettings.xlsx");

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectNameForDispatch, "TestSystemSettings");

        editorPage.getEditorToolbarPanelComponent()
                .clickTestDropdown()
                .runTests();

        assertThat(editorPage.getTestResultValidationComponent().isTestTableFailed()).as("Tests should fail with dispatching validation enabled").isTrue();

        systemSettings = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();
        systemSettings.setDispatchingValidation(false);
        systemSettings.applySettingsAndRelogin(User.ADMIN);

        new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectNameForDispatch, "TestSystemSettings");

        editorPage.getEditorToolbarPanelComponent()
                .clickTestDropdown()
                .runTests();

        assertThat(editorPage.getTestResultValidationComponent().isTestTablePassed()).as("Tests should pass with dispatching validation disabled").isTrue();

        systemSettings = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();
        String originalThreadCount = systemSettings.getTestThreadCount();
        for (String invalidThreadCount : INVALID_THREAD_COUNTS) {
            validateThreadCountError(systemSettings, invalidThreadCount, originalThreadCount);
        }

        systemSettings = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();

        String originalDateFormat = systemSettings.getDateFormat();
        String originalTimeFormat = systemSettings.getTimeFormat();

        systemSettings.setDateFormat("yyyy-MM-dd");
        systemSettings.setTimeFormat("HH:mm:ss");
        systemSettings.applySettingsAndRelogin(User.ADMIN);

        systemSettings = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();
        assertThat(systemSettings.getDateFormat()).isEqualTo("yyyy-MM-dd");
        assertThat(systemSettings.getTimeFormat()).isEqualTo("HH:mm:ss");

        systemSettings.setDateFormat(originalDateFormat);
        systemSettings.setTimeFormat(originalTimeFormat);
        systemSettings.applySettingsAndRelogin(User.ADMIN);

        systemSettings = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();
        systemSettings.setDateFormat("abc");
        systemSettings.clickApplyButton();
        assertThat(systemSettings.getAllMessages())
                .as("A date format of 'abc' must be refused")
                .contains("Error: Invalid date pattern");

        systemSettings.setDateFormat(originalDateFormat);
        systemSettings.setTimeFormat("xyz");
        systemSettings.clickApplyButton();
        assertThat(systemSettings.getAllMessages())
                .as("A time format of 'xyz' must be refused")
                .contains("Error: Invalid time pattern");

        systemSettings.setTimeFormat(originalTimeFormat);
        systemSettings.setDateFormat("");
        systemSettings.clickApplyButton();
        assertThat(systemSettings.getAllMessages())
                .as("An empty date format must be refused")
                .contains("Error: Cannot be empty.");

        systemSettings.closeAllMessages();
        systemSettings.setDateFormat(originalDateFormat);
        systemSettings.setTimeFormat("");
        systemSettings.clickApplyButton();
        assertThat(systemSettings.getAllMessages())
                .as("An empty time format must be refused")
                .contains("Error: Cannot be empty.");
    }

    private void validateThreadCountError(SystemSettingsPageComponent systemSettings, String invalidValue,
                                          String validValue) {
        systemSettings.setTestThreadCount(invalidValue);
        systemSettings.clickApplyButton();
        assertThat(systemSettings.getTestThreadCountError())
                .as("A thread count of '%s' must be refused under the field", invalidValue)
                .isEqualTo(THREAD_COUNT_REFUSED);
        systemSettings.setTestThreadCount(validValue);
        systemSettings.waitForTestThreadCountErrorHidden();
    }
}
