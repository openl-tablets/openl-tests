package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.stream.Stream;

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
    private WebElement rulesModuleError;
    private WebElement dataModuleError;
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
        rulesModuleError = new WebElement(page, OVERVIEW + "//*[@data-testid='edit-openapi-algorithm-error']", "rulesModuleError");
        dataModuleError = new WebElement(page, OVERVIEW + "//*[@data-testid='edit-openapi-model-error']", "dataModuleError");
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
        errorMsgs = createElementList("xpath=//div[contains(@class,'ant-form-item-explain-error')]"
                + " | //div[contains(@class,'ant-notification-notice-description')]"
                + " | //div[contains(@class,'ant-notification-notice-message')]", "errorMsgs");
    }

    public void waitForFilePathField() {
        openApiFilePathInput.waitForVisible(DEFAULT_TIMEOUT_MS);
    }

    public void selectGenerateFromRules() {
        generateFromRulesRadio.click();
    }

    public void setOpenApiFilePath(String path) {
        pickInSelect(openApiFilePathInput, path);
    }

    public List<String> getOfferedSpecifications() {
        openForWriting();
        openApiFilePathInput.waitForVisible(DEFAULT_TIMEOUT_MS);
        openApiFilePathInput.click();
        String openList = "//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]";
        new WebElement(page, "xpath=" + openList, "specificationsList").waitForVisible(DEFAULT_TIMEOUT_MS);
        WebElement offered = new WebElement(page,
                "xpath=" + openList + "//div[contains(@class,'ant-select-item-option-content')]",
                "offeredSpecifications");
        offered.isVisible(CANCEL_PROBE_MS);
        List<String> names = offered.getLocator().allInnerTexts().stream()
                .map(String::trim).filter(text -> !text.isEmpty()).toList();
        page.keyboard().press("Escape");
        return names;
    }

    public String getSelectedMode() {
        WebElement selected = new WebElement(page, MODE + "//label[contains(@class,'ant-segmented-item-selected')]", "selectedMode");
        selected.waitForVisible(DEFAULT_TIMEOUT_MS);
        return selected.getText().trim();
    }

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

    public void clickImportReconciliation() {
        selectReconciliationMode();
        importReconciliationBtn.click();
        waitUntilSpinnerLoaded();
    }

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

    public List<String> getModuleNameErrors() {
        return Stream.of(rulesModuleError, dataModuleError)
                .filter(WebElement::isVisible)
                .map(error -> error.getText().trim())
                .toList();
    }

    public boolean isSaveEnabled() {
        return importReconciliationBtn.isEnabled();
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
