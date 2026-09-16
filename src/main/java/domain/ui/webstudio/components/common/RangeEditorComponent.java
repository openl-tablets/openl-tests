package domain.ui.webstudio.components.common;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

public class RangeEditorComponent extends BaseComponent {

    private WebElement doneBtn;
    private WebElement discardChangesBtn;

    public RangeEditorComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public RangeEditorComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        doneBtn = createScopedElement("xpath=.//button[@data-testid='range-write']", "Range Editor Done Button");
        discardChangesBtn = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal-container')]//button[@data-testid='table-edit-discard']",
                "Discard Changes Button");
    }

    /**
     * Opens the range panel of the cell being written: a cell that holds a range offers it from its own
     * editor, which is what a reader presses once the cell is open for writing.
     */
    public void openFromCellEditor() {
        WebElement cellEditor = new WebElement(page, "xpath=//*[@data-testid='table-cell-input']", "cellEditor");
        cellEditor.waitForVisible(DEFAULT_TIMEOUT_MS);
        cellEditor.click();
    }

    public boolean isOpen() {
        return doneBtn.isVisible();
    }

    public boolean isOpen(int timeoutInMillis) {
        return doneBtn.isVisible(timeoutInMillis);
    }

    public void clickDone() {
        doneBtn.click();
    }

    public void discardChangesIfPresent() {
        if (discardChangesBtn.isVisible(1000)) {
            discardChangesBtn.click();
        }
    }
}
