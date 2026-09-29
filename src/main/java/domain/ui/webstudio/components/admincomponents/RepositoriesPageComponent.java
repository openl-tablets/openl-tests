package domain.ui.webstudio.components.admincomponents;

import com.microsoft.playwright.Locator;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.BaseComponent;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RepositoriesPageComponent extends BaseComponent {

    private static final int UNSAVED_CHANGES_PROBE_MS = 2000;
    private static final String DROPDOWN_OPTION = "//div[contains(concat(' ', normalize-space(@class), ' '), ' ant-select-item-option ')]";
    private static final String ACTIVE_OPTION = "//div[contains(@class,'ant-select-item-option-active')]";
    private static final String UNSAVED_CHANGES_PROMPT = "xpath=//div[contains(@class,'ant-modal-container')]"
            + "[.//div[contains(@class,'ant-modal-title') and normalize-space()='You have unsaved changes']]";

    private WebElement designRepositoriesTab;
    private WebElement deploymentRepositoriesTab;
    private WebElement addRepositoryBtn;
    private WebElement addDeploymentRepositoryBtn;

    private WebElement remoteRepositoryNameField;
    private WebElement remoteRepositoryTypeSelector;
    private WebElement remoteRepositoryCheckBox;
    private WebElement remoteRepositoryPathField;
    private WebElement remoteRepositoryLoginField;
    private WebElement remoteRepositoryPasswordField;
    private WebElement remoteRepositoryBranchField;
    private WebElement remoteRepositoryProtectedBranchesField;
    private WebElement flatFolderStructureCheckBox;
    private WebElement secureConnectionCheckbox;
    private WebElement applyChangesBtn;
    private WebElement designRepoActiveTab;
    private WebElement typeOption;
    private WebElement repositoryTabTemplate;
    private WebElement deleteRepositoryBtnTemplate;
    private List<WebElement> repositoryTypeOptions;
    private WebElement settingField;
    private WebElement settingSelector;
    private WebElement unsavedChangesPrompt;
    private WebElement unsavedChangesLeaveBtn;

    public RepositoriesPageComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public RepositoriesPageComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        designRepositoriesTab = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-tab') and contains(text(),'Design Repositories')]", "designRepositoriesTab");
        deploymentRepositoriesTab = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-tab') and contains(text(),'Deployment Repositories')]", "deploymentRepositoriesTab");
        addRepositoryBtn = createScopedElement("xpath=.//button[./span[contains(text(),'Add Design Repository')]]", "addRepositoryBtn");
        addDeploymentRepositoryBtn = createScopedElement("xpath=.//button[./span[contains(text(),'Add Deployment Repository')]]", "addDeploymentRepositoryBtn");

        remoteRepositoryNameField = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='name']", "remoteRepositoryNameField");
        remoteRepositoryTypeSelector = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//div[contains(@class,'ant-select') and .//input[@id='type']]//div[contains(@class,'ant-select-content')]", "remoteRepositoryTypeSelector");
        remoteRepositoryCheckBox = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='settings_remoteRepository']", "remoteRepositoryCheckBox");
        remoteRepositoryPathField = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='settings_uri']", "remoteRepositoryPathField");
        remoteRepositoryLoginField = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='settings_login']", "remoteRepositoryLoginField");
        remoteRepositoryPasswordField = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='settings_password']", "remoteRepositoryPasswordField");
        remoteRepositoryBranchField = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='settings_branch']", "remoteRepositoryBranchField");
        remoteRepositoryProtectedBranchesField = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='settings_protectedBranches']", "remoteRepositoryProtectedBranchesField");
        flatFolderStructureCheckBox = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='settings_flatFolderStructure']", "flatFolderStructureCheckBox");
        secureConnectionCheckbox = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='settings_secure']", "secureConnectionCheckbox");
        applyChangesBtn = createScopedElement("xpath=.//button[@type='submit']", "applyChangesBtn");
        designRepoActiveTab = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//div[contains(@class,'ant-tabs-tab-active') and .//*[text()='%s']]", "designRepoActiveTab");
        typeOption = new WebElement(page, "xpath=//div[contains(@class,'ant-select-item-option') and .//div[text()='%s']]", "typeOption");
        repositoryTabTemplate = new WebElement(page, "xpath=//div[contains(@class,'ant-tabs-card')]//div[contains(@class,'ant-tabs-nav-list')]//div[contains(@class,'ant-tabs-tab') and .//*[text()='%s']]", "repositoryTab");
        deleteRepositoryBtnTemplate = new WebElement(page, "xpath=//div[contains(@class,'ant-tabs-card')]//div[contains(@class,'ant-tabs-nav-list')]//div[contains(@class,'ant-tabs-tab') and .//*[text()='%s']]//button[contains(@class,'ant-tabs-tab-remove')]", "deleteRepositoryBtn");
        repositoryTypeOptions = createElementList("xpath=//div[contains(@class,'ant-select-dropdown') and not(contains(@class,'ant-select-dropdown-hidden'))]//div[contains(@class,'ant-select-item') and contains(@class,'ant-select-item-option') and not(contains(@class,'ant-select-item-option-content'))]", "repoTypeOptions");
        settingField = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//input[@id='settings_%s']", "settingField");
        settingSelector = createScopedElement("xpath=.//div[contains(@class,'ant-tabs-card')]//div[contains(@class,'ant-select') and .//input[@id='settings_%s']]//div[contains(@class,'ant-select-content')]", "settingSelector");
        unsavedChangesPrompt = new WebElement(page, UNSAVED_CHANGES_PROMPT, "unsavedChangesPrompt");
        unsavedChangesLeaveBtn = new WebElement(page, UNSAVED_CHANGES_PROMPT + "//button[normalize-space()='OK']", "unsavedChangesLeaveBtn");
    }

    public void deleteRepository(String repositoryName, User user) {
        repositoryTabTemplate.format(repositoryName).hover();
        deleteRepositoryBtnTemplate.format(repositoryName).click();
        getModalOkBtn().waitForVisible().click();
        relogin(user);
    }

    public RepositoriesPageComponent clickDesignRepositoriesTab() {
        designRepositoriesTab.click();
        return this;
    }

    public RepositoriesPageComponent clickAddRepository() {
        addRepositoryBtn.sleep(500).click();
        return this;
    }

    public RepositoriesPageComponent setRepositoryPath(String path) {
        remoteRepositoryPathField.fillSequentially(path);
        return this;
    }

    public void addDesignRepository() {
        addDesignRepository("Design1");
    }

    public void addDesignRepository(String expectedTabName) {
        clickDesignRepositoriesTab();
        clickAddRepository();
        designRepoActiveTab.format(expectedTabName).waitForVisible(5000);
    }

    public RepositoriesPageComponent selectDesignRepositoryByName(String name) {
        clickDesignRepositoryTab(name);
        return waitForRepositoryShown(name);
    }

    public RepositoriesPageComponent selectDesignRepositoryLeavingChanges(String name) {
        clickDesignRepositoryTab(name);
        if (isUnsavedChangesPromptShown()) {
            unsavedChangesLeaveBtn.click();
        }
        return waitForRepositoryShown(name);
    }

    public RepositoriesPageComponent clickDesignRepositoryTab(String name) {
        repositoryTabTemplate.format(name).waitForVisible(DEFAULT_TIMEOUT_MS).click();
        return this;
    }

    public boolean isUnsavedChangesPromptShown() {
        return unsavedChangesPrompt.isVisible(UNSAVED_CHANGES_PROBE_MS);
    }

    private RepositoriesPageComponent waitForRepositoryShown(String name) {
        WaitUtil.requireCondition(() -> name.equals(remoteRepositoryNameField.getCurrentInputValue()), DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the settings of the repository '" + name + "' to be shown");
        return this;
    }

    public String getDesignRepositoryNameValue() {
        return remoteRepositoryNameField.getCurrentInputValue();
    }

    public String getDesignRepositoryType() {
        return remoteRepositoryTypeSelector.getText();
    }

    public List<String> getAllRepositoryTypes() {
        remoteRepositoryTypeSelector.click();
        List<String> types = repositoryTypeOptions.stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
        page.keyboard().press("Escape");
        return types;
    }

    public boolean isDesignRepositoryRemote() {
        return remoteRepositoryCheckBox.isChecked();
    }

    public String getDesignRepositoryLocalPath() {
        return remoteRepositoryPathField.getCurrentInputValue();
    }

    public String getDesignRepositoryUrl() {
        return remoteRepositoryPathField.getCurrentInputValue();
    }

    public RepositoriesPageComponent setDesignRepositoryType(String type) {
        remoteRepositoryTypeSelector.click();
        typeOption.format(type).click();
        return this;
    }

    public String getSettingValue(String setting) {
        return settingField.format(setting).getCurrentInputValue();
    }

    public boolean isSettingChecked(String setting) {
        return settingField.format(setting).isChecked();
    }

    public RepositoriesPageComponent setS3Connection(String serviceEndpoint, String bucketName, String regionName,
                                                     String accessKey, String secretKey) {
        settingField.format("serviceEndpoint").waitForVisible(3000).fillSequentially(serviceEndpoint);
        settingField.format("bucketName").fillSequentially(bucketName);
        selectRegion(regionName);
        settingField.format("accessKey").fillSequentially(accessKey);
        settingField.format("secretKey").fillSequentially(secretKey);
        return this;
    }

    public List<String> getRegionOptions() {
        String list = openList(settingField.format("regionName"));
        WaitUtil.waitForCondition(() -> page.locator(list + DROPDOWN_OPTION).count() > 0,
                DEFAULT_TIMEOUT_MS, 200, "Waiting for the Region name list to offer the regions");
        List<String> regions = page.locator(list + DROPDOWN_OPTION).all().stream()
                .map(option -> option.getAttribute("title")).toList();
        page.keyboard().press("Escape");
        return regions;
    }

    public RepositoriesPageComponent setSseAlgorithm(String algorithm) {
        pickInSelect(settingField.format("sseAlgorithm"), algorithm);
        return this;
    }

    public String getSseAlgorithm() {
        return settingSelector.format("sseAlgorithm").getText().trim();
    }

    private void selectRegion(String regionName) {
        Locator active = page.locator(openList(settingField.format("regionName")) + ACTIVE_OPTION);
        WaitUtil.requireCondition(() -> {
            String marked = active.getAttribute("title");
            if (regionName.equals(marked)) {
                return true;
            }
            page.keyboard().press("ArrowDown");
            WaitUtil.waitForCondition(() -> !Objects.equals(marked, active.getAttribute("title")), 2000, 20,
                    "Waiting for the next region to be marked");
            return false;
        }, 60000, 10, "Moving through the Region name list to '" + regionName + "'");
        page.keyboard().press("Enter");
    }

    private String openList(WebElement selectInput) {
        selectInput.click();
        WaitUtil.requireCondition(() -> "true".equals(selectInput.getAttribute("aria-expanded")), DEFAULT_TIMEOUT_MS, 100,
                "Waiting for the list to open");
        return "xpath=//div[contains(concat(' ', normalize-space(@class), ' '), ' ant-select-dropdown ')]"
                + "[.//*[@id='" + selectInput.getAttribute("aria-controls") + "']]";
    }

    public RepositoriesPageComponent setDesignRepositoryJdbcUrl(String url) {
        remoteRepositoryPathField.waitForVisible(3000).clear();
        remoteRepositoryPathField.fillSequentially(url);
        return this;
    }

    public RepositoriesPageComponent setDesignRepositoryLogin(String login) {
        remoteRepositoryLoginField.clear();
        remoteRepositoryLoginField.fillSequentially(login);
        return this;
    }

    public RepositoriesPageComponent setDesignRepositoryPassword(String password) {
        remoteRepositoryPasswordField.clear();
        remoteRepositoryPasswordField.fillSequentially(password);
        return this;
    }

    public RepositoriesPageComponent setProtectedBranches(String pattern) {
        remoteRepositoryProtectedBranchesField.clear();
        remoteRepositoryProtectedBranchesField.fillSequentially(pattern);
        return this;
    }

    public String getProtectedBranches() {
        return remoteRepositoryProtectedBranchesField.getCurrentInputValue();
    }

    public RepositoriesPageComponent setSecureConnection(boolean enabled) {
        if (enabled != secureConnectionCheckbox.isChecked()) {
            secureConnectionCheckbox.click();
        }
        return this;
    }

    public RepositoriesPageComponent setFlatFolderStructure(boolean flat) {
        if (flat != flatFolderStructureCheckBox.isChecked()) {
            flatFolderStructureCheckBox.click();
        }
        return this;
    }

    public void createDesignRepository(String repositoryUrl, String login, String password, String branch, User user) {
        addDesignRepository();
        remoteRepositoryPathField.waitForVisible(1000).sleep(500).clear();
        remoteRepositoryPathField.fillSequentially(repositoryUrl);
        remoteRepositoryLoginField.fillSequentially(login);
        remoteRepositoryPasswordField.fillSequentially(password);
        remoteRepositoryBranchField.fillSequentially(branch);
        applyChangesAndRelogin(user);
    }

    public void applyChangesAndRelogin(User user) {
        applyChangesBtn.click();
        getModalOkBtn().click();
        WaitUtil.sleep(2000, "Wait for changes to re-login before reloading the page");
        WaitUtil.retryAction(() -> DriverPool.getPage().reload(), 10000, 1000, "Reload page after applying changes");
        WaitUtil.sleep(1000, "Wait for changes to re-login after reloading the page");
        WebElement loginForm = new WebElement(DriverPool.getPage(), "xpath=//input[@id='username']", "loginFormProbe");
        if (loginForm.isVisible(3000)) {
            relogin(user);
        }
    }

    public void clickDeploymentRepositoriesTab() {
        deploymentRepositoriesTab.click();
    }

    public void clickAddDeploymentRepository() {
        addDeploymentRepositoryBtn.sleep(500).click();
    }

    public void addDeploymentRepository() {
        clickDeploymentRepositoriesTab();
        clickAddDeploymentRepository();
    }

    public void createH2DeploymentRepository(User user) {
        String repoUrl = String.format("jdbc:h2:mem:repo%s;DB_CLOSE_DELAY=-1", StringUtil.generateUniqueName(5));
        addDeploymentRepository();
        remoteRepositoryPathField.fillSequentially(repoUrl);
        applyChangesAndRelogin(user);
    }

    private void relogin(User user) {
        new LoginService(page).login(UserService.getUser(user));
    }
}
