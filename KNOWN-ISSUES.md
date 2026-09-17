# Known issues found while adapting the tests to the JSF-free OpenL Studio

The product removed its JSF interface in `EPBDS-16599` (openl-tablets `7ab8ab2570`, 2026-09-16), and every
screen of OpenL Studio is drawn by the React client now. While migrating the UI tests to it, the checks below
turned out to fail not because the test was written against the old markup, but because something the old
interface offered is gone from the product. Each one names the tests that stay red — or are skipped — until it
is decided.

Nothing here is a JIRA ticket yet. Raise them, then replace the references in the tests with
`@KnownIssue("<key>")` so the report shows them as known rather than as failures.

---

## 1. "Hide Utility Tables" is gone and the setting cannot be reached

**What changed.** The JSF tables tree had a filter dialog with a *Hide Utility Tables* checkbox, on by
default. `EPBDS-16599` deleted the dialog together with the tree, and the React module screen offers no
replacement: its extended search filters by scope, kind, name, header, text and properties, and carries no
such option. The server side kept the behaviour but not the switch — `WorkspaceProjectService` always
excludes `XLS_OTHER` from the table listing, and the `includeOther` flag is never bound from the request, so
utility tables are now permanently hidden and nobody can show them.

**Why it matters.** A rule author can no longer see utility tables at all. The information exists in the
model; only the way to ask for it was removed.

**Blocked tests.**
- `tests.ui.webstudio.rules_editor.TestOrderingModeTableList#testTableListOrdering2` — the first half
  (ordering with utility tables hidden) still runs and passes; the scenario then stops with `SkipException`,
  because the second half needs the filter turned off.

---

## 2. The "Vocabulary" group disappeared from the tables tree

**What changed.** In the JSF tree's *By Type* view, alias datatypes were grouped under their own
`Vocabulary` node (`TableTreeNodeBuilder`, see `git show 6.4.0:.../ui/tree/TableTreeNodeBuilder.java`). The
React tree groups by the `kind` field of the tables API (`TableKind`), which has no `Vocabulary` constant, so
an alias datatype is now indistinguishable from an ordinary datatype — in the grouping and in the icon.

The capability itself was not removed: `SummaryTableReader` still sets `tableType = "Vocabulary"` on every
alias datatype and ships it in the very payload the tree is drawn from, the dependency graph still colours
Vocabulary separately, the Create Table dialog still offers "Vocabulary", and `Docs/ref/table-types.md`
still documents the type. The tree simply stopped reading the field.

Worth settling in the same ticket: the way a vocabulary is recognised also changed, from the compiled model
(`getMember() instanceof InternalDatatypeClass && getType() instanceof DomainOpenClass`) to a text search for
`<`…`>` in the table header (`OpenLTableUtils.isVocabularyTable`). Development should say which is
authoritative.

**Blocked tests.**
- `tests.ui.webstudio.rules_editor.TestOrderingModeDefaults#testDefaultOrderForMultiUser` — step 2.7 expects
  a `Vocabulary` group in the Type view.
- `tests.ui.webstudio.rules_editor.TestRangeDataTypes` — selects `Vocabulary1` inside the `Vocabulary` folder.
- `tests.ui.webstudio.studio_smoke.TestTableIcons` — expects a distinct icon for a vocabulary table.

---

## 3. The extended search offers a table kind it can never find

**What changed.** The extended search of the module screen lists "Other" among the kinds to search by, and
the REST API declares it too, but `WorkspaceProjectService` unconditionally drops `XLS_OTHER` from the
results. Choosing "Other" therefore always returns nothing. Same root cause as issue 1.

**Blocked tests.** None yet — no test covers that filter. Recorded because it was found while checking
issue 1, and because a search option that cannot return a result misleads the user.

---

## 4. Creating a project with `/` in its name answers with a generic server error

**What changed.** Creating a project whose name holds a forbidden character used to be refused with a message
naming the problem. It now answers `Something went wrong on API server!`, which says nothing about the name.

**Blocked tests.**
- `tests.ui.webstudio.repository.TestProjectNameValidationUi#testSlashNameShowsSpecificValidationMessage` —
  already carries `@KnownIssue("EPBDS-16439")`.

---

## 5. Deploy notifications still name the removed "Deploy Configuration" feature

**What changed.** The deploy notification reads `Deploy Configuration added / The deployment configuration has
been successfully added.`, although the feature it names was removed from the product. A deployment refused
because of a forbidden character in its name answers with an empty message.

**Blocked tests.**
- `tests.ui.webstudio.repository.TestDeployProjectMessagesAndValidationUi#testDeploySuccessMessageNamesTheProject`
  — carries `@KnownIssue("EPBDS-16273")`.
