package domain.ui.webstudio.components.admincomponents;

import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.common.TableComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import helpers.utils.WaitUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class UsersPageComponent extends BaseComponent {

    private static final int COL_USERNAME = 1;
    private static final int COL_FULLNAME = 2;
    private static final int COL_EMAIL = 3;
    private static final int COL_ACTIONS = 4;

    private TableComponent usersTable;
    private WebElement addUserBtn;

    private WebElement drawer;
    private WebElement drawerCloseBtn;
    private WebElement usernameField;
    private WebElement emailField;
    private WebElement passwordField;
    private WebElement firstNameField;
    private WebElement lastNameField;
    private WebElement displayNamePatternDropdown;
    private WebElement displayNameField;
    private WebElement drawerSubmitBtn;
    private WebElement cancelBtn;

    private WebElement projectsTab;
    private WebElement deployReposTab;
    private WebElement addRoleBtn;
    private WebElement roleRepositoryTemplate;
    private WebElement roleNameTemplate;
    private WebElement deployRepoRepositoryTemplate;
    private WebElement deployRepoRoleTemplate;
    private WebElement projectSelectorTemplate;
    private WebElement projectRoleSelectorTemplate;
    private WebElement removeRoleBtn;

    private WebElement resendVerificationEmailBtn;
    private WebElement tooltip;

    private WebElement errorNotification;
    private WebElement errorDescription;

    public UsersPageComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public UsersPageComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        usersTable = createScopedComponent(TableComponent.class, "xpath=.//table", "usersTable");
        addUserBtn = createScopedElement("xpath=.//button[./span[text()='Add User']]", "addUserBtn");

        drawer = new WebElement(page, "xpath=//div[contains(@class,'ant-drawer-open')]", "drawer");
        drawerCloseBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-drawer-open')]//button[contains(@class,'ant-drawer-close')]", "drawerCloseBtn");

        usernameField = new WebElement(page, "xpath=//input[@id='username']", "usernameField");
        emailField = new WebElement(page, "xpath=//input[@id='email']", "emailField");
        passwordField = new WebElement(page, "xpath=//input[@id='password']", "passwordField");
        firstNameField = new WebElement(page, "xpath=//input[@id='firstName']", "firstNameField");
        lastNameField = new WebElement(page, "xpath=//input[@id='lastName']", "lastNameField");
        displayNamePatternDropdown = new WebElement(page, "xpath=//input[@id='displayNameSelect']", "displayNamePatternDropdown");
        displayNameField = new WebElement(page, "xpath=//input[@id='displayName']", "displayNameField");

        drawerSubmitBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-drawer-open')]//button[./span[text()='Save']]", "drawerSubmitBtn");
        cancelBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-drawer-open')]//button[./span[text()='Cancel']]", "cancelBtn");

        projectsTab = new WebElement(page, "xpath=//div[@data-node-key='projects']", "projectsTab");
        deployReposTab = new WebElement(page, "xpath=//div[@data-node-key='deployRepos']", "deployReposTab");
        addRoleBtn = new WebElement(page, "xpath=//div[@role='tabpanel' and @aria-hidden='false']//button[./span[contains(text(),'Add Role')]]", "addRoleBtn");
        roleRepositoryTemplate = new WebElement(page, "xpath=//input[@id='designRepos_%s_id']", "designRepoSelectorTemplate");
        roleNameTemplate = new WebElement(page, "xpath=//input[@id='designRepos_%s_role']", "roleSelectorTemplate");
        deployRepoRepositoryTemplate = new WebElement(page, "xpath=//input[@id='deployRepos_%s_id']", "deployRepoSelectorTemplate");
        deployRepoRoleTemplate = new WebElement(page, "xpath=//input[@id='deployRepos_%s_role']", "deployRepoRoleTemplate");
        projectSelectorTemplate = new WebElement(page, "xpath=//input[@id='projects_%s_id']", "projectSelectorTemplate");
        projectRoleSelectorTemplate = new WebElement(page, "xpath=//input[@id='projects_%s_role']", "projectRoleSelectorTemplate");
        removeRoleBtn = new WebElement(page, "xpath=//button[./span[contains(@aria-label,'delete')] and ancestor::div[contains(@class,'ant-form-item')]]", "removeRoleBtn");

        resendVerificationEmailBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-drawer-open')]//button[./span[text()='Resend Verification Email']]", "resendVerificationEmailBtn");
        tooltip = new WebElement(page, "xpath=//div[@role='tooltip'][contains(@class,'ant-tooltip-container')][not(ancestor::div[contains(@class,'ant-tooltip-hidden')])]", "tooltip");

        errorNotification = new WebElement(page, "xpath=//div[contains(@class,'ant-notification-notice-error')]", "errorNotification");
        errorDescription = new WebElement(page, "xpath=//div[contains(@class,'ant-notification-notice-error')]//div[contains(@class,'ant-notification-notice-description')]", "errorDescription");
    }

    private WebElement rowElement(int row, String xpath) {
        return usersTable.getCell(row, COL_USERNAME).child("xpath=ancestor::tr[1]" + xpath);
    }

    private com.microsoft.playwright.Locator rowActionButton(int row, String icon) {
        return rowElement(row, "//button[.//*[@data-icon='" + icon + "']]").getLocator();
    }

    public int getUserRow(String username) {
        WaitUtil.waitForCondition(() -> getAllUsernames().contains(username), DEFAULT_TIMEOUT_MS, 100, "Waiting for user '" + username + "' to appear in users table");
        int rowCount = usersTable.getRowsCount();
        for (int i = 1; i <= rowCount; i++) {
            String cellText = usersTable.getCellText(i, COL_USERNAME);
            if (cellText.contains(username)) {
                return i;
            }
        }
        throw new NoSuchElementException("User not found in table: " + username);
    }

    public boolean isUserShownOnline(String username) {
        return rowElement(getUserRow(username),
                "//sup[contains(@class,'ant-badge-dot')][contains(@class,'ant-badge-color-green')]")
                .getLocator().count() > 0;
    }

    public String getEmailNotVerifiedTooltip(String username) {
        rowElement(getUserRow(username), "//td[" + COL_EMAIL + "]//*[@data-icon='exclamation-circle']").hover();
        tooltip.waitForVisible(DEFAULT_TIMEOUT_MS);
        return tooltip.getInnerText().trim();
    }

    public boolean isResendVerificationEmailEnabled() {
        resendVerificationEmailBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        return resendVerificationEmailBtn.isEnabled();
    }

    public UsersPageComponent clickResendVerificationEmail() {
        resendVerificationEmailBtn.click();
        return this;
    }

    public UsersPageComponent reloadPage() {
        page.reload();
        waitUntilSpinnerLoaded();
        return this;
    }

    public String getUsernameFromRow(int rowIndex) {
        return usersTable.getCellText(rowIndex, COL_USERNAME).replaceAll("\\s+", " ").trim();
    }

    public String getEmailFromRow(int rowIndex) {
        return usersTable.getCellText(rowIndex, COL_EMAIL);
    }

    public String getFullNameFromRow(int rowIndex) {
        return usersTable.getCellText(rowIndex, COL_FULLNAME);
    }

    public boolean isUserInList(String username) {
        try {
            getUserRow(username);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public List<String> getAllUsernames() {
        List<String> usernames = new ArrayList<>();
        int rowCount = usersTable.getRowsCount();
        for (int i = 1; i <= rowCount; i++) {
            usernames.add(getUsernameFromRow(i));
        }
        return usernames;
    }

    public int getUsersCount() {
        return usersTable.getRowsCount();
    }

    public boolean areActionsAvailableForUser(String username) {
        int row = getUserRow(username);
        return rowActionButton(row, "edit").isVisible() && rowActionButton(row, "delete").isVisible();
    }

    public UsersPageComponent clickEditUser(String username) {
        int row = getUserRow(username);
        rowActionButton(row, "edit").first().click();
        drawer.waitForVisible(3000);
        WaitUtil.sleep(150, "Waiting for user edit drawer to fully open");
        return this;
    }

    public void clickDeleteUser(String username) {
        int usersBefore = usersTable.getRowsCount();
        int row = getUserRow(username);
        rowActionButton(row, "delete").first().click();
        getModalOkBtn().click();
        WaitUtil.waitForCondition(() -> {
            int usersAfter = usersTable.getRowsCount();
            return usersAfter == usersBefore - 1;
            }, 5000, 100, "Waiting for user deletion to complete");

    }

    public List<String> getUserGroups(String username) {
        return rowElement(getUserRow(username), "//span[contains(@class,'ant-tag')]").getLocator().allInnerTexts()
                .stream()
                .map(String::trim)
                .toList();
    }

    public boolean isShowLessGroupsVisible(String username) {
        return showLessGroupsBtn(username).isVisible();
    }

    public UsersPageComponent expandUserGroups(String username, String badge) {
        WebElement showLessBtn = showLessGroupsBtn(username);
        rowElement(getUserRow(username), "//span[contains(@class,'ant-tag') and normalize-space()='" + badge + "']")
                .click();
        showLessBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        return this;
    }

    public UsersPageComponent collapseUserGroups(String username) {
        WebElement showLessBtn = showLessGroupsBtn(username);
        showLessBtn.click();
        showLessBtn.waitForHidden(DEFAULT_TIMEOUT_MS);
        return this;
    }

    private WebElement showLessGroupsBtn(String username) {
        return rowElement(getUserRow(username), "//button[./span[text()='Show less']]");
    }

    public UsersPageComponent clickAddUser() {
        addUserBtn.click();
        drawer.waitForVisible(3000);
        return this;
    }

    public UsersPageComponent setUsername(String username) {
        usernameField.fill(username);
        return this;
    }

    public String getUsername() {
        return usernameField.getAttribute("value");
    }

    public UsersPageComponent setEmail(String email) {
        emailField.fill(email);
        return this;
    }

    public String getEmail() {
        return emailField.getAttribute("value");
    }

    public UsersPageComponent setFirstName(String firstName) {
        firstNameField.fill(firstName);
        return this;
    }

    public String getFirstName() {
        return firstNameField.getAttribute("value");
    }

    public UsersPageComponent setLastName(String lastName) {
        lastNameField.fill(lastName);
        return this;
    }

    public String getLastName() {
        return lastNameField.getAttribute("value");
    }

    public UsersPageComponent setPassword(String password) {
        passwordField.fill(password);
        return this;
    }

    public UsersPageComponent clickAddRoleBtn() {
        addRoleBtn.click();
        return this;
    }

    public UsersPageComponent setRoleRepository(int row, String repositoryName) {
        pickInSelect(roleRepositoryTemplate.format(row), repositoryName);
        return this;
    }

    public UsersPageComponent setRole(int row, String role) {
        pickInSelect(roleNameTemplate.format(row), role);
        return this;
    }

    public UsersPageComponent clickProjectsTab() {
        projectsTab.click();
        return this;
    }

    public UsersPageComponent clickDeployReposTab() {
        deployReposTab.click();
        return this;
    }

    public UsersPageComponent setDeployRoleRepository(int row, String repositoryName) {
        pickInSelect(deployRepoRepositoryTemplate.format(row), repositoryName);
        return this;
    }

    public UsersPageComponent setDeployRole(int row, String role) {
        pickInSelect(deployRepoRoleTemplate.format(row), role);
        return this;
    }

    public UsersPageComponent setProject(int row, String projectName) {
        pickInSelect(projectSelectorTemplate.format(row), projectName);
        return this;
    }

    public UsersPageComponent setProjectRole(int row, String roleName) {
        pickInSelect(projectRoleSelectorTemplate.format(row), roleName);
        return this;
    }

    public String getRoleRepository(int row) {
        return roleRepositoryTemplate.format(row).child("xpath=/..").getAttribute("title");
    }

    public String getRole(int row) {
        return roleNameTemplate.format(row).child("xpath=/..").getAttribute("title");
    }

    public UsersPageComponent clearAllRoles() {
        while (removeRoleBtn.isVisible(1000)) {
            removeRoleBtn.click();
        }
        return this;
    }

    public void saveUser() {
        saveUser(true);
    }

    public void saveUser(boolean waitForDrawerToGetHidden) {
        fillRequiredIdentityFields();
        drawerSubmitBtn.click();
        if (waitForDrawerToGetHidden)
            drawer.waitForHidden(3000);
    }

    private void fillRequiredIdentityFields() {
        if (emailField.isEnabled() && emailField.getCurrentInputValue().isBlank()) {
            emailField.fill(getUsername() + "@test.com");
        }
        if (displayNameField.isEnabled() && displayNameField.getCurrentInputValue().isBlank()) {
            String composed = (getFirstName() + " " + getLastName()).trim();
            displayNameField.fill(composed.isBlank() ? getUsername() : composed);
        }
    }

    public void cancelUser() {
        WaitUtil.retryOnException(() -> {
            page.keyboard().press("Escape");
            drawer.waitForHidden(2000);
            return true;
        }, 10000, 500, "Closing user drawer via Escape");
    }
}
