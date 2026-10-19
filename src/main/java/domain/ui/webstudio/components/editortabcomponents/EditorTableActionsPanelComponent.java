package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ViewportSize;
import domain.ui.webstudio.components.BaseComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import helpers.utils.WaitUtil;

import java.util.stream.Stream;

public class EditorTableActionsPanelComponent extends BaseComponent {

    private static final int SAVE_PROBE_MS = 1000;
    private static final long SAVE_SETTLE_MS = 10000;
    private static final int DISCARD_PROBE_MS = 3000;
    private static final String SAVE_REFUSED = "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' ant-notification-notice-wrapper ')]"
            + "[.//*[normalize-space()='Failed to save the table']]";

    private WebElement saveChangesBtn;
    private WebElement undoChangesBtn;
    private WebElement redoChangesBtn;
    private WebElement insertRowAfterBtn;
    private WebElement removeRowBtn;
    private WebElement insertColumnBeforeBtn;
    private WebElement removeColumnBtn;
    private WebElement closeEditorBtn;
    private WebElement discardDialog;

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
        closeEditorBtn = createScopedElement("xpath=.//button[@data-testid='table-edit-cancel']", "closeEditorBtn");
        discardDialog = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal')][.//button[@data-testid='table-edit-discard']]",
                "discardChangesDialog");
    }

    private void waitWhileTablePanelActionExecuted() {
        WaitUtil.sleep(250, "Waiting for table panel action to complete and UI to update");
    }

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
        WebElement writing = new WebElement(page, "xpath=//*[@data-testid='table-edit-saving']", "tableBeingWritten");
        WaitUtil.waitForCondition(() -> !writing.exists(), SAVE_SETTLE_MS, 200,
                "Waiting for the table to be written");
        WebElement refusal = new WebElement(page, SAVE_REFUSED, "tableSaveRefused");
        boolean kept = WaitUtil.waitForCondition(
                () -> !refusal.exists()
                        && (!saveChangesBtn.isVisible(SAVE_PROBE_MS) || !saveChangesBtn.isEnabled()),
                SAVE_SETTLE_MS, 250, "Waiting for the table changes to be kept");
        if (!kept) {
            throw new AssertionError("The table still has changes to keep, so keeping them was refused: "
                    + refusalSaid());
        }
    }

    private String refusalSaid() {
        WebElement notice = new WebElement(page,
                "xpath=(//div[contains(concat(' ',normalize-space(@class),' '),' ant-notification-notice-wrapper ')]"
                        + " | //div[contains(concat(' ',normalize-space(@class),' '),' ant-message-notice ')])[1]",
                "saveRefusal");
        return notice.isVisible(SAVE_PROBE_MS) ? notice.getInnerText().trim().replace("\n", " ")
                : "the screen says nothing";
    }

    public void closeTableEditor() {
        if (!closeEditorBtn.isVisible(SAVE_PROBE_MS)) {
            return;
        }
        closeEditorBtn.click();
        if (discardDialog.isVisible(DISCARD_PROBE_MS)) {
            String why = saveChangesBtn.isVisible(SAVE_PROBE_MS)
                    ? String.valueOf(saveChangesBtn.getAttribute("title"))
                    : "";
            throw new AssertionError("Leaving the table was questioned, so something written into it was "
                    + "never kept: " + discardDialog.getInnerText().trim().replace("\n", " ")
                    + (why.isEmpty() || "null".equals(why) ? "" : "; keeping it is offered as: " + why));
        }
    }

    public String clickSaveChangesAndReadRefusal() {
        saveChangesBtn.click();
        return new WebElement(page, SAVE_REFUSED, "tableSaveRefused").waitForVisible(SAVE_SETTLE_MS).getInnerText()
                .trim().replace("\n", " ");
    }

    public void discardChanges() {
        closeEditorBtn.click();
        discardDialog.child("xpath=.//button[@data-testid='table-edit-discard']").click();
    }

    public boolean isInView() {
        BoundingBox box = getRootLocator().getLocator().first().boundingBox();
        ViewportSize viewport = page.viewportSize();
        return box != null && box.x >= 0 && box.y >= 0
                && box.x + box.width <= viewport.width && box.y + box.height <= viewport.height;
    }

    public boolean areEditActionsShown() {
        return Stream.of(saveChangesBtn, undoChangesBtn, redoChangesBtn, insertRowAfterBtn, removeRowBtn,
                        insertColumnBeforeBtn, removeColumnBtn)
                .allMatch(WebElement::isVisible);
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