- `tests.ui.webstudio.repository.TestDeployProjectMessagesAndValidationUi#testDeploymentNameRejectsForbiddenCharacters`
  — carries `@KnownIssue("EPBDS-16271")`.

---

## 6. A row can only be added below the current one, and the button says the opposite

**What changed.** The JSF table toolbar had *Insert row before* and *Insert row after*. The React edit
toolbar has one button: its tooltip reads **Insert Row Before** (`browser.module.edit_insert_row`), while the
code behind it inserts **after** the cell in hand — `onInsertRow={() => step({ kind: 'insertRow', at: at.row
+ rowsOfChosen })}` in `TableEditor.tsx`. Inserting a row above the current one is therefore not offered at
all, and the one button that is offered is labelled as the action it does not perform. (Insert Column Before
is both labelled and implemented as "before", so only the row action is affected.)

**Effect on the tests.** `TestSimpleLookupSimpleRules` used *Insert row before*; it now adds the row below
and writes it from the row above, which reaches the same table and keeps the assertion. Nothing covers
"insert above" any more, because the product no longer does it.

## 7. Half the table properties cannot be added: the screens ask for the wrong dictionary

**What changed.** Two screens read the dictionary of properties the same wrong way. The table details panel
offers "Add a property" from a dictionary it reads with `getProjectProperties(projectId)` — without a table
type (`TableDetailsPanel.tsx`, the effect that fills `dictionary`) — and the extended search reads the
property it can narrow by exactly the same way (`TableSearchModal.tsx`, the effect that fills `properties`). The endpoint answers differently depending on that argument: with a table type it returns the
properties a *table* may declare (`InheritanceLevel.TABLE`), and without one the properties a *Properties
table* may declare at Global, Module or Category scope (`ProjectMetadataService.PROPERTIES`). The panel then
offers the intersection of that wrong dictionary with the table's own `available` list, so every property that
exists only at table scope is silently dropped from the list.

**Verified against a running 6.5.0-SNAPSHOT.** For a table that declares nothing
(`GET /web/projects/{id}/tables/{id}/details`), the server answers
`available: [category, description, tags, effectiveDate, …, active, id, …]`, while
`GET /web/projects/{id}/properties` (no `tableType`) answers a list without `description`, `tags`, `id` and
`active`. In the UI, typing "Desc" into "Add a property" shows "No data" — the property cannot be added at
all, although the server would accept it.

**Effect:** `description`, `tags`, `id` and `active` cannot be set on a table through the properties panel,
and the extended search cannot narrow by any of them either — a search by Description or by Tags, which the
old editor offered, has no property to pick.

**Blocked tests.**
- `tests.ui.webstudio.rules_editor.TestAddAndDeleteProperty#testAddAndDeleteProperty` — adds Description,
  Tags and ID among others. The scenario now stops with `SkipException` after the properties that can still
  be added; the Category part still runs and passes.
- `tests.ui.webstudio.rules_editor.TestSearchOnProjectLevel#testAdvancedSearchOnProjectLevel` — the three
  cases that narrowed the search by Description and by Tags are gone from it; everything else it asked — the
  scopes, the table kinds, the name, the header, the counts, opening a table from the results — still runs.
  The three are the coverage to restore when the search reads the dictionary for a table.

---

## 8. A module can no longer be added, renamed or removed from the project's card

**What changed.** The JSF project page listed the modules with "Add Module", "Edit" and "Remove" beside
them. The React Overview panel lists the modules as read-only (`modules-readonly`) and says instead: *"These
modules are discovered automatically from the rules and tests folders. To add a module, put its Excel file in
the rules or tests folder."* For a project whose descriptor declares its modules explicitly the panel still
shows them, but the actions are gone from the card either way.

This looks deliberate — modules are files now, and files are managed on the Files tab — but it is a
capability the old screen offered and the new one does not, so the tests that used it have nothing to press.
Confirm with development whether editing an explicitly declared module is meant to come back.

**Blocked tests.**
- `tests.ui.webstudio.studio_issues.TestAddModuleWithPathExistingModule`
- `tests.ui.webstudio.rules_editor.TestCreateProjectFromOpenApiYamlWithCustomModuleNames`
- `tests.ui.webstudio.rules_editor.TestCreateProjectFromOpenApiJsonFile`
- `tests.ui.webstudio.rules_editor.TestMigratedMethodFilterReloadUi` — its setup needs a module carrying a
  method filter of its own. Note that what this test watched for — an endless re-POST after a JSF view
  expired — cannot happen on a screen that holds no JSF view; what is worth restoring with the capability is
  the rest of it: the screen settles after the descriptor is rewritten, and the table stays.

---

## 9. "Compare Excel files" no longer opens the comparison of two Excel files

