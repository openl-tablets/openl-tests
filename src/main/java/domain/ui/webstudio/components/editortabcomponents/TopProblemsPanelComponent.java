package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

import java.util.List;

public class TopProblemsPanelComponent extends BaseComponent {

    private List<WebElement> errorItems;

    public TopProblemsPanelComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public TopProblemsPanelComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        // The errors the project reports are listed in the compilation problems panel; a project that
        // compiles clean carries no panel at all.
        errorItems = createElementList("xpath=//section[@data-testid='compile-problems']"
                + "[.//span[@data-testid='compile-problems-errors']]"
                + "//div[@data-testid='compile-problems-body']/ul[1]/li", "errorItems");
    }

    public String getText() {
        if (errorItems.isEmpty()) {
            return "";
        }
        return errorItems.getFirst().getText().trim();
    }

    public boolean isVisible() {
        return !errorItems.isEmpty();
    }

    public boolean isAbsent() {
        return errorItems.isEmpty();
    }

    public boolean containsError(String errorText) {
        return errorItems.stream().anyMatch(item -> item.getText().contains(errorText));
    }

    public List<String> getAllErrors() {
        return errorItems.stream().map(item -> item.getText().trim()).toList();
    }
}
