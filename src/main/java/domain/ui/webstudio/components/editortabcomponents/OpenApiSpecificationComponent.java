package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

public class OpenApiSpecificationComponent extends BaseComponent {

    private static final int GENERATION_TIMEOUT_MS = 90000;
    private static final String NOTICE = "xpath=//div[contains(@class,'ant-notification-notice-wrapper')]"
            + "[.//div[contains(@class,'ant-notification-notice-title')][starts-with(normalize-space(),'%s')]]";

    private final WebElement generateBtn;
    private final WebElement generatingBtn;
    private final WebElement generatedNotices;
    private final WebElement failedNotices;

    public OpenApiSpecificationComponent() {
        super(DriverPool.getPage());
        generateBtn = new WebElement(getPage(), "xpath=//button[@data-testid='openapi-write']", "generateSpecificationBtn");
        generatingBtn = new WebElement(getPage(),
                "xpath=//button[@data-testid='openapi-write'][contains(@class,'ant-btn-loading')]", "generatingSpecificationBtn");
        generatedNotices = new WebElement(getPage(), String.format(NOTICE, "The OpenAPI specification was"),
                "specificationGeneratedNotices");
        failedNotices = new WebElement(getPage(), String.format(NOTICE, "Failed to write the OpenAPI specification"),
                "specificationFailedNotices");
    }

    public String generateSpecification() {
        generateBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        int generatedBefore = generatedNotices.getLocator().count();
        int failedBefore = failedNotices.getLocator().count();
        generateBtn.click();
        WaitUtil.requireCondition(
                () -> !generatingBtn.exists() && (generatedNotices.getLocator().count() > generatedBefore
                        || failedNotices.getLocator().count() > failedBefore),
                GENERATION_TIMEOUT_MS, 250, "Waiting for the OpenAPI specification to be generated");
        if (failedNotices.getLocator().count() > failedBefore) {
            throw new AssertionError("The studio refused to generate the OpenAPI specification: "
                    + failedNotices.getLocator().last().innerText().trim().replace("\n", " "));
        }
        return generatedNotices.getLocator().last().innerText().trim().replace("\n", " ");
    }
}