**What happens.** The module's More menu offers **Compare Excel files**. Pressing it opens the comparison of
the *project* against one of its own revisions — the revision picker — not the screen that takes two
workbooks and compares them against each other.

**Where it went.** The comparison of two uploaded workbooks is still built and still works: it is what
`/compare` draws when it is opened with no project named (`ComparePage.tsx`, the `compare-files` dragger).
The only door to it was the Rules Editor top panel of the old UI, whose link opened `/compare` without a
query; `7ab8ab2570` ("Draw every screen in the browser and take the rendered UI out") deleted that panel and
nothing took the link over. Every remaining caller of the comparison window names a project
(`compare.ts`: `openCompareWindow`, `openConflictCompareWindow`, `openVersionsCompareWindow`), so the page
never reaches its file-picking state from the UI.

Two defects, of which the first is the loss:
1. the feature has no entry point left, and
2. the menu item has carried the wrong label since `6ca12ff948`: it is named after the comparison of files
   and performs the comparison of revisions. The button that screen then offers reads "Select other files"
   and leads back to the revision picker.

That this is a loss rather than a decision is settled by the product's own documentation, which at this same
commit still describes the removed screen: *"select More > Compare Excel Files … Drag the two files to
compare into the box"* (`Docs/user-guides/openl-studio/rules-editor.md`).

**Workaround.** Open the comparison window from any project and strip the `?projectId=…` from its address.
The tests do not use it: a screen a user cannot reach is a screen the tests must not pretend to reach.

**Blocked tests.**
- `tests.ui.webstudio.rules_editor.TestCompareExcelFiles`

The comparison of a project against its own revisions is a different screen and still reachable, so
`tests.ui.webstudio.git.TestGitSortingExcelFilesInComparePopUp`, which uses that one, is not blocked by this.

---

## 10. Where a generated module is written can no longer be chosen

**What changed.** Generating tables from an OpenAPI specification used to ask where each module goes: the
dialog showed the path of the rules module and of the data module, each could be typed over and reset back.
The React card names only the modules; the path each is written to is computed by the project and shown, not
offered — the confirmation lists `<module> — the workbook <path> is replaced` or `a workbook is added at
<path>`.

**Why this is a defect and not a simplification.** The capability is intact everywhere but the screen: the
request the browser sends still carries `algorithmModulePath` and `modelModulePath` as required fields, the
service still honours a caller-supplied path, and the published API documentation still describes it as the
path *"used for a module the project does not declare yet"*. The screen sends the server its own answer
back. The neighbouring flow still asks: creating a project from a specification offers both module paths
(`new-project-openapi-rules-path`, `new-project-openapi-data-path`). And the commit that built the new flow
documents every other narrowing it made and says nothing about this one.

**One validation is gone outright.** Of the six refusals the old dialog could raise, three survive
server-side in new wording and two are unreachable in their old form; but the rule that a generated module
must be written to an Excel workbook has no successor anywhere — neither the screen nor the service checks
the extension now, so a caller can have a workbook written to `rules/Alg.txt` and declared as a module.

**Detour for a user who needs a particular path.** Declare the module at that path in the project's
descriptor first; the generation then writes into the workbook the descriptor names.

**Tests changed rather than blocked.** `TestImportNewModulesWithPathEditingAndMixedScenarios` keeps what it
can still ask — which module is replaced, which is added, that the module names survive a cancel, and where
the project itself writes them — and no longer types paths. `TestImportPathValidationErrors` keeps the two
refusals that still exist by provoking them the way the new screen allows: both modules named the same, and
a workbook already standing at the path the project computes.

---

## 11. The search trims what it is given, so a space cannot be searched for

**What happens.** The extended search sends `name`, `header` and the text in the cells trimmed
(`TableSearchModal.tsx`). A search for `Balance ` and one for ` Balance` therefore ask the same question as
`Balance`, and a value that differs from another only by a leading or trailing space cannot be told apart.
The old single box sent what was typed, spaces and all, and matched it as a substring.

**Also.** The Kind list of the extended search still offers **Other**, which can never match: the server
leaves free-form tables out of every answer it gives the UI (see issue 3), so that kind returns nothing
whatever else is asked. A free-form table is thus unfindable by search as well as absent from the rail.

**Tests changed rather than blocked.** `TestSearchOnProjectLevel` no longer asserts that a leading space and
a trailing space find different numbers of tables; it asserts what the search still answers.

---

## 12. The generation plan says a workbook is replaced when none stands there

**What happens.** Before writing the tables a specification describes, the project shows what it will do to
each module: `the workbook <path> is replaced`, or `a workbook is added at <path>`. Which of the two it says
is decided by whether the project's descriptor already *leads to* that path — which, for a project that
finds its modules by a pattern (`rules/**/*.xlsx`, the layout every project created from a template has),
it always does. So a module that does not exist, whose workbook does not exist either, is announced as a
workbook about to be replaced.

