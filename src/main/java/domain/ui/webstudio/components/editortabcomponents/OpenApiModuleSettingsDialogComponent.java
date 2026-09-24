package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OpenApiModuleSettingsDialogComponent extends BaseComponent {

    public enum PlanModule {
        SERVICES("algorithm"),
        DATA_TYPES("model");

        private final String testId;

        PlanModule(String testId) {
            this.testId = testId;
        }
    }

    public enum NoticeTone {
        WARNING,
        SECONDARY
    }

    public record ModulePlan(NoticeTone tone, String notice, String name, String workbook) {
    }

    private static final String SUBMIT = "//button[@data-testid='openapi-generate-submit']";
    private static final String DIALOG = "xpath=//div[@role='dialog'][." + SUBMIT + "]";
    private static final String TYPOGRAPHY = "contains(concat(' ',normalize-space(@class),' '),' ant-typography ')";
    private static final String NAME = DIALOG + "//span[@data-testid='openapi-plan-%s-name']";
    private static final String REFUSAL = "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' ant-notification-notice ')]"
            + "[.//div[contains(@class,'ant-notification-notice-title')][normalize-space()='Failed to generate the tables']]";
    private static final int PROBE_MS = 3000;

    private WebElement generateBtn;
    private WebElement cancelBtn;
    private WebElement samePathError;
    private WebElement refusalDescriptions;

    public OpenApiModuleSettingsDialogComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public OpenApiModuleSettingsDialogComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        generateBtn = new WebElement(page, "xpath=" + SUBMIT, "openApiGenerateBtn");
        cancelBtn = new WebElement(page, DIALOG + "//div[contains(@class,'ant-modal-footer')]"
                + "//button[not(@data-testid='openapi-generate-submit')]", "openApiGenerateCancelBtn");
        samePathError = new WebElement(page, DIALOG + "//*[@data-testid='openapi-plan-same-path']", "openApiSamePathError");
        refusalDescriptions = new WebElement(page, REFUSAL + "//div[contains(@class,'ant-notification-notice-description')]",
                "openApiRefusalDescriptions");
    }

    private WebElement name(PlanModule module) {
        return new WebElement(page, String.format(NAME, module.testId), "openApiPlanName_" + module.testId);
    }

    private WebElement notice(PlanModule module) {
        return new WebElement(page, String.format(NAME, module.testId)
                + "/ancestor::div[./*[" + TYPOGRAPHY + "]][1]/*[" + TYPOGRAPHY + "]", "openApiPlanNotice_" + module.testId);
    }

    private WebElement workbook(PlanModule module) {
        return new WebElement(page, DIALOG + "//*[@data-testid='openapi-plan-" + module.testId + "-path']",
                "openApiPlanWorkbook_" + module.testId);
    }

    private WebElement workbookInput(PlanModule module) {
        return new WebElement(page, DIALOG + "//input[@data-testid='openapi-plan-" + module.testId + "-path']",
                "openApiPlanWorkbookInput_" + module.testId);
    }

    private WebElement workbookReset(PlanModule module) {
        return new WebElement(page, DIALOG + "//button[@data-testid='openapi-plan-" + module.testId + "-path-reset']",
                "openApiPlanWorkbookReset_" + module.testId);
    }

    private WebElement workbookError(PlanModule module) {
        return new WebElement(page, DIALOG + "//*[@data-testid='openapi-plan-" + module.testId + "-path-error']",
                "openApiPlanWorkbookError_" + module.testId);
    }

    public ModulePlan getModulePlan(PlanModule module) {
        WebElement notice = notice(module);
        return new ModulePlan(toneOf(notice.getAttribute("class")), notice.getText().trim(),
                getModuleName(module), getWorkbook(module));
    }

    private static NoticeTone toneOf(String noticeClass) {
        List<String> classes = List.of(noticeClass.trim().split("\\s+"));
        if (classes.contains("ant-typography-warning")) {
            return NoticeTone.WARNING;
        }
        if (classes.contains("ant-typography-secondary")) {
            return NoticeTone.SECONDARY;
        }
        throw new IllegalStateException("The notice of the module is drawn neither as a warning nor as secondary text: "
                + noticeClass);
    }

    public String getModuleName(PlanModule module) {
        return name(module).getText().trim();
    }

    public String getWorkbook(PlanModule module) {
        return isWorkbookEditable(module)
                ? workbookInput(module).getCurrentInputValue()
                : workbook(module).getText().trim();
    }

    public boolean isWorkbookEditable(PlanModule module) {
        name(module).waitForVisible(DEFAULT_TIMEOUT_MS);
        return workbookInput(module).exists();
    }

    public void setWorkbook(PlanModule module, String path) {
        WebElement input = workbookInput(module);
        input.clear();
        input.fillSequentially(path);
    }

    public void clearWorkbook(PlanModule module) {
        workbookInput(module).clear();
    }

    public void resetWorkbook(PlanModule module) {
        workbookReset(module).click();
    }

    public String getWorkbookError(PlanModule module) {
        WebElement error = workbookError(module);
        return error.isVisible(PROBE_MS) ? error.getText().trim() : "";
    }

    public String getSamePathError() {
        return samePathError.isVisible(PROBE_MS) ? samePathError.getText().trim() : "";
    }

    public boolean isGenerateEnabled() {
        generateBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        return generateBtn.isEnabled();
    }

    public String getGenerateButtonText() {
        return generateBtn.getText().trim();
    }

    public void clickGenerate() {
        generateBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        generateBtn.click();
    }

    public void clickImportAndOverride() {
        List<String> shownBefore = getErrorMessages();
        clickGenerate();
        Set<String> refusals = new LinkedHashSet<>();
        boolean closed = WaitUtil.waitForCondition(() -> {
            getErrorMessages().stream().filter(message -> !shownBefore.contains(message)).forEach(refusals::add);
            return !refusals.isEmpty() || !generateBtn.isVisible();
        }, DEFAULT_TIMEOUT_MS * 2L, 250, "Waiting for the tables the specification describes to be written");
        if (!refusals.isEmpty() || !closed) {
            throw new AssertionError("The Generate tables dialog stayed open after Generate; refusals shown: " + refusals);
        }
        waitUntilSpinnerLoaded();
    }

    public void clickCancel() {
        cancelBtn.click();
        generateBtn.waitForHidden(DEFAULT_TIMEOUT_MS);
    }

    public List<String> getErrorMessages() {
        return refusalDescriptions.getLocator().allTextContents().stream().map(String::trim).toList();
    }

    public List<String> getErrorMessagesUntilShown(String expected) {
        Set<String> seen = new LinkedHashSet<>();
        WaitUtil.waitForCondition(() -> {
            seen.addAll(getErrorMessages());
            return seen.contains(expected);
        }, DEFAULT_TIMEOUT_MS, 250, "Waiting for the generation to be refused with: " + expected);
        return List.copyOf(seen);
    }

    public boolean isVisible() {
        return generateBtn.isVisible(PROBE_MS);
    }

    public void waitForVisible() {
        generateBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
    }
}
