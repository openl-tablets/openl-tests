package domain.ui.webstudio.pages.mainpages;

import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.common.MultiselectArrayEditorComponent;
import domain.ui.webstudio.components.common.RangeEditorComponent;
import domain.ui.webstudio.components.common.SaveChangesComponent;
import domain.ui.webstudio.components.common.SyncChangesDialogComponent;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.*;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftProjectModuleSelectorComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.BasePage;
import helpers.utils.WaitUtil;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
public class EditorPage extends BasePage {

    private static final Logger LOGGER = LogManager.getLogger(EditorPage.class);
    private static final int OVERVIEW_PROBE_MS = 3000;

    private EditorLeftProjectModuleSelectorComponent editorLeftProjectModuleSelectorComponent;
    private EditorLeftRulesTreeComponent editorLeftRulesTreeComponent;
    private RightTableDetailsComponent rightTableDetailsComponent;
    private TabSwitcherComponent tabSwitcherComponent;
    private TableComponent centerTable;
    private ProblemsPanelComponent problemsPanelComponent;
    private ProjectDetailsComponent projectDetailsComponent;
    private AddModuleComponent addModulePopupComponent;
    private EditorToolbarPanelComponent editorToolbarPanelComponent;
    private EditorTableActionsPanelComponent editorTableActionsPanelComponent;
    private TestResultValidationComponent testResultValidationComponent;
    private EditorMainContentProblemsPanelComponent editorMainContentProblemsPanelComponent;
    private ProjectModuleDetailsComponent projectModuleDetailsComponent;
    private SyncChangesDialogComponent syncChangesDialogComponent;
    private SaveChangesComponent saveChangesComponent;
    private EditProjectDialogComponent editProjectDialogComponent;
    private ExportProjectDialogComponent exportProjectDialogComponent;
    private CopyModuleDialogComponent copyModuleDialogComponent;
    private RemoveModuleDialogComponent removeModulePopupComponent;
    private CreateTableDialogComponent createTableDialogComponent;
    private TopProblemsPanelComponent topProblemsPanelComponent;
    private EditModuleDialogComponent editModuleDialogComponent;
    private WebElement exportProjectBtn;
    private WebElement editProjectIconTemplate;
    private WebElement projectHeaderTemplate;
    private WebElement moduleHeader;
    private WebElement copyModuleBtn;
    private ImportOpenApiDialogComponent importOpenApiDialogComponent;
    private OpenApiModuleSettingsDialogComponent openApiModuleSettingsDialogComponent;
    private WebElement openApiSectionHeader;
    private WebElement importOpenApiImg;
    private WebElement openApiPropertyValueTemplate;
    private WebElement migrateProjectBtn;
    private WebElement overviewSaveBtn;
    private WebElement migrateConfirmBtn;
    private ManageDependenciesDialogComponent manageDependenciesDialogComponent;
    private WebElement dependencyGraphSearch;
    private WebElement dependencyGraphSearchInput;
    private WebElement dependencyGraphOptionTemplate;
    private WebElement dependencyGraphOpenInEditorBtn;
    private WebElement refreshBtn;
    private SearchFilterComponent searchFilterComponent;
    private RangeEditorComponent rangeEditorComponent;
    private MultiselectArrayEditorComponent multiselectArrayEditorComponent;

    public EditorPage() {
        super();
        initializeComponents();
    }

