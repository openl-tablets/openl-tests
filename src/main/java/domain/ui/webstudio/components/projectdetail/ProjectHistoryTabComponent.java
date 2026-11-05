package domain.ui.webstudio.components.projectdetail;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.editortabcomponents.CompareLocalChangesDialogComponent;
import helpers.utils.WaitUtil;

import java.util.ArrayList;
import java.util.List;

public class ProjectHistoryTabComponent extends BaseComponent {

    private static final String AUTHOR_RELATIVE_TO_COMMENT = "xpath=../following-sibling::div[1]/span[1]";
    private static final String DATE_RELATIVE_TO_COMMENT = "xpath=../following-sibling::div[1]/span[2]";
    private static final String COMMENT_PREDICATE = "[starts-with(@data-testid,'revision-comment-') and not(contains(@data-testid,'-toggle'))]";
    private static final String REVISION = ".//li[.//*" + COMMENT_PREDICATE + "[starts-with(normalize-space(),\"%s\")]]";
    private static final long CHANGES_SHOWN_MS = 15000;
    private static final String TEXT_WITHOUT_TOGGLE = "comments => comments.map(comment => [...comment.childNodes]"
            + ".filter(node => node.nodeName !== 'BUTTON').map(node => node.textContent).join('').trim())";

    private final WebElement revisionEntries;
    private final WebElement technicalRevisionEntries;
    private final WebElement technicalRevisionsSwitch;
    private final WebElement searchField;
    private final WebElement openRevisionButton;
    private final WebElement emptyState;
    private final WebElement revisionComment;
    private final WebElement commentLink;

    public ProjectHistoryTabComponent(Page page) {
        this(new WebElement(page, "xpath=//*[@data-testid='project-detail']", "projectDetail"));
    }

    public ProjectHistoryTabComponent(WebElement rootLocator) {
        super(rootLocator);
        revisionEntries = createScopedElement("xpath=.//*" + COMMENT_PREDICATE, "revisionEntries");
        technicalRevisionEntries = createScopedElement("xpath=.//li[.//span[contains(@class,'ant-tag') and normalize-space()='technical']]"
                + "//*" + COMMENT_PREDICATE, "technicalRevisionEntries");
        technicalRevisionsSwitch = createScopedElement("xpath=.//*[@data-testid='revisions-tech']", "technicalRevisionsSwitch");
        searchField = createScopedElement("xpath=.//*[starts-with(@data-testid,'revisions-search-')]/descendant-or-self::input", "searchField");
        openRevisionButton = createScopedElement("xpath=.//li[.//*" + COMMENT_PREDICATE + "[normalize-space()='%s']]"
                + "//button[starts-with(@data-testid,'revision-open-')]", "openRevisionButton");
        revisionComment = createScopedElement("xpath=.//*" + COMMENT_PREDICATE + "[normalize-space()='%s']", "revisionComment");
        commentLink = createScopedElement("xpath=.//*" + COMMENT_PREDICATE + "//a[normalize-space()='%s']", "commentLink");
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

    public boolean isCommentLinkOffered(String linkText) {
        return commentLink.format(linkText).isVisible(DEFAULT_TIMEOUT_MS / 5);
    }

    public void clickCommentLink(String linkText) {
        commentLink.format(linkText).click();
    }

    public String getEmptyStateText() {
        return emptyState.isVisible(DEFAULT_TIMEOUT_MS) ? emptyState.getText().trim() : "";
    }

    private static String revisionIdOf(String testId) {
        return testId == null ? "" : testId.substring(testId.lastIndexOf('-') + 1);
    }

    @SuppressWarnings("unchecked")
    private List<String> texts(WebElement entries) {
        return (List<String>) entries.getLocator().evaluateAll(TEXT_WITHOUT_TOGGLE);
    }

    public List<String> getChangedFiles(String description) {
        Locator revision = expandRevision(description);
        Locator files = changedFilesOf(revision);
        Locator nothing = changesNoteOf(revision);
        WaitUtil.requireCondition(() -> files.count() > 0 || nothing.count() > 0, CHANGES_SHOWN_MS, 250,
                "Waiting for the files of the revision '" + description + "' to be listed");
        @SuppressWarnings("unchecked")
        List<String> listed = (List<String>) files.evaluateAll("rows => rows.map(row => "
                + "row.querySelector('[aria-label]').getAttribute('aria-label') + ' ' + row.querySelector('strong').innerText)");
        return listed;
    }

    public String getChangesNote(String description) {
        Locator note = changesNoteOf(expandRevision(description));
        WaitUtil.requireCondition(() -> note.count() > 0, CHANGES_SHOWN_MS, 250,
                "Waiting for the revision '" + description + "' to tell what it changed");
        return note.innerText().trim();
    }

    public boolean areChangedFilesShown(String description) {
        return changedFilesOf(revision(description)).all().stream().anyMatch(Locator::isVisible);
    }

    public boolean isRevisionExpanded(String description) {
        return "true".equals(toggleOf(description).getAttribute("aria-expanded"));
    }

    public void collapseRevision(String description) {
        if (isRevisionExpanded(description)) {
            toggleOf(description).click();
        }
    }

    public String getRevisionDate(String description) {
        return revision(description).locator("xpath=.//*" + COMMENT_PREDICATE)
                .locator(DATE_RELATIVE_TO_COMMENT).innerText().trim();
    }

    public CompareLocalChangesDialogComponent openChangedFile(String description, String fileName) {
        Locator file = changedFilesOf(expandRevision(description))
                .filter(new Locator.FilterOptions().setHas(page.locator("xpath=.//strong[normalize-space()='" + fileName + "']")));
        return compareWindowOpenedBy(file::click);
    }

    public ProjectHistoryTabComponent pickRevision(String description) {
        revision(description).locator("xpath=.//label[.//input[starts-with(@data-testid,'revision-pick-')]]").click();
        return this;
    }

    public CompareLocalChangesDialogComponent compareRevisions() {
        return compareWindowOpenedBy(() -> createScopedElement("xpath=.//button[@data-testid='revisions-compare']",
                "compareRevisions").click());
    }

    private CompareLocalChangesDialogComponent compareWindowOpenedBy(Runnable click) {
        Page popup = page.waitForPopup(click);
        popup.waitForLoadState();
        return new CompareLocalChangesDialogComponent(popup);
    }

    private static Locator changedFilesOf(Locator revision) {
        return revision.locator("xpath=.//*[@data-testid='revision-changes-file']");
    }

    private static Locator changesNoteOf(Locator revision) {
        return revision.locator("xpath=.//*[contains(@class,'ant-empty-description')]");
    }

    private Locator expandRevision(String description) {
        if (!isRevisionExpanded(description)) {
            toggleOf(description).click();
        }
        return revision(description);
    }

    private Locator toggleOf(String description) {
        return revision(description).locator("xpath=.//button[starts-with(@data-testid,'revision-toggle-')]");
    }

    private Locator revision(String description) {
        return createScopedElement("xpath=" + String.format(REVISION, description), "revision").getLocator();
    }

    public int getRevisionsCount() {
        revisionEntries.getLocator().first().waitFor();
        return revisionEntries.getLocator().count();
    }
}
