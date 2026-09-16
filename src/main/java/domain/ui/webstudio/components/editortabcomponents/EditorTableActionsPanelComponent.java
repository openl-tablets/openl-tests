package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import domain.ui.webstudio.components.BaseComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import helpers.utils.WaitUtil;

public class EditorTableActionsPanelComponent extends BaseComponent {

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
