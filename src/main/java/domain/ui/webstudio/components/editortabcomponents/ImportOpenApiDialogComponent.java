package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;

public class ImportOpenApiDialogComponent extends BaseComponent {

    private static final String OVERVIEW = "xpath=//div[@data-testid='overview-panel']";
    private static final String MODE = OVERVIEW + "//div[@data-testid='edit-openapi-mode']";
    private static final int CANCEL_PROBE_MS = 2000;

    private WebElement openApiFilePathInput;
    private WebElement generateFromRulesRadio;
    private WebElement uploadInRepositoryRadio;
    private WebElement reconciliationRadio;
    private WebElement generationRadio;
    private WebElement rulesModuleInput;
    private WebElement dataModuleInput;
    private WebElement importReconciliationBtn;
    private WebElement importTablesGenerationBtn;
    private WebElement createOrUpdateSchemaBtn;
    private WebElement cancelBtn;
    private WebElement editBtn;
    private WebElement sectionHeader;
    private WebElement errorMsg;
    private WebElement anyErrorMsg;
    private List<WebElement> errorMsgs;

    public ImportOpenApiDialogComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public ImportOpenApiDialogComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        openApiFilePathInput = new WebElement(page, OVERVIEW + "//div[@data-testid='edit-openapi-path']//input", "openApiFilePathInput");
        generateFromRulesRadio = new WebElement(page, OVERVIEW + "//button[@data-testid='openapi-write']", "generateFromRulesBtn");
        uploadInRepositoryRadio = new WebElement(page, OVERVIEW + "//div[@data-testid='edit-openapi-path']//input", "uploadInRepositoryInput");
        reconciliationRadio = new WebElement(page, MODE + "//label[contains(normalize-space(.),'Reconciliation')]", "reconciliationMode");
        generationRadio = new WebElement(page, MODE + "//label[contains(normalize-space(.),'Tables generation')]", "generationMode");
        rulesModuleInput = new WebElement(page, OVERVIEW + "//input[@data-testid='edit-openapi-algorithm']", "rulesModuleInput");
        dataModuleInput = new WebElement(page, OVERVIEW + "//input[@data-testid='edit-openapi-model']", "dataModuleInput");
        importReconciliationBtn = new WebElement(page, OVERVIEW + "//button[@data-testid='overview-save']", "saveOverviewBtn");
        importTablesGenerationBtn = new WebElement(page, "xpath=//button[@data-testid='openapi-generate']", "generateTablesBtn");
        createOrUpdateSchemaBtn = new WebElement(page, "xpath=//button[@data-testid='openapi-write']", "writeSchemaBtn");
        cancelBtn = new WebElement(page, OVERVIEW + "//button[@data-testid='overview-cancel']", "cancelBtn");
        editBtn = new WebElement(page, OVERVIEW + "//button[@data-testid='overview-edit']", "overviewEditBtn");
        sectionHeader = new WebElement(page, OVERVIEW + "//button[normalize-space()='OpenAPI']", "openApiSectionHeader");
        errorMsg = new WebElement(page, "xpath=(" + OVERVIEW.substring("xpath=".length())
                + "//div[contains(@class,'ant-form-item-explain-error')]"
                + " | //div[contains(@class,'ant-notification-notice-description')])[1]", "errorMsg");
        anyErrorMsg = new WebElement(page, "xpath=//div[contains(@class,'ant-form-item-explain-error')] | //div[contains(@class,'ant-notification-notice-description')]", "anyErrorMsg");
        // What the settings refuse is said beside the field it belongs to; what the server refuses arrives
        // as a notice of its own, so both are read.
        errorMsgs = createElementList("xpath=//div[contains(@class,'ant-form-item-explain-error')]"
                + " | //div[contains(@class,'ant-notification-notice-description')]"
                + " | //div[contains(@class,'ant-notification-notice-message')]", "errorMsgs");
    }

    /**
     * Waits for the field the specification is named in. The settings point the project at a file it already
     * holds, so there is no choice of source to make: naming the path is the whole of it.
     */
    public void waitForFilePathField() {
        openApiFilePathInput.waitForVisible(DEFAULT_TIMEOUT_MS);
    }


    /** Writes a specification out of the rules the project already holds. */
    public void selectGenerateFromRules() {
        generateFromRulesRadio.click();
    }

    /** Points the project at one of the specifications it already holds. */
    public void setOpenApiFilePath(String path) {
        pickInSelect(openApiFilePathInput, path);
    }

    /** The mode the settings currently stand at, as the switch shows it. */
    public String getSelectedMode() {
        WebElement selected = new WebElement(page, MODE + "//label[contains(@class,'ant-segmented-item-selected')]", "selectedMode");
        selected.waitForVisible(DEFAULT_TIMEOUT_MS);
        return selected.getText().trim();
    }

    /**
     * Opens the settings for writing, which the panel leaves as soon as an import is refused or kept. A
     * reader who wants to name them again presses Edit again, and so does this.
     */
    private void openForWriting() {
        if (editBtn.isVisible(CANCEL_PROBE_MS)) {
            editBtn.click();
        }
        sectionHeader.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (!"true".equals(sectionHeader.getAttribute("aria-expanded"))) {
            sectionHeader.click();
        }
    }

    public void selectReconciliationMode() {
        openForWriting();
        reconciliationRadio.click();
    }

    public void selectTablesGenerationMode() {
        openForWriting();
        generationRadio.click();
    }

    public void setRulesModuleName(String name) {
        rulesModuleInput.clear();
        rulesModuleInput.fillSequentially(name);
    }

    public String getRulesModuleName() {
        return rulesModuleInput.getCurrentInputValue();
    }

    public void setDataModuleName(String name) {
        dataModuleInput.clear();
        dataModuleInput.fillSequentially(name);
    }

    public String getDataModuleName() {
        return dataModuleInput.getCurrentInputValue();
    }

    /** Keeps what was named on the screen: the file, the mode and the modules it writes into. */
    public void clickImportReconciliation() {
        selectReconciliationMode();
        importReconciliationBtn.click();
        waitUntilSpinnerLoaded();
    }

    /** Keeps the settings and then writes the tables the specification describes. */
    public void clickImportTablesGeneration() {
        selectTablesGenerationMode();
        importReconciliationBtn.click();
        waitUntilSpinnerLoaded();
        importTablesGenerationBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        importTablesGenerationBtn.click();
        waitUntilSpinnerLoaded();
    }

    public void clickCreateOrUpdateSchema() {
        createOrUpdateSchemaBtn.click();
        waitUntilSpinnerLoaded();
    }

    /**
     * Leaves the settings without keeping them. Refusing what an import offered leaves them read again by
     * itself, and there is then nothing left to leave.
     */
    public void clickCancel() {
        if (cancelBtn.isVisible(CANCEL_PROBE_MS)) {
            cancelBtn.click();
        }
        WaitUtil.requireCondition(() -> !cancelBtn.isVisible(CANCEL_PROBE_MS), DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the settings to be left without being kept");
    }

    public String getErrorMessage() {
        return errorMsg.getTextAfterDelay(3000);
    }

    public List<String> getErrorMessages() {
        WaitUtil.waitForListNotEmpty(() -> errorMsgs, 5000, 250, "Waiting for error messages to appear");
        return errorMsgs.stream()
                .map(e -> e.getText().trim())
                .toList();
    }

    public String getAnyErrorMessage() {
        return anyErrorMsg.getTextAfterDelay(3000);
    }

    public boolean isReconciliationModeSelected() {
        return reconciliationRadio.isChecked();
    }

    public boolean isVisible() {
        return cancelBtn.isVisible(3000);
    }

    public void waitForVisible() {
        WaitUtil.waitForCondition(this::isVisible, 10000, 250, "Waiting for Import OpenAPI dialog to appear");
    }
}
