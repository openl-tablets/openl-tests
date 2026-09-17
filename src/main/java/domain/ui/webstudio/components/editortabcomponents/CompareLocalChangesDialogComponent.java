package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;

/**
 * The comparison of two versions of a project, drawn in a window of its own.
 *
 * <p>The window lists what differs as a tree, and shows the picked element as two tables side by side: the
 * first version on the left, the second on the right. A cell the versions read differently is painted; the
 * rows that read the same are left out until the reader asks for them.
 */
public class CompareLocalChangesDialogComponent extends BaseComponent {

    private static final String TREE = "//div[@data-testid='compare-tree']";
    private static final String TREE_NODE = TREE + "//div[contains(@class,'ant-tree-treenode')]";
    private static final String PANE = "//div[@data-testid='compare-pane-%s']";
    // The grid leads each row with the number of its line, which is not a cell of the table.
    private static final String CELL = "./td[not(.//span[@data-testid='table-line-number'])]";
    protected static final int PROBE_MS = 1000;
    /** A comparison is worked out on the server, table by table, so it is waited for far longer than a screen. */
    protected static final int COMPARISON_TIMEOUT_MS = 90000;

    protected final WebElement tree;
    protected final WebElement identicalNotice;
    private final WebElement showEqualRowsCheckbox;
    private final WebElement showEqualElementsCheckbox;
    private final WebElement conflictText;
    private final WebElement errorNotice;
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
        errorNotice = new WebElement(getPage(), "xpath=//*[@data-testid='compare-error']", "compareError");
        // An element is named exactly where it can be, and by what its name starts with where the tree adds
        // to it — a table is listed under its header and then what changed about it. The first of the rows
        // that answer is the one meant: the tree lists a name once.
        String named = "[.//span[contains(@class,'ant-tree-title')]"
                + "[normalize-space()=\"%1$s\" or starts-with(normalize-space(),\"%1$s\")]]";
        treeNodeTemplate = new WebElement(getPage(),
                "xpath=(" + TREE_NODE + named + ")[1]", "compareTreeNode");
        treeSwitcherTemplate = new WebElement(getPage(),
                "xpath=(" + TREE_NODE + named + ")[1]"
                + "/span[contains(@class,'ant-tree-switcher') and not(contains(@class,'ant-tree-switcher-noop'))]",
                "compareTreeSwitcher");
    }

    /** Used where the comparison is drawn in the page itself rather than in a window of its own. */
    public CompareLocalChangesDialogComponent(Page page, boolean inlineModal) {
        this(page);
    }

    public CompareLocalChangesDialogComponent waitForDialogToAppear() {
        WaitUtil.requireCondition(this::isComparisonDrawn, COMPARISON_TIMEOUT_MS, 250,
                "Waiting for the comparison to be drawn");
        return this;
    }

    /**
     * Whether the comparison has answered. The box the answer is drawn in appears as soon as the comparison
     * starts, so its presence says nothing; what says the comparison is done is the answer itself — what
     * differs, that nothing does, or why it could not be made.
     */
    protected boolean isComparisonDrawn() {
        if (errorNotice.isVisible(PROBE_MS / 2)) {
            throw new AssertionError("The comparison could not be made: " + errorNotice.getText().trim());
        }
        return identicalNotice.isVisible(PROBE_MS / 2) || !treeTitles().isEmpty();
    }

    public CompareLocalChangesDialogComponent waitForTextCompareToAppear() {
        conflictText.waitForVisible(DEFAULT_TIMEOUT_MS);
        return this;
    }

    /** What the comparison found, one line per element, as the tree lists them. */
    public List<String> getLeftModulesList() {
        WaitUtil.waitForListNotEmpty(this::treeTitles, COMPARISON_TIMEOUT_MS, 250,
                "Waiting for the comparison to list what differs");
        return treeTitles().stream().map(WebElement::getText).map(String::trim).toList();
    }

    /** The same tree: one comparison is shown, not one list per version. */
    public List<String> getRightModulesList() {
        return getLeftModulesList();
    }

    private List<WebElement> treeTitles() {
        return createElementList("xpath=" + TREE_NODE + "//span[contains(@class,'ant-tree-title')]", "compareTreeTitles");
    }

    /**
     * Opens what the named element holds. The element may itself be held by another — a workbook holds its
     * sheets and a sheet its tables — and the tree draws only what is open, so the way down to it is opened
     * first: every folded row is unfolded until the element is drawn, and then the element itself.
     */
    public void openTreeNode(String nodeName) {
        WaitUtil.requireCondition(() -> {
            if (!treeNodeTemplate.format(nodeName).isVisible(PROBE_MS)) {
                return unfoldOne();
            }
            WebElement switcher = treeSwitcherTemplate.format(nodeName);
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

    /** What the tree draws right now, for saying what was there when something looked-for was not. */
    private List<String> drawnTitles() {
        return treeTitles().stream().map(WebElement::getText).map(String::trim).toList();
    }

    /** Unfolds the first row that is still folded, so the tree is walked down one step. */
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
        WebElement node = treeNodeTemplate.format(nodeName);
        node.waitForVisible(DEFAULT_TIMEOUT_MS);
        node.getLocator().locator("xpath=.//span[contains(@class,'ant-tree-title')]").first().click();
        WaitUtil.requireCondition(() -> rowsOf(1).count() > 0 || rowsOf(2).count() > 0 || identicalNotice.isVisible(PROBE_MS),
                DEFAULT_TIMEOUT_MS, 250, "Waiting for '" + nodeName + "' to be shown side by side");
    }

    public boolean isTreeItemPresent(String name) {
        return treeNodeTemplate.format(name).isVisible(PROBE_MS);
    }

    public boolean isFirstFragmentPresent() {
        return rowsOf(1).count() > 0;
    }

    public boolean isSecondFragmentPresent() {
        return rowsOf(2).count() > 0;
    }

    public String getCellContent(int fragment, int row, int col) {
        return cellOf(fragment, row, col).innerText().trim();
    }

    public boolean isCellContainsExpectedValue(int row, int col, String fragment, String expectedValue) {
        return getCellContent(Integer.parseInt(fragment), row, col).equalsIgnoreCase(expectedValue);
    }

    /** A cell the two versions read differently is painted; one they read the same is left as the workbook draws it. */
    public boolean isCellHighlighted(int row, int col, int fragment) {
        String colour = colourOf(fragment, row, col);
        return !"rgb(255, 255, 255)".equals(colour) && !"rgba(0, 0, 0, 0)".equals(colour);
    }

    public boolean isCellHighlightedWhite(int row, int col, String fragment) {
        return !isCellHighlighted(row, col, Integer.parseInt(fragment));
    }

    public boolean isCellHighlightedGreen(int row, int col, String fragment) {
        return isCellHighlighted(row, col, Integer.parseInt(fragment));
    }

    public boolean isCellHighlightedWithColor(int row, int col, String fragment, String colorRGBA) {
        return colourOf(Integer.parseInt(fragment), row, col).equals(colorRGBA);
    }

    /**
     * Whether the row carries a cell the two versions read differently. The grid draws a merged cell once
     * and leaves no place for the cells it covers, so which column a difference falls in is the grid's own
     * counting; which row it falls on is the table's.
     */
    public boolean isRowHighlighted(int fragment, int row) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        String corner = getPage().locator("xpath=" + pane + "//td[@data-cell]").first().getAttribute("data-cell");
        int line = Integer.parseInt(corner.replaceAll("[^0-9]", "")) + row - 1;
        // The address is a column of letters and a line of digits, so the line is what the letters leave.
        Locator cells = getPage().locator("xpath=" + pane + "//td[@data-cell]"
                + "[translate(@data-cell,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','') = '" + line + "']");
        for (int index = 0; index < cells.count(); index++) {
            String colour = cells.nth(index).evaluate("node => getComputedStyle(node).backgroundColor").toString();
            if (!"rgb(255, 255, 255)".equals(colour) && !"rgba(0, 0, 0, 0)".equals(colour)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the version shows the given text as one of the things that differ. A cell is found by what it
     * reads rather than by where it sits: which line a difference is drawn on depends on what the reader
     * asked to be left out, and which column on how the table is drawn.
     */
    public boolean isDifferenceShown(int fragment, String text) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        Locator cells = getPage().locator("xpath=" + pane + "//td[@data-cell][normalize-space()=\"" + text + "\"]");
        for (int index = 0; index < cells.count(); index++) {
            String colour = cells.nth(index).evaluate("node => getComputedStyle(node).backgroundColor").toString();
            if (!"rgb(255, 255, 255)".equals(colour) && !"rgba(0, 0, 0, 0)".equals(colour)) {
                return true;
            }
        }
        return false;
    }

    /** How many cells of the version are painted, which is how many the two versions read differently. */
    public int getHighlightedCellCount(int fragment) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        Locator cells = getPage().locator("xpath=" + pane + "//td[@data-cell]");
        int painted = 0;
        for (int index = 0; index < cells.count(); index++) {
            String colour = cells.nth(index).evaluate("node => getComputedStyle(node).backgroundColor").toString();
            if (!"rgb(255, 255, 255)".equals(colour) && !"rgba(0, 0, 0, 0)".equals(colour)) {
                painted++;
            }
        }
        return painted;
    }

    private String colourOf(int fragment, int row, int col) {
        Locator cell = cellOf(fragment, row, col);
        return cell.evaluate("node => getComputedStyle(node).backgroundColor").toString();
    }

    /**
     * A cell of one of the two versions, named by where it sits in the table: the line of the table and the
     * place along it, both counted from one.
     *
     * <p>Every cell carries the address it has in the workbook, and that is what the cell is found by. Where
     * a cell is drawn is no use: the rows the two versions read the same are left out while the reader has
     * not asked for them, so the same cell would answer to a different place each time that is switched.
     */
    private Locator cellOf(int fragment, int row, int col) {
        String address = addressOf(fragment, row, col);
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        return getPage().locator("xpath=" + pane + "//td[@data-cell='" + address + "']");
    }

    /** The address the cell at that place in the table has in the workbook the table is written in. */
    private String addressOf(int fragment, int row, int col) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        Locator firstAddressed = getPage().locator("xpath=" + pane + "//td[@data-cell]").first();
        firstAddressed.waitFor();
        String corner = firstAddressed.getAttribute("data-cell");
        String letters = corner.replaceAll("[0-9]", "");
        int firstRow = Integer.parseInt(corner.replaceAll("[^0-9]", ""));
        return columnName(columnNumber(letters) + col - 1) + (firstRow + row - 1);
    }

    /** The place a column letter stands for: A is 1, Z is 26, AA is 27. */
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

    /**
     * Rows of one of the two versions. The table is drawn anew whenever the reader asks for other rows, so
     * the count is read once it stops changing: a count taken mid-redraw is the count of half a table.
     */
    public int getNumberOfRows(int fragment) {
        WaitUtil.waitForCondition(() -> rowsOf(fragment).count() > 0, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the rows of version " + fragment);
        WaitUtil.waitForStableSize(() -> rowsOf(fragment).count(), DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the rows of version " + fragment + " to settle");
        return rowsOf(fragment).count();
    }

    public int getNumberOfColumns(int fragment) {
        Locator rows = rowsOf(fragment);
        int widest = 0;
        for (int index = 0; index < rows.count(); index++) {
            widest = Math.max(widest, rows.nth(index).locator("xpath=" + CELL).count());
        }
        return widest;
    }

    private Locator rowsOf(int fragment) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        return getPage().locator("xpath=" + pane + "//table//tbody/tr");
    }

    /** Shows or hides the rows the two versions read the same. */
    public void setShowEqualRows(boolean value) {
        setCheckbox(showEqualRowsCheckbox, value);
    }

    /** Shows or hides the elements the two versions read the same. */
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

    /** Whether the comparison found nothing to show, which it says instead of drawing two empty tables. */
    public boolean isCompareTextFilesFormClear() {
        return identicalNotice.isVisible(PROBE_MS);
    }

    public void close() {
        if (comparePopup != null && !comparePopup.isClosed()) {
            comparePopup.close();
        }
    }
}
