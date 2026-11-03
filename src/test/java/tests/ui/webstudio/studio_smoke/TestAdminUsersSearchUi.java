package tests.ui.webstudio.studio_smoke;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.api.UsersMethod;
import domain.serviceclasses.constants.User;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.admincomponents.AdminNavigationComponent;
import domain.ui.webstudio.components.admincomponents.AdminTableSearchComponent;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.LoginPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAdminUsersSearchUi extends BaseTest {

    private static final String SECOND_USER = "searchable_15806";

    @Test
    @TestCaseId({"IPBQA-33038", "EPBDS-16234"})
    @Description("EPBDS-15806 ordered by EPBDS-16233: the admin Users table must filter by the search input - a "
            + "matching fragment keeps only the matching user, a garbage query hides every user, and clearing "
            + "the search restores the full list. EPBDS-16216 ordered by EPBDS-16234: in the multi user mode the "
            + "table marks as online the users signed in through the login form - the admin itself and the second "
            + "user while signed in in another browser - and not the second user before signing in or after signing "
            + "out.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testUsersTableFiltersBySearch() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        assertThat(new UsersMethod().createUser(SECOND_USER, SECOND_USER).getStatusCode())
                .as("Fixture: a second user must exist so the search has something to filter out")
                .isEqualTo(204);

        editorPage.openUserMenu().navigateToAdministration();
        new AdminNavigationComponent().clickNavigationItem(AdminNavigationComponent.NavigationItem.USERS);
        UsersPageComponent usersPage = new UsersPageComponent();
        assertThat(usersPage.isUserInList(SECOND_USER))
                .as("Precondition: the second user must be listed before searching")
                .isTrue();

        assertThat(usersPage.isUserShownOnline("admin"))
                .as("The admin signed in through the login form must be shown online")
                .isTrue();
        assertThat(usersPage.isUserShownOnline(SECOND_USER))
                .as("A user who never signed in must not be shown online")
                .isFalse();
        DriverPool.BrowserSession secondUserSession = DriverPool.openAnotherSession();
        try {
            DriverPool.actIn(secondUserSession, () -> new LoginService(DriverPool.getPage())
                    .login(new UserData(SECOND_USER, SECOND_USER)));
            assertThat(usersPage.reloadPage().isUserShownOnline(SECOND_USER))
                    .as("A user signed in through the login form in another browser must be shown online")
                    .isTrue();
            DriverPool.actIn(secondUserSession, () -> {
                new EditorPage().openUserMenu().signOut();
                assertThat(new LoginPage().isLoginFormDisplayed(10000))
                        .as("Signing out must bring the second user to the login form")
                        .isTrue();
            });
        } finally {
            secondUserSession.close();
        }
        assertThat(usersPage.reloadPage().isUserShownOnline(SECOND_USER))
                .as("A user who signed out must no longer be shown online")
                .isFalse();

        AdminTableSearchComponent usersSearch = new AdminTableSearchComponent("users-search-input");
        usersSearch.search("searchable");
        assertThat(usersPage.isUserInList(SECOND_USER))
                .as("A matching fragment must keep the matching user visible")
                .isTrue();
        assertThat(usersPage.isUserInList("admin"))
                .as("A non-matching user must be filtered out of the table")
                .isFalse();

        usersSearch.search("no-such-user-xyz");
        assertThat(usersPage.isUserInList("admin"))
                .as("A garbage query must hide every user from the table")
                .isFalse();
        assertThat(usersPage.isUserInList(SECOND_USER))
                .as("A garbage query must hide the second user as well")
                .isFalse();

        usersSearch.clear();
        assertThat(usersPage.isUserInList("admin"))
                .as("Clearing the search must restore the full users list")
                .isTrue();
        assertThat(usersPage.isUserInList(SECOND_USER))
                .as("Clearing the search must restore the second user as well")
                .isTrue();
    }
}
