package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.Map;

public class CompareLocalChangesDialogComponent extends BaseComponent {

    public static final int COMBINED = 0;

    private static final String TREE = "//div[@data-testid='compare-tree']";
    private static final String TREE_NODE = TREE + "//div[contains(@class,'ant-tree-treenode')]";
    private static final String PANE = "//div[@data-testid='compare-pane-%s']";
    private static final String ANY_PANE = "//div[starts-with(@data-testid,'compare-pane-')]";
    private static final String ROWS = "//table//tbody[not(@aria-hidden='true')]/tr";
    private static final String ADDRESSED = "td[@data-cell][not(starts-with(@data-cell,'#'))]";
    private static final String VERSION_ROW = ROWS + "[" + ADDRESSED + "]";
    private static final String LEFT_OUT_ROW = ROWS + "[not(" + ADDRESSED + ")][contains(normalize-space(),'equal row')]";
    private static final String VIEW_OPTION = "//*[@data-testid='%s']//label[contains(@class,'ant-segmented-item')]";
    private static final String SIDE_BY_SIDE = "Side by side";
    private static final String ALL_VERSIONS = "All Versions";
    private static final String CHANGED_FRAME = "rgb(250, 173, 20)";
    private static final String CONFLICT_FRAME = "rgb(255, 77, 79)";
    private static final List<String> ADDED_OR_REMOVED = List.of("rgb(246, 255, 237)", "rgb(255, 242, 240)");
    private static final String COUNT_MARKED = "(cells, marks) => cells.filter(cell => {"
            + " const style = getComputedStyle(cell);"
            + " return marks.frames.some(colour => style.boxShadow.includes(colour))"
            + " || marks.paints.includes(style.backgroundColor); }).length";
    protected static final int PROBE_MS = 1000;
    protected static final int COMPARISON_TIMEOUT_MS = 90000;

    protected final WebElement tree;
    protected final WebElement identicalNotice;
    private final WebElement showEqualRowsCheckbox;
    private final WebElement showEqualElementsCheckbox;
    private final WebElement conflictText;
    private final WebElement errorNotice;
    private final WebElement backBtn;
    private final WebElement treeNodeTemplate;
    private final WebElement treeSwitcherTemplate;

    private final Page comparePopup;

    public CompareLocalChangesDialogComponent(Page comparePopup) {
        super(comparePopup);
        this.comparePopup = comparePopup;
        tree = new WebElement(getPage(), "xpath=" + TREE, "compareTree");
        identicalNotice = new WebElement(getPage(), "xpath=//*[@data-testid='compare-identical']", "compareIdentical");
        showEqualRowsCheckbox = new WebElement(getPage(), "xpath=//input[@data-testid='compare-show-equal-rows']", "showEqualRows");
        showEqualElementsCheckbox = new WebElement(getPage(), "xpath=//input[@data-testid='compare-show-equal-elements']", "showEqualElements");
        conflictText = new WebElement(getPage(), "xpath=//*[@data-testid='compare-conflict-text']", "compareConflictText");
        backBtn = new WebElement(getPage(), "xpath=//button[@data-testid='compare-back']", "compareBackBtn");
        errorNotice = new WebElement(getPage(), "xpath=//*[@data-testid='compare-error']", "compareError");
        String named = "[.//span[contains(@class,'ant-tree-title')]"
                + "[normalize-space()=\"%1$s\" or starts-with(normalize-space(),\"%1$s\")]]";
        treeNodeTemplate = new WebElement(getPage(),
                "xpath=(" + TREE_NODE + named + ")[1]", "compareTreeNode");
        treeSwitcherTemplate = new WebElement(getPage(),
                "xpath=(" + TREE_NODE + named + ")[1]"
                + "/span[contains(@class,'ant-tree-switcher') and not(contains(@class,'ant-tree-switcher-noop'))]",
                "compareTreeSwitcher");
    }

    public CompareLocalChangesDialogComponent waitForDialogToAppear() {
        WaitUtil.requireCondition(this::isComparisonDrawn, COMPARISON_TIMEOUT_MS, 250,
                "Waiting for the comparison to be drawn");
        return this;
    }