    private void initializeComponents() {
        editorLeftProjectModuleSelectorComponent = new EditorLeftProjectModuleSelectorComponent();
        editorLeftRulesTreeComponent = new EditorLeftRulesTreeComponent();
        rightTableDetailsComponent = createScopedComponent(RightTableDetailsComponent.class, "xpath=//aside[@data-testid='table-details']", "rightTableDetailsComponent");
        tabSwitcherComponent = createScopedComponent(TabSwitcherComponent.class, "xpath=//ul[@role='menu' and contains(@class,'ant-menu-horizontal')]", "tabSwitcherComponent");
        centerTable = createScopedComponent(TableComponent.class, "xpath=//table[@data-testid='module-table']", "centerTable");
        editorToolbarPanelComponent = createScopedComponent(EditorToolbarPanelComponent.class, "xpath=//div[@id='tableToolbarPanel']", "editorToolbarPanelComponent");
        testResultValidationComponent = new TestResultValidationComponent();
        problemsPanelComponent = createScopedComponent(ProblemsPanelComponent.class, "xpath=//section[@data-testid='compile-problems']", "problemsPanelComponent");
        projectDetailsComponent = createScopedComponent(ProjectDetailsComponent.class, "xpath=//div[@class='page']", "projectDetailsComponent");
        addModulePopupComponent = createScopedComponent(AddModuleComponent.class, "xpath=//div[@id='editModulePopup_container']", "addModulePopupComponent");
        editorTableActionsPanelComponent = createScopedComponent(EditorTableActionsPanelComponent.class, "xpath=//div[@data-testid='table-edit-toolbar']", "editorTableActionsPanelComponent");
        editorMainContentProblemsPanelComponent = new EditorMainContentProblemsPanelComponent();
        projectModuleDetailsComponent = createScopedComponent(ProjectModuleDetailsComponent.class, "xpath=//div[contains(@class, 'ui-layout-center') and @id='content']", "projectModuleDetailsComponent");
        syncChangesDialogComponent = createScopedComponent(SyncChangesDialogComponent.class, "xpath=//div[@role='dialog' and .//form[@id='merge_branches_form']]", "syncChangesDialogComponent");
        saveChangesComponent = new SaveChangesComponent();
        editProjectDialogComponent = createScopedComponent(EditProjectDialogComponent.class, "xpath=//div[@id='editProjectPopup_content']", "editProjectDialogComponent");
        exportProjectDialogComponent = new ExportProjectDialogComponent();
        copyModuleDialogComponent = createScopedComponent(CopyModuleDialogComponent.class, "xpath=//div[@id='copyModulePopup_container']", "copyModuleDialogComponent");
        removeModulePopupComponent = createScopedComponent(RemoveModuleDialogComponent.class, "xpath=//div[@id='removeModulePopup_content']", "removeModulePopupComponent");
        createTableDialogComponent = createScopedComponent(CreateTableDialogComponent.class,
                "xpath=//div[contains(@class,'ant-modal')][.//div[contains(@class,'ant-modal-title')][contains(normalize-space(.),'Create Table')]]",
                "createTableDialogComponent");
        topProblemsPanelComponent = new TopProblemsPanelComponent();
        editModuleDialogComponent = createScopedComponent(EditModuleDialogComponent.class, "xpath=//div[@id='editModulePopup_container']", "editModuleDialogComponent");
        projectHeaderTemplate = new WebElement(getPage(), "xpath=//div[@id='content']//h1[@class='page-header']/span[text()='%s']/..", "projectHeaderTemplate");
        editProjectIconTemplate = new WebElement(getPage(), "xpath=//div[@id='content']//h1[@class='page-header']/span[text()='%s']/..//a[@title='Edit']", "editProjectIconTemplate");
        moduleHeader = new WebElement(getPage(), "xpath=//div[@id='content']//div[@class='page editable']/h1", "moduleHeader");
        copyModuleBtn = new WebElement(getPage(), "xpath=//div[@id='content']//div[@class='page editable']/h1//a[@title='Copy']", "copyModuleBtn");
        importOpenApiDialogComponent = createScopedComponent(ImportOpenApiDialogComponent.class, "xpath=//div[@data-testid='overview-panel']", "importOpenApiDialogComponent");
        openApiModuleSettingsDialogComponent = createScopedComponent(OpenApiModuleSettingsDialogComponent.class, "xpath=//form[@id='generateOpenAPIForm']", "openApiModuleSettingsDialogComponent");
        openApiSectionHeader = new WebElement(getPage(), "xpath=//div[@data-testid='overview-panel']//button[normalize-space()='OpenAPI']", "openApiSectionHeader");
        importOpenApiImg = new WebElement(getPage(), "xpath=//button[@data-testid='overview-edit']", "importOpenApiImg");
        openApiPropertyValueTemplate = new WebElement(getPage(), "xpath=//div[@data-testid='overview-panel']//dt[normalize-space()='%s']/following-sibling::dd[1]", "openApiPropertyValueTemplate");
        migrateProjectBtn = new WebElement(getPage(), "xpath=//button[@data-testid='overview-migrate']", "migrateProjectBtn");
        overviewSaveBtn = new WebElement(getPage(), "xpath=//button[@data-testid='overview-save']", "overviewSaveBtn");
        migrateConfirmBtn = new WebElement(getPage(), "xpath=//div[contains(@class,'ant-modal-confirm')]//button[normalize-space()='Migrate']", "migrateConfirmBtn");
        manageDependenciesDialogComponent = createScopedComponent(ManageDependenciesDialogComponent.class, "xpath=//div[@id='manageDependenciesPopup_container']", "manageDependenciesDialogComponent");
        dependencyGraphSearch = new WebElement(getPage(), "xpath=//div[@data-testid='table-graph-search']", "dependencyGraphSearch");
        dependencyGraphSearchInput = new WebElement(getPage(), "xpath=//div[@data-testid='table-graph-search']//input", "dependencyGraphSearchInput");
        dependencyGraphOptionTemplate = new WebElement(getPage(), "xpath=//div[contains(@class,'ant-select-item-option-content') and normalize-space(.)='%s']", "dependencyGraphOptionTemplate");
        dependencyGraphOpenInEditorBtn = new WebElement(getPage(), "xpath=//button[normalize-space(.)='Open in editor']", "dependencyGraphOpenInEditorBtn");
        refreshBtn = new WebElement(getPage(), "xpath=//button[@data-testid='module-refresh']", "refreshBtn");
        searchFilterComponent = new SearchFilterComponent();
        rangeEditorComponent = createScopedComponent(RangeEditorComponent.class, "xpath=//div[@data-testid='range-editor']", "rangeEditorComponent");
        multiselectArrayEditorComponent = createScopedComponent(MultiselectArrayEditorComponent.class,
                "xpath=//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]",
                "multiselectArrayEditorComponent");
    }