The service itself is careful about this: it computes "whether the project already leads to it" and its own
documentation says the generation "writes over the workbook such a module reads rather than laying a second
one beside it". The screen turns that into a sentence about a workbook being replaced, which for a path
nothing stands at is not true. A user is told they are about to lose something that does not exist.

Narrow, and nothing is lost by it — but it is the one line the user reads before agreeing to a write.

**Tests changed rather than blocked.** The generation tests state which module is written into which
workbook, which is what the plan is for and what it gets right. They no longer state which of the two
sentences the plan uses, except where the workbook genuinely stands there already.

---

## 13. A project can no longer be renamed

**What changed.** The JSF project screen offered Edit Project, whose first field was the project's name: a
project could be renamed where it stood. The React card offers what the descriptor says — the description,
the modules, the dependencies, the OpenAPI settings — and the actions the project has
(`PROJECT_ACTIONS`: save, open, close, copy, delete branch, open revision, sync, deploy, compare, export,
delete, unlock). None of them renames it, and the only Rename in the whole application belongs to a file.

Copying the project into one of another name is the nearest thing left, and it is not the same act: the
history stays with the old name, and the old project stays behind to be deleted.

**Blocked tests.**
- `tests.ui.webstudio.studio_issues.TestRenameProjectFromOldRevisionConflictUi` — renames a project twice,
  from an old revision, to check the conflict that used to raise.
- `tests.ui.webstudio.repository.TestProjectDeleteUnsavedEditUi` — renames a project and then deletes it
  while the rename is still unsaved, to check the row is found under the new name.

---

## Renamings that are not bugs

For the record, so they are not raised twice. These are the same tree, named the way the tables API has named
it since `EPBDS-13931` (2023); `EPBDS-16599` only removed the second, JSF-only vocabulary:

| JSF tree (≤ 6.4.0) | React tree (6.5.0-SNAPSHOT) |
|---|---|
| `Decision` | `Rules` |
| `Configuration` | `Environment` |
| `By Excel Sheet`, `By Type`, `By Category…` (view names in the tree) | `Excel Sheet`, `Type`, `Category…` |

The tests were updated to the new names. Note that the *user settings* still offer the old wording
(`By Excel Sheet` and friends come from the server-side tree profiles), so the same view is called two
different things depending on the screen — worth tidying, but it breaks nothing.

---

## Behaviour the React screens changed, which the tests now follow

Recorded so the next reader does not mistake them for defects. Each was checked against the React sources of
the build under test (`6.5.0-b48c86279338`) and against the screens themselves.

| What changed | What the tests do now |
|---|---|
| A project whose workbooks lie in its root has no `rules.xml`, and the project card offers **Migrate** in place of **Edit** until they are moved under `rules/`. Writing a descriptor without moving them first would stop the project finding them, which is why the card withholds the settings (`OverviewPanel.tsx`, `useProjectMigration`). | The OpenAPI tests perform the move as an explicit step (`EditorPage.migrateProject()`) before writing the settings, as a user must. |
| The card names the OpenAPI **Mode** only when the project declares one. Choosing the mode the engine falls back to (Reconciliation) writes nothing into `rules.xml`, so the row is absent. | The mode is read from the card when it names one, and otherwise from the settings, which stand at the mode the project falls back to. A project reading in Tables generation still names it, so a lost mode is still caught. |
| The result of running a rule is a window of its own, with one column per input and one for the result. The row carries no number, and each value is shown as the literal its type reads as (`"Tom"`, not `Tom`). | The expected rows were rewritten to what the window shows, keeping the value equality the test was written for. |
| Running anything opens a window that covers the screen, and the module cannot be worked on again until it is closed. | `TestResultValidationComponent.closeResults()` is pressed once the results have been read. |
| A table is removed behind a question the screen asks in a window of its own, not behind the browser's own confirm dialog. | `removeCurrentTable()` answers the screen's window. |

## Class names of the component library, for whoever writes the next locator

Ant Design 6 renamed two containers that a great many locators were written against. Both were confirmed in
the running application, not only in the sources:

| Ant Design 5 (≤ 6.4.0) | Ant Design 6 (6.5.0) |
|---|---|
| `ant-modal-content` | `ant-modal-container` |
| `rc-virtual-list-holder-inner` (the box a Select's options are drawn in) | `ant-select-dropdown-list-holder-inner` |

`ant-select-content` (not `ant-select-selector`) is the box a Select shows its value in. An option is
`div.ant-select-item-option` and carries the value as its `title`; the text inside it sits in a box whose
class reads as the option's own, so a locator must ask for the `title` to count each option once.
