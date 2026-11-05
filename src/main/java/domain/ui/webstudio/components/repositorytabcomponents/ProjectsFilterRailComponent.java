package domain.ui.webstudio.components.repositorytabcomponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;

import helpers.utils.WaitUtil;

import java.util.ArrayList;
import java.util.List;

public class ProjectsFilterRailComponent extends BaseComponent {

    public static final String REPOSITORY_FILTER_GROUP = "repository";
    public static final String STATUS_FILTER_GROUP = "status";
    public static final String BRANCH_FILTER_GROUP = "branch";

    private static final int PROBE_MS = DEFAULT_TIMEOUT_MS / 5;

    private final WebElement filterGroupToggle;
    private final WebElement filterGroupShow;
    private final WebElement filterRepoCheckbox;
    private final WebElement filterStatusCheckbox;
    private final WebElement filterBranchCheckbox;

    public ProjectsFilterRailComponent(Page page) {
        this(new WebElement(page, "xpath=//*[@data-testid='projects-rail']", "projectsRail"));
    }

    public ProjectsFilterRailComponent(WebElement rootLocator) {
        super(rootLocator);
        filterGroupToggle = createScopedElement("xpath=.//*[@data-testid='filter-toggle-%s']", "filterGroupToggle");
        filterGroupShow = createScopedElement("xpath=.//*[@data-testid='filter-show-%s']", "filterGroupShow");
        filterRepoCheckbox = createScopedElement("xpath=.//*[@data-testid='filter-repo-%s']", "filterRepoCheckbox");
        filterStatusCheckbox = createScopedElement("xpath=.//*[@data-testid='filter-status-%s']", "filterStatusCheckbox");
        filterBranchCheckbox = createScopedElement("xpath=.//*[@data-testid='filter-branch-%s']", "filterBranchCheckbox");
    }

    public void expandFilterGroup(String groupId) {
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
        Locator options = getRootLocator().getLocator()
                .locator("xpath=.//input[@type='checkbox'][starts-with(@data-testid,'filter-branch-')]");
        List<String> branches = new ArrayList<>();
        for (int i = 0; i < options.count(); i++) {
            branches.add(options.nth(i).getAttribute("data-testid").substring("filter-branch-".length()));
        }
        return branches;
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