    public EditorToolbarPanelComponent getEditorToolbarPanelComponent() {
        waitUntilSpinnerLoaded();
        return editorToolbarPanelComponent;
    }

    public void navigateToProjectsInBreadcrumbs() {
        getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
    }

    public EditorPage reloadPage() {
        page.reload();
        waitUntilSpinnerLoaded();
        return this;
    }

    public TableComponent getCenterTable() {
        waitUntilSpinnerLoaded();
        WaitUtil.waitForCondition(
                () -> centerTable.isVisible(),
                5000, 250,
                "Waiting for center table to become visible"
        );
        WaitUtil.sleep(500, "Waiting for center table to fully load");
        return centerTable;
    }

    /** Opens what the project says about itself for writing, on the project's own screen. */
    public EditProjectDialogComponent openEditProjectDialog(String projectName) {
        new EditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        importOpenApiImg.waitForVisible(DEFAULT_TIMEOUT_MS);
        importOpenApiImg.click();
        editProjectDialogComponent.waitForDialogToAppear();
        return editProjectDialogComponent;
    }

    public CopyModuleDialogComponent openCopyModuleDialog() {
        moduleHeader.hover();
        copyModuleBtn.click();
        copyModuleDialogComponent.waitForDialogToAppear();
        return copyModuleDialogComponent;
    }

    /**
     * Opens the project's settings for writing, which is where the OpenAPI specification it reads, the mode
     * it reads it in and the modules it writes into are named.
     */
    public ImportOpenApiDialogComponent openImportOpenApiDialog() {
        importOpenApiImg.waitForVisible(DEFAULT_TIMEOUT_MS);
        importOpenApiImg.click();
        importOpenApiDialogComponent.waitForVisible();
        expandOpenApiSection();
        return importOpenApiDialogComponent;
    }

