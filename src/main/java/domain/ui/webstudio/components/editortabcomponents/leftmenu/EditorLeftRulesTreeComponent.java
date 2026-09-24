package domain.ui.webstudio.components.editortabcomponents.leftmenu;

import com.microsoft.playwright.PlaywrightException;
import domain.ui.webstudio.components.BaseComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import helpers.utils.WaitUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EditorLeftRulesTreeComponent extends BaseComponent {

    private static final String OPEN_DROPDOWN = "(//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))])[last()]";

    private static final String TREE = "xpath=//div[@data-testid='module-tables-tree']";
    private static final String TREE_NODE = TREE + "//div[contains(@class,'ant-tree-treenode')]";
    private static final String ROW_NAME = "//span[contains(@class,'ant-tree-title')]"
            + "//span[not(.//span)][not(@data-testid='module-table-errors')]";
    private static final int SETTLE_POLL_MS = 200;
    private static final int NODE_PROBE_MS = 1000;
    private static final int NODE_CLICK_MS = 3000;
    private static final int PROBE_MS = 1000;
    private static final long EXPAND_TIMEOUT_MS = 10000;
    private static final long TREE_DRAWN_TIMEOUT_MS = 90000;
    private static final String FILTER_DIALOG = "//div[contains(@class,'ant-modal-wrap')]"
            + "[.//input[@data-testid='module-tables-other']]";

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
    private WebElement filterBtn;
    private WebElement filterDialog;
    private WebElement showUtilityTablesCheckbox;
    private WebElement filterApplyBtn;
    private WebElement filterCancelBtn;
    private WebElement tablesReloading;

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
        selectedNodeTitle = new WebElement(page, TREE + "//div[contains(@class,'ant-tree-treenode-selected')]" + ROW_NAME, "selectedNodeTitle");
        tableIconTemplate = new WebElement(page, TREE_NODE + "[not(contains(@id,'-grp-'))]"
                + "[." + ROW_NAME + "[normalize-space()='%s']]"
                + "//span[contains(@class,'ant-tree-iconEle')]", "tableIcon");
        filterBtn = new WebElement(page, "xpath=//button[@data-testid='module-tables-filter']", "tablesFilterBtn");
        filterDialog = new WebElement(page, "xpath=" + FILTER_DIALOG, "tablesFilterDialog");
        showUtilityTablesCheckbox = new WebElement(page, "xpath=" + FILTER_DIALOG
                + "//input[@data-testid='module-tables-other']", "showUtilityTablesCheckbox");
        filterApplyBtn = new WebElement(page, "xpath=" + FILTER_DIALOG
                + "//div[contains(@class,'ant-modal-footer')]//button[normalize-space()='Apply']", "tablesFilterApplyBtn");
        filterCancelBtn = new WebElement(page, "xpath=" + FILTER_DIALOG
                + "//div[contains(@class,'ant-modal-footer')]//button[normalize-space()='Cancel']", "tablesFilterCancelBtn");
        tablesReloading = new WebElement(page, "xpath=//div[@data-testid='module-tables-reloading']"
                + "[contains(concat(' ',normalize-space(@class),' '),' ant-spin-spinning ')]", "tablesReloading");
    }

    private record TreeRow(int index, int depth, String title, boolean folder, boolean expanded, String nodeId, WebElement node) {
    }

    public String getViewFilterValue() {
        waitUntilSpinnerLoaded();
        return viewSelectValue.getText().trim();
    }

    public List<String> getCategoriesVisible() {
        waitUntilSpinnerLoaded();
        return readRows().stream()
                .map(TreeRow::title)
                .filter(title -> !title.isEmpty())
                .toList();
    }

    public List<String> getFoldersVisible() {
        waitUntilSpinnerLoaded();
        return readRows().stream()
                .filter(TreeRow::folder)
                .map(TreeRow::title)
                .filter(title -> !title.isEmpty())
                .toList();
    }

    private List<TreeRow> leavesNamed(String tableName) {
        return readRows().stream()
                .filter(row -> !row.folder() && tableName.equals(row.title()))
                .toList();
    }

    public int countLeavesNamed(String tableName) {
        return leavesNamed(tableName).size();
    }

    public long countInactiveLeavesNamed(String tableName) {
        return leavesNamed(tableName).stream()
                .filter(row -> "module-table-inactive".equals(row.node().getAttribute("data-testid")))
                .count();
    }

    public EditorLeftRulesTreeComponent selectLeafNamed(String tableName, int occurrence) {
        List<TreeRow> drawn = leavesNamed(tableName);
        if (drawn.size() < occurrence) {
            throw new RuntimeException("The tables tree draws " + drawn.size() + " tables named " + tableName);
        }
        clickNode(drawn.get(occurrence - 1));
        return this;
    }

    public EditorLeftRulesTreeComponent selectItemInFolderRaisingErrors(String folderName, String itemName,
                                                                       boolean raisingErrors) {
        waitUntilSpinnerLoaded();
        expandFolderInTree(folderName);
        TreeRow item = WaitUtil.waitForResult(() -> itemsOfFolder(folderName).stream()
                        .filter(row -> !row.folder())
                        .filter(row -> itemName.equals(row.title()))
                        .filter(row -> raisesErrors(row) == raisingErrors)
                        .findFirst(),
                DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS,
                "Searching for the '" + itemName + "' of folder '" + folderName + "' that "
                        + (raisingErrors ? "raised errors" : "raised none"))
                .orElseThrow(() -> new AssertionError(String.format(
                        "No table '%s' of folder '%s' %s errors", itemName, folderName,
                        raisingErrors ? "raised" : "raised no")));
        clickNode(item);
        return this;
    }

    private boolean raisesErrors(TreeRow row) {
        return row.node().getLocator().locator("xpath=.//*[@data-testid='module-table-errors']").count() > 0;
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
        closeWindowsOverTheScreen();
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
        closeWindowsOverTheScreen();
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
            return WaitUtil.waitForCondition(
                    () -> findFolder(folderName).map(TreeRow::expanded).orElse(false),
                    EXPAND_TIMEOUT_MS, SETTLE_POLL_MS, "Waiting for folder '" + folderName + "' to open");
        }, DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS, "Expanding folder '" + folderName + "' in the tables tree");
        return this;
    }

    public EditorLeftRulesTreeComponent selectItemInFolder(String folderName, String itemName) {
        return selectItemInFolderByIndex(folderName, itemName, 1);
    }

    public EditorLeftRulesTreeComponent selectItemInFolderByIndex(String folderName, String itemName, int index) {
        waitUntilSpinnerLoaded();
        expandFolderInTree(folderName);
        TreeRow item = WaitUtil.waitForResult(() -> itemsOfFolder(folderName).stream()
                        .filter(row -> !row.folder())
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

    public String getTableIconName(String tableName) {
        TreeRow row = readRows().stream()
                .filter(drawn -> tableName.equals(drawn.title()) && !drawn.folder())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("The tables tree lists no table named " + tableName));
        revealNode(row.nodeId());
        WebElement icon = row.node().child("xpath=(.//span[contains(@class,'ant-tree-iconEle')]//*[@data-icon])[1]");
        icon.waitForVisible(DEFAULT_TIMEOUT_MS);
        return icon.getAttribute("data-icon");
    }

    public EditorLeftRulesTreeComponent searchByName(String text) {
        searchInput.click();
        searchInput.fill(text);
        WaitUtil.sleep(500, "Waiting for the tables tree to be filtered by name");
        return this;
    }

    public void openExtendedSearch() {
        extendedSearchBtn.click();
    }

    public EditorLeftRulesTreeComponent showUtilityTables() {
        waitUntilSpinnerLoaded();
        closeWindowsOverTheScreen();
        filterBtn.click();
        if (showUtilityTablesCheckbox.isChecked()) {
            filterCancelBtn.click();
        } else {
            showUtilityTablesCheckbox.check();
            page.waitForResponse(
                    response -> response.url().contains("/tables?") && response.url().contains("includeOther=true"),
                    filterApplyBtn::click);
        }
        filterDialog.waitForHidden(DEFAULT_TIMEOUT_MS);
        tablesReloading.waitForHidden(TREE_DRAWN_TIMEOUT_MS);
        return this;
    }

    public List<String> getTablesOfFolder(String folderName) {
        expandFolderInTree(folderName);
        return itemsOfFolder(folderName).stream()
                .filter(row -> !row.folder())
                .map(TreeRow::title)
                .toList();
    }

    private Optional<TreeRow> findFolder(String folderName) {
        return readRows().stream()
                .filter(row -> row.folder() && folderName.equals(row.title()))
                .findFirst();
    }

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

    private void waitForTreeDrawn() {
        WaitUtil.requireCondition(() -> tree.isVisible(SETTLE_POLL_MS), TREE_DRAWN_TIMEOUT_MS, SETTLE_POLL_MS,
                "Waiting for the module tables tree to be drawn");
    }

    @SuppressWarnings("unchecked")
    private List<TreeRow> readRows() {
        waitForTreeDrawn();
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
        closeWindowsOverTheScreen();
        WaitUtil.requireCondition(() -> {
            TreeRow current = rowStandingFor(row);
            if (current == null || !Boolean.TRUE.equals(page.evaluate(REVEAL_NODE_SCRIPT, current.nodeId()))) {
                return false;
            }
            WebElement title = current.node().child("xpath=./span[contains(@class,'ant-tree-node-content-wrapper')]");
            if (!title.isVisible(NODE_PROBE_MS)) {
                return false;
            }
            movePointerAway();
            try {
                title.click(NODE_CLICK_MS);
            } catch (PlaywrightException covered) {
                LOGGER.info("The row '{}' could not be pressed, trying again: {}", row.title(), covered.getMessage());
                return false;
            }
            return true;
        }, DEFAULT_TIMEOUT_MS, SETTLE_POLL_MS, "Selecting '" + row.title() + "' in the tables tree");
        movePointerAway();
        waitUntilSpinnerLoaded();
    }

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
