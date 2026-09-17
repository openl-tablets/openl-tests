package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Locator;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;

/**
 * What the module screen reports about the table on it: the errors and warnings raised against that table,
 * listed under it. Errors are listed before warnings, and each list is left out when it would be empty.
 */
public class EditorMainContentProblemsPanelComponent extends BaseComponent {

    private static final String PANEL = "xpath=//section[@data-testid='table-problems']";
    private static final String BODY = PANEL + "//div[@data-testid='table-problems-body']";
    private static final int PROBE_MS = 1000;

    private WebElement problemsPanel;
    private WebElement toggle;
    private WebElement errorsCounter;
    private WebElement warningsCounter;
    private List<WebElement> errorMessages;
    private List<WebElement> warningsAfterErrors;
    private List<WebElement> warningsAlone;

    public EditorMainContentProblemsPanelComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public EditorMainContentProblemsPanelComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        problemsPanel = new WebElement(page, PANEL, "tableProblemsPanel");
        toggle = new WebElement(page, PANEL + "//button[@data-testid='table-problems-toggle']", "tableProblemsToggle");
        errorsCounter = new WebElement(page, PANEL + "//span[@data-testid='table-problems-errors']", "tableProblemsErrors");
        warningsCounter = new WebElement(page, PANEL + "//span[@data-testid='table-problems-warnings']", "tableProblemsWarnings");
        errorMessages = createElementList(BODY + "/ul[1]/li", "tableErrorMessages");
        warningsAfterErrors = createElementList(BODY + "/ul[2]/li", "tableWarningMessagesAfterErrors");
        warningsAlone = createElementList(BODY + "/ul[1]/li", "tableWarningMessages");
    }

    /** Opens the panel, which a reader may have folded away. */
    private void openPanel() {
        if (problemsPanel.isVisible(PROBE_MS) && !warningsCounter.isVisible(PROBE_MS / 4)
                && !errorsCounter.isVisible(PROBE_MS / 4)) {
            return;
        }
        if (problemsPanel.isVisible(PROBE_MS) && !new WebElement(page, BODY, "tableProblemsBody").isVisible(PROBE_MS / 2)) {
            toggle.click();
        }
    }

    /**
     * Opens the panel. The panel used to keep its errors and its warnings behind a tab each and now lists
     * both at once, so reaching either is the same thing: opening the panel.
     */
    public EditorMainContentProblemsPanelComponent clickErrorsTab() {
        openPanel();
        return this;
    }

    /** @see #clickErrorsTab() */
    public EditorMainContentProblemsPanelComponent clickWarningsTab() {
        openPanel();
        return this;
    }

    public EditorMainContentProblemsPanelComponent closePanel() {
        if (problemsPanel.isVisible(PROBE_MS)) {
            toggle.click();
        }
        return this;
    }

    public boolean isProblemsPanelVisible() {
        return problemsPanel.isVisible(PROBE_MS);
    }

    /** Whether the panel reports errors — what the Errors tab used to stand for. */
    public boolean isErrorsTabActive() {
        return errorsCounter.isVisible(PROBE_MS);
    }

    /** Whether the panel reports warnings — what the Warnings tab used to stand for. */
    public boolean isWarningsTabActive() {
        return warningsCounter.isVisible(PROBE_MS);
    }

    public EditorMainContentProblemsPanelComponent clickHideProblemsBtn() {
        return closePanel();
    }

    public EditorMainContentProblemsPanelComponent clickShowProblemsBtn() {
        openPanel();
        return this;
    }

    /** Opens what stands behind a message: the stack trace it was raised with. */
    public EditorMainContentProblemsPanelComponent expandProblemDescription(int elementPosition) {
        openPanel();
        WaitUtil.requireCondition(() -> {
            if (errorMessages.size() <= elementPosition) {
                return false;
            }
            Locator message = errorMessages.get(elementPosition).getLocator();
            if (isStacktraceShown(message)) {
                return true;
            }
            Locator stacktraceToggle = message.locator("xpath=.//button[contains(@data-testid,'-toggle')]");
            if (stacktraceToggle.count() == 0) {
                return false;
            }
            stacktraceToggle.first().click();
            return WaitUtil.waitForCondition(() -> isStacktraceShown(message), DEFAULT_TIMEOUT_MS / 4, 250,
                    "Waiting for the stack trace of the message to be read");
        }, DEFAULT_TIMEOUT_MS, 250, "Opening the description of problem " + elementPosition);
        return this;
    }

    public EditorMainContentProblemsPanelComponent hideProblemDescription(int elementPosition) {
        openPanel();
        WaitUtil.requireCondition(() -> {
            if (errorMessages.size() <= elementPosition) {
                return false;
            }
            Locator message = errorMessages.get(elementPosition).getLocator();
            if (!isStacktraceShown(message)) {
                return true;
            }
            Locator stacktraceToggle = message.locator("xpath=.//button[contains(@data-testid,'-toggle')]");
            if (stacktraceToggle.count() == 0) {
                return false;
            }
            stacktraceToggle.first().click();
            return !isStacktraceShown(message);
        }, DEFAULT_TIMEOUT_MS, 250, "Closing the description of problem " + elementPosition);
        return this;
    }

    public boolean isProblemDescriptionVisible(int elementPosition) {
        return WaitUtil.waitForCondition(
                () -> elementPosition < errorMessages.size()
                        && isStacktraceShown(errorMessages.get(elementPosition).getLocator()),
                PROBE_MS, 100, "Checking whether the description of problem " + elementPosition + " is open");
    }

    /** The trace is drawn below the message, in a box of its own, only while it is open. */
    private boolean isStacktraceShown(Locator message) {
        return message.locator("xpath=.//div[starts-with(@data-testid,'table-message-')]").count() > 0;
    }

    /**
     * Whether the table reports errors. Warnings are listed where the errors would be when there are none,
     * so what is asked first is whether any errors are reported at all.
     */
    public boolean isErrorMessageListPresent() {
        openPanel();
        return errorsCounter.isVisible(PROBE_MS)
                && WaitUtil.isListNotEmpty(() -> errorMessages, DEFAULT_TIMEOUT_MS, 250,
                        "Waiting for the errors of the table to be listed");
    }

    public List<String> getErrorMessages() {
        openPanel();
        if (!errorsCounter.isVisible(PROBE_MS)) {
            throw new AssertionError("The table reports no errors: " + problemsPanel.getText().trim());
        }
        WaitUtil.waitForListNotEmpty(() -> errorMessages, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the errors of the table to be listed");
        return errorMessages.stream().map(e -> CompileMessageReader.textOf(e.getLocator())).toList();
    }

    public List<String> getWarningMessages() {
        openPanel();
        List<WebElement> warnings = warningMessages();
        WaitUtil.waitForListNotEmpty(this::warningMessages, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the warnings of the table to be listed");
        return warnings.stream().map(e -> CompileMessageReader.textOf(e.getLocator())).toList();
    }

    /** Warnings are listed after the errors, so which list holds them depends on whether there are errors. */
    private List<WebElement> warningMessages() {
        return errorsCounter.isVisible(PROBE_MS / 4) ? warningsAfterErrors : warningsAlone;
    }
}
