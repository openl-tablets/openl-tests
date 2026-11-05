package domain.ui.webstudio.components.admincomponents;

import com.microsoft.playwright.Locator;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.common.TableComponent;
import helpers.utils.WaitUtil;

import java.util.List;

public class TagsPageComponent extends BaseComponent {

    private static final int COL_TAG_TYPE = 2;
    private static final int COL_EXTENSIBLE = 3;
    private static final int COL_REQUIRED = 4;
    private static final int COL_TAGS = 5;
    private static final int COL_ACTIONS = 6;

    private TableComponent tagsTable;

    private WebElement newTagTypeInput;
    private WebElement projectNameTemplatesTextarea;
    private WebElement saveTemplatesBtn;
    private WebElement fillTagsForProjectBtn;
    private WebElement fillPreviewApplyBtn;
    private WebElement fillResultSummary;
    private WebElement fillResultCloseBtn;

    public TagsPageComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public TagsPageComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        tagsTable = createScopedComponent(TableComponent.class, "xpath=.//div[contains(@class,'ant-table-wrapper')]//table", "tagsTable");

        newTagTypeInput = createScopedElement("xpath=.//input[@name='tag-type' and @placeholder='New Tag Type']", "newTagTypeInput");
        projectNameTemplatesTextarea = createScopedElement("xpath=.//textarea[contains(@class,'ant-input')]", "projectNameTemplatesTextarea");
        saveTemplatesBtn = createScopedElement("xpath=.//button[.//span[text()='Save Templates']]", "saveTemplatesBtn");
        fillTagsForProjectBtn = createScopedElement("xpath=.//button[.//span[text()='Fill Tags for Project']]", "fillTagsForProjectBtn");
        fillPreviewApplyBtn = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal-footer')]//button[.//span[normalize-space()='Fill Tags']]",
                "fillPreviewApplyBtn");
        fillResultSummary = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal')]//*[@data-testid='fill-result-summary']",
                "fillResultSummary");
        fillResultCloseBtn = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal-footer')]//button[.//span[normalize-space()='Close']]",
                "fillResultCloseBtn");
    }

    public TableComponent getTagsTable() {
        tagsTable.isVisible();
        return tagsTable;
    }

    public int getTagTypeRowByName(String tagTypeName) {
        return WaitUtil.retryOnException(() -> {
            for (int i = 1; i <= tagsTable.getRows().size(); i++)
                if (getTagTypeName(i).equals(tagTypeName))
                    return i;
            throw new RuntimeException("No such tagTypeName found: " + tagTypeName);
        }, DEFAULT_TIMEOUT_MS, 250, "Waiting for tag type '" + tagTypeName + "' to be drawn in the Tags table");
    }

    public TagsPageComponent editTagTypeName(String oldTagTypeName, String newTagTypeName) {
        int row = getTagTypeRowByName(oldTagTypeName);
        tagsTable.getCell(row, COL_TAG_TYPE).getLocator().locator("xpath=.//div[@class='editable-cell-wrap']").click();
        tagsTable.getCell(row, COL_TAG_TYPE).getLocator().locator("xpath=.//div/input").clear();
        tagsTable.getCell(row, COL_TAG_TYPE).getLocator().locator("xpath=.//div/input").fill(newTagTypeName);
        return this;
    }

    public String getTagTypeName(int rowIndex) {
        return tagsTable.getCell(rowIndex, COL_TAG_TYPE).getText().trim();
    }

    public boolean isExtensibleChecked(String tagTypeName) {
        return checkbox(tagTypeName, COL_EXTENSIBLE).isChecked();
    }

    public TagsPageComponent setExtensible(String tagTypeName, boolean checked) {
        return setCheckbox(tagTypeName, COL_EXTENSIBLE, checked);
    }

    public boolean isRequiredChecked(String tagTypeName) {
        return checkbox(tagTypeName, COL_REQUIRED).isChecked();
    }

    public TagsPageComponent setRequired(String tagTypeName, boolean required) {
        return setCheckbox(tagTypeName, COL_REQUIRED, required);
    }

    private Locator checkbox(String tagTypeName, int column) {
        return tagsTable.getCell(getTagTypeRowByName(tagTypeName), column).getLocator().locator("xpath=.//input[@type='checkbox']");
    }

    private TagsPageComponent setCheckbox(String tagTypeName, int column, boolean checked) {
        if (checkbox(tagTypeName, column).isChecked() != checked) {
            checkbox(tagTypeName, column).click();
            WaitUtil.retryOnException(() -> {
                if (checkbox(tagTypeName, column).isChecked() != checked)
                    throw new IllegalStateException("Checkbox of tag type '" + tagTypeName + "' did not take the new state yet");
                return true;
            }, DEFAULT_TIMEOUT_MS, 250, "Waiting for the saved checkbox state of tag type '" + tagTypeName + "'");
        }
        return this;
    }

    public TagsPageComponent addTag(String tagTypeName, String tagToAdd) {
        int row = getTagTypeRowByName(tagTypeName);
        tagsTable.getCell(row, COL_TAGS).getLocator().locator("xpath=.//span[contains(@class,'ant-tag') and contains(@style, 'dashed')]").click();
        tagsTable.getCell(row, COL_TAGS).getLocator().locator("xpath=.//input").fill(tagToAdd);
        tagsTable.getCell(row, COL_TAGS).getLocator().press("Enter");
        return this;
    }

    public TagsPageComponent removeTag(String tagTypeName, String tagText) {
        int row = getTagTypeRowByName(tagTypeName);
        tagsTable.getCell(row, COL_TAGS).getLocator()
                .locator(String.format("xpath=.//span[@class='ant-tag' and .//span[text()='%s']]", tagText))
                .locator("xpath=./span[@class='anticon anticon-close']").click();
        return this;
    }

    public List<String> getAllTagsForTagType(String tagTypeName) {
        int row = getTagTypeRowByName(tagTypeName);
        return tagsTable.getCell(row, COL_TAGS).getLocator()
                .locator("xpath=.//div/span[not(contains(@style,'dashed'))]/span[1]")
                .allTextContents();
    }

    public TagsPageComponent deleteTagType(String tagTypeName) {
        int row = getTagTypeRowByName(tagTypeName);
        tagsTable.getCell(row, COL_ACTIONS).getLocator().locator("xpath=.//span[@role='img' and @aria-label='delete']").click();
        return this;
    }

    public TagsPageComponent addNewTagType(String tagTypeName) {
        newTagTypeInput.fill(tagTypeName);
        newTagTypeInput.press("Enter");
        WaitUtil.sleep(500, "Waiter to let created Tag to appear inside the Tag Table");
        return this;
    }

    public TagsPageComponent setProjectNameTemplates(String templates) {
        projectNameTemplatesTextarea.fill(templates);
        return this;
    }

    public String getProjectNameTemplates() {
        return projectNameTemplatesTextarea.getCurrentInputValue();
    }

    public TagsPageComponent saveTemplates() {
        saveTemplatesBtn.click();
        return this;
    }

    public TagsPageComponent fillTagsForProject() {
        fillTagsForProjectBtn.click();
        fillPreviewApplyBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        waitUntilSpinnerLoaded();
        return this;
    }

    public String getFillResultSummary() {
        return fillResultSummary.waitForVisible(DEFAULT_TIMEOUT_MS).getText().trim();
    }

    public String getFillResultTags(String projectName, String tagType) {
        WebElement tags = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal')]//*[@data-testid='fill-result-" + projectName + "-" + tagType + "']",
                "fillResultTags");
        return tags.waitForVisible(DEFAULT_TIMEOUT_MS).getText().replaceAll("\\s+", " ").trim();
    }

    public TagsPageComponent closeFillResult() {
        fillResultCloseBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        fillResultCloseBtn.waitForHidden(DEFAULT_TIMEOUT_MS);
        return this;
    }
}
