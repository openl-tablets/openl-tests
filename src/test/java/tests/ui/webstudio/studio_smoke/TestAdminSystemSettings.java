package tests.ui.webstudio.studio_smoke;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.AuditLogPageComponent;
import domain.ui.webstudio.components.admincomponents.AuditLogPageComponent.AuditEvent;
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
    private static final String RETENTION_REFUSED = "Write a period longer than zero, such as 90d, 2w, 6m or 1y, or leave it empty";
    private static final String VERIFY_ON_EDIT = "compile.auto";

    @Test
    @TestCaseId({"IPBQA-30651", "EPBDS-17079"})
    @Description("System Settings - Test Dispatching Validation, Verify on Edit, Thread Number and audit log retention validation; "
            + "the audit log records each settings change and the restarts that end the sessions (EPBDS-14601, EPBDS-17027)")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
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
        systemSettings.setAuditRetention("90");
        assertThat(systemSettings.getAuditRetentionError())
                .as("A retention of 90 without its unit must be refused under the field")
                .isEqualTo(RETENTION_REFUSED);
        systemSettings.setAuditRetention("");
        systemSettings.waitForAuditRetentionErrorHidden();

        AuditLogPageComponent auditLog = editorPage.openUserMenu().navigateToAdministration().navigateToAuditLogPage();
        List<AuditEvent> events = auditLog.getEvents();
        assertThat(events).as("A settings save restarts OpenL Studio: %s", events)
                .anyMatch(event -> event.action().equals("OpenL Studio restarted"));
        assertThat(events.stream().filter(event -> byAdmin(event, "Settings changed") && event.object().contains(VERIFY_ON_EDIT))
                        .map(auditLog::getDetails))
                .as("The save that turns Verify on Edit off names it with its value before and after")
                .anyMatch(details -> details.contains(VERIFY_ON_EDIT + ": true → false"));
        assertThat(events.stream().filter(event -> byAdmin(event, "Session ended")).map(auditLog::getDetails))
                .as("The restart ends the session of admin with that reason")
                .anyMatch(details -> details.contains("OpenL Studio restarted"));
    }

    private static boolean byAdmin(AuditEvent event, String action) {
        return event.user().equals("admin") && event.action().equals(action);
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
