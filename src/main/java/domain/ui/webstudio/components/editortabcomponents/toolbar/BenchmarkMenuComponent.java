package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;

public class BenchmarkMenuComponent extends TableInputLauncherComponent {

    private final WebElement benchmarkBtn;

    public BenchmarkMenuComponent(Page page) {
        super(page, "benchmark-start");
        benchmarkBtn = buttonOf("benchmark-start");
    }

    public void clickBenchmarkInsideMenu() {
        benchmarkBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        TestResultValidationComponent.putAwayShownResults(page);
        benchmarkBtn.click();
    }
}
