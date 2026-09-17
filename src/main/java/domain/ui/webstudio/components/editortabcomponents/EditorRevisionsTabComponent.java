package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;

/**
 * What a project has been through, as the screen lists it: one entry per revision, newest first, each with
 * the message it was saved under and the way back to it.
 */
public class EditorRevisionsTabComponent extends BaseComponent {

    private static final String LIST = "//ol[starts-with(@data-testid,'revisions-')]";
    private static final String ENTRY = LIST + "/li";
    private static final int CLOSE_PROBE_MS = 1000;

    private List<WebElement> revisionEntries;
    private WebElement commentTemplate;
    private WebElement openTemplate;

    public EditorRevisionsTabComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public EditorRevisionsTabComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        revisionEntries = createElementList("xpath=" + ENTRY, "revisionEntries");
        commentTemplate = new WebElement(page,
                "xpath=(" + ENTRY + ")[%s]//span[starts-with(@data-testid,'revision-comment-')]", "revisionComment");
        openTemplate = new WebElement(page,
                "xpath=(" + ENTRY + ")[%s]//button[starts-with(@data-testid,'revision-open-')]", "revisionOpen");
    }

    /**
     * Closes the window the revisions stand in, if that is where they stand. On a module screen they are
     * shown over it, and the screen underneath cannot be reached until they are put away; on the project's
     * own card they are a side of it and there is nothing to close.
     */
    public void closeIfOpen() {
        WebElement window = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal-wrap')][.//ol[starts-with(@data-testid,'revisions-')]]",
                "revisionsWindow");
        if (!window.isVisible(CLOSE_PROBE_MS)) {
            return;
        }
        new WebElement(page, "xpath=//div[contains(@class,'ant-modal-wrap')]"
                + "[.//ol[starts-with(@data-testid,'revisions-')]]//button[@aria-label='Close']",
                "closeRevisionsBtn").click();
        window.waitForHidden(DEFAULT_TIMEOUT_MS);
    }

    public void waitForTableToLoad() {
        WaitUtil.waitForCondition(() -> !revisionEntries.isEmpty(), DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the revisions of the project to be listed");
    }

    /** The message the revision at that place was saved under; the places count from one, newest first. */
    public String getCommentForRow(int rowIndex) {
        return commentTemplate.format(String.valueOf(rowIndex)).getText().trim();
    }

    /**
     * Opens the revision standing at that place. The one the workspace already holds offers no way back to
     * itself, so the entries that do are counted among all of them.
     */
    public void openRevision(int rowIndex) {
        WebElement open = openTemplate.format(String.valueOf(rowIndex));
        open.waitForVisible(DEFAULT_TIMEOUT_MS);
        open.click();
        waitUntilSpinnerLoaded();
    }

    public int getRowCount() {
        waitForTableToLoad();
        return revisionEntries.size();
    }
}
