package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The compilation problems of the project, as the module and project screens show them.
 *
 * <p>The panel is drawn only while the project has something to report: a project that compiles clean carries
 * no panel at all, which is what "no problems" looks like on these screens. Errors are listed before warnings,
 * and each list is paged, so reading them all means opening the panel and asking for the rest of each page.
 */
public class ProblemsPanelComponent extends BaseComponent {

    private static final String PANEL = "xpath=//section[@data-testid='compile-problems']";
    private static final String BODY = PANEL + "//div[@data-testid='compile-problems-body']";
    private static final String COMPILING_LABEL = "Compiling";
    private static final Set<String> FINISHED_STATES = Set.of("ok", "warnings", "errors");
    private static final long COMPILATION_TIMEOUT_MS = 30000;
    private static final int COMPILATION_POLL_MS = 250;
    private static final int PROBE_MS = 1000;
    private static final int PAGE_PROBE_MS = 500;
    private static final int MAX_PAGES = 100;

    private WebElement panel;
    private WebElement header;
    private WebElement errorsCounter;
    private WebElement warningsCounter;
    private WebElement body;
    private WebElement compileState;
    private WebElement compilingScreen;
    private List<WebElement> messageLists;
    private WebElement showMoreBtn;

    public ProblemsPanelComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public ProblemsPanelComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        panel = new WebElement(page, PANEL, "problemsPanel");
        header = new WebElement(page, PANEL + "//button[@data-testid='compile-problems-header']", "problemsPanelHeader");
        errorsCounter = new WebElement(page, PANEL + "//span[@data-testid='compile-problems-errors']", "errorsCounter");
        warningsCounter = new WebElement(page, PANEL + "//span[@data-testid='compile-problems-warnings']", "warningsCounter");
        body = new WebElement(page, BODY, "problemsPanelBody");
        compileState = new WebElement(page, "xpath=//span[@data-testid='module-compile-state']", "compileStateIndicator");
        compilingScreen = new WebElement(page, "xpath=//div[@data-testid='module-compiling']", "moduleCompilingScreen");
        messageLists = createElementList(BODY + "/ul", "problemLists");
        showMoreBtn = new WebElement(page, BODY + "//button[.//span[starts-with(normalize-space(),'Show')][contains(normalize-space(),'more')]]", "showMoreProblemsBtn");
    }

    /** What the compilation indicator reports, read from the screen rather than from the server. */
    public record ServerCompileStatus(String compileState, int errors, int warnings, int compiled, int total) {
        public boolean isCompiling() {
            return "compiling".equals(compileState);
        }

        public boolean isFinished() {
            return FINISHED_STATES.contains(compileState) && compiled == total;
        }
    }

    public void showProblemsPanel() {
        if (!panel.isVisible(PROBE_MS)) {
            return;
        }
        if (!"true".equals(header.getAttribute("aria-expanded"))) {
            header.click();
            body.waitForVisible(DEFAULT_TIMEOUT_MS);
        }
    }

    public void hideProblemsPanel() {
        if (panel.isVisible(PROBE_MS) && "true".equals(header.getAttribute("aria-expanded"))) {
            header.click();
        }
    }

    public int getErrorsCount() {
        waitForCompilationToComplete();
        return counterValue(errorsCounter);
    }

    public int getWarningsCount() {
        waitForCompilationToComplete();
        return counterValue(warningsCounter);
    }

    /** The count the panel shows, or none at all — the panel leaves out a counter that would read zero. */
    private int counterValue(WebElement counter) {
        if (!panel.isVisible(PROBE_MS) || !counter.isVisible(PROBE_MS)) {
            return 0;
        }
        String text = counter.getText().replaceAll("\\D+", "");
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    public boolean isCompilationInProgress() {
        return compilingScreen.isVisible(PROBE_MS / 4)
                || (compileState.isVisible(PROBE_MS / 4)
                    && String.valueOf(compileState.getAttribute("aria-label")).contains(COMPILING_LABEL));
    }

    public ServerCompileStatus fetchServerCompileStatusViaPage() {
        if (!compileState.isVisible(PROBE_MS)) {
            return null;
        }
        String label = String.valueOf(compileState.getAttribute("aria-label"));
        String state = label.contains(COMPILING_LABEL) ? "compiling"
                : label.contains("Errors") ? "errors"
                : label.contains("Warnings") ? "warnings"
                : label.contains("Compiled") ? "ok"
                : "idle";
        int errors = counterValue(errorsCounter);
        int warnings = counterValue(warningsCounter);
        return new ServerCompileStatus(state, errors, warnings, 1, 1);
    }

    public boolean hasErrors() {
        return getErrorsCount() > 0;
    }

    public boolean hasWarnings() {
        return getWarningsCount() > 0;
    }

    public boolean isProblemsPanelVisible() {
        return panel.isVisible(PROBE_MS);
    }

    public String getProblemsInfo() {
        return String.format("Errors: %d, Warnings: %d", getErrorsCount(), getWarningsCount());
    }

    public void checkNoProblems() {
        boolean compiled = waitForCompilationToComplete(COMPILATION_TIMEOUT_MS, COMPILATION_POLL_MS);
        if (!compiled) {
            throw new AssertionError("Compilation did not finish within " + COMPILATION_TIMEOUT_MS + " ms, state: "
                    + fetchServerCompileStatusViaPage());
        }
        boolean noProblems = WaitUtil.waitForCondition(
                () -> {
                    try {
                        return counterValue(errorsCounter) == 0 && counterValue(warningsCounter) == 0;
                    } catch (RuntimeException panelIsBeingRedrawn) {
                        return false;
                    }
                },
                DEFAULT_TIMEOUT_MS, 500, "Waiting for the problems panel to report no errors and no warnings");
        if (!noProblems) {
            throw new AssertionError("Expected no problems but found: " + getProblemsInfo()
                    + ", errors: " + getAllErrors() + ", warnings: " + getAllWarnings());
        }
    }

    public List<String> getAllErrors() {
        return readMessages(true);
    }

    public List<String> getAllWarnings() {
        return readMessages(false);
    }

    /**
     * The messages of one severity, as the panel lists them.
     *
     * <p>The panel lists errors first and warnings after, each in a list of its own and each list left out
     * when it would be empty, so which list is which is decided by the counts in the panel's header.
     */
    private List<String> readMessages(boolean errors) {
        waitForCompilationToComplete();
        if (!panel.isVisible(PROBE_MS)) {
            return List.of();
        }
        showProblemsPanel();
        expandAllPages();
        int errorCount = counterValue(errorsCounter);
        int warningCount = counterValue(warningsCounter);
        if ((errors && errorCount == 0) || (!errors && warningCount == 0)) {
            return List.of();
        }
        int listIndex = errors || errorCount == 0 ? 0 : 1;
        if (messageLists.size() <= listIndex) {
            return List.of();
        }
        List<String> messages = new ArrayList<>();
        messageLists.get(listIndex).getLocator().locator("xpath=./li").allInnerTexts()
                .forEach(text -> messages.add(text.trim()));
        return messages;
    }

    /** Asks each list for the rest of its messages, so what is read is everything the panel holds. */
    private void expandAllPages() {
        for (int page = 0; page < MAX_PAGES && showMoreBtn.isVisible(PAGE_PROBE_MS); page++) {
            showMoreBtn.click();
        }
    }

    public void waitForCompilationToComplete() {
        waitForCompilationToComplete(COMPILATION_TIMEOUT_MS, COMPILATION_POLL_MS);
    }

    public boolean waitForCompilationToComplete(long timeoutMillis, long pollIntervalMillis) {
        boolean finished = WaitUtil.waitForCondition(() -> !isCompilationInProgress(),
                timeoutMillis, (int) pollIntervalMillis, "Waiting for the project compilation to complete");
        if (!finished) {
            LOGGER.warn("Compilation timeout reached, state: {}", fetchServerCompileStatusViaPage());
        }
        return finished;
    }

    public void selectProblemByText(String text) {
        showProblemsPanel();
        expandAllPages();
        List<WebElement> allProblems = createElementList(BODY + "//li", "problemRows");
        allProblems.stream()
                .filter(element -> element.getText().contains(text))
                .findFirst()
                .ifPresent(WebElement::click);
    }

    public void selectProblemByIndex(int index) {
        showProblemsPanel();
        expandAllPages();
        List<WebElement> errorRows = createElementList(BODY + "/ul[1]/li", "errorRows");
        if (index > 0 && index <= errorRows.size()) {
            errorRows.get(index - 1).click();
        }
    }

    public boolean isErrorPresent(String errorMessage) {
        return getAllErrors().stream().anyMatch(error -> error.contains(errorMessage));
    }

    public boolean isWarningPresent(String warningMessage) {
        return getAllWarnings().stream().anyMatch(warning -> warning.contains(warningMessage));
    }

    public boolean isCompilationProgressBarVisible() {
        return compilingScreen.isVisible(PROBE_MS);
    }

    public boolean isCompilationProgressBarNotSavedProjectVisible() {
        return compileState.isVisible(PROBE_MS);
    }

    public String getCompilationProgressBarText() {
        if (!compilingScreen.isVisible(PROBE_MS)) {
            return "";
        }
        return compilingScreen.getText();
    }

    public String getCompilationProgressBarNotSavedProjectText() {
        if (!compileState.isVisible(PROBE_MS)) {
            return "";
        }
        return String.valueOf(compileState.getAttribute("aria-label"));
    }

    public void waitForCompilationProgressBarToContain(String text, long timeoutMs) {
        WaitUtil.waitForCondition(
                () -> getCompilationProgressBarText().contains(text)
                        || getCompilationProgressBarNotSavedProjectText().contains(text),
                timeoutMs, 1000,
                "Waiting for the compilation indicator to report: " + text
        );
    }
}
