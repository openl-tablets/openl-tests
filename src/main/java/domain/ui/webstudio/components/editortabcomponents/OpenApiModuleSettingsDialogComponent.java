package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * What the project asks before it writes the tables its specification describes: the plan of the writing.
 *
 * <p>The plan names each module the specification is generated into and says what becomes of the workbook
 * behind it — the one there is replaced, or one is added where none stands. Where the module is written is
 * the project's own business now: the settings name the module, and the project answers with the path.
 */
public class OpenApiModuleSettingsDialogComponent extends BaseComponent {

    private static final String PLAN_DIALOG = "xpath=//div[contains(@class,'ant-modal-confirm')]"
            + "[.//ul[@data-testid='openapi-generation-plan']]";
    private static final int PROBE_MS = 3000;

    private WebElement planBody;
    private WebElement generateBtn;
    private WebElement cancelBtn;
    private List<WebElement> planLines;

    public OpenApiModuleSettingsDialogComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public OpenApiModuleSettingsDialogComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        planBody = new WebElement(page, PLAN_DIALOG + "//div[contains(@class,'ant-modal-confirm-content')]", "openApiPlanBody");
        generateBtn = new WebElement(page, PLAN_DIALOG + "//button[contains(@class,'ant-btn-primary')]", "openApiGenerateBtn");
        cancelBtn = new WebElement(page, PLAN_DIALOG + "//div[contains(@class,'ant-modal-confirm-btns')]"
                + "//button[not(contains(@class,'ant-btn-primary'))]", "openApiPlanCancelBtn");
        planLines = createElementList(PLAN_DIALOG + "//ul[@data-testid='openapi-generation-plan']/li", "openApiPlanLines");
    }

    /** What the plan says, line by line, as it is read on the screen. */
    public String getContentText() {
        return Arrays.stream(planBody.getInnerTextAfterDelay(1000).split("\n"))
                .map(line -> line.replaceAll("\\s+", " ").trim())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    /** The modules the specification is written into, one line each. */
    public List<String> getPlanLines() {
        WaitUtil.waitForListNotEmpty(() -> planLines, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the plan of the generation to be drawn");
        return planLines.stream().map(WebElement::getText).map(line -> line.replaceAll("\\s+", " ").trim()).toList();
    }

    public String getImportButtonText() {
        return generateBtn.getText().trim();
    }

    /**
     * Goes ahead with the writing the plan describes, and waits for the question to be done with: the plan
     * stands over the project's screen, which cannot be read while it is there.
     */
    public void clickImportAndOverride() {
        generateBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        generateBtn.click();
        WaitUtil.requireCondition(() -> !generateBtn.isVisible(PROBE_MS), DEFAULT_TIMEOUT_MS * 2, 250,
                "Waiting for the tables the specification describes to be written");
        waitUntilSpinnerLoaded();
    }

    public void clickCancel() {
        cancelBtn.click();
    }

    /** What the project says went wrong, which it says in a notice of its own rather than beside a field. */
    public String getErrorMessage() {
        WebElement notice = new WebElement(page,
                "xpath=(//div[contains(@class,'ant-notification-notice-description')])[1]", "openApiGenerateError");
        return notice.isVisible(PROBE_MS) ? notice.getText().trim() : "";
    }

    public List<String> getErrorMessages() {
        List<WebElement> notices = createElementList(
                "xpath=//div[contains(@class,'ant-notification-notice-description')]"
                        + " | //div[contains(@class,'ant-notification-notice-message')]", "openApiGenerateErrors");
        WaitUtil.waitForListNotEmpty(() -> notices, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the project to say what went wrong");
        return notices.stream().map(WebElement::getText).map(String::trim).toList();
    }

    public boolean isVisible() {
        return generateBtn.isVisible(PROBE_MS);
    }

    public void waitForVisible() {
        generateBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
    }
}