    /**
     * Moves the workbooks lying in the project's root under {@code rules/} and writes the rules.xml that
     * names them. A project without one declares nothing, and writing a descriptor without moving them
     * first would stop the project finding them, so the card withholds its settings until this is done.
     *
     * <p>The move is refused for a project holding workbooks no descriptor may name, and the card says so
     * on the button instead of offering it; that reason is reported rather than waited out.
     */
    public void migrateProject() {
        if (importOpenApiImg.isVisible(OVERVIEW_PROBE_MS)) {
            return;
        }
        migrateProjectBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (!migrateProjectBtn.isEnabled()) {
            throw new AssertionError("The project card offers the move but refuses to make it: the project holds "
                    + "workbooks a descriptor may not name");
        }
        migrateProjectBtn.click();
        migrateConfirmBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        migrateConfirmBtn.click();
        waitUntilSpinnerLoaded();
        WaitUtil.requireCondition(() -> importOpenApiImg.isVisible(OVERVIEW_PROBE_MS), DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the project settings to be offered for writing after the move");
    }

    /** Each section of the panel can be folded away, so the one the settings stand in is opened first. */
    private void expandOpenApiSection() {
        openApiSectionHeader.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (!"true".equals(openApiSectionHeader.getAttribute("aria-expanded"))) {
            openApiSectionHeader.click();
        }
    }

    public String getOpenApiPropertyValue(String propertyName) {
        waitUntilOverviewIsRead();
        expandOpenApiSection();
        return openApiPropertyValueTemplate.format(propertyName).getText().trim();
    }

    /** Whether the card says anything under that name at all; a setting the project does not hold is left out. */
    public boolean hasOpenApiProperty(String propertyName) {
        waitUntilOverviewIsRead();
        expandOpenApiSection();
        return openApiPropertyValueTemplate.format(propertyName).isVisible(OVERVIEW_PROBE_MS);
    }

    /**
     * Waits for the card to be back to what it says rather than what it is being told. While the settings
     * are open for writing every row holds the control it is written with, and a row read then would read
     * as the whole of what that control offers.
     */
    private void waitUntilOverviewIsRead() {
        WaitUtil.requireCondition(() -> !overviewSaveBtn.isVisible(OVERVIEW_PROBE_MS / 3), DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the project settings to be kept and read back");
    }

    /**
     * The mode the project reads its specification in.
     *
     * <p>The card names the mode the project declares. A project that declares none reads in the mode the
     * engine falls back to, and the card says so by naming no mode at all; that mode is then read from the
     * settings, which stand at it. A card naming no specification either is not a project to ask this of.
     */
    public String getOpenApiMode() {
        waitUntilOverviewIsRead();
        expandOpenApiSection();
        WebElement shownMode = openApiPropertyValueTemplate.format("Mode");
        if (shownMode.isVisible(OVERVIEW_PROBE_MS)) {
            return shownMode.getText().trim();
        }
        if (!openApiPropertyValueTemplate.format("File").isVisible(OVERVIEW_PROBE_MS)) {
            throw new AssertionError("The project card declares no OpenAPI specification");
        }
        String mode = openImportOpenApiDialog().getSelectedMode();
        importOpenApiDialogComponent.clickCancel();
        waitUntilOverviewIsRead();
        return mode;
    }

    /** Whether the project says it declares no OpenAPI specification. */
    public boolean isOpenApiPropertiesSectionEmpty() {
        return new WebElement(page, "xpath=//span[@data-testid='openapi-none']", "openApiNone").isVisible(DEFAULT_TIMEOUT_MS / 5);
    }

    /**
     * Reads the module again from what stands on disk, which is what a reader presses after the project has
     * been changed from outside. The module is compiled anew, so the screen is waited for afterwards.
     */
    public void refresh() {
        refreshBtn.click(DEFAULT_TIMEOUT_MS);
        waitUntilSpinnerLoaded();
        problemsPanelComponent.waitForCompilationToComplete();
    }

    public void clickTableInDependenciesView(String tableName) {
        dependencyGraphSearch.click(); // open the antd Select so its search input becomes active
        dependencyGraphSearchInput.fill(tableName);
        dependencyGraphOptionTemplate.format(tableName).click();
        dependencyGraphOpenInEditorBtn.click();
        WaitUtil.sleep(1000, "Waiting for table to open from dependency graph");
    }

    public ManageDependenciesDialogComponent openManageDependenciesDialog() {
        manageDependenciesDialogComponent.openForEditing();
        return manageDependenciesDialogComponent;
    }
}