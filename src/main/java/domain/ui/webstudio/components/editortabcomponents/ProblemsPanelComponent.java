package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProblemsPanelComponent extends BaseComponent {

    private static final String PANEL = "xpath=//section[@data-testid='compile-problems']";
    private static final String BODY = PANEL + "//div[@data-testid='compile-problems-body']";
    private static final String COMPILING_LABEL = "Compiling";
    private static final Set<String> FINISHED_STATES = Set.of("ok", "warnings", "errors");
    private static final long COMPILATION_TIMEOUT_MS = 90000;
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

    private int counterValue(WebElement counter) {
        if (!panel.isVisible(PROBE_MS) || !counter.isVisible(PROBE_MS)) {
            return 0;
        }
        String text = counter.getText().replaceAll("\\D+", "");
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    public boolean isCompilationInProgress() {
        return compilingScreen.isVisible(PROBE_MS / 4)
                || (compileState.isVisible(PROBE_MS / 4) && compileIndicatorSays(COMPILING_LABEL));
    }

    private boolean compileIndicatorSays(String word) {
        return compileState.getText().contains(word)
                || String.valueOf(compileState.getAttribute("aria-label")).contains(word);
    }

    public ServerCompileStatus fetchServerCompileStatusViaPage() {
        if (!compileState.isVisible(PROBE_MS)) {
            return null;
        }
        String said = compileState.getText();
        String label = String.valueOf(compileState.getAttribute("aria-label"));
        String state = said.contains(COMPILING_LABEL) || label.contains(COMPILING_LABEL) ? "compiling"
                : label.contains("Errors") || label.contains("error") ? "errors"
                : label.contains("Warnings") || label.contains("warning") ? "warnings"
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
        waitUntilThePanelHasSettled();
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

    public List<String> errorsSaying(String said) {
        List<String> matched = new ArrayList<>();
        boolean shown = WaitUtil.waitForCondition(() -> {
            List<String> now;
            try {
                now = messagesNow(true);
            } catch (RuntimeException panelIsBeingRedrawn) {
                return false;
            }
            if (now.stream().noneMatch(error -> error.contains(said))) {
                matched.clear();
                return false;
            }
            boolean readTwice = matched.equals(now);
            matched.clear();
            matched.addAll(now);
            return readTwice;
        }, COMPILATION_TIMEOUT_MS, COMPILATION_POLL_MS * 2, "Waiting for the panel to say '" + said + "'");
        return shown ? List.copyOf(matched) : readMessages(true);
    }

    public List<String> getAllWarnings() {
        return readMessages(false);
    }

    private List<String> readMessages(boolean errors) {
        waitForCompilationToComplete();
        waitUntilThePanelHasSettled();
        return messagesNow(errors);
    }

    private List<String> messagesNow(boolean errors) {
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
        return CompileMessageReader.textsOf(messageLists.get(listIndex).getLocator());
    }

    private void expandAllPages() {
        for (int page = 0; page < MAX_PAGES && showMoreBtn.isVisible(PAGE_PROBE_MS); page++) {
            showMoreBtn.click();
        }
    }

    public void waitForCompilationToComplete() {
        waitForCompilationToComplete(COMPILATION_TIMEOUT_MS, COMPILATION_POLL_MS);
    }

    private void waitUntilThePanelHasSettled() {
        String[] last = {null};
        WaitUtil.waitForCondition(() -> {
            if (isCompilationInProgress()) {
                last[0] = null;
                return false;
            }
            String said = counterValue(errorsCounter) + "/" + counterValue(warningsCounter) + "/"
                    + (compileState.isVisible(PROBE_MS / 4) ? String.valueOf(compileState.getAttribute("aria-label")) : "");
            boolean settled = said.equals(last[0]);
            last[0] = said;
            return settled;
        }, COMPILATION_TIMEOUT_MS, COMPILATION_POLL_MS * 2, "Waiting for the problems panel to settle");
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
        waitForCompilationToComplete();
        showProblemsPanel();
        expandAllPages();
        List<WebElement> allProblems = createElementList(BODY + "//li", "problemRows");
        allProblems.stream()
                .filter(element -> element.getText().contains(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No problem says '" + text + "'; the panel says: "
                        + allProblems.stream().map(WebElement::getText).toList()))
                .click();
    }

    public void selectProblemByIndex(int index) {
        waitForCompilationToComplete();
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
