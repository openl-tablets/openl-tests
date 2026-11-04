package tests.ui.webstudio.sso;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.api.AuthorizedApiMethod;
import domain.api.GroupsMethod;
import domain.ui.webstudio.components.admincomponents.AdminTableSearchComponent;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUsersViewRolesOauth2Ui extends AbstractUsersViewRolesSsoTest {

    private static final String GROUPS_USER = "groupsuser";
    private static final String MATCHED_GROUP = "grp-matched";
    private static final String NOT_MATCHED_BADGE = "+10";
    private static final List<String> EXPANDED_GROUPS = Stream.concat(Stream.of(MATCHED_GROUP),
                    IntStream.rangeClosed(1, 10).mapToObj(i -> String.format("grp-ext-%02d", i)))
            .toList();

    @Test
    @TestCaseId({"IPBQA-32789", "EPBDS-15513"})
    @Description("OAuth2 external auth: admin assigns Manager then Viewer to an SSO user via the "
            + "Admin 'Users' view; repository access matches the assigned role after re-login. EPBDS-15513: the ten "
            + "Keycloak groups of groupsuser that match no Studio group are collapsed into +10; +10 expands them, "
            + "Show less collapses them back to +10 without a page reload, and +10 expands them again.")
    @AppContainerConfig(startParams = AppContainerStartParameters.STUDIO_OIDC_PARAMS)
    public void testUsersViewRolesUnderOauth2() {
        runUsersViewRoleFlow();
        expandAndCollapseNotMatchedGroups();
    }

    private void expandAndCollapseNotMatchedGroups() {
        AuthorizedApiMethod.setBearerToken(keycloak.getAccessToken("admin", "admin"));
        try {
            assertThat(new GroupsMethod().createGroup(MATCHED_GROUP).getStatusCode())
                    .as("create Studio group %s", MATCHED_GROUP).isLessThan(300);
        } finally {
            AuthorizedApiMethod.clearBearerToken();
        }

        assertThat(ssoLogin(GROUPS_USER, GROUPS_USER).openUserMenu().isAdministrationMenuItemVisible())
                .as("%s signs in through Keycloak as a user without administration rights", GROUPS_USER)
                .isFalse();
        UsersPageComponent users = ssoLogin("admin", "admin").openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();
        AdminTableSearchComponent search = new AdminTableSearchComponent("users-search-input").search(GROUPS_USER);
        assertThat(users.getAllUsernames()).as("the search leaves only %s in the list", GROUPS_USER)
                .containsExactly(GROUPS_USER);

        assertCollapsed(users, "before the badge is clicked");

        users.expandUserGroups(GROUPS_USER, NOT_MATCHED_BADGE);
        assertExpanded(users, "after the first click on +10");

        users.collapseUserGroups(GROUPS_USER);
        assertCollapsed(users, "after Show less");
        assertThat(search.getQuery()).as("the search survives Show less, so the page was not reloaded")
                .isEqualTo(GROUPS_USER);
        assertThat(users.getAllUsernames()).as("the list is still filtered to %s after Show less", GROUPS_USER)
                .containsExactly(GROUPS_USER);

        users.expandUserGroups(GROUPS_USER, NOT_MATCHED_BADGE);
        assertExpanded(users, "after +10 is clicked again");
    }

    private void assertCollapsed(UsersPageComponent users, String moment) {
        assertThat(users.getUserGroups(GROUPS_USER))
                .as("only the matched group and +10 are shown %s", moment)
                .containsExactly(MATCHED_GROUP, NOT_MATCHED_BADGE);
        assertThat(users.isShowLessGroupsVisible(GROUPS_USER))
                .as("Show less is hidden %s", moment)
                .isFalse();
    }

    private void assertExpanded(UsersPageComponent users, String moment) {
        assertThat(users.getUserGroups(GROUPS_USER))
                .as("the ten not matched groups are shown next to the matched one instead of +10 %s", moment)
                .containsExactlyInAnyOrderElementsOf(EXPANDED_GROUPS);
        assertThat(users.isShowLessGroupsVisible(GROUPS_USER))
                .as("Show less is shown %s", moment)
                .isTrue();
    }
}