    protected boolean isComparisonDrawn() {
        if (errorNotice.isVisible(PROBE_MS / 2)) {
            throw new AssertionError("The comparison could not be made: " + errorNotice.getText().trim());
        }
        return identicalNotice.isVisible(PROBE_MS / 2) || !treeTitles().isEmpty();
    }

    public CompareLocalChangesDialogComponent waitForTextCompareToAppear() {
        conflictText.waitForVisible(COMPARISON_TIMEOUT_MS);
        return this;
    }

    public List<String> getLeftModulesList() {
        WaitUtil.waitForListNotEmpty(this::treeTitles, COMPARISON_TIMEOUT_MS, 250,
                "Waiting for the comparison to list what differs");
        return treeTitles().stream().map(WebElement::getText).map(String::trim).toList();
    }

    public List<String> getRightModulesList() {
        return getLeftModulesList();
    }

    private List<WebElement> treeTitles() {
        return createElementList("xpath=" + TREE_NODE + "//span[contains(@class,'ant-tree-title')]", "compareTreeTitles");
    }

    public void openTreeNode(String nodeName) {
        WaitUtil.requireCondition(() -> {
            if (!treeNodeTemplate.format(asShown(nodeName)).isVisible(PROBE_MS)) {
                return unfoldOne();
            }
            WebElement switcher = treeSwitcherTemplate.format(asShown(nodeName));
            if (!switcher.isVisible(PROBE_MS)) {
                return true;
            }
            if (!switcher.getAttribute("class").contains("ant-tree-switcher_open")) {
                switcher.click();
            }
            return switcher.getAttribute("class").contains("ant-tree-switcher_open");
        }, DEFAULT_TIMEOUT_MS * 2, 250,
                "Opening '" + nodeName + "' in the comparison tree, which lists " + drawnTitles());
    }

