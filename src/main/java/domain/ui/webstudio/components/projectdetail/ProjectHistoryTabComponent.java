package domain.ui.webstudio.components.projectdetail;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;

import java.util.ArrayList;
import java.util.List;

public class ProjectHistoryTabComponent extends BaseComponent {

    private static final String AUTHOR_RELATIVE_TO_COMMENT = "xpath=../following-sibling::div[1]/span[1]";

    private final WebElement revisionEntries;
    private final WebElement technicalRevisionEntries;
    private final WebElement technicalRevisionsSwitch;
    private final WebElement searchField;
    private final WebElement openRevisionButton;
    private final WebElement emptyState;
    private final WebElement revisionComment;

    public ProjectHistoryTabComponent(Page page) {
        this(new WebElement(page, "xpath=//*[@data-testid='project-detail']", "projectDetail"));
    }

    public ProjectHistoryTabComponent(WebElement rootLocator) {
        super(rootLocator);
        revisionEntries = createScopedElement("xpath=.//*[starts-with(@data-testid,'revision-comment-')]", "revisionEntries");
        technicalRevisionEntries = createScopedElement("xpath=.//li[.//span[contains(@class,'ant-tag') and normalize-space()='technical']]"
                + "//*[starts-with(@data-testid,'revision-comment-')]", "technicalRevisionEntries");
        technicalRevisionsSwitch = createScopedElement("xpath=.//*[@data-testid='revisions-tech']", "technicalRevisionsSwitch");
        searchField = createScopedElement("xpath=.//*[starts-with(@data-testid,'revisions-search-')]/descendant-or-self::input", "searchField");
        openRevisionButton = createScopedElement("xpath=.//li[.//*[starts-with(@data-testid,'revision-comment-') and normalize-space()='%s']]"
                + "//button[starts-with(@data-testid,'revision-open-')]", "openRevisionButton");
        revisionComment = createScopedElement("xpath=.//*[starts-with(@data-testid,'revision-comment-') and normalize-space()='%s']", "revisionComment");
        emptyState = createScopedElement("xpath=.//div[contains(@class,'ant-empty-description')]", "emptyState");
    }

    public List<String> getRevisionDescriptions() {
        revisionEntries.getLocator().first().waitFor();
        return getShownRevisionDescriptions();
    }

    public List<String> getRevisionAuthors() {
        Locator entries = revisionEntries.getLocator();
        entries.first().waitFor();
        List<String> authors = new ArrayList<>();
        int count = entries.count();
        for (int i = 0; i < count; i++) {
            authors.add(entries.nth(i).locator(AUTHOR_RELATIVE_TO_COMMENT).textContent().trim());
        }
        return authors;
    }

    public String getLatestRevisionId() {
        Locator first = revisionEntries.getLocator().first();
        first.waitFor();
        return revisionIdOf(first.getAttribute("data-testid"));
    }

    public List<String> getShownRevisionDescriptions() {
        return texts(revisionEntries);
    }

    public List<String> getShownTechnicalRevisionDescriptions() {
        return texts(technicalRevisionEntries);
    }

    public boolean isTechnicalRevisionsSwitchOffered() {
        return technicalRevisionsSwitch.isVisible(DEFAULT_TIMEOUT_MS);
    }

    public boolean isSearchOffered() {
        return searchField.isVisible(DEFAULT_TIMEOUT_MS);
    }

    public ProjectHistoryTabComponent showTechnicalRevisions(boolean shown) {
        if (shown != "true".equals(technicalRevisionsSwitch.getAttribute("aria-checked"))) {
            technicalRevisionsSwitch.click();
        }
        return this;
    }

    public ProjectHistoryTabComponent search(String text) {
        searchField.clear();
        searchField.fillSequentially(text);
        return this;
    }

    public String getRevisionId(String description) {
        return revisionIdOf(revisionComment.format(description).getAttribute("data-testid"));
    }

    public boolean isOpenOffered(String description) {
        return openRevisionButton.format(description).exists();
    }

    public String getEmptyStateText() {
        return emptyState.isVisible(DEFAULT_TIMEOUT_MS) ? emptyState.getText().trim() : "";
    }

    private static String revisionIdOf(String testId) {
        return testId == null ? "" : testId.substring(testId.lastIndexOf('-') + 1);
    }

    private List<String> texts(WebElement entries) {
        return entries.getLocator().allTextContents().stream().map(String::trim).toList();
    }

    public int getRevisionsCount() {
        revisionEntries.getLocator().first().waitFor();
        return revisionEntries.getLocator().count();
    }
}
