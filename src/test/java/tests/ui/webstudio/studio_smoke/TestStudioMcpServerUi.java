package tests.ui.webstudio.studio_smoke;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.api.GetApplicationInfoMethod;
import domain.api.StudioMcpMethod;
import domain.serviceclasses.constants.User;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStudioMcpServerUi extends BaseTest {

    private static final UserData MCP_USER = new UserData("mcp_user", "mcp_user");
    private static final String TOKEN_NAME = "aqa-mcp";
    private static final List<String> PLUGIN_TOOLS =
            List.of("openl_get_project_agent_context", "openl_search_project_files");

    @Test
    @TestCaseId({"IPBQA-33074", "EPBDS-16813"})
    @Description("EPBDS-16813: the MCP server built into OpenL Studio, which the OpenL AI plugin connects to on "
            + "Studio 7.0 and later, refuses a request without a token, and with the personal access token of a new "
            + "user names the version Studio publishes without sign-in and offers the tools that timed out with "
            + "OpenL MCP 1.2.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void newUserTokenReachesTheMcpServerBuiltIntoStudio() {
        String studioVersion = GetApplicationInfoMethod.restPublic().getOpenLVersion();
        assertThat(studioVersion)
                .as("Studio must publish openl.version without sign-in")
                .matches("\\d+\\.\\d+\\.\\d+.*");

        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        addContributor(editorPage);
        editorPage.openUserMenu().signOut();

        String token = new LoginService(DriverPool.getPage()).login(MCP_USER)
                .openUserMenu()
                .navigateToPersonalAccessTokens()
                .createToken(TOKEN_NAME, "No expiration");

        StudioMcpMethod mcp = new StudioMcpMethod();
        assertThat(mcp.getDiscoverStatusWithoutAuthorization())
                .as("The MCP server must refuse a request without a token")
                .isEqualTo(401);
        assertThat(mcp.getServerInfo(token))
                .as("The MCP server must name itself and the version Studio publishes")
                .containsEntry("name", "studio-mcp")
                .containsEntry("version", studioVersion);
        assertThat(mcp.getToolNames(token))
                .as("The MCP server must offer the tools the OpenL AI plugin relies on")
                .containsAll(PLUGIN_TOOLS);
    }

    private void addContributor(EditorPage editorPage) {
        UsersPageComponent users = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();
        users.clickAddUser()
                .setUsername(MCP_USER.getLogin())
                .setPassword(MCP_USER.getPassword())
                .saveUser();
        users.clickEditUser(MCP_USER.getLogin())
                .clickAddRoleBtn()
                .setRoleRepository(0, "Design")
                .setRole(0, "Contributor")
                .saveUser();
    }
}
