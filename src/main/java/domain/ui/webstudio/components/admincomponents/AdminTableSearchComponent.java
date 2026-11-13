package domain.ui.webstudio.components.admincomponents;

import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.common.HeaderSearchComponent;
import helpers.utils.WaitUtil;

public class AdminTableSearchComponent extends BaseComponent {

    private final HeaderSearchComponent headerSearch;

    public AdminTableSearchComponent(HeaderSearchComponent.Scope scope) {
        super(DriverPool.getPage());
        headerSearch = new HeaderSearchComponent(page, scope);
    }

    public AdminTableSearchComponent search(String query) {
        headerSearch.search(query);
        WaitUtil.sleep(600, "Letting the client-side admin table filter apply");
        return this;
    }

    public String getQuery() {
        return headerSearch.getText();
    }

    public AdminTableSearchComponent clear() {
        headerSearch.search("");
        WaitUtil.sleep(600, "Letting the admin table filter reset");
        return this;
    }
}
