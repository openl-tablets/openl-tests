package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;

import java.util.List;

/**
 * The Run launcher of the table toolbar: the input the rule is run with, and the buttons that run it.
 */
public class RunMenuComponent extends TableInputLauncherComponent implements IRunMenu {

    private final WebElement runBtn;
    private final WebElement runIntoFileBtn;

    public RunMenuComponent(Page page) {
        super(page, "run-start");
        runBtn = buttonOf("run-start");
        runIntoFileBtn = buttonOf("run-into-file");
    }

    /**
     * Creates the value of the first parameter that stands unset. A structure is drawn only once it exists,
     * so it is created before anything can be written into it.
     */
    @Override
    public IRunMenu clickCreateItem() {
        waitForFields();
        List<WebElement> creators = createElementList(
                "xpath=//button[starts-with(@data-testid,'create-')]", "createButtons");
        if (!creators.isEmpty()) {
            creators.get(0).click();
        }
        return this;
    }

    @Override
    public IRunMenu clickAddElementToCollectionBtn(String parameterName) {
        growStructure(parameterName);
        return this;
    }

    /** Folds open the first parameter that holds anything, so what it holds can be written. */
    @Override
    public IRunMenu clickExpandCollection() {
        waitForFields();
        List<WebElement> switchers = createElementList(
                "xpath=//div[contains(@class,'ant-tree-treenode')]"
                        + "/span[contains(@class,'ant-tree-switcher') and not(contains(@class,'ant-tree-switcher-noop'))"
                        + " and not(contains(@class,'ant-tree-switcher_open'))]", "collectionSwitchers");
        if (!switchers.isEmpty()) {
            switchers.get(0).click();
        }
        return this;
    }

    @Override
    public IRunMenu clickRunInsideMenu() {
        runBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        runBtn.click();
        return this;
    }

    /** Runs the rule and writes what it returns into a file. */
    public IRunMenu clickRunIntoFile() {
        runIntoFileBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        runIntoFileBtn.click();
        return this;
    }

    @Override
    public IRunMenu clickAddedElementsExpander(String parameterName) {
        toggleRow(parameterName);
        return this;
    }

    /** The values the first element of the collection offers, which is what its type allows. */
    @Override
    public List<String> getAliasDropdownValues() {
        return fieldOptions(firstElementPath());
    }

    @Override
    public IRunMenu setInputTextField(String index, String value) {
        writeField(fieldAt(index, false), value);
        return this;
    }

    @Override
    public IRunMenu setInputSelectField(String index, String value) {
        chooseInField(fieldAt(index, true), value);
        return this;
    }

    /** Writes the value of the parameter of the given name, whatever place it stands in. */
    public IRunMenu setParameterField(String parameterName, String value) {
        writeField(parameterName, value);
        return this;
    }

    /**
     * The field standing at the given place among the fields of its kind, counted from one: the boxes that
     * are typed in are numbered apart from the ones that are chosen from a list, as the tests name them.
     * Which kind a field is shows only once it is open, so the fields are looked at in the order they are
     * drawn until the one asked for is reached.
     */
    private String fieldAt(String index, boolean chosenFromList) {
        waitForFields();
        int position = Integer.parseInt(index.trim());
        int seen = 0;
        List<String> paths = writablePaths();
        for (String path : paths) {
            if (isFieldChosenFromList(path) == chosenFromList && ++seen == position) {
                return path;
            }
        }
        throw new AssertionError("The launcher offers no " + (chosenFromList ? "list" : "box") + " number "
                + position + " among its fields: " + paths);
    }

    /** The first element of the first collection the launcher holds, which is written as {@code name[0]}. */
    private String firstElementPath() {
        return writablePaths().stream()
                .filter(path -> path.endsWith("]"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The launcher holds no element of a collection: " + writablePaths()));
    }


}
