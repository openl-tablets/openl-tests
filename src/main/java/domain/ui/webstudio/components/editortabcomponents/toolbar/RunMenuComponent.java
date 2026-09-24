package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;

import java.util.List;

public class RunMenuComponent extends TableInputLauncherComponent implements IRunMenu {

    private final WebElement runBtn;
    private final WebElement runIntoFileBtn;

    public RunMenuComponent(Page page) {
        super(page, "run-start");
        runBtn = buttonOf("run-start");
        runIntoFileBtn = buttonOf("run-into-file");
    }

    @Override
    public IRunMenu clickCreateItem() {
        createFirstUnsetStructure();
        return this;
    }

    @Override
    public IRunMenu clickAddElementToCollectionBtn(String parameterName) {
        growStructure(parameterName);
        return this;
    }

    @Override
    public IRunMenu clickExpandCollection() {
        expandFirstCollection();
        return this;
    }

    @Override
    public IRunMenu clickRunInsideMenu() {
        runBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        runBtn.click();
        return this;
    }

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

    public IRunMenu setParameterField(String parameterName, String value) {
        writeField(parameterName, value);
        return this;
    }

    private String fieldAt(String index, boolean chosenFromList) {
        waitForFields();
        int position = Integer.parseInt(index.trim());
        int seen = 0;
        List<String> paths = writablePaths();
        for (String path : paths) {
            boolean fieldIsList = isFieldChosenFromList(path);
            if (fieldIsList == chosenFromList && ++seen == position) {
                return path;
            }
            if (fieldIsList) {
                closeListOf(path);
            }
        }
        throw new AssertionError("The launcher offers no " + (chosenFromList ? "list" : "box") + " number "
                + position + " among its fields: " + paths);
    }
}
