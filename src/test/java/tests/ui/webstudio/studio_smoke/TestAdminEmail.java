package tests.ui.webstudio.studio_smoke;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.admincomponents.EmailPageComponent;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.pages.mainpages.AdminPage;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.LoginPage;
import helpers.service.LoginService;
import helpers.service.MailMockService;
import helpers.service.UserService;
import helpers.utils.WaitUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAdminEmail extends BaseTest {

    private final MailMockService mailMock = new MailMockService();

    @BeforeMethod
    public void startMailMock() {
        mailMock.start();
    }

    @AfterMethod(alwaysRun = true)
    public void stopMailMock() {
        mailMock.stop();
    }

    @Test
    @TestCaseId({"IPBQA-32798", "EPBDS-16602", "EPBDS-16617"})
    @Description("Admin UI 'Email' page - email verification configuration and persistence, "
            + "backed by an ephemeral in-network SMTP mock (Mailpit) instead of an external mail provider. "
            + "EPBDS-16602: the unverified email icon in the Users list shows the readable tooltip 'Email not verified', not the raw key. "
            + "EPBDS-16617: Resend Verification Email is enabled on the untouched Edit User form and sends a new email.")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_MAIL_MOCK_PARAMS)
    public void testAdminEmail() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        AdminPage adminPage = editorPage.openUserMenu().navigateToAdministration();
        EmailPageComponent emailPageComponent = adminPage.navigateToEmailPage();

        assertThat(emailPageComponent.isEmailVerificationEnabled())
                .as("Email verification checkbox should be initially disabled")
                .isFalse();

        emailPageComponent.enableEmailVerificationWithCredentials(
                MailMockService.SMTP_URL, MailMockService.USERNAME, MailMockService.PASSWORD);

        WaitUtil.retryAction(() -> DriverPool.getPage().navigate(DriverPool.getAppUrl()), 10000, 1000,
                "Navigate to the app root after applying email settings restarts the security context");
        editorPage = new LoginPage().login(UserService.getUser(User.ADMIN));

        adminPage = editorPage.openUserMenu().navigateToAdministration();
        emailPageComponent = adminPage.navigateToEmailPage();

        WaitUtil.waitForCondition(emailPageComponent::isEmailVerificationEnabled, 2000, 100, "Waiting for Email verification checkbox to load its state");
        assertThat(emailPageComponent.isEmailVerificationEnabled())
                .as("Email verification should remain enabled after logout/login")
                .isTrue();
        assertThat(emailPageComponent.getEmailUrl())
                .as("Email URL should persist after logout/login")
                .isEqualTo(MailMockService.SMTP_URL);
        assertThat(emailPageComponent.getEmailUsername())
                .as("Email username should persist after logout/login")
                .isEqualTo(MailMockService.USERNAME);

        assertThat(emailPageComponent.getEmailPassword())
                .as("Saved password field should be empty after relogin")
                .isEmpty();

        emailPageComponent.setEmailPassword("qwerty");
        assertThat(emailPageComponent.getPasswordFieldType())
                .as("Password field should have type='password' initially (hidden)")
                .isEqualTo("password");
        assertThat(emailPageComponent.isPasswordVisible())
                .as("Password should be hidden initially")
                .isFalse();

        emailPageComponent.togglePasswordVisibility();
        assertThat(emailPageComponent.getPasswordFieldType())
                .as("Password field should be type='text' after clicking eye icon")
                .isEqualTo("text");
        assertThat(emailPageComponent.isPasswordVisible())
                .as("Password should not be hidden after clicking eye icon")
                .isTrue();

        int messagesBefore = mailMock.getReceivedMessagesCount();
        UserData mailUser = new UserData("mailTestUser", "password123");
        UsersPageComponent usersPage = adminPage.navigateToUsersPage();
        usersPage.clickAddUser()
                .setUsername(mailUser.getLogin())
                .setPassword(mailUser.getPassword())
                .setFirstName("Ann")
                .setLastName("Lee")
                .saveUser();

        WaitUtil.waitForCondition(() -> mailMock.getReceivedMessagesCount() > messagesBefore, 15000, 500,
                "Waiting for the verification email to be delivered to Mailpit");
        assertThat(mailMock.getReceivedMessagesCount())
                .as("Mailpit should have received the verification email for the newly created user")
                .isGreaterThan(messagesBefore);

        assertThat(usersPage.getEmailNotVerifiedTooltip(mailUser.getLogin()))
                .as("Tooltip of the unverified email icon should be the readable message, not the raw key")
                .isEqualTo("Email not verified");

        int messagesBeforeResend = mailMock.getReceivedMessagesCount();
        usersPage.clickEditUser(mailUser.getLogin());
        assertThat(usersPage.isResendVerificationEmailEnabled())
                .as("Resend Verification Email should be enabled on the untouched Edit User form")
                .isTrue();
        usersPage.clickResendVerificationEmail();

        WaitUtil.waitForCondition(() -> mailMock.getReceivedMessagesCount() > messagesBeforeResend, 15000, 500,
                "Waiting for the resent verification email to be delivered to Mailpit");
        assertThat(mailMock.getReceivedMessagesCount())
                .as("Mailpit should have received the resent verification email")
                .isGreaterThan(messagesBeforeResend);
    }
}
