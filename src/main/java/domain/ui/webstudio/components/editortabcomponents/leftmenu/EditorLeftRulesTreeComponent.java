package domain.ui.webstudio.components.editortabcomponents.leftmenu;

import com.microsoft.playwright.PlaywrightException;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import helpers.utils.WaitUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The tables rail of the module screen: the tree of the module's tables, the view it is grouped by and the
 * search above it.
 *
 * <p>The tree is an Ant Design tree, so its rows are siblings in the DOM and the hierarchy is carried by the
 * indent each row draws: a row's depth is the number of indent units before it, and the items of a folder are
 * the rows that follow it until the next row at its own depth or shallower. The rail also draws only the rows
 * it has room for, so reading the whole tree means scrolling it.
 */
public class EditorLeftRulesTreeComponent extends BaseComponent {

    /** The list the Select last opened: a list closed before it stays in the page, and the newest is last. */
    private static final String OPEN_DROPDOWN = "(//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))])[last()]";

    private static final String TREE = "xpath=//div[@data-testid='module-tables-tree']";
    private static final String TREE_NODE = TREE + "//div[contains(@class,'ant-tree-treenode')]";
    private static final int SETTLE_POLL_MS = 200;
    private static final int NODE_PROBE_MS = 1000;
    private static final int NODE_CLICK_MS = 3000;
    private static final int PROBE_MS = 1000;
    private static final long EXPAND_TIMEOUT_MS = 10000;

    private static final String READ_ROWS_SCRIPT = """
            async () => {
                const find = (xpath, root) => {
                    const found = document.evaluate(xpath, root ?? document, null,
                        XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
                    return Array.from({ length: found.snapshotLength }, (unused, i) => found.snapshotItem(i));
                };
                const [tree] = find("//div[@data-testid='module-tables-tree']");
                if (!tree) {
                    return [];
                }
                const [holder] = find(".//div[contains(@class,'ant-tree-list-holder')]", tree);
                const seen = new Map();
                const collect = () => {
                    for (const node of find(".//div[contains(@class,'ant-tree-treenode')]", tree)) {
                        if (!seen.has(node.id)) {
                            const [title] = find(".//span[contains(@class,'ant-tree-title')]", node);
                            // A row carrying errors writes their number beside its name; the name is the row.
                            const named = title?.cloneNode(true);
                            named?.querySelectorAll("[data-testid='module-table-errors']").forEach(count => count.remove());
                            seen.set(node.id, {
                                id: node.id,
                                depth: find(".//span[contains(@class,'ant-tree-indent-unit')]", node).length,
                                title: (named?.textContent ?? '').trim(),
                                leaf: node.className.split(/\\s+/).includes('ant-tree-treenode-leaf'),
                                expanded: node.getAttribute('aria-expanded') === 'true'
                            });
                        }
                    }
                };
                if (!holder) {
                    collect();
                    return Array.from(seen.values());
                }
                holder.scrollTop = 0;
                await new Promise(resolve => setTimeout(resolve, 100));
                const step = Math.max(120, Math.floor(holder.clientHeight * 0.7));
                for (let top = 0; top <= holder.scrollHeight; top += step) {
                    holder.scrollTop = top;
                    await new Promise(resolve => setTimeout(resolve, 80));
                    collect();
                }
                return Array.from(seen.values());
            }
            """;

    private static final String REVEAL_NODE_SCRIPT = """
            async (nodeId) => {
                const find = (xpath, root) => {
                    const found = document.evaluate(xpath, root ?? document, null,
                        XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
                    return Array.from({ length: found.snapshotLength }, (unused, i) => found.snapshotItem(i));
                };
                const [tree] = find("//div[@data-testid='module-tables-tree']");
                if (!tree) {
                    return false;
                }
                const drawn = () => find(".//div[@id=" + JSON.stringify(nodeId) + "]", tree).length > 0;
                const [holder] = find(".//div[contains(@class,'ant-tree-list-holder')]", tree);
                if (!holder || drawn()) {
                    return drawn();
                }
                const step = Math.max(120, Math.floor(holder.clientHeight * 0.7));
                for (let top = 0; top <= holder.scrollHeight; top += step) {
                    holder.scrollTop = top;
                    await new Promise(resolve => setTimeout(resolve, 80));
                    if (drawn()) {
                        return true;
                    }
                }
                return drawn();
            }
            """;

