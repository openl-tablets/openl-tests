package domain.ui.webstudio.pages.mainpages;

import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.admincomponents.AdminNavigationComponent;
import domain.ui.webstudio.components.admincomponents.AuditLogPageComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.admincomponents.EmailPageComponent;
import domain.ui.webstudio.components.admincomponents.MyProfilePageComponent;
import domain.ui.webstudio.components.admincomponents.NotificationPageComponent;
import domain.ui.webstudio.components.admincomponents.PersonalAccessTokenPageComponent;
import domain.ui.webstudio.components.admincomponents.RepositoriesPageComponent;
import domain.ui.webstudio.components.admincomponents.SecurityPageComponent;
import domain.ui.webstudio.components.admincomponents.SystemSettingsPageComponent;
import domain.ui.webstudio.components.admincomponents.TagsPageComponent;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.pages.BasePage;
import lombok.Getter;

public class AdminPage extends BasePage {

    @Getter
    private AdminNavigationComponent adminNavigationComponent;
    @Getter
    private TabSwitcherComponent tabSwitcherComponent;
    private EmailPageComponent emailPageComponent;
    private SystemSettingsPageComponent systemSettingsPageComponent;
    private UsersPageComponent usersPageComponent;
    private SecurityPageComponent securityPageComponent;
    private MyProfilePageComponent myProfilePageComponent;
    private RepositoriesPageComponent repositoriesPageComponent;
    private NotificationPageComponent notificationPageComponent;
    private TagsPageComponent tagsPageComponent;
    private PersonalAccessTokenPageComponent personalAccessTokenPageComponent;
    private AuditLogPageComponent auditLogPageComponent;

    private WebElement adminNavigation;
    private WebElement contentContainer;

    public AdminPage() {
        super();
        initializeAdminComponents();
    }

    private void initializeAdminComponents() {
        adminNavigation = new WebElement(page, "xpath=//div[@id='main-menu']", "Admin Navigation Container");
        contentContainer = new WebElement(page, "xpath=//div[@id='content']", "Content Container");

        adminNavigationComponent = new AdminNavigationComponent(adminNavigation);
        tabSwitcherComponent = createScopedComponent(TabSwitcherComponent.class, "xpath=//ul[@role='menu' and contains(@class,'ant-menu-horizontal')]", "tabSwitcherComponent");
        emailPageComponent = new EmailPageComponent(contentContainer);
        systemSettingsPageComponent = new SystemSettingsPageComponent(contentContainer);
        usersPageComponent = new UsersPageComponent(contentContainer);
        securityPageComponent = new SecurityPageComponent(contentContainer);
        myProfilePageComponent = new MyProfilePageComponent(contentContainer);
        repositoriesPageComponent = new RepositoriesPageComponent(contentContainer);
        notificationPageComponent = new NotificationPageComponent(contentContainer);
        tagsPageComponent = new TagsPageComponent(contentContainer);
        personalAccessTokenPageComponent = new PersonalAccessTokenPageComponent(contentContainer);
        auditLogPageComponent = new AuditLogPageComponent(contentContainer);
    }

    public EmailPageComponent navigateToEmailPage() {
        adminNavigationComponent.clickMail();
        return emailPageComponent;
    }

    public SystemSettingsPageComponent navigateToSystemSettingsPage() {
        adminNavigationComponent.clickSystem();
        return systemSettingsPageComponent;
    }

    public UsersPageComponent navigateToUsersPage() {
        adminNavigationComponent.clickUsers();
        return usersPageComponent;
    }

    public SecurityPageComponent navigateToSecurityPage() {
        adminNavigationComponent.clickSecurity();
        return securityPageComponent;
    }

    public MyProfilePageComponent navigateToMyProfilePage() {
        adminNavigationComponent.clickMyProfile();
        return myProfilePageComponent;
    }

    public PersonalAccessTokenPageComponent navigateToPersonalAccessTokensPage() {
        adminNavigationComponent.clickPersonalAccessTokens();
        return personalAccessTokenPageComponent;
    }

    public RepositoriesPageComponent navigateToRepositoriesPage() {
        adminNavigationComponent.clickRepositories();
        return repositoriesPageComponent;
    }

    public NotificationPageComponent navigateToNotificationPage() {
        adminNavigationComponent.clickNotification();
        return notificationPageComponent;
    }

    public TagsPageComponent navigateToTagsPage() {
        adminNavigationComponent.clickTags();
        return tagsPageComponent;
    }

    public AuditLogPageComponent navigateToAuditLogPage() {
        adminNavigationComponent.clickAuditLog();
        return auditLogPageComponent;
    }
}