    private static String asShown(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private List<String> drawnTitles() {
        return treeTitles().stream().map(WebElement::getText).map(String::trim).toList();
    }

    private boolean unfoldOne() {
        List<WebElement> folded = createElementList("xpath=" + TREE_NODE
                + "/span[contains(@class,'ant-tree-switcher') and not(contains(@class,'ant-tree-switcher-noop'))"
                + " and not(contains(@class,'ant-tree-switcher_open'))]", "foldedRows");
        if (folded.isEmpty()) {
            return false;
        }
        folded.get(0).click();
        return false;
    }

    public void clickTreeNode(String nodeName) {
        WebElement node = treeNodeTemplate.format(asShown(nodeName));
        node.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (!node.getAttribute("class").contains("ant-tree-treenode-selected")) {
            node.getLocator().locator("xpath=.//span[contains(@class,'ant-tree-title')]").first().click();
        }
        WaitUtil.requireCondition(() -> node.getAttribute("class").contains("ant-tree-treenode-selected")
                        && (getPage().locator("xpath=" + ANY_PANE + ROWS).count() > 0 || identicalNotice.isVisible(PROBE_MS)),
                DEFAULT_TIMEOUT_MS, 250, "Waiting for '" + nodeName + "' to be drawn");
    }

    public boolean isDrawnAsOneTable() {
        return getPage().locator("xpath=" + pane(COMBINED)).count() > 0
                && "Combined".equals(selectedOption("compare-view"));
    }

    public void showSideBySide() {
        if (SIDE_BY_SIDE.equals(selectedOption("compare-view"))) {
            return;
        }
        Locator option = getPage().locator("xpath=" + String.format(VIEW_OPTION, "compare-view")
                + "[normalize-space()='" + SIDE_BY_SIDE + "']");
        option.click();
        WaitUtil.requireCondition(() -> getPage().locator("xpath=" + pane(COMBINED)).count() == 0
                        && (getPage().locator("xpath=" + pane(1)).count() > 0 || getPage().locator("xpath=" + pane(2)).count() > 0),
                DEFAULT_TIMEOUT_MS, 250, "Waiting for the two versions to stand side by side");
    }

    public String getConflictView() {
        return selectedOption("compare-conflict-view");
    }

    public void showConflictView(String view) {
        getPage().locator("xpath=" + String.format(VIEW_OPTION, "compare-conflict-view")
                + "[normalize-space()='" + view + "']").click();
        WaitUtil.requireCondition(() -> view.equals(selectedOption("compare-conflict-view")) && isComparisonDrawn()
                        && (getPage().locator("xpath=" + paneOf("yours")).count() > 0) == ALL_VERSIONS.equals(view),
                COMPARISON_TIMEOUT_MS, 250, "Waiting for the view '" + view + "' of the conflicted file");
    }

    private String selectedOption(String testId) {
        Locator selected = getPage().locator("xpath=" + String.format(VIEW_OPTION, testId)
                + "[contains(@class,'ant-segmented-item-selected')]");
        return selected.count() == 0 ? "" : selected.first().innerText().trim();
    }

    public boolean isTreeItemPresent(String name) {
        return treeNodeTemplate.format(asShown(name)).isVisible(PROBE_MS);
    }

    public boolean isFirstFragmentPresent() {
        return versionRows(pane(1)).count() > 0;
    }

    public boolean isSecondFragmentPresent() {
        return versionRows(pane(2)).count() > 0;
    }

    public String getCellContent(int fragment, int row, int col) {
        return cellOf(pane(fragment), row, col).innerText().trim();
    }

    public boolean isCellFramed(int fragment, int row, int col) {
        return countMarked(cellOf(pane(fragment), row, col), List.of(CHANGED_FRAME, CONFLICT_FRAME), List.of()) > 0;
    }

    public boolean isRowHighlighted(int fragment, int row) {
        return countMarked(versionRow(pane(fragment), row).locator("xpath=./" + ADDRESSED)) > 0;
    }

    public boolean isSheetRowHighlighted(int fragment, int sheetRow) {
        return countMarked(getPage().locator("xpath=" + pane(fragment) + "//" + ADDRESSED
                + "[translate(@data-cell,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','') = '" + sheetRow + "']")) > 0;
    }

    public boolean isDifferenceShown(int fragment, String text) {
        return countMarked(cellsReading(pane(fragment), text)) > 0;
    }

    public boolean isDifferenceShownIn(String version, String text) {
        return countMarked(cellsReading(paneOf(version), text), List.of(CHANGED_FRAME), ADDED_OR_REMOVED) > 0;
    }

    public boolean isConflictShownIn(String version, String text) {
        return countMarked(cellsReading(paneOf(version), text), List.of(CONFLICT_FRAME), List.of()) > 0;
    }

    public int getHighlightedCellCount(int fragment) {
        return countMarked(getPage().locator("xpath=" + pane(fragment) + "//" + ADDRESSED));
    }

    public int getNumberOfCells(int fragment) {
        return getPage().locator("xpath=" + pane(fragment) + "//" + ADDRESSED).count();
    }

    public int getNumberOfCellsIn(String version) {
        return getPage().locator("xpath=" + paneOf(version) + "//" + ADDRESSED).count();
    }

    public int getHighlightedCellCountIn(String version) {
        return countMarked(getPage().locator("xpath=" + paneOf(version) + "//" + ADDRESSED));
    }

    public List<String> getEqualRowsLeftOut(int fragment) {
        return getPage().locator("xpath=" + pane(fragment) + LEFT_OUT_ROW).allInnerTexts().stream()
                .map(text -> text.replaceAll("\\s+", " ").trim()).toList();
    }

    private Locator cellsReading(String pane, String text) {
        return getPage().locator("xpath=" + pane + "//" + ADDRESSED + "[normalize-space()=\"" + text + "\"]");
    }

    private static int countMarked(Locator cells) {
        return countMarked(cells, List.of(CHANGED_FRAME, CONFLICT_FRAME), ADDED_OR_REMOVED);
    }

    private static int countMarked(Locator cells, List<String> frames, List<String> paints) {
        Object marked = cells.evaluateAll(COUNT_MARKED, Map.of("frames", frames, "paints", paints));
        return ((Number) marked).intValue();
    }

    private Locator cellOf(String pane, int row, int col) {
        Locator corner = getPage().locator("xpath=(" + pane + "//" + ADDRESSED + ")[1]");
        corner.waitFor();
        String letters = columnName(columnNumber(corner.getAttribute("data-cell").replaceAll("[0-9]", "")) + col - 1);
        Locator cell = versionRow(pane, row).locator("xpath=./" + ADDRESSED
                + "[translate(@data-cell,'0123456789','') = '" + letters + "']");
        if (cell.count() == 0) {
            throw new AssertionError("Row " + row + " of " + pane + " draws no cell in column " + letters);
        }
        return cell.first();
    }

    private static int columnNumber(String letters) {
        int number = 0;
        for (char letter : letters.toCharArray()) {
            number = number * 26 + (Character.toUpperCase(letter) - 'A' + 1);
        }
        return number;
    }

    private static String columnName(int number) {
        StringBuilder name = new StringBuilder();
        for (int left = number; left > 0; left = (left - 1) / 26) {
            name.insert(0, (char) ('A' + (left - 1) % 26));
        }
        return name.toString();
    }

    public int getNumberOfRows(int fragment) {
        Locator rows = versionRows(pane(fragment));
        WaitUtil.waitForCondition(() -> rows.count() > 0, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the rows of version " + fragment);
        WaitUtil.waitForStableSize(rows::count, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the rows of version " + fragment + " to settle");
        return rows.count();
    }

    public int getNumberOfColumns(int fragment) {
        Locator rows = versionRows(pane(fragment));
        int widest = 0;
        for (int index = 0; index < rows.count(); index++) {
            widest = Math.max(widest, rows.nth(index).locator("xpath=./" + ADDRESSED).count());
        }
        return widest;
    }

    private Locator versionRows(String pane) {
        return getPage().locator("xpath=" + pane + VERSION_ROW);
    }

    private Locator versionRow(String pane, int row) {
        Locator rows = versionRows(pane);
        if (rows.count() < row) {
            throw new AssertionError(pane + " draws " + rows.count() + " rows, not row " + row);
        }
        return rows.nth(row - 1);
    }

    private static String pane(int fragment) {
        return String.format(PANE, switch (fragment) {
            case COMBINED -> "combined";
            case 1 -> "first";
            case 2 -> "second";
            default -> throw new IllegalArgumentException("No version " + fragment + " is compared");
        });
    }

    private static String paneOf(String version) {
        return String.format(PANE, version);
    }

    public void setShowEqualRows(boolean value) {
        setCheckbox(showEqualRowsCheckbox, value);
    }

    protected void setEqualElementsShown(boolean value) {
        setCheckbox(showEqualElementsCheckbox, value);
    }

    private void setCheckbox(WebElement checkbox, boolean value) {
        checkbox.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (checkbox.isChecked() == value) {
            return;
        }
        checkbox.click();
        WaitUtil.requireCondition(() -> checkbox.isChecked() == value, DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the comparison to be asked for what reads the same");
    }

    public boolean isShowEqualRowsCheckboxVisible() {
        return showEqualRowsCheckbox.isVisible(PROBE_MS);
    }

    public boolean isCompareTextFilesFormClear() {
        return identicalNotice.isVisible(PROBE_MS);
    }

    public void backToFilePicking() {
        backBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        tree.waitForHidden(DEFAULT_TIMEOUT_MS);
    }

    public void close() {
        if (comparePopup != null && !comparePopup.isClosed()) {
            comparePopup.close();
        }
    }

    public String getComparedText() {
        return new WebElement(getPage(), "xpath=//*[@data-testid='compare-text-diff']", "compareTextDiff")
                .waitForVisible(COMPARISON_TIMEOUT_MS).getInnerText();
    }

    public String getEarlierRevision() {
        return revisionCard("compare-revisions-base");
    }

    public String getLaterRevision() {
        return revisionCard("compare-revisions-revision");
    }

    public List<String> getFilesChangedBetweenRevisions() {
        new WebElement(getPage(), "xpath=(//*[@data-testid='revision-changes-file'])[1]", "revisionChangesFile")
                .waitForVisible(COMPARISON_TIMEOUT_MS);
        return getPage().locator("xpath=//*[@data-testid='revision-changes-file']//strong").allInnerTexts().stream()
                .map(String::trim).toList();
    }

    private String revisionCard(String testId) {
        return new WebElement(getPage(), "xpath=//*[@data-testid='" + testId + "']", testId)
                .waitForVisible(COMPARISON_TIMEOUT_MS).getInnerText().replaceAll("\\s+", " ").trim();
    }
}
