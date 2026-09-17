package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Dialog;
import domain.ui.webstudio.components.BaseComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;

public class ProjectDetailsComponent extends BaseComponent {

    private WebElement modulesHeaderElement;
    private WebElement addModuleBtn;
    private WebElement editModuleHoverTemplate;
    private WebElement editModuleIconTemplate;
    private WebElement removeModuleHoverTemplate;
    private WebElement removeModuleIconTemplate;
    // Offered only while the project still carries module-level method filters to convert.
    private WebElement migrateMethodFiltersBtn;
    private WebElement migrateConfirmBtn;

    public ProjectDetailsComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public ProjectDetailsComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        migrateMethodFiltersBtn = new WebElement(page, "xpath=//button[@data-testid='overview-migrate']", "migrateMethodFiltersBtn");
        migrateConfirmBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-modal-confirm')]//button[normalize-space()='Migrate']", "migrateConfirmBtn");
        modulesHeaderElement = createScopedElement("xpath=.//h3/span[text()='Modules']", "modulesHeaderElement");
        addModuleBtn = createScopedElement("xpath=.//h3/span[text()='Modules']/following-sibling::a[@title='Add Module']", "addModuleBtn");
        editModuleHoverTemplate = createScopedElement("xpath=.//div[@class='list-item editable-inner']//a[contains(text(), '%s')]/../..", "editModuleHoverTemplate");
        editModuleIconTemplate = createScopedElement("xpath=.//div[@class='list list-modules']//a[contains(text(), '%s')]/../..//a[contains(@onclick, 'editModule')]/img", "editModuleIconTemplate");
        removeModuleHoverTemplate = createScopedElement("xpath=.//div[@class='list list-modules']//a[contains(text(), '%s')]", "removeModuleHoverTemplate");
        removeModuleIconTemplate = createScopedElement("xpath=.//div[@class='list list-modules']//a[contains(text(), '%s')]/../..//a[contains(@onclick, 'removeModule')]", "removeModuleIconTemplate");
    }

    /**
     * Whether the project offers to be brought to the form its descriptor is written in today. The move
     * that turned a module's method filters into the project's own exposed methods is part of that one
     * rewrite now, rather than an action of its own.
     */
    public boolean isMigrateMethodFiltersVisible() {
        return migrateMethodFiltersBtn.isVisible(DEFAULT_TIMEOUT_MS / 2);
    }

    /** Rewrites the descriptor, which the project asks about before it does. */
    public void clickMigrateMethodFilters() {
        migrateMethodFiltersBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        migrateConfirmBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        waitUntilSpinnerLoaded();
    }

    public void openAddModulePopup() {
        modulesHeaderElement.hover();
        addModuleBtn.click();
    }

    public void openEditModuleDialog(String moduleName) {
        editModuleHoverTemplate.format(moduleName).hover();
        editModuleIconTemplate.format(moduleName).waitForVisible();
        editModuleIconTemplate.format(moduleName).click();
    }

    public void openRemoveModuleDialog(String moduleName) {
        removeModuleHoverTemplate.format(moduleName).hover();
        removeModuleIconTemplate.format(moduleName).waitForVisible();
        removeModuleIconTemplate.format(moduleName).click();
    }

    public boolean isModulesHeaderVisible() {
        return modulesHeaderElement.isVisible();
    }

    public boolean isAddModuleButtonVisible() {
        return addModuleBtn.isVisible();
    }
}
