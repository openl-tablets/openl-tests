package tests.api.webstudio.client.base;

import configuration.appcontainer.AppContainerPool;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.projectconfig.ProjectConfiguration;
import configuration.projectconfig.PropertyNameSpace;
import domain.api.AuthorizedApiMethod;
import domain.api.ProjectBranchesMethod;
import domain.api.ProjectStatusMethod;
import domain.api.ProjectTestsMethod;
import domain.api.ProjectsMethod;
import helpers.utils.StringUtil;
import helpers.utils.WaitUtil;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public abstract class AbstractStudioCentralProjectsApi implements ITest {
    protected static final Logger LOGGER = LogManager.getLogger(AbstractStudioCentralProjectsApi.class);
    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofMinutes(60);
    private static final Duration CLONE_PROJECTS_TIMEOUT = Duration.ofMinutes(90);
    private static final long CLONE_POLL_INTERVAL_MS = 20_000;
    private static final int TEST_SUMMARY_POLL_INTERVAL_MS = 2_000;
    private static final int TEST_SUMMARY_POLL_TIMEOUT_MS = 10 * 60 * 1_000;
    private static final int COMPILE_POLL_INTERVAL_MS = 1_500;
    private static final int COMPILE_POLL_TIMEOUT_MS = 60 * 1_000;
    private static final String MAINLINE_BRANCHES_PROPERTY = "studio.central.branches";
    private static final String DEFAULT_MAINLINE_BRANCHES = "master,main,development";
    private static final Set<String> MAINLINE_BRANCHES = mainlineBranches();
    private static final int BRANCHES_SHOWN_PER_PROJECT = 3;

    private final Map<String, Map<String, Object>> projectsByName = new LinkedHashMap<>();
    private final ThreadLocal<String> currentTestName = new ThreadLocal<>();

    protected abstract AppContainerStartParameters params();

    protected abstract String groupLabel();

    @BeforeClass
    public void setUp() {
        startContainer();
        AuthorizedApiMethod.startSession();

        List<Map<String, Object>> projects = waitForClonedProjects();
        selectMainlineProjects(projects);
        if (projectsByName.isEmpty()) {
            throw new IllegalStateException(String.format("None of the %d projects of group [%s] is on the branches %s",
                    projects.size(), groupLabel(), MAINLINE_BRANCHES));
        }

        openAllProjects();
    }

    private void selectMainlineProjects(List<Map<String, Object>> projects) {
        int ignored = 0;
        int unreadable = 0;
        for (Map<String, Object> project : projects) {
            String name = String.valueOf(project.get("name"));
            Optional<List<String>> branches = projectBranches(project, name);
            if (branches.isEmpty()) {
                unreadable++;
            } else if (branches.get().stream().noneMatch(AbstractStudioCentralProjectsApi::isMainlineBranch)) {
                ignored++;
                LOGGER.info("Ignored project [{}] of repository [{}]: it is on none of the branches {}, only on {}",
                        name, project.get("repository"), MAINLINE_BRANCHES, describeBranches(branches.get()));
                continue;
            } else if (!isMainlineBranch(String.valueOf(project.get("branch")))) {
                LOGGER.warn("Project [{}] is on {}, but Studio shows it on the branch [{}], which is the one validated",
                        name, describeBranches(branches.get()), project.get("branch"));
            }
            projectsByName.put(name, project);
        }
        LOGGER.info("Found {} projects in group [{}]: {} validated ({} of them with unreadable branches), {} ignored as on none of the branches {}",
                projects.size(), groupLabel(), projectsByName.size(), unreadable, ignored, MAINLINE_BRANCHES);
    }

    private Optional<List<String>> projectBranches(Map<String, Object> project, String name) {
        Response resp = new ProjectBranchesMethod().listBranches(String.valueOf(project.get("id")));
        if (resp.getStatusCode() != 200) {
            LOGGER.warn("Could not read the branches of project [{}]: HTTP {} — {}. The project stays in the run.",
                    name, resp.getStatusCode(), resp.getBody().asString());
            return Optional.empty();
        }
        try {
            List<String> branches = resp.jsonPath().getList("name", String.class);
            if (branches != null && !branches.isEmpty()) {
                return Optional.of(branches);
            }
            LOGGER.warn("Project [{}] reports no branches. The project stays in the run.", name);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not parse the branches of project [{}]: {}. The project stays in the run.", name, e.toString());
        }
        return Optional.empty();
    }

    private static Set<String> mainlineBranches() {
        String configured = ProjectConfiguration.getProperty(MAINLINE_BRANCHES_PROPERTY);
        String value = configured == null || configured.isBlank() ? DEFAULT_MAINLINE_BRANCHES : configured;
        Set<String> branches = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(branch -> !branch.isEmpty())
                .map(branch -> branch.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(branches);
    }

    private static boolean isMainlineBranch(String branch) {
        return MAINLINE_BRANCHES.contains(branch.toLowerCase(Locale.ROOT));
    }

    private static String describeBranches(List<String> branches) {
        int shown = Math.min(branches.size(), BRANCHES_SHOWN_PER_PROJECT);
        String listed = String.join(", ", branches.subList(0, shown));
        return branches.size() > shown ? listed + " and " + (branches.size() - shown) + " more" : listed;
    }

    private List<Map<String, Object>> waitForClonedProjects() {
        long deadline = System.currentTimeMillis() + CLONE_PROJECTS_TIMEOUT.toMillis();
        LOGGER.info("Waiting for the lazy git clone to produce projects for group [{}] (up to {} min)...",
                groupLabel(), CLONE_PROJECTS_TIMEOUT.toMinutes());
        int attempt = 0;
        while (true) {
            attempt++;
            Response resp = new ProjectsMethod().getAllProjects(500);
            if (resp.getStatusCode() == 200) {
                List<Map<String, Object>> projects = extractProjects(resp);
                Map<String, Object> indexHealth = indexHealth(resp);
                List<String> indexing = repositoriesInIndexState(indexHealth, "indexing");
                if (!projects.isEmpty() && indexing.isEmpty()) {
                    warnAboutDegradedIndex(indexHealth);
                    LOGGER.info("Clone produced {} project(s) for group [{}] after {} attempt(s)",
                            projects.size(), groupLabel(), attempt);
                    return projects;
                }
                if (projects.isEmpty()) {
                    LOGGER.info("...clone not finished for group [{}] — 0 projects yet (attempt {})", groupLabel(), attempt);
                } else {
                    LOGGER.info("...project index of {} not built across all branches yet for group [{}] — {} projects listed so far (attempt {})",
                            indexing, groupLabel(), projects.size(), attempt);
                }
            } else {
                LOGGER.info("...project listing not ready for group [{}]: HTTP {} (attempt {})",
                        groupLabel(), resp.getStatusCode(), attempt);
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new IllegalStateException(String.format(
                        "No projects appeared for group [%s] within %d min — the design-repo clone did not complete.",
                        groupLabel(), CLONE_PROJECTS_TIMEOUT.toMinutes()));
            }
            WaitUtil.sleep(CLONE_POLL_INTERVAL_MS, "waiting for the design-repo clone to produce projects");
        }
    }

    private void openAllProjects() {
        ProjectsMethod projects = new ProjectsMethod();
        int opened = 0;
        int alreadyOpen = 0;
        int failed = 0;
        for (Map.Entry<String, Map<String, Object>> entry : projectsByName.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> project = entry.getValue();
            String status = String.valueOf(project.get("status"));
            String id = String.valueOf(project.get("id"));
            if ("OPENED".equalsIgnoreCase(status)) {
                alreadyOpen++;
                continue;
            }
            Response resp = projects.openProject(id);
            if (resp.getStatusCode() < 300) {
                opened++;
                project.put("status", "OPENED");
            } else {
                failed++;
                LOGGER.warn("Failed to open project [{}]: HTTP {} — {}",
                        name, resp.getStatusCode(), resp.getBody().asString());
            }
        }
        LOGGER.info("Bulk-open for group [{}]: opened={}, alreadyOpen={}, failed={}",
                groupLabel(), opened, alreadyOpen, failed);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        AuthorizedApiMethod.clearSession();
        if (AppContainerPool.get() != null) {
            AppContainerPool.closeAppContainer();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void recordTestName(Method method, Object[] params) {
        String projectName = (params != null && params.length > 0) ? String.valueOf(params[0]) : "";
        currentTestName.set(method.getName() + "[" + projectName + "]");
    }

    @Override
    public String getTestName() {
        String n = currentTestName.get();
        return n != null ? n : "testStudioCentralProject";
    }

    @DataProvider(name = "studioCentralProjects")
    public Object[][] studioCentralProjects() {
        return projectsByName.keySet().stream()
                .map(name -> new Object[]{name})
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "studioCentralProjects")
    public void testStudioCentralProject(String projectName) {
        Map<String, Object> project = projectsByName.get(projectName);
        assertThat(project).as("%s", "Project not found in discovered set: " + projectName).isNotNull();
        validateProject(project);
    }

    private void startContainer() {
        AppContainerStartParameters startParams = params();
        String containerName = StringUtil.generateUniqueName("studio_central_" + startParams.name().toLowerCase());
        Map<String, String> envVars = startParams.getParameterMap();
        envVars.forEach((k, v) -> LOGGER.info("[{}] -> [{}]", k, StringUtil.maskSecretValue(k, v)));
        String dockerImage = ProjectConfiguration.getProperty(PropertyNameSpace.DOCKER_IMAGE_NAME);

        LOGGER.info("Starting WebStudio container for group [{}]. First boot clones design repos and may take up to {} minutes.",
                groupLabel(), CONTAINER_STARTUP_TIMEOUT.toMinutes());
        AtomicBoolean done = new AtomicBoolean(false);
        long started = System.currentTimeMillis();
        Thread heartbeat = new Thread(() -> {
            try {
                while (!done.get()) {
                    Thread.sleep(30_000);
                    if (done.get()) return;
                    long elapsedSec = (System.currentTimeMillis() - started) / 1000;
                    LOGGER.info("...still cloning/booting [{}] — elapsed {}m {}s",
                            groupLabel(), elapsedSec / 60, elapsedSec % 60);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "studio-central-warmup-heartbeat");
        heartbeat.setDaemon(true);
        heartbeat.start();
        try {
            AppContainerPool.setAppContainer(containerName, null, envVars, null, dockerImage, CONTAINER_STARTUP_TIMEOUT);
        } finally {
            done.set(true);
            heartbeat.interrupt();
        }
    }

    private static Map<String, Object> indexHealth(Response response) {
        try {
            Map<String, Object> health = response.jsonPath().getMap("projectIndexHealth");
            return health == null ? Map.of() : health;
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    private static List<String> repositoriesInIndexState(Map<String, Object> indexHealth, String state) {
        return indexHealth.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Map<?, ?> health && state.equals(health.get("state")))
                .map(Map.Entry::getKey)
                .toList();
    }

    private void warnAboutDegradedIndex(Map<String, Object> indexHealth) {
        indexHealth.forEach((repository, value) -> {
            if (value instanceof Map<?, ?> health && "degraded".equals(health.get("state"))) {
                LOGGER.warn("Project index of repository [{}] is degraded: branches {} could not be indexed ({}). A project whose mainline branch is among them is ignored wrongly.",
                        repository, health.get("failedBranches"), health.get("lastError"));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractProjects(Response response) {
        JsonPath json = response.jsonPath();
        Object content = json.get("content");
        if (content instanceof List) {
            return (List<Map<String, Object>>) content;
        }
        return (List<Map<String, Object>>) (List<?>) json.getList("$");
    }

    private void validateProject(Map<String, Object> project) {
        String projectId = String.valueOf(project.get("id"));
        String projectName = String.valueOf(project.get("name"));
        LOGGER.info("Validating project [{}] (id={})", projectName, projectId);

        Response open = new ProjectsMethod().openProject(projectId);
        assertThat(open.getStatusCode() < 300).as("%s", String.format("Failed to set project %s as current: HTTP %d — %s",
                        projectName, open.getStatusCode(), open.getBody().asString())).isTrue();

        Response runResponse = new ProjectTestsMethod().runAllTests(projectId);
        int runStatus = runResponse.getStatusCode();
        if (runStatus == 404 || runStatus == 204) {
            LOGGER.info("Project [{}] has no modules to compile/run — skipping", projectName);
            return;
        }
        assertThat(runStatus == 200 || runStatus == 202).as("%s", String.format("Failed to compile/run tests for project %s: HTTP %d — %s",
                        projectName, runStatus, runResponse.getBody().asString())).isTrue();

        Response statusResp = awaitCompilation(projectId);
        assertThat(statusResp.getStatusCode()).as("%s", String.format("Project status failed for project %s: HTTP %d — %s",
                        projectName, statusResp.getStatusCode(), statusResp.getBody().asString())).isEqualTo(200);
        JsonPath status = statusResp.jsonPath().setRootPath("compileStatus");
        String compileState = status.getString("compileState");
        assertThat(compileState).as("%s", String.format("No compileStatus in GET /rest/projects/{id}?include=status for [%s]: %s",
                projectName, statusResp.getBody().asString())).isNotNull();
        int compileErrors = intOrZero(status.get("compilation.messages.errors"));
        if ("errors".equalsIgnoreCase(compileState) || compileErrors > 0) {
            String detail = buildCompileErrorReport(projectName, status);
            LOGGER.error(detail);
            fail(detail);
        }

        Response summary = pollTestsSummary(projectId, projectName);
        assertThat(summary).as("%s", String.format("Test summary timed out for project [%s]", projectName)).isNotNull();
        int code = summary.getStatusCode();
        if (code == 404) {
            LOGGER.info("Project [{}] has no Test tables — compile validated, nothing to run", projectName);
            return;
        }
        assertThat(code).as("%s", String.format("Tests summary returned HTTP %d for project [%s]: %s",
                        code, projectName, summary.getBody().asString())).isEqualTo(200);

        Integer failures = summary.jsonPath().getInt("numberOfFailures");
        Integer total = summary.jsonPath().getInt("numberOfTests");
        LOGGER.info("Project [{}] tests: total={}, failures={}", projectName, total, failures);
        if (failures != null && failures > 0) {
            String detail = buildFailureReport(projectName, total, failures, summary.jsonPath());
            LOGGER.error(detail);
            fail(detail);
        }
    }

    private Response awaitCompilation(String projectId) {
        ProjectStatusMethod statusApi = new ProjectStatusMethod();
        long deadline = System.currentTimeMillis() + COMPILE_POLL_TIMEOUT_MS;
        Response last = null;
        while (System.currentTimeMillis() < deadline) {
            last = statusApi.getStatus(projectId, false);
            if (last.getStatusCode() == 200) {
                String state = last.jsonPath().getString("compileStatus.compileState");
                if (state != null && !state.equalsIgnoreCase("idle") && !state.equalsIgnoreCase("compiling")) {
                    return last;
                }
            }
            sleepInterruptible(COMPILE_POLL_INTERVAL_MS);
        }
        return last;
    }

    @SuppressWarnings("unchecked")
    private String buildCompileErrorReport(String projectName, JsonPath status) {
        List<Map<String, Object>> items = status.getList("compilation.messages.items");
        List<String> errors = new java.util.ArrayList<>();
        if (items != null) {
            for (Map<String, Object> msg : items) {
                if ("ERROR".equalsIgnoreCase(stringOrNull(msg.get("severity")))) {
                    String summary = stringOrNull(msg.get("summary"));
                    errors.add(summary != null ? summary : "(empty)");
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Compilation errors detected in project: %s%n", projectName));
        sb.append(String.format("ERRORS (%d):%n", errors.size()));
        for (int i = 0; i < errors.size(); i++) {
            sb.append(String.format("  %d. %s%n", i + 1, errors.get(i)));
        }
        return sb.toString();
    }

    private int intOrZero(Object o) {
        Integer v = toInt(o);
        return v == null ? 0 : v;
    }

    @SuppressWarnings("unchecked")
    private String buildFailureReport(String projectName, int total, int failures, JsonPath summaryJson) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Project [%s] has %d test failures (of %d total)", projectName, failures, total));
        List<Map<String, Object>> testCases = summaryJson.getList("testCases");
        if (testCases == null || testCases.isEmpty()) {
            return sb.toString();
        }
        for (Map<String, Object> testCase : testCases) {
            Integer caseFailures = toInt(testCase.get("numberOfFailures"));
            if (caseFailures == null || caseFailures == 0) {
                continue;
            }
            String caseName = String.valueOf(testCase.get("name"));
            Integer caseTotal = toInt(testCase.get("numberOfTests"));
            sb.append(String.format("%n  TestCase [%s] — %d/%d failed", caseName, caseFailures, caseTotal == null ? 0 : caseTotal));
            List<Map<String, Object>> testUnits = (List<Map<String, Object>>) testCase.get("testUnits");
            if (testUnits == null) continue;
            for (Map<String, Object> unit : testUnits) {
                String status = stringOrNull(unit.get("status"));
                if (status == null || status.equalsIgnoreCase("TR_OK")) continue;
                appendFailedUnit(sb, unit);
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendFailedUnit(StringBuilder sb, Map<String, Object> unit) {
        String description = stringOrNull(unit.get("description"));
        String id = stringOrNull(unit.get("id"));
        String status = stringOrNull(unit.get("status"));
        sb.append(String.format("%n    Unit [%s] status=%s",
                description != null ? description : id, status));

        List<Map<String, Object>> params = (List<Map<String, Object>>) unit.get("parameters");
        if (params != null && !params.isEmpty()) {
            sb.append(String.format("%n      input: "));
            sb.append(params.stream().map(this::formatParamValue).collect(Collectors.joining(", ")));
        }
        List<Map<String, Object>> assertions = (List<Map<String, Object>>) unit.get("testAssertions");
        if (assertions != null) {
            for (Map<String, Object> assertion : assertions) {
                String aStatus = stringOrNull(assertion.get("status"));
                if (aStatus != null && aStatus.equalsIgnoreCase("TR_OK")) {
                    continue;
                }
                String aDesc = stringOrNull(assertion.get("description"));
                Object expected = assertion.get("expectedValue");
                Object actual = assertion.get("actualValue");
                sb.append(String.format("%n      assertion[%s]: expected=%s actual=%s",
                        aDesc != null ? aDesc : "-", expected, actual));
            }
        }
        List<Map<String, Object>> errors = (List<Map<String, Object>>) unit.get("errors");
        if (errors != null && !errors.isEmpty()) {
            for (Map<String, Object> err : errors) {
                String severity = stringOrNull(err.get("severity"));
                String summary = stringOrNull(err.get("summary"));
                sb.append(String.format("%n      error[%s]: %s", severity, summary));
            }
        }
    }

    private String formatParamValue(Map<String, Object> param) {
        Object name = param.get("name");
        Object value = param.get("value");
        return String.valueOf(name) + "=" + String.valueOf(value);
    }

    private String stringOrNull(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Response pollTestsSummary(String projectId, String projectName) {
        long deadline = System.currentTimeMillis() + TEST_SUMMARY_POLL_TIMEOUT_MS;
        ProjectTestsMethod client = new ProjectTestsMethod();
        Response last = null;
        int consecutive404 = 0;
        while (System.currentTimeMillis() < deadline) {
            last = client.getTestsSummary(projectId, false, 100, false);
            int code = last.getStatusCode();
            if (code == 200) {
                return last;
            }
            if (code == 202 || code == 409) {
                consecutive404 = 0;
                sleepInterruptible();
                continue;
            }
            if (code == 404) {
                consecutive404++;
                if (consecutive404 >= 3) {
                    return last;
                }
                sleepInterruptible();
                continue;
            }
            LOGGER.warn("Unexpected test summary status {} for project [{}]: {}",
                    code, projectName, last.getBody().asString());
            return last;
        }
        return last;
    }

    private void sleepInterruptible() {
        sleepInterruptible(TEST_SUMMARY_POLL_INTERVAL_MS);
    }

    private void sleepInterruptible(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
