package configuration.driver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import configuration.appcontainer.AppContainerData;
import configuration.appcontainer.AppContainerPool;
import configuration.projectconfig.ProjectConfiguration;
import configuration.projectconfig.PropertyNameSpace;
import helpers.utils.DebugArtifactUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.Network;

public final class DriverPool {

    private static final Logger LOGGER = LogManager.getLogger(DriverPool.class);
    private static final int DEFAULT_TIMEOUT_MS = Integer.parseInt(ProjectConfiguration.getProperty(PropertyNameSpace.PLAYWRIGHT_DEFAULT_TIMEOUT));

    private static final ThreadLocal<BrowserSession> ACTIVE_SESSION = new ThreadLocal<>();

    private DriverPool() {
    }

    public record BrowserSession(BrowserContext context, Page page) {

        public void close() {
            try {
                context.close();
            } catch (RuntimeException alreadyClosed) {
                LOGGER.warn("Another browser session could not be closed: {}", alreadyClosed.getMessage());
            }
        }
    }

    public static Browser.NewContextOptions defaultContextOptions() {
        return new Browser.NewContextOptions()
                .setViewportSize(1280, 720)
                .setLocale("en-US")
                .setTimezoneId("America/New_York")
                .setAcceptDownloads(true)
                .setIgnoreHTTPSErrors(true);
    }

    public static BrowserSession openAnotherSession() {
        BrowserContext context = getBrowserContext().browser().newContext(defaultContextOptions());
        Page page = context.newPage();
        page.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
        LOGGER.info("Opened another browser session with cookies of its own");
        return new BrowserSession(context, page);
    }

    public static void actIn(BrowserSession session, Runnable steps) {
        BrowserSession previous = ACTIVE_SESSION.get();
        ACTIVE_SESSION.set(session);
        try {
            steps.run();
        } catch (RuntimeException | AssertionError failure) {
            DebugArtifactUtil.attachScreenshotOnFailure("another-session", "Another browser session at the failure");
            throw failure;
        } finally {
            if (previous == null) {
                ACTIVE_SESSION.remove();
            } else {
                ACTIVE_SESSION.set(previous);
            }
        }
    }

    public static void initializePlaywright(Network network) {
        switch (ExecutionMode.current()) {
            case PLAYWRIGHT_LOCAL -> LocalDriverPool.setPlaywright();
            case PLAYWRIGHT_DOCKER -> DockerDriverPool.setPlaywrightDocker(network);
        }
        LOGGER.info("Playwright initialized in {} mode", ExecutionMode.current());
    }

    public static Page getPage() {
        BrowserSession session = ACTIVE_SESSION.get();
        if (session != null) {
            return session.page();
        }
        return switch (ExecutionMode.current()) {
            case PLAYWRIGHT_LOCAL -> LocalDriverPool.getPage();
            case PLAYWRIGHT_DOCKER -> DockerDriverPool.getPage();
        };
    }

    public static BrowserContext getBrowserContext() {
        BrowserSession session = ACTIVE_SESSION.get();
        if (session != null) {
            return session.context();
        }
        return switch (ExecutionMode.current()) {
            case PLAYWRIGHT_LOCAL -> LocalDriverPool.getBrowserContext();
            case PLAYWRIGHT_DOCKER -> DockerDriverPool.getBrowserContext();
        };
    }

    public static void closePlaywright() {
        switch (ExecutionMode.current()) {
            case PLAYWRIGHT_LOCAL -> LocalDriverPool.closePlaywright();
            case PLAYWRIGHT_DOCKER -> DockerDriverPool.closePlaywrightDocker();
        }
    }

    public static Page createNewPage() {
        Page newPage = getBrowserContext().newPage();
        newPage.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
        LOGGER.debug("Created new page in browser context");
        return newPage;
    }

    public static byte[] takeScreenshot() {
        return getPage().screenshot(new Page.ScreenshotOptions()
                .setFullPage(true)
                .setType(ScreenshotType.PNG));
    }

    public static void navigateToApp() {
        switch (ExecutionMode.current()) {
            case PLAYWRIGHT_LOCAL -> {
                String url = getAppUrl();
                LOGGER.info("Navigating to application via host URL (LOCAL): {}", url);
                getPage().navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(DEFAULT_TIMEOUT_MS));
            }
            case PLAYWRIGHT_DOCKER -> DockerDriverPool.navigateToApp();
        }
    }

    public static String getAppUrl() {
        ExecutionMode mode = ExecutionMode.current();
        AppContainerData appData = AppContainerPool.get();
        if (appData == null) {
            throw new IllegalStateException("No application container found while resolving app URL for mode " + mode);
        }

        switch (mode) {
            case PLAYWRIGHT_LOCAL -> {
                var container = appData.getAppContainer();
                int defaultAppPort = Integer.parseInt(ProjectConfiguration.getProperty(PropertyNameSpace.DEFAULT_APP_PORT));
                Integer mappedPort = container.getMappedPort(defaultAppPort);
                String deployedAppPath = ProjectConfiguration.getProperty(PropertyNameSpace.DEPLOYED_APP_PATH);

                String hostUrl = String.format("http://localhost:%d%s", mappedPort, deployedAppPath);
                LOGGER.info("App URL (LOCAL): {}", hostUrl);
                return hostUrl;
            }
            case PLAYWRIGHT_DOCKER -> {
                String containerNetworkUrl = appData.getAppHostUrl();
                LOGGER.info("App URL (DOCKER): {}", containerNetworkUrl);
                return containerNetworkUrl;
            }
            default -> throw new UnsupportedOperationException("Unknown execution mode: " + mode);
        }
    }

    public static ExecutionMode getCurrentExecutionMode() {
        return ExecutionMode.current();
    }

    public static String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append(String.format("Execution Mode: %s%n", ExecutionMode.current()));

        switch (ExecutionMode.current()) {
            case PLAYWRIGHT_LOCAL -> info.append(LocalDriverPool.getLocalDebugInfo());
            case PLAYWRIGHT_DOCKER -> info.append(DockerDriverPool.getDockerInfo());
        }

        info.append("\nConfiguration:\n");
        info.append(String.format("Host Resource Path: %s%n",
                ProjectConfiguration.getProperty(PropertyNameSpace.HOST_RESOURCE_PATH)));
        return info.toString();
    }

    static void closeQuietly(String what, Runnable closeAction) {
        try {
            closeAction.run();
            LOGGER.debug("Closed {} successfully", what);
        } catch (Exception e) {
            LOGGER.warn("Error closing {}: {}", what, e.getMessage());
        }
    }
}
