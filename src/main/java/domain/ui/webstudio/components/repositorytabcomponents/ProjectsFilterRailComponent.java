package domain.ui.webstudio.components.repositorytabcomponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;

import helpers.utils.WaitUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProjectsFilterRailComponent extends BaseComponent {

    public static final String REPOSITORY_FILTER_GROUP = "repository";
    public static final String STATUS_FILTER_GROUP = "status";
    public static final String BRANCH_FILTER_GROUP = "branch";

    private static final int PROBE_MS = DEFAULT_TIMEOUT_MS / 5;
    private static final int DRAG_START_PX = 8;
    private static final int DRAG_STEPS = 20;
    private static final double TITLE_X_SHARE = 0.35;
    private static final String GROUP_ID_PREFIX = "filter-group-";
    private static final String RAIL_MODE = "//*[@data-testid='projects-rail-mode']//label[contains(@class,'ant-segmented-item')]";

    private final WebElement filterGroupToggle;
    private final WebElement filterGroupShow;
    private final WebElement filterGroupHandle;
    private final WebElement filterGroup;
    private final WebElement arrangeButton;
    private final WebElement arrangeDoneButton;
    private final WebElement filterRepoCheckbox;
    private final WebElement filterStatusCheckbox;
    private final WebElement filterBranchCheckbox;
    private final WebElement filtersMode;
    private final WebElement filtersModeSelected;
    private final WebElement treeMode;
    private final WebElement treeModeSelected;
    private final WebElement groupBtn;
    private final WebElement firstLevelSelect;
    private final WebElement levelOptions;
    private final WebElement groupingCloseBtn;

    public ProjectsFilterRailComponent(Page page) {
        this(new WebElement(page, "xpath=//*[@data-testid='projects-rail']", "projectsRail"));
    }

    public ProjectsFilterRailComponent(WebElement rootLocator) {
        super(rootLocator);
        filterGroupToggle = createScopedElement("xpath=.//*[@data-testid='filter-toggle-%s']", "filterGroupToggle");
        filterGroupShow = createScopedElement("xpath=.//*[@data-testid='filter-show-%s']", "filterGroupShow");
        filterGroup = createScopedElement("xpath=.//*[@data-testid='filter-group-%s']", "filterGroup");
        filterGroupHandle = createScopedElement("xpath=.//*[@data-testid='filter-drag-%s']", "filterGroupHandle");
        arrangeButton = createScopedElement("xpath=.//*[@data-testid='projects-filter-arrange']", "arrangeButton");
        arrangeDoneButton = createScopedElement("xpath=.//*[@data-testid='projects-filter-arrange-done']", "arrangeDoneButton");
        filterRepoCheckbox = createScopedElement("xpath=.//*[@data-testid='filter-repo-%s']", "filterRepoCheckbox");
        filterStatusCheckbox = createScopedElement("xpath=.//*[@data-testid='filter-status-%s']", "filterStatusCheckbox");
        filterBranchCheckbox = createScopedElement("xpath=.//*[@data-testid='filter-branch-%s']", "filterBranchCheckbox");
        filtersMode = new WebElement(page, "xpath=" + RAIL_MODE + "[normalize-space()='Filters']", "projectsRailFiltersMode");
        filtersModeSelected = new WebElement(page, "xpath=" + RAIL_MODE
                + "[contains(@class,'ant-segmented-item-selected')][normalize-space()='Filters']", "projectsRailFiltersModeSelected");
        treeMode = new WebElement(page, "xpath=" + RAIL_MODE + "[normalize-space()='Tree']", "projectsRailTreeMode");
        treeModeSelected = new WebElement(page, "xpath=" + RAIL_MODE
                + "[contains(@class,'ant-segmented-item-selected')][normalize-space()='Tree']", "projectsRailTreeModeSelected");
        groupBtn = new WebElement(page, "xpath=//*[@data-testid='projects-tree-group']", "groupProjectsBtn");
        firstLevelSelect = new WebElement(page, "xpath=//*[@data-testid='grouping-level-1']//input", "groupingLevelOne");
        levelOptions = new WebElement(page,
                "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' ant-select-dropdown ')]"
                        + "[not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "//div[contains(concat(' ',normalize-space(@class),' '),' ant-select-item-option ')]",
                "groupingLevelOptions");
        groupingCloseBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-modal')]"
                + "[.//*[@data-testid='grouping-level-1']]//button[contains(@class,'ant-modal-close')]", "groupingCloseBtn");
    }

    public void showFilters() {
        if (filtersMode.isVisible(PROBE_MS) && !filtersModeSelected.exists()) {
            filtersMode.click();
            filtersModeSelected.waitForVisible(DEFAULT_TIMEOUT_MS);
        }
    }

    public List<String> getGroupingLevelOptions() {
        if (treeMode.isVisible(PROBE_MS) && !treeModeSelected.exists()) {
            treeMode.click();
            treeModeSelected.waitForVisible(DEFAULT_TIMEOUT_MS);
        }
        groupBtn.click();
        firstLevelSelect.click();
        levelOptions.getLocator().first().waitFor();
        List<String> names = new ArrayList<>();
        Locator options = levelOptions.getLocator();
        for (int i = 0; i < options.count(); i++) {
            names.add(options.nth(i).innerText().trim());
        }
        if ("true".equals(firstLevelSelect.getAttribute("aria-expanded"))) {
            firstLevelSelect.press("Escape");
        }
        groupingCloseBtn.click();
        return names;
    }

    public void startArranging() {
        showFilters();
        arrangeButton.click();
        arrangeDoneButton.waitForVisible(DEFAULT_TIMEOUT_MS);
    }

    public void finishArranging() {
        arrangeDoneButton.click();
        arrangeButton.waitForVisible(DEFAULT_TIMEOUT_MS);
    }

    public List<String> getGroupOrder() {
        showFilters();
        List<List<String>> reads = new ArrayList<>(List.of(List.of()));
        WaitUtil.waitForCondition(() -> {
            List<String> current = readTestIdSuffixes("*", GROUP_ID_PREFIX);
            boolean settled = !current.isEmpty() && current.equals(reads.getLast());
            reads.add(current);
            return settled;
        }, DEFAULT_TIMEOUT_MS, 250, "Waiting for the filter rail to list its groups");
        return reads.getLast();
    }

    public int countFilterValues() {
        return getRootLocator().getLocator().locator("xpath=.//input[@type='checkbox']").count();
    }

    public boolean waitForFilterValues(boolean shown) {
        showFilters();
        return WaitUtil.waitForCondition(() -> (countFilterValues() > 0) == shown, DEFAULT_TIMEOUT_MS, 100,
                "Waiting for the filter values to be " + (shown ? "listed" : "hidden"));
    }

    public void dragGroupOnto(String groupId, String targetGroupId) {
        showFilters();
        BoundingBox group = boxOf(filterGroup.format(groupId));
        BoundingBox from = boxOf(filterGroupHandle.format(groupId));
        BoundingBox to = boxOf(filterGroupHandle.format(targetGroupId));
        double x = group.x + group.width * TITLE_X_SHARE;
        double fromY = from.y + from.height / 2;
        double toY = to.y + to.height / 2;
        page.mouse().move(x, fromY);
        page.mouse().down();
        page.mouse().move(x, fromY + Math.signum(toY - fromY) * DRAG_START_PX, new Mouse.MoveOptions().setSteps(3));
        page.mouse().move(x, toY, new Mouse.MoveOptions().setSteps(DRAG_STEPS));
        page.mouse().up();
    }

    private BoundingBox boxOf(WebElement element) {
        return Objects.requireNonNull(element.waitForVisible(DEFAULT_TIMEOUT_MS).getLocator().boundingBox(),
                "The filter rail element is not drawn");
    }

    public void expandFilterGroup(String groupId) {
        showFilters();
        WebElement showBtn = filterGroupShow.format(groupId);
        if (showBtn.isVisible(PROBE_MS)) {
            showBtn.click();
        }
        WebElement toggle = filterGroupToggle.format(groupId);
        if (toggle.isVisible(PROBE_MS) && "false".equals(toggle.getAttribute("aria-expanded"))) {
            toggle.click();
        }
    }

    public void ensureRepoFilterChecked(String repositoryNameLower) {
        expandFilterGroup(REPOSITORY_FILTER_GROUP);
        WebElement checkbox = filterRepoCheckbox.format(repositoryNameLower);
        if (checkbox.isVisible(PROBE_MS) && !checkbox.isChecked()) {
            checkbox.click();
            waitUntilSpinnerLoaded();
        }
    }

    public void setStatusFilter(String status, boolean checked) {
        expandFilterGroup(STATUS_FILTER_GROUP);
        WebElement checkbox = filterStatusCheckbox.format(status);
        if (checkbox.isVisible(PROBE_MS) && checkbox.isChecked() != checked) {
            checkbox.click();
            waitUntilSpinnerLoaded();
        }
    }

    public List<String> getBranchFilterOptions() {
        expandFilterGroup(BRANCH_FILTER_GROUP);
        List<List<String>> reads = new ArrayList<>(List.of(List.of()));
        WaitUtil.waitForCondition(() -> {
            List<String> current = readBranchFilterOptions();
            boolean settled = !current.isEmpty() && current.equals(reads.getLast());
            reads.add(current);
            return settled;
        }, DEFAULT_TIMEOUT_MS, 500, "Waiting for the branch filter to list its branches");
        return reads.getLast();
    }

    private List<String> readBranchFilterOptions() {
        return readTestIdSuffixes("input[@type='checkbox']", "filter-branch-");
    }

    private List<String> readTestIdSuffixes(String element, String prefix) {
        Locator found = getRootLocator().getLocator()
                .locator("xpath=.//" + element + "[starts-with(@data-testid,'" + prefix + "')]");
        List<String> suffixes = new ArrayList<>();
        for (int i = 0; i < found.count(); i++) {
            suffixes.add(found.nth(i).getAttribute("data-testid").substring(prefix.length()));
        }
        return suffixes;
    }

    public void setBranchFilter(String branch, boolean checked) {
        expandFilterGroup(BRANCH_FILTER_GROUP);
        WebElement checkbox = filterBranchCheckbox.format(branch);
        checkbox.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (checkbox.isChecked() != checked) {
            checkbox.click();
            waitUntilSpinnerLoaded();
        }
    }
}