    private WebElement viewSelect;
    private WebElement viewSelectValue;
    private WebElement viewOptionTemplate;
    private WebElement searchInput;
    private WebElement extendedSearchBtn;
    private WebElement tree;
    private WebElement selectedNodeTitle;
    private WebElement tableIconTemplate;

    public EditorLeftRulesTreeComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public EditorLeftRulesTreeComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        viewSelect = new WebElement(page, "xpath=//div[@data-testid='module-tables-view']", "viewSelect");
        viewSelectValue = new WebElement(page, "xpath=//div[@data-testid='module-tables-view']//div[contains(@class,'ant-select-content')]", "viewSelectValue");
        viewOptionTemplate = new WebElement(page, "xpath=" + OPEN_DROPDOWN + "//div[contains(@class,'ant-select-item-option')][@title='%s']", "viewOption");
        searchInput = new WebElement(page, "xpath=//input[@data-testid='module-tables-search']", "tablesSearchInput");
        extendedSearchBtn = new WebElement(page, "xpath=//button[@data-testid='module-tables-search-extended']", "extendedSearchBtn");
        tree = new WebElement(page, TREE, "tablesTree");
        selectedNodeTitle = new WebElement(page, TREE + "//div[contains(@class,'ant-tree-treenode-selected')]//span[contains(@class,'ant-tree-title')]", "selectedNodeTitle");
        // A group may be named after a table it gathers — the Constants group holds the Constants table — so
        // the row read here is the table's: a group's row is named after what it groups by.
        tableIconTemplate = new WebElement(page, TREE_NODE + "[not(contains(@id,'-grp-'))]"
                + "[.//span[contains(@class,'ant-tree-title')][normalize-space()='%s']]"
                + "//span[contains(@class,'ant-tree-iconEle')]", "tableIcon");
    }

    private record TreeRow(int index, int depth, String title, boolean folder, boolean expanded, String nodeId, WebElement node) {
    }

    public String getViewFilterValue() {
        waitUntilSpinnerLoaded();
        return viewSelectValue.getText().trim();
    }

    /** The names the tree shows right now: the folders it groups by, and whatever stands open inside them. */
    public List<String> getCategoriesVisible() {
        waitUntilSpinnerLoaded();
        return readRows().stream()
                .map(TreeRow::title)
                .filter(title -> !title.isEmpty())
                .toList();
    }

    /** The groups the rail draws, without the tables filed under them. */
    public List<String> getFoldersVisible() {
        waitUntilSpinnerLoaded();
        return readRows().stream()
                .filter(TreeRow::folder)
                .map(TreeRow::title)
                .filter(title -> !title.isEmpty())
                .toList();
    }

    public List<String> getAllEndNodesNames() {
        waitUntilSpinnerLoaded();
        return readRows().stream()
                .filter(row -> !row.folder())
                .map(TreeRow::title)
                .toList();
    }

    public EditorLeftRulesTreeComponent waitForTreeFoldersToLoad() {
        WaitUtil.requireCondition(() -> !readRows().isEmpty(), DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS,
                "Waiting for the tables tree to be drawn");
        return this;
    }

    /** Selects a leaf of the tree, tolerating a tree that is still being rebuilt after a save. */
    public EditorLeftRulesTreeComponent selectVisibleLeafNode(String itemName) {
        waitUntilSpinnerLoaded();
        TreeRow leaf = WaitUtil.waitForResult(() -> readRows().stream()
                        .filter(row -> !row.folder() && itemName.equals(row.title()))
                        .findFirst(),
                DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS, "Searching for visible leaf node '" + itemName + "' in the tables tree")
                .orElseThrow(() -> new RuntimeException(String.format("Visible leaf node with name %s not found", itemName)));
        clickNode(leaf);
        return this;
    }

    public EditorLeftRulesTreeComponent setViewFilter(FilterOptions filterOption) {
        waitUntilSpinnerLoaded();
        clearWindowsOverTheRail();
        WaitUtil.requireCondition(() -> {
            if (filterOption.getValue().equals(getViewFilterValue())) {
                return true;
            }
            viewSelect.click();
            WebElement option = viewOptionTemplate.format(filterOption.getValue());
            if (!option.isVisible(PROBE_MS)) {
                return false;
            }
            option.click();
            waitUntilSpinnerLoaded();
            return filterOption.getValue().equals(getViewFilterValue());
        }, DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS, "Applying the '" + filterOption.getValue() + "' view to the tables tree");
        return this;
    }

    public EditorLeftRulesTreeComponent expandFolderInTree(String folderName) {
        waitUntilSpinnerLoaded();
        clearWindowsOverTheRail();
        WaitUtil.requireCondition(() -> {
            Optional<TreeRow> folder = findFolder(folderName);
            if (folder.isEmpty()) {
                return false;
            }
            if (folder.get().expanded()) {
                return true;
            }
            revealNode(folder.get().nodeId());
            movePointerAway();
            try {
                folder.get().node().child("xpath=./span[contains(@class,'ant-tree-switcher')]").click(NODE_CLICK_MS);
            } catch (PlaywrightException covered) {
                LOGGER.info("The folder '{}' could not be pressed, trying again: {}", folderName, covered.getMessage());
                return false;
            }
            // The row redraws as it opens, so its state is read back before another press is considered.
            return WaitUtil.waitForCondition(
                    () -> findFolder(folderName).map(TreeRow::expanded).orElse(false),
                    EXPAND_TIMEOUT_MS, SETTLE_POLL_MS, "Waiting for folder '" + folderName + "' to open");
        }, DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS, "Expanding folder '" + folderName + "' in the tables tree");
        return this;
    }

    /**
     * Selects an item of a folder. Saving a table makes the project recompile and the tree is rebuilt while
     * that runs, so the lookup is retried rather than failing on a tree that is mid-refresh.
     */
    public EditorLeftRulesTreeComponent selectItemInFolder(String folderName, String itemName) {
        return selectItemInFolderByIndex(folderName, itemName, 1);
    }

    public EditorLeftRulesTreeComponent selectItemInFolderByIndex(String folderName, String itemName, int index) {
        waitUntilSpinnerLoaded();
        expandFolderInTree(folderName);
        TreeRow item = WaitUtil.waitForResult(() -> itemsOfFolder(folderName).stream()
                        .filter(row -> itemName.equals(row.title()))
                        .skip(Math.max(0, index - 1))
                        .findFirst(),
                DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS,
                "Searching for '" + itemName + "' in folder '" + folderName + "'")
                .orElseThrow(() -> new RuntimeException(
                        String.format("Item %s not found in folder %s", itemName, folderName)));
        clickNode(item);
        return this;
    }

    public boolean isItemExistsInTree(String itemName) {
        return WaitUtil.waitForCondition(() -> readRows().stream()
                        .anyMatch(row -> !row.folder() && itemName.equals(row.title())),
                DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS, "Checking if item '" + itemName + "' exists in the tables tree");
    }

    public boolean isFolderExistsInTree(String folderName) {
        return WaitUtil.waitForCondition(() -> findFolder(folderName).isPresent(),
                DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS, "Checking if folder '" + folderName + "' exists in the tables tree");
    }

    public String getSelectedItemText() {
        if (selectedNodeTitle.isVisible(PROBE_MS)) {
            return selectedNodeTitle.getText().trim();
        }
        return "";
    }

    public void checkRulesTablePresent(String folderName, String tableName) {
        if (!isItemExistsInFolder(folderName, tableName)) {
            throw new AssertionError(String.format("Table '%s' not found in folder '%s'", tableName, folderName));
        }
    }

    public void checkRulesTableAbsent(String folderName, String tableName) {
        if (!isItemNotExistsInFolder(folderName, tableName)) {
            throw new AssertionError(String.format("Table '%s' should not exist in folder '%s'", tableName, folderName));
        }
    }

    public boolean isItemExistsInFolder(String folderName, String itemName) {
        return WaitUtil.waitForCondition(() -> {
            try {
                expandFolderInTree(folderName);
                return itemsOfFolder(folderName).stream().anyMatch(row -> itemName.equals(row.title()));
            } catch (RuntimeException treeIsBeingRebuilt) {
                return false;
            }
        }, DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS, "Checking if item '" + itemName + "' exists in folder '" + folderName + "'");
    }

    /**
     * Whether the folder is without the item. The tree has to be drawn first, so a tree still being rebuilt
     * after a save is not mistaken for a tree the item has gone from.
     */
    public boolean isItemNotExistsInFolder(String folderName, String itemName) {
        waitForTreeFoldersToLoad();
        if (findFolder(folderName).isEmpty()) {
            return true;
        }
        expandFolderInTree(folderName);
        return itemsOfFolder(folderName).stream().noneMatch(row -> itemName.equals(row.title()));
    }

    public WebElement getTableIcon(String tableName) {
        return tableIconTemplate.format(tableName);
    }

    /**
     * The name of the glyph a table wears. The rail draws a drawn icon rather than a small picture, and each
     * one names itself, so what a table wears is read by that name.
     */
    public String getTableIconName(String tableName) {
        // The rail draws only the rows a reader could see, so the row is scrolled to before it is read.
        TreeRow row = readRows().stream()
                .filter(drawn -> tableName.equals(drawn.title()) && !drawn.folder())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("The tables tree lists no table named " + tableName));
        revealNode(row.nodeId());
        // A table some test exercises wears a tick over its own icon, so the first glyph is the table's.
        WebElement icon = row.node().child("xpath=(.//span[contains(@class,'ant-tree-iconEle')]//*[@data-icon])[1]");
        icon.waitForVisible(DEFAULT_TIMEOUT_MS);
        return icon.getAttribute("data-icon");
    }

    /** Filters the tree by the name typed into the search box above it. */
    public EditorLeftRulesTreeComponent searchByName(String text) {
        searchInput.click();
        searchInput.fill(text);
        WaitUtil.sleep(500, "Waiting for the tables tree to be filtered by name");
        return this;
    }

    public void openExtendedSearch() {
        extendedSearchBtn.click();
    }

    /**
     * Puts away whatever the reader opened over the screen. The rail lies under it, and nothing on the rail
     * can be pressed through a window: the history of the module and the report of a run both stand there.
     */
    private void clearWindowsOverTheRail() {
        new ChangesDialogComponent().closeIfOpen();
        new TestResultValidationComponent().closeResults();
    }

    private Optional<TreeRow> findFolder(String folderName) {
        return readRows().stream()
                .filter(row -> row.folder() && folderName.equals(row.title()))
                .findFirst();
    }

    /** The rows that sit under the folder: everything down to the next row at its own depth or shallower. */
    private List<TreeRow> itemsOfFolder(String folderName) {
        List<TreeRow> rows = readRows();
        TreeRow folder = rows.stream()
                .filter(row -> row.folder() && folderName.equals(row.title()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("Folder with name %s not found", folderName)));
        List<TreeRow> items = new ArrayList<>();
        for (int i = folder.index() + 1; i < rows.size(); i++) {
            TreeRow row = rows.get(i);
            if (row.depth() <= folder.depth()) {
                break;
            }
            items.add(row);
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private List<TreeRow> readRows() {
        tree.waitForVisible(DEFAULT_TIMEOUT_MS);
        Object read = page.evaluate(READ_ROWS_SCRIPT);
        List<TreeRow> rows = new ArrayList<>();
        if (!(read instanceof List<?> rowsRead)) {
            return rows;
        }
        int index = 0;
        for (Object entry : rowsRead) {
            Map<String, Object> row = (Map<String, Object>) entry;
            String id = String.valueOf(row.get("id"));
            rows.add(new TreeRow(index++,
                    ((Number) row.get("depth")).intValue(),
                    String.valueOf(row.get("title")).trim(),
                    !Boolean.TRUE.equals(row.get("leaf")),
                    Boolean.TRUE.equals(row.get("expanded")),
                    id,
                    nodeById(id)));
        }
        return rows;
    }

    private WebElement nodeById(String nodeId) {
        return new WebElement(page, TREE + "//div[@id=\"" + nodeId + "\"]", "treeNode[" + nodeId + "]");
    }

    /**
     * Selects the row. The rail draws itself anew whenever the module is read again — after a run, a save or
     * a table opened — and gives every row a fresh id as it does, so a row read a moment ago may no longer be
     * the row standing in the page. The row is therefore looked up again by what names it rather than by the
     * id it was read under.
     */
    private void clickNode(TreeRow row) {
        clearWindowsOverTheRail();
        WaitUtil.requireCondition(() -> {
            TreeRow current = rowStandingFor(row);
            if (current == null || !Boolean.TRUE.equals(page.evaluate(REVEAL_NODE_SCRIPT, current.nodeId()))) {
                return false;
            }
            WebElement title = current.node().child("xpath=./span[contains(@class,'ant-tree-node-content-wrapper')]");
            if (!title.isVisible(NODE_PROBE_MS)) {
                return false;
            }
            // The rail names a row in a label under the pointer, and that label lies over the rows beside
            // it until the pointer leaves the row it belongs to.
            movePointerAway();
            try {
                title.click(NODE_CLICK_MS);
            } catch (PlaywrightException covered) {
                LOGGER.info("The row '{}' could not be pressed, trying again: {}", row.title(), covered.getMessage());
                return false;
            }
            return true;
        }, DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS, "Selecting '" + row.title() + "' in the tables tree");
        waitUntilSpinnerLoaded();
    }

    /**
     * The row now standing where the given one stood: the one carrying its id while the rail is unchanged,
     * and otherwise the one drawn under the same name at the same depth, nearest to where it stood — which
     * is what tells two tables of one name apart.
     */
    private TreeRow rowStandingFor(TreeRow wanted) {
        List<TreeRow> rows = readRows();
        return rows.stream()
                .filter(row -> row.nodeId().equals(wanted.nodeId()))
                .findFirst()
                .orElseGet(() -> rows.stream()
                        .filter(row -> row.depth() == wanted.depth()
                                && row.folder() == wanted.folder()
                                && row.title().equals(wanted.title()))
                        .min(java.util.Comparator.comparingInt(row -> Math.abs(row.index() - wanted.index())))
                        .orElse(null));
    }

    /** Scrolls the rail until the row is drawn: a row the rail has no room for is not in the page at all. */
    private void revealNode(String nodeId) {
        if (!Boolean.TRUE.equals(page.evaluate(REVEAL_NODE_SCRIPT, nodeId))) {
            throw new RuntimeException("The tables tree never drew the row " + nodeId + ", even scrolled to the end");
        }
    }

    @Getter
    public enum FilterOptions {
        BY_TYPE("Type"),
        BY_EXCEL_SHEET("Excel Sheet"),
        BY_CATEGORY("Category"),
        BY_CATEGORY_DETAILED("Category Detailed"),
        BY_CATEGORY_INVERSED("Category Inversed");

        private String value;

        FilterOptions(String value) {
            this.value = value;
        }
    }
}
