package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import domain.ui.webstudio.components.BaseComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import helpers.utils.WaitUtil;

public class EditorTableActionsPanelComponent extends BaseComponent {

    private static final int SAVE_PROBE_MS = 1000;
    private static final long SAVE_SETTLE_MS = 10000;
    private static final int DISCARD_PROBE_MS = 3000;

    private WebElement saveChangesBtn;
    private WebElement undoChangesBtn;
    private WebElement redoChangesBtn;
    private WebElement insertRowAfterBtn;
    private WebElement removeRowBtn;
    private WebElement insertColumnBeforeBtn;
    private WebElement removeColumnBtn;

    public EditorTableActionsPanelComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public EditorTableActionsPanelComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        saveChangesBtn = createScopedElement("xpath=.//button[@data-testid='table-edit-save']", "saveChangesBtn");
        undoChangesBtn = createScopedElement("xpath=.//button[@data-testid='table-edit-undo']", "undoChangesBtn");
        redoChangesBtn = createScopedElement("xpath=.//button[@data-testid='table-edit-redo']", "redoChangesBtn");
        insertRowAfterBtn = createScopedElement("xpath=.//button[@data-testid='table-edit-insert_row']", "insertRowAfterBtn");
        removeRowBtn = createScopedElement("xpath=.//button[@data-testid='table-edit-remove_row']", "removeRowBtn");
        insertColumnBeforeBtn = createScopedElement("xpath=.//button[@data-testid='table-edit-insert_column']", "insertColumnBeforeBtn");
        removeColumnBtn = createScopedElement("xpath=.//button[@data-testid='table-edit-remove_column']", "removeColumnBtn");
    }

    private void waitWhileTablePanelActionExecuted() {
        WaitUtil.sleep(250, "Waiting for table panel action to complete and UI to update");
    }

    /**
     * Keeps what was written into the table, and says so when it was not kept: the screen offers to keep
     * changes only while there are any, so a table that still has something to keep was refused.
     */
    public void clickSaveChanges() {
        try {
            page.waitForResponse(
                    response -> true,
                    () -> saveChangesBtn.click()
            );
        } catch (RuntimeException e) {
            LOGGER.debug("No response captured after save click: {}", e.getMessage());
        }
        waitWhileTablePanelActionExecuted();
        waitUntilSpinnerLoaded();
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000));
        boolean kept = WaitUtil.waitForCondition(
                () -> !saveChangesBtn.isVisible(SAVE_PROBE_MS) || !saveChangesBtn.isEnabled(),
                SAVE_SETTLE_MS, 250, "Waiting for the table changes to be kept");
        if (!kept) {
            throw new AssertionError("The table still has changes to keep, so keeping them was refused: "
                    + refusalSaid());
        }
    }

    /** What the screen says about a refusal, while it is still saying it. */
    private String refusalSaid() {
        WebElement notice = new WebElement(page,
                "xpath=//div[contains(@class,'ant-notification-notice')] | //div[contains(@class,'ant-message-notice')]",
                "saveRefusal");
        return notice.isVisible(SAVE_PROBE_MS) ? notice.getInnerText().trim() : "the screen says nothing";
    }

    /**
     * Leaves the table as it stands. Everything written into it has been kept by then, so the screen has
     * nothing to ask about; it asking is a change that was not kept, and that is said rather than discarded.
     */
    public void closeTableEditor() {
        WebElement close = createScopedElement("xpath=.//button[@data-testid='table-edit-cancel']", "closeEditorBtn");
        if (!close.isVisible(SAVE_PROBE_MS)) {
            return;
        }
        close.click();
        WebElement asked = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal')][.//button[@data-testid='table-edit-discard']]",
                "discardChangesDialog");
        if (asked.isVisible(DISCARD_PROBE_MS)) {
            String why = saveChangesBtn.isVisible(SAVE_PROBE_MS)
                    ? String.valueOf(saveChangesBtn.getAttribute("title"))
                    : "";
            throw new AssertionError("Leaving the table was questioned, so something written into it was "
                    + "never kept: " + asked.getInnerText().trim().replace("\n", " ")
                    + (why.isEmpty() || "null".equals(why) ? "" : "; keeping it is offered as: " + why));
        }
    }

    public void undoClickChanges() {
        undoChangesBtn.click();
        waitWhileTablePanelActionExecuted();
    }

    public void redoClickChanges() {
        redoChangesBtn.click();
        waitWhileTablePanelActionExecuted();
    }

    public void clickInsertRowAfter() {
        insertRowAfterBtn.click();
        waitWhileTablePanelActionExecuted();
    }

    public void clickRemoveRow() {
        removeRowBtn.click();
        waitWhileTablePanelActionExecuted();
    }

    public void clickInsertColumnBefore() {
        insertColumnBeforeBtn.click();
        waitWhileTablePanelActionExecuted();
    }

    public void clickRemoveColumn() {
        removeColumnBtn.click();
        waitWhileTablePanelActionExecuted();
    }
}
