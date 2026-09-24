package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import helpers.utils.WaitUtil;

public class TraceMenuComponent extends TableInputLauncherComponent implements ITraceMenu {

    private static final int POPUP_TIMEOUT_MS = 60000;

    private final WebElement traceBtn;
    private final WebElement traceIntoFileBtn;
    private final WebElement advancedCheckbox;
    private final WebElement jsonModeBtn;
    private final WebElement jsonEditor;

    public TraceMenuComponent(Page page) {
        super(page, "trace-start");
        traceBtn = buttonOf("trace-start");
        traceIntoFileBtn = buttonOf("trace-download");
        advancedCheckbox = new WebElement(page, "xpath=//input[@data-testid='trace-advanced']", "advancedCheckbox");
        jsonModeBtn = new WebElement(page, "xpath=//label[contains(@class,'ant-radio-button-wrapper')][normalize-space()='JSON']", "jsonModeBtn");
        jsonEditor = new WebElement(page, "xpath=//div[@data-testid='input-json']//div[contains(@class,'cm-content')]", "jsonEditor");
    }

    @Override
    public ITraceMenu setFactorTextField(String text) {
        return setParameterField("factor", text);
    }

    @Override
    public ITraceMenu setParameterField(String parameterName, String value) {
        writeField(parameterName, value);
        return this;
    }

    @Override
    public ITraceMenu clickCreateItem() {
        createFirstUnsetStructure();
        return this;
    }

    @Override
    public ITraceMenu clickAddElementToCollectionBtn(String parameterName) {
        growStructure(parameterName);
        return this;
    }

    @Override
    public ITraceMenu clickExpandCollection() {
        expandFirstCollection();
        return this;
    }

    @Override
    public ITraceMenu selectJSONTrace(String json) {
        jsonModeBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        jsonModeBtn.click();
        jsonEditor.waitForVisible(DEFAULT_TIMEOUT_MS);
        jsonEditor.click();
        page.keyboard().press("ControlOrMeta+a");
        page.keyboard().press("Delete");
        page.keyboard().insertText(json);
        return this;
    }

    @Override
    public ITraceMenu clickTraceIntoFile() {
        traceIntoFileBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        traceIntoFileBtn.click();
        return this;
    }

    @Override
    public ITraceWindow clickTraceInsideMenu() {
        return clickTraceInsideMenu(true);
    }

    @Override
    public ITraceWindow clickTraceInsideMenu(boolean isPopupExpected) {
        requestAdvancedTrace();
        return startTrace(isPopupExpected);
    }

    @Override
    public ITraceWindow clickTraceInsideMenuBusiness() {
        return startTrace(true);
    }

    private void requestAdvancedTrace() {
        advancedCheckbox.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (!advancedCheckbox.isChecked()) {
            advancedCheckbox.click();
            WaitUtil.requireCondition(advancedCheckbox::isChecked, DEFAULT_TIMEOUT_MS, 100,
                    "Waiting for the trace to be asked to run step by step");
        }
    }

    private ITraceWindow startTrace(boolean isPopupExpected) {
        traceBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (!isPopupExpected) {
            traceBtn.click();
            return null;
        }
        Page popup = page.waitForPopup(new Page.WaitForPopupOptions().setTimeout(POPUP_TIMEOUT_MS), traceBtn::click);
        popup.waitForLoadState();
        popup.waitForSelector("xpath=//div[@id='trace-view']", new Page.WaitForSelectorOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return new TraceWindowComponent(popup);
    }
}
