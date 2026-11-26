package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.CompareExcelFilesDialogComponent;
import helpers.utils.WaitUtil;

import java.util.ArrayList;
import java.util.List;

public class MoreMenuComponent extends BaseComponent implements IMoreMenu {

    private static final int ACTION_VISIBLE_TIMEOUT_MS = 500;
    private static final int ACTION_CLICK_TIMEOUT_MS = 2000;
    private static final long ACTION_RETRY_TIMEOUT_MS = DEFAULT_TIMEOUT_MS * 2L;
    private static final int SETTLE_LOOKS = 4;
    private static final int SETTLE_LOOK_MS = 800;

    private static final String ACTION_BAR = "xpath=//div[@data-testid='module-actions']";
    private static final String OVERFLOW = "//div[@data-testid='module-actions-overflow']";
    private static final String ACTION_BUTTON = "[starts-with(@data-testid,'module-')][not(@data-testid='module-actions-more')]";

    private final WebElement toggle;
    private final WebElement actionBar;
    private final WebElement actionTemplate;
    private final WebElement reformatConfirmBtn;
    private final WebElement barActionLabels;
    private final WebElement overflowPanel;
    private final WebElement overflowActionLabels;
    private final WebElement revisionsTab;

    public MoreMenuComponent(Page page) {
        this(new WebElement(page, "xpath=//button[@data-testid='module-actions-more']", "moreBtn"));
    }

    public MoreMenuComponent(WebElement rootLocator) {
        super(rootLocator);
        toggle = rootLocator;
        actionBar = new WebElement(page, ACTION_BAR, "moduleActionBar");
        actionTemplate = new WebElement(page, "xpath=//button[@data-testid='module-%s']", "moduleAction");
        reformatConfirmBtn = new WebElement(page, "xpath=//button[@data-testid='reformat-ok']", "reformatConfirmBtn");
        barActionLabels = new WebElement(page, ACTION_BAR + "//button" + ACTION_BUTTON, "moduleBarActions");
        overflowPanel = new WebElement(page, "xpath=" + OVERFLOW, "moduleActionsMenu");
        overflowActionLabels = new WebElement(page, "xpath=" + OVERFLOW + "//button" + ACTION_BUTTON, "moduleMenuActions");
        revisionsTab = new WebElement(page, "xpath=//div[@data-testid='project-tabs']//div[@data-node-key='history']", "revisionsTab");
    }

    public MoreMenuComponent open() {
        closeWindowsOverTheScreen();
        WaitUtil.sleep(1000, "Waiting before using the module actions");
        return this;
    }

    @Override
    public ChangesDialogComponent clickChanges() {
        waitUntilSpinnerLoaded();
        actionBar.waitForVisible(DEFAULT_TIMEOUT_MS);
        ChangesDialogComponent changes = new ChangesDialogComponent();
        boolean opened = WaitUtil.retryAction(() -> {
            clickAction("localChanges", "Local Changes");
            if (!changes.isViewShown(DEFAULT_TIMEOUT_MS)) {
                throw new IllegalStateException("Local Changes view did not open after clicking the action");
            }
            for (int look = 0; look < SETTLE_LOOKS; look++) {
                WaitUtil.sleep(SETTLE_LOOK_MS, "Letting the module screen settle under the history");
                if (!changes.isViewShown(ACTION_VISIBLE_TIMEOUT_MS * 2)) {
                    throw new IllegalStateException("Local Changes closed again while the module screen settled");
                }
            }
        }, ACTION_RETRY_TIMEOUT_MS + DEFAULT_TIMEOUT_MS, 500, "Opening Local Changes from the module actions");
        if (!opened) {
            throw new IllegalStateException("Local Changes view did not open within " + (ACTION_RETRY_TIMEOUT_MS + DEFAULT_TIMEOUT_MS) + " ms");
        }
        return changes.waitForLoaded();
    }

    @Override
    public void clickRevisions() {
        if (actionBar.isVisible(ACTION_VISIBLE_TIMEOUT_MS * 2)) {
            clickAction("revisions", "Revisions");
        } else {
            revisionsTab.waitForVisible(DEFAULT_TIMEOUT_MS);
            revisionsTab.click();
        }
        WaitUtil.sleep(500, "Waiting for the revisions to be listed");
    }

    @Override
    public void clickTableDependencies() {
        clickAction("dependencies", "Table Dependencies");
        WaitUtil.sleep(1000, "Waiting for Table Dependencies view to load");
    }

    @Override
    public void reformatExcelFiles() {
        clickAction("reformat", "Reformat Excel files");
        reformatConfirmBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        Response reformatted = page.waitForResponse(
                response -> "POST".equals(response.request().method()) && response.url().endsWith("/format"),
                new Page.WaitForResponseOptions().setTimeout(DEFAULT_TIMEOUT_MS * 4L),
                reformatConfirmBtn::click);
        if (!reformatted.ok()) {
            throw new IllegalStateException("Reformat Excel files was answered with " + reformatted.status());
        }
    }

    private void clickAction(String key, String actionName) {
        WebElement action = actionTemplate.format(key);
        boolean clicked = WaitUtil.retryAction(() -> {
            if (!action.isVisible(ACTION_VISIBLE_TIMEOUT_MS)) {
                toggle.click();
            }
            action.click(ACTION_CLICK_TIMEOUT_MS);
        }, ACTION_RETRY_TIMEOUT_MS, 500, "Clicking '" + actionName + "' in the module actions, opening their menu if the bar has no room for it");
        if (!clicked) {
            throw new IllegalStateException("'" + actionName + "' in the module actions could not be clicked within " + ACTION_RETRY_TIMEOUT_MS + " ms");
        }
    }

    @Override
    public CompareExcelFilesDialogComponent clickCompareExcelFiles() {
        Page popup = page.waitForPopup(() -> clickAction("compare", "Compare Excel files"));
        popup.waitForLoadState();
        return new CompareExcelFilesDialogComponent(popup);
    }

    public List<String> getMenuItems() {
        actionBar.waitForVisible(DEFAULT_TIMEOUT_MS);
        List<String> items = new ArrayList<>(labelsOf(barActionLabels));
        if (toggle.isVisible(ACTION_VISIBLE_TIMEOUT_MS * 2)) {
            toggle.click();
            overflowPanel.waitForVisible(DEFAULT_TIMEOUT_MS);
            items.addAll(labelsOf(overflowActionLabels));
            page.keyboard().press("Escape");
        }
        return items;
    }

    private List<String> labelsOf(WebElement actions) {
        return actions.getLocator().allInnerTexts().stream()
                .map(text -> text.split("\n")[0].trim())
                .filter(text -> !text.isEmpty())
                .toList();
    }
}
