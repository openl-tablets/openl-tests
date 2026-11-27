package domain.ui.webstudio.components.projectdetail;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectFilesTabComponent extends BaseComponent {

    public static final String PROJECT_NOT_IN_TREE = "The projects tree does not list the project the page shows";
    public static final String FILE_OPENS_EDITOR = "Picking the file in the projects tree opened Rules Editor instead of its pane";

    private static final int FILE_DIALOG_TIMEOUT_MS = DEFAULT_TIMEOUT_MS * 3;
    private static final int TREE_LOAD_TIMEOUT_MS = DEFAULT_TIMEOUT_MS * 6;
    private static final int POLL_MS = 200;
    private static final int WHEEL_STEP_PX = 300;
    private static final Pattern FILE_PARAM = Pattern.compile("[?&]file=([^&#]*)");
    private static final Pattern PROJECT_ID = Pattern.compile("/projects/([^/?#]+)");
    private static final Pattern EDITOR_URL = Pattern.compile("/projects/[^/?#]+/modules/");
    private static final String TREE = "//*[@data-testid='projects-tree']";
    private static final String CLOSED_ROW = TREE + "//div[contains(@class,'ant-tree-treenode-switcher-close')]";
    private static final String NODE_BY_NAME = TREE + "//span[starts-with(@data-testid,'tree-file-%1$s-') "
            + "or starts-with(@data-testid,'tree-folder-%1$s-')][span[1][normalize-space()='%2$s']]";

    private final WebElement tree;
    private final WebElement shownTooltip;
    private final WebElement treeOrEmpty;
    private final WebElement treeMode;
    private final WebElement actionBar;
    private final WebElement projectNode;
    private final WebElement closedRowSwitcher;
    private final WebElement firstClosedGroup;
    private final WebElement firstClosedFolder;
    private final WebElement presentNodeByName;
    private final WebElement fileNodeByPath;
    private final WebElement projectFileNodes;
    private final WebElement treeRows;
    private final WebElement addBtn;
    private final WebElement actionsMoreBtn;
    private final WebElement overflowPanel;
    private final WebElement fileActionsBtn;
    private final WebElement addMenuItem;
    private final WebElement fileActionsMenuItem;
    private final WebElement fileDeleteSubmitBtn;
    private final WebElement errorNotification;
    private final WebElement updateFileInput;
    private final WebElement updateFileSubmitBtn;
    private final WebElement updateFileNameWarning;
    private final WebElement uploadInput;
    private final WebElement uploadNameField;
    private final WebElement uploadPathField;
    private final WebElement uploadSubmitBtn;
    private final WebElement folderPathInput;
    private final WebElement folderSubmitBtn;
    private final WebElement filePane;
    private final WebElement filePreviewEmpty;
    private final WebElement filePreviewError;
    private final WebElement filePaneLoading;

    public ProjectFilesTabComponent(Page page) {
        this(new WebElement(page, "xpath=//*[@data-testid='project-detail']", "projectDetail"));
    }

    public ProjectFilesTabComponent(WebElement rootLocator) {
        super(rootLocator);
        tree = new WebElement(page, "xpath=" + TREE, "projectsTree");
        shownTooltip = new WebElement(page, "xpath=//div[contains(concat(' ',normalize-space(@class),' '),' ant-tooltip ')]"
                + "[not(contains(@class,'ant-tooltip-hidden'))]", "shownTooltip");
        treeOrEmpty = new WebElement(page, "xpath=(" + TREE + " | //*[@data-testid='projects-tree-empty'])[1]",
                "projectsTreeOrEmpty");
        treeMode = new WebElement(page, "xpath=//*[@data-testid='projects-rail-mode']"
                + "//label[contains(@class,'ant-segmented-item')][normalize-space()='Tree']", "projectsRailTreeMode");
        actionBar = new WebElement(page, "xpath=//*[@data-testid='project-actions']", "projectActions");
        projectNode = new WebElement(page, "xpath=(" + TREE + "//*[@data-testid='tree-project-%s'])[1]", "treeProject");
        closedRowSwitcher = new WebElement(page, "xpath=(" + CLOSED_ROW + "[.//*[@data-testid=\"%s\"]]"
                + "/span[contains(@class,'ant-tree-switcher')])[1]", "closedTreeRowSwitcher");
        firstClosedGroup = new WebElement(page, "xpath=(" + CLOSED_ROW
                + "//*[starts-with(@data-testid,'tree-group-')])[1]", "firstClosedTreeGroup");
        firstClosedFolder = new WebElement(page, "xpath=(" + CLOSED_ROW
                + "//span[starts-with(@data-testid,'tree-folder-%s-')])[1]", "firstClosedTreeFolder");
        presentNodeByName = new WebElement(page, "xpath=(" + NODE_BY_NAME + "[not(@data-change='deleted')])[1]", "presentTreeFileNode");
        fileNodeByPath = new WebElement(page, "xpath=(" + TREE + "//span[@data-testid=\"tree-file-%s-%s\"][not(@data-change='deleted')])[1]", "treeFileNodeByPath");
        projectFileNodes = new WebElement(page, "xpath=" + TREE + "//span[starts-with(@data-testid,'tree-file-%s-')]"
                + "[not(@data-change='deleted')]", "projectFileNodes");
        treeRows = new WebElement(page, "xpath=(" + TREE + "//div[contains(@class,'ant-tree-list-holder')])[1]",
                "projectsTreeRows");
        addBtn = new WebElement(page, "xpath=//*[@data-testid='addFiles-%s']", "addFilesBtn");
        actionsMoreBtn = new WebElement(page, "xpath=//*[@data-testid='project-actions-more']", "projectActionsMoreBtn");
        overflowPanel = new WebElement(page, "xpath=//div[@data-testid='project-actions-overflow']", "projectActionsMenu");
        fileActionsBtn = createScopedElement("xpath=.//*[@data-testid='file-actions']", "fileActionsBtn");
        addMenuItem = new WebElement(page, "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]//li[contains(@class,'ant-dropdown-menu-item')][.//span[@data-testid='%s']]", "filesAddMenuItem");
        fileActionsMenuItem = new WebElement(page, "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]//li[contains(@class,'ant-dropdown-menu-item')][normalize-space()='%s']", "fileActionsMenuItem");
        fileDeleteSubmitBtn = new WebElement(page, "xpath=//*[@data-testid='file-delete-submit']", "fileDeleteSubmitBtn");
        errorNotification = new WebElement(page, "xpath=(//div[contains(@class,'ant-notification-notice-error')])[last()]", "errorNotification");
        updateFileInput = new WebElement(page, "xpath=//input[@data-testid='update-file-dragger']", "updateFileInput");
        updateFileSubmitBtn = new WebElement(page, "xpath=//*[@data-testid='update-file-submit']", "updateFileSubmitBtn");
        updateFileNameWarning = new WebElement(page, "xpath=//*[@data-testid='update-file-name-warning']", "updateFileNameWarning");
        uploadInput = new WebElement(page, "xpath=//input[@data-testid='files-upload-dragger']", "filesUploadInput");
        uploadNameField = new WebElement(page, "xpath=//*[@data-testid='files-upload-name']", "filesUploadNameField");
        uploadPathField = new WebElement(page, "xpath=//*[@data-testid='files-upload-path']//input", "filesUploadPathField");
        uploadSubmitBtn = new WebElement(page, "xpath=//*[@data-testid='files-upload-submit']", "filesUploadSubmitBtn");
        folderPathInput = new WebElement(page, "xpath=//*[@data-testid='files-folder-path']//input", "folderPathInput");
        folderSubmitBtn = new WebElement(page, "xpath=//*[@data-testid='files-folder-submit']", "folderSubmitBtn");
        filePane = createScopedElement("xpath=.//*[@data-testid='project-file-pane']", "projectFilePane");
        filePreviewEmpty = createScopedElement("xpath=.//*[@data-testid='file-preview-empty']", "filePreviewEmpty");
        filePreviewError = createScopedElement("xpath=.//*[@data-testid='file-preview-error']", "filePreviewError");
        filePaneLoading = createScopedElement("xpath=.//*[@data-testid='file-pane-loading']", "filePaneLoading");
    }

    public void showProjectInTree() {
        if (!treeOrEmpty.isVisible(DEFAULT_TIMEOUT_MS / 4)) {
            treeMode.click();
        }
        String projectId = shownProjectId();
        WebElement project = projectNode.format(projectId);
        scrollTree(-Integer.MAX_VALUE / 2);
        WaitUtil.requireCondition(() -> isRendered(project, firstClosedGroup),
                DEFAULT_TIMEOUT_MS * 2, POLL_MS, PROJECT_NOT_IN_TREE + " (" + projectId + ")");
        unfold("tree-project-" + projectId);
    }

    public void deleteFile(String fileName) {
        pickFile(fileName);
        fileActionsBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        fileActionsMenuItem.format("Delete").click();
        fileDeleteSubmitBtn.click();
        waitUntilSpinnerLoaded();
        WebElement present = presentNodeByName.format(shownProjectId(), fileName);
        WaitUtil.requireCondition(() -> !present.exists(), DEFAULT_TIMEOUT_MS, POLL_MS,
                "The projects tree still lists '" + fileName + "' as not deleted");
    }

    public String deleteFileExpectingRefusal(String fileName) {
        pickFile(fileName);
        fileActionsBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        fileActionsMenuItem.format("Delete").click();
        fileDeleteSubmitBtn.click();
        errorNotification.waitForVisible(DEFAULT_TIMEOUT_MS);
        String refusal = errorNotification.getText().trim();
        fileDeleteSubmitBtn.waitForHidden(DEFAULT_TIMEOUT_MS);
        WaitUtil.requireCondition(() -> presentNodeByName.format(shownProjectId(), fileName).exists(),
                DEFAULT_TIMEOUT_MS, POLL_MS, "The projects tree no longer lists '" + fileName + "' although its deletion was refused");
        return refusal;
    }

    public void copyFile(String fileName, String copyName) {
        pickFile(fileName);
        fileActionsBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        fileActionsMenuItem.format("Copy").click();
        WebElement name = new WebElement(page, "xpath=//*[@data-testid='file-copy-name']", "fileCopyName");
        name.waitForVisible(DEFAULT_TIMEOUT_MS);
        name.fill(copyName);
        new WebElement(page, "xpath=//*[@data-testid='file-copy-submit']", "fileCopySubmit").click();
        waitUntilSpinnerLoaded();
        requireListed(copyName);
    }

    public void renameFile(String fileName, String newName) {
        pickFile(fileName);
        fileActionsBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        fileActionsMenuItem.format("Rename").click();
        WebElement name = new WebElement(page, "xpath=//*[@data-testid='file-move-name']", "fileMoveName");
        name.waitForVisible(DEFAULT_TIMEOUT_MS);
        name.fill(newName);
        new WebElement(page, "xpath=//*[@data-testid='file-move-submit']", "fileMoveSubmit").click();
        waitUntilSpinnerLoaded();
        requireListed(newName);
    }

    public void selectFile(String fileName) {
        pickFile(fileName);
    }

    public void openFolderInLocalization(String folderName) {
        pickFile(folderName);
        clickOpenLocalization("folder-open-localization");
    }

    public void openFileInLocalization(String filePath) {
        pickFileAt(filePath);
        clickOpenLocalization("file-open-localization");
    }

    public String readFileText(String filePath) {
        WebElement text = new WebElement(page, "xpath=//*[@data-testid='file-preview']//div[contains(@class,'cm-content')]", "filePreviewText");
        String shownBefore = text.exists() ? text.getInnerText() : null;
        pickFileAt(filePath);
        text.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (shownBefore != null) {
            WaitUtil.requireCondition(() -> !text.getLocator().innerText().equals(shownBefore), DEFAULT_TIMEOUT_MS, POLL_MS,
                    "Waiting for the file pane to show '" + filePath + "' instead of the file shown before");
        }
        return text.getInnerText();
    }

    private void clickOpenLocalization(String testId) {
        new WebElement(page, "xpath=//*[@data-testid='" + testId + "']", testId).waitForVisible(DEFAULT_TIMEOUT_MS).click();
    }

    private void pickFileAt(String filePath) {
        if (!isFileListedAt(filePath)) {
            throw new AssertionError("The projects tree does not list '" + filePath + "'");
        }
        pickNode(fileNodeByPath.format(shownProjectId(), filePath), filePath);
    }

    public boolean isResourceNotFoundShown() {
        return getRootLocator().getLocator()
                .locator("xpath=.//*[not(*)][contains(normalize-space(.),'The resource is not found')]").count() > 0;
    }

    public boolean waitForFileSelectionDropped(String fileName) {
        return WaitUtil.waitForCondition(() -> !selectedFileParam().contains(fileName), DEFAULT_TIMEOUT_MS, POLL_MS,
                "Waiting for the project page to drop '" + fileName + "' from the URL selection");
    }

    public long waitForTreeToList(String fileName) {
        long started = System.currentTimeMillis();
        showProjectInTree();
        WaitUtil.requireCondition(() -> isListed(presentNodeByName, fileName), TREE_LOAD_TIMEOUT_MS, POLL_MS,
                "Waiting for the projects tree to list '" + fileName + "'");
        long elapsed = System.currentTimeMillis() - started;
        LOGGER.info("Projects tree listed '{}' after {} ms", fileName, elapsed);
        return elapsed;
    }

    public String describeFilePaneState(String expectedFileName) {
        String pane = filePaneLoading.exists() ? "loading"
                : filePreviewError.exists() ? "error: " + textOrNothing(filePreviewError)
                : filePreviewEmpty.exists() ? "empty"
                : filePane.exists() ? "preview"
                : "none";
        return "tree lists '" + expectedFileName + "': "
                + presentNodeByName.format(shownProjectId(), expectedFileName).exists()
                + ", file pane: " + pane + ", url file param: '" + selectedFileParam() + "'";
    }

    private String textOrNothing(WebElement element) {
        try {
            return element.getLocator().first().innerText().replaceAll("\\s+", " ").trim();
        } catch (RuntimeException beingRedrawn) {
            return "(gone while being read)";
        }
    }

    public String selectedFileParam() {
        Matcher matcher = FILE_PARAM.matcher(page.url());
        return matcher.find() ? URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8) : "";
    }

    public boolean isFilePaneShown() {
        return filePane.isVisible(DEFAULT_TIMEOUT_MS / 4);
    }

    public boolean isFileListedAt(String filePath) {
        showProjectInTree();
        String projectId = shownProjectId();
        WebElement file = fileNodeByPath.format(projectId, filePath);
        WebElement firstClosed = firstClosedFolder.format(projectId);
        return WaitUtil.waitForCondition(() -> isRendered(file, firstClosed), TREE_LOAD_TIMEOUT_MS, POLL_MS,
                "Waiting for the projects tree to list '" + filePath + "'");
    }

    public List<String> getListedFilePaths() {
        showProjectInTree();
        String projectId = shownProjectId();
        WebElement files = projectFileNodes.format(projectId);
        WebElement firstClosed = firstClosedFolder.format(projectId);
        WaitUtil.requireCondition(() -> !unfoldFirst(firstClosed) && files.exists(), TREE_LOAD_TIMEOUT_MS, POLL_MS,
                "Waiting for the projects tree to unfold every folder of the project");
        if (Boolean.TRUE.equals(treeRows.getLocator().evaluate("rows => rows.scrollHeight > rows.clientHeight"))) {
            throw new IllegalStateException("The projects tree scrolls, so it does not render every file of the project");
        }
        String prefix = "tree-file-" + projectId + "-";
        return files.getLocator().all().stream()
                .map(file -> file.getAttribute("data-testid").substring(prefix.length()))
                .sorted()
                .toList();
    }

    public boolean isNodePresent(String nodeName) {
        showProjectInTree();
        return WaitUtil.waitForCondition(() -> isListed(presentNodeByName, nodeName), DEFAULT_TIMEOUT_MS, POLL_MS,
                "Waiting for the projects tree to list '" + nodeName + "'");
    }

    public void uploadFileAs(String filePath, String targetName, String targetFolder) {
        openAddMenuItem("files-upload");
        uploadInput.setInputFiles(filePath);
        if (targetName != null && !targetName.isEmpty()) {
            uploadNameField.waitForVisible(DEFAULT_TIMEOUT_MS).fill(targetName);
        }
        if (targetFolder != null && !targetFolder.isEmpty()) {
            uploadPathField.waitForVisible(DEFAULT_TIMEOUT_MS).fill(targetFolder);
        }
        uploadSubmitBtn.click();
        waitUntilSpinnerLoaded();
    }

    public boolean isAddMenuAvailable() {
        actionBar.waitForVisible(DEFAULT_TIMEOUT_MS);
        WebElement add = addBtn.format(shownProjectId());
        if (add.isVisible(DEFAULT_TIMEOUT_MS / 4)) {
            return true;
        }
        if (!actionsMoreBtn.isVisible(DEFAULT_TIMEOUT_MS / 4) || !actionsMoreBtn.isEnabled()) {
            return false;
        }
        actionsMoreBtn.click();
        overflowPanel.waitForVisible(DEFAULT_TIMEOUT_MS);
        boolean offered = add.isVisible(DEFAULT_TIMEOUT_MS / 4);
        page.keyboard().press("Escape");
        return offered;
    }

    public void pickUpdateFile(String fileName, String newFilePath) {
        pickFile(fileName);
        fileActionsBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        fileActionsMenuItem.format("Update").click();
        updateFileSubmitBtn.waitForVisible(FILE_DIALOG_TIMEOUT_MS);
        updateFileInput.setInputFiles(newFilePath);
    }

    public void confirmUpdateFile() {
        updateFileSubmitBtn.click();
        waitUntilSpinnerLoaded();
    }

    public boolean isUpdateFileNameWarningShown() {
        return updateFileNameWarning.isVisible(DEFAULT_TIMEOUT_MS / 2);
    }

    public void createFolder(String folderPath) {
        openAddMenuItem("files-new-folder");
        folderPathInput.fill(folderPath);
        folderSubmitBtn.click();
        waitUntilSpinnerLoaded();
    }

    private void openAddMenuItem(String itemTestId) {
        WebElement add = addBtn.format(shownProjectId());
        if (!add.isVisible(DEFAULT_TIMEOUT_MS / 4) && actionsMoreBtn.isVisible(DEFAULT_TIMEOUT_MS / 4)
                && actionsMoreBtn.isEnabled()) {
            actionsMoreBtn.click();
            overflowPanel.waitForVisible(DEFAULT_TIMEOUT_MS);
        }
        add.hover();
        addMenuItem.format(itemTestId).click();
    }

    private void pickFile(String name) {
        pickNode(reveal(presentNodeByName, name), name);
    }

    private void pickNode(WebElement node, String name) {
        node.click();
        WaitUtil.requireCondition(() -> isEditorShown() || isSelected(name), DEFAULT_TIMEOUT_MS, POLL_MS,
                "Waiting for the project page to show '" + name + "'");
        if (isEditorShown()) {
            throw new IllegalStateException(FILE_OPENS_EDITOR + " ('" + name + "')");
        }
    }

    private boolean isSelected(String name) {
        String selected = selectedFileParam();
        return selected.equals(name) || selected.endsWith("/" + name);
    }

    private boolean isEditorShown() {
        return EDITOR_URL.matcher(page.url()).find();
    }

    private WebElement reveal(WebElement nodes, String name) {
        showProjectInTree();
        WaitUtil.requireCondition(() -> isListed(nodes, name), TREE_LOAD_TIMEOUT_MS, POLL_MS,
                "Waiting for the projects tree to list '" + name + "'");
        return nodes.format(shownProjectId(), name);
    }

    private void requireListed(String name) {
        WaitUtil.requireCondition(() -> isListed(presentNodeByName, name), DEFAULT_TIMEOUT_MS, POLL_MS,
                "Waiting for the projects tree to list '" + name + "'");
    }

    private boolean isListed(WebElement nodes, String name) {
        String projectId = shownProjectId();
        return isRendered(nodes.format(projectId, name), firstClosedFolder.format(projectId));
    }

    private boolean isRendered(WebElement node, WebElement firstClosed) {
        if (node.exists()) {
            return true;
        }
        if (!unfoldFirst(firstClosed)) {
            scrollTree(WHEEL_STEP_PX);
        }
        return node.exists();
    }

    private void scrollTree(int pixels) {
        if (!tree.exists()) {
            return;
        }
        page.mouse().move(0, 0);
        if (shownTooltip.exists()) {
            WaitUtil.waitForCondition(() -> !shownTooltip.exists(), DEFAULT_TIMEOUT_MS / 4, POLL_MS,
                    "Waiting for the tooltip over the projects tree to close");
        }
        tree.hover();
        page.mouse().wheel(0, pixels);
    }

    private boolean unfoldFirst(WebElement closedNode) {
        String testId;
        try {
            testId = closedNode.getLocator().getAttribute("data-testid", new Locator.GetAttributeOptions().setTimeout(POLL_MS));
        } catch (PlaywrightException noneRendered) {
            return false;
        }
        if (testId == null) {
            return false;
        }
        unfold(testId);
        return true;
    }

    private void unfold(String nodeTestId) {
        WebElement switcher = closedRowSwitcher.format(nodeTestId);
        if (!switcher.exists()) {
            return;
        }
        try {
            switcher.getLocator().click(new Locator.ClickOptions().setTimeout(DEFAULT_TIMEOUT_MS / 5));
        } catch (PlaywrightException openedMeanwhile) {
            LOGGER.info("Tree row {} was unfolded before the click", nodeTestId);
        }
        switcher.waitForHidden(DEFAULT_TIMEOUT_MS);
    }

    private String shownProjectId() {
        Matcher matcher = PROJECT_ID.matcher(page.url());
        if (!matcher.find()) {
            throw new IllegalStateException("The page shows no project: " + page.url());
        }
        return matcher.group(1);
    }
}
