package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompareLocalChangesDialogComponent extends BaseComponent {

    private static final String TREE = "//div[@data-testid='compare-tree']";
    private static final String TREE_NODE = TREE + "//div[contains(@class,'ant-tree-treenode')]";
    private static final String PANE = "//div[@data-testid='compare-pane-%s']";
    private static final String CELL = "./td[not(.//span[@data-testid='table-line-number'])]";
    protected static final int PROBE_MS = 1000;
    protected static final int COMPARISON_TIMEOUT_MS = 90000;
    private static final Pattern RGB = Pattern.compile("rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)(?:,\\s*([\\d.]+))?\\)");

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
        WaitUtil.requireCondition(() -> rowsOf(1).count() > 0 || rowsOf(2).count() > 0 || identicalNotice.isVisible(PROBE_MS),
                DEFAULT_TIMEOUT_MS, 250, "Waiting for '" + nodeName + "' to be shown side by side");
    }

    public boolean isTreeItemPresent(String name) {
        return treeNodeTemplate.format(asShown(name)).isVisible(PROBE_MS);
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

    public boolean isCellHighlighted(int row, int col, int fragment) {
        return isPaintedAsDifference(colourOf(fragment, row, col));
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

    public boolean isRowHighlighted(int fragment, int row) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        String corner = getPage().locator("xpath=" + pane + "//td[@data-cell]").first().getAttribute("data-cell");
        return isSheetRowHighlighted(fragment, Integer.parseInt(corner.replaceAll("[^0-9]", "")) + row - 1);
    }

    public boolean isSheetRowHighlighted(int fragment, int sheetRow) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        return countPainted(getPage().locator("xpath=" + pane + "//td[@data-cell]"
                + "[translate(@data-cell,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','') = '" + sheetRow + "']")) > 0;
    }

    private static int countPainted(Locator cells) {
        int painted = 0;
        for (int index = 0; index < cells.count(); index++) {
            if (isPaintedAsDifference(cells.nth(index).evaluate("node => getComputedStyle(node).backgroundColor").toString())) {
                painted++;
            }
        }
        return painted;
    }

    private static boolean isPaintedAsDifference(String colour) {
        Matcher channels = RGB.matcher(colour);
        if (!channels.matches()) {
            return false;
        }
        boolean opaque = channels.group(4) == null || Double.parseDouble(channels.group(4)) > 0;
        boolean grey = channels.group(1).equals(channels.group(2)) && channels.group(2).equals(channels.group(3));
        return opaque && !grey;
    }

    public boolean isDifferenceShown(int fragment, String text) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        return countPainted(getPage().locator("xpath=" + pane + "//td[@data-cell][normalize-space()=\"" + text + "\"]")) > 0;
    }

    public int getHighlightedCellCount(int fragment) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        return countPainted(getPage().locator("xpath=" + pane + "//td[@data-cell]"));
    }

    private String colourOf(int fragment, int row, int col) {
        Locator cell = cellOf(fragment, row, col);
        return cell.evaluate("node => getComputedStyle(node).backgroundColor").toString();
    }

    private Locator cellOf(int fragment, int row, int col) {
        String address = addressOf(fragment, row, col);
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        return getPage().locator("xpath=" + pane + "//td[@data-cell='" + address + "']");
    }

    private String addressOf(int fragment, int row, int col) {
        String pane = String.format(PANE, fragment == 1 ? "first" : "second");
        Locator firstAddressed = getPage().locator("xpath=" + pane + "//td[@data-cell]").first();
        firstAddressed.waitFor();
        String corner = firstAddressed.getAttribute("data-cell");
        String letters = corner.replaceAll("[0-9]", "");
        int firstRow = Integer.parseInt(corner.replaceAll("[^0-9]", ""));
        return columnName(columnNumber(letters) + col - 1) + (firstRow + row - 1);
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
}
