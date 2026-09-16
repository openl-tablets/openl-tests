package domain.ui.webstudio.components.editortabcomponents.leftmenu;

import domain.ui.webstudio.components.BaseComponent;
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

    private static final String TREE = "xpath=//div[@data-testid='module-tables-tree']";
    private static final String TREE_NODE = TREE + "//div[contains(@class,'ant-tree-treenode')]";
    private static final int SETTLE_POLL_MS = 200;
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
                            seen.set(node.id, {
                                id: node.id,
                                depth: find(".//span[contains(@class,'ant-tree-indent-unit')]", node).length,
                                title: (title?.textContent ?? '').trim(),
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
        viewOptionTemplate = new WebElement(page, "xpath=//div[contains(@class,'ant-select-item-option')][@title='%s']", "viewOption");
        searchInput = new WebElement(page, "xpath=//input[@data-testid='module-tables-search']", "tablesSearchInput");
        extendedSearchBtn = new WebElement(page, "xpath=//button[@data-testid='module-tables-search-extended']", "extendedSearchBtn");
        tree = new WebElement(page, TREE, "tablesTree");
        selectedNodeTitle = new WebElement(page, TREE + "//div[contains(@class,'ant-tree-treenode-selected')]//span[contains(@class,'ant-tree-title')]", "selectedNodeTitle");
        tableIconTemplate = new WebElement(page, TREE_NODE + "[.//span[contains(@class,'ant-tree-title')][normalize-space()='%s']]//span[contains(@class,'ant-tree-iconEle')]", "tableIcon");
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
        WaitUtil.requireCondition(() -> {
            Optional<TreeRow> folder = findFolder(folderName);
            if (folder.isEmpty()) {
                return false;
            }
            if (folder.get().expanded()) {
                return true;
            }
            revealNode(folder.get().nodeId());
            folder.get().node().child("xpath=./span[contains(@class,'ant-tree-switcher')]").click();
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

    private void clickNode(TreeRow row) {
        revealNode(row.nodeId());
        row.node().child("xpath=./span[contains(@class,'ant-tree-node-content-wrapper')]").click();
        waitUntilSpinnerLoaded();
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
