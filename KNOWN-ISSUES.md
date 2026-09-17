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
- `tests.ui.webstudio.studio_issues.TestAddProperty` and
  `tests.ui.webstudio.studio_issues.TestAddPropertyExtraStateAppears` — both added Description; both now add
  Category, which the panel still offers. What each guards — the write on a two-column table, and that only
  the property added is written — is unchanged; that it is Description is the coverage to restore.
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

## 14. The three category views of the tables tree draw one and the same tree

**What happens.** The tables rail offers five ways of grouping what a module holds, three of them by the
category a table declares: **Category**, **Category Detailed** (the first part of the category) and
**Category Inversed** (the second part first). All three now draw the same tree, and the rail still offers
all three; "Default Order" in the user's settings still offers all five.

**Two causes, both in the client-side grouping.**
1. *The part separator changed.* A category is written with a hyphen — `Calculaiton-Spreadsheet` — and is
   what the product's own documentation still describes. The grouping cuts it on a dot instead, so the
   second part is never found and the two detailed views fall back to the whole category, which is what the
   plain Category view already shows.
2. *The fallback to the sheet is gone.* A table that declares no category used to be filed under the sheet
   it is written on. It is now left at the top level beside the groups. What is shipped to the screen is
   also the table's own properties only, so a category inherited from the module or the category table is
   not seen either.

**What says the old behaviour was the intended one.** The hyphen is what the deleted builder cut on —
`StringUtils.split(category, '-')` in `CategoryNTreeNodeBuilder`, recoverable with
`git show 7ab8ab2570^:STUDIO/org.openl.rules.webstudio/src/org/openl/rules/ui/tree/CategoryNTreeNodeBuilder.java`
— and the sheet fallback is what `CategoryTreeNodeBuilder` did and what the shipped documentation still
describes: *"or, if the property is not defined, based on the Excel table sheet names"*
(`Docs/user-guides/openl-studio/getting-started.md`, which names no separator, only "the first value" and
"the second value"). Against that stands `tableGrouping.test.ts`, which pins the dot as intended and passes.
Somebody wrote the dot on purpose, so which of the two is the product's answer is a call for development to
make, not one to settle here.

**Also reached by this.** `TestAddProperty` opened its table through the folder the category view named
after the sheet; it now opens it through the Excel Sheet view, which still names it. The grouping itself is
covered by `TestOrderingModeDefaults`.

**Tests changed rather than blocked.** `TestOrderingModeDefaults` keeps everything the views still answer —
the default order per user, the selector reflecting it, the Excel Sheet and Type views, and the categories
themselves under the Category view. Steps 1.7, 1.9, 2.4 and 1.10 no longer name what the halves, the sheet
fallback and the Default Order should produce; each only checks that the view drew something, with a pointer
here. Those four are the coverage to restore with the grouping.

**What is not covered by a test either way.** Nothing in `TestOrderingMode.zip` inherits a category from a
module- or category-scope properties table, so the lost inherited category rests on reading the code.

**A third loss, in the same screen.** The chosen view is now kept in the browser's own store
(`tableGrouping.ts`, `openl.module.tableView`) and is read *in preference to* the user's Default Order, so a
view chosen by hand once outranks that setting from then on — on that browser, for good. The setting is
still offered in My Settings and still documented. Whether the old per-session behaviour or this one is
right is the same call for development; until it is made, step 1.10 of `TestOrderingModeDefaults`, which
reopened the browser to check that the Default Order applies again, only checks that a view is chosen at
all.

---

## 15. The tables tree no longer shows the order the workbook holds

**What happens.** Every level of the rail is now sorted by name — the groups and the tables under them alike
(`tableGrouping.ts`, `buildTableTree` and `tableNodes`, both ending in `.sort(byLabel)`). The **By Excel
Sheet** view therefore lists the sheets alphabetically and the tables inside each sheet alphabetically, and a
table moved up or down a sheet does not move in the rail.

**What the product says it does.** The shipped user guide, unchanged: *"By default, tables are sorted by
their location in Excel sheets"*, and of the Excel Sheet view: *"The following tree is sorted by the order
the tables are stored in the Excel file"* (`Docs/user-guides/openl-studio/getting-started.md`). The old tree
kept that order; the picture in the guide still shows it.

**Why it matters.** Where a table sits in a workbook is the one thing this view was for: it is how an author
finds the table they are looking at in Excel, and how they see that an insert landed where they meant it to.

**Blocked tests.** `tests.ui.webstudio.rules_editor.TestOrderingModeTableList` — both scenarios are about
that order. What each still asks is kept: that the view opens on Excel Sheet, that every sheet of the
workbook is a group of its own, that both versions of an overloaded table are drawn, that a row inserted
lands where it was meant to, and that a table created or removed appears and disappears. Only the sequences
— the sheets in workbook order, the tables in the order they sit on the sheet, the order changing after a
row is inserted — are gone from them, and are the coverage to restore with the order.

---

## 16. A properties table cannot be opened where a value is written in the case the screen shows

**What happens.** Opening a Properties table whose `validateDT` reads `on` — the case the engine itself
accepts — fails outright. The request the module screen makes for the table,
`GET /web/projects/{id}/tables/{tableId}`, answers

```
HTTP 500  {"code":"openl.error.500.default.message",
           "message":"No enum constant org.openl.rules.enumeration.ValidateDTEnum.on"}
```

and the screen is left with no table to draw.

**Verified against a running 6.5.0-SNAPSHOT** (`6.5.0-b48c86279338`) with
`src/test/resources/test_data/TestModuleCategoryInheritedProperties/TestModuleCategoryInheritedProperties.xlsx`,
whose module-scope properties table writes `validateDT` as `on` and its category-scope one as `off`. The
module compiles clean and the properties panel reads the inherited values from those very tables, so only
the reading of the table for the screen is affected. The sibling fixture
`TestAddDeleteEditProperties.xlsx`, which writes `ON`, opens.

**Where it comes from.** The table is read cell by cell — `CellValueReader` asks
`XlsDataFormatterFactory.getFormatter` for a formatter and hands it the cell's text. For a cell the meta
info types as an enum that is `EnumFormatter`, which parses through `EnumUtils` and therefore through
`Enum.valueOf`: the **constant name**, case-sensitive. `ON` passes, `on` does not. Two other readers of the
same cell are case-insensitive — `String2EnumConvertor`, which is why the module compiles, and
`ValidateDTEnum.fromString`, which reads the display name — so three parsers disagree about the same text.

**How far it reaches.** For `validateDT` the constant and the display name differ only in case. For every
other dimension property they differ entirely — `CaRegionsEnum` declares `QC("Québec")` — so a properties
table written the way the panel *shows* the values cannot be opened at all. Every table read this way is
open to it, not only properties tables.

**Blocked tests.**
- `tests.ui.webstudio.rules_editor.TestModuleCategoryInheritedProperties` — the step that follows the panel's
  arrow to the properties table the value is inherited from. Everything the panel itself answers — the
  inherited values, which are overwritten at the table, where each value comes from — still runs.

---

## 17. A date is shown in place of the format the workbook writes it with

**What happens.** A date is drawn as `2018-05-14` on the table and in the properties panel alike, whatever
the cell's own format says. The workbook is unharmed: a date written through the panel is still a date cell
carrying Excel's built-in `mm-dd-yy` (`numFmtId="14"`, read back out of `xl/styles.xml` of the exported
workbook), which Excel shows as `5/14/18`. The old editor drew the cell as the workbook writes it; the new
screen does not read the format at all.

The same is true of a percentage: a cell formatted as one is drawn as the bare number it holds.

**Why it matters.** The table screen is where an author checks that what they wrote is what the workbook
holds. Showing a cell differently from Excel is exactly the difference they are looking for.

**Tests changed rather than blocked.** The dates the tests expect are written the way the screen shows them
— the same dates, compared exactly — in `TestEditingProperties` and `TestModuleCategoryInheritedProperties`.
`tests.ui.webstudio.studio_issues.TestAddDeleteRowWithoutSaving` expects `0%` where the screen now reads
`0`; that one is the percentage half of this issue.

---

## 18. The Run menu no longer offers the test cases by range

**What changed.** The contextual menu of Run / Test / Trace / Benchmark used to let a reader write which
cases to run as a range — *2-4,7,10-12* or *id3-id7* — with a note beside the field saying how such a range
is written, and it refused to list the cases one by one for a table with more than twenty of them
(EPBDS-14039).

The React launcher keeps the box that takes them all — **All cases**, ticked unless the reader picks some
(`TableInputLauncher.tsx`, `launch-all-cases`) — and lists every case with a box beside it, a box at the
head to tick them all, and a pager saying *Total test cases: N* (`TestCaseSelector.tsx`). What is gone is
the range **expression**: there is nowhere to write one, nothing explains how one is written, and there is
no longer a limit past which the cases stop being offered one by one — a table of a thousand is ticked a
page at a time.

**The capability behind it is still there.** The launcher sends `testRanges` to the server exactly as
before, built from the ticked ids (`TableInputLauncher.tsx`, `caseIds.join(',')`), and sends none at all
when every case is taken; the server still reads a range expression.

**Tests changed rather than blocked.** `TestRunContextualMenuRunAll` keeps what the screen still answers:
that a table of several cases offers to take them all, that it does so unless the reader picks some, that
picking one stops it, that taking them all again stands, and how many cases the table holds. What it no
longer asks — the range filled in and locked by the box, the note beside the field, and the box being
withheld for a table of twenty cases or fewer — is the coverage to restore with the range.

---

## 19. A table without a body takes the whole module's tables listing down with it

**What happens.** A module holding a table whose header stands alone — `Spreadsheet ` in a cell with nothing
written under it — cannot be opened at all. `GET /web/projects/{id}/tables?module={name}` answers HTTP 500
with `Cannot invoke "org.openl.rules.table.ITable.getHeight()" because "table" is null`, and the module
screen draws nothing but the red box *Failed to load repositories* carrying that same sentence: no tables
tree, no table, no problems panel.

**Where it comes from.** `OpenLTableUtils.isSimpleSpreadsheet`
(`STUDIO/org.openl.rules.webstudio/.../projects/service/tables/OpenLTableUtils.java:154-160`) hands
`table.getSyntaxNode().getTableBody()` straight to `getHeightWithoutEmptyRows` (`:190-191`), and
`TableSyntaxNode.getTableBody()` (`DEV/org.openl.rules/.../lang/xls/syntax/TableSyntaxNode.java:117-123`)
returns `null` by contract for a table of one row. The listing
(`WorkspaceProjectService.getTables`, `:1658-1661`) reads every table in one stream with nothing standing
between them, so the one that cannot be read answers for all of them.

**That a table can have no body is a state the compiler itself expects**: `SpreadsheetBoundNode`
(`DEV/org.openl.rules/.../calc/SpreadsheetBoundNode.java:264-272`) raises *Table has no body. Try to merge
header cell horizontally to identify table.* for exactly this shape, and that message is what the problems
panel is meant to list. Four of the nine table readers of the sibling `...service/tables/read` package
already guard the null (`DatatypeTableReader:34`, `VocabularyTableReader:33`, `DataTableReader:34`,
`TestTableReader:34`); the rest do not, so the same 500 is reachable through
`GET /{projectId}/tables/{tableId}` for a body-less `Rules`, `SmartRules` or `Lookup` table
(`SimpleRulesTableReader:42`, `SmartRulesTableReader:44`, `LookupTableReader:41`). For a body-less
`Spreadsheet` the fall is earlier still: `SimpleSpreadsheetReader.supports:26` and
`SpreadsheetTableReader.supports:30` both ask `isSimpleSpreadsheet`, so no reader is even chosen.

**Why it matters.** One malformed table — the very thing an author opens the editor to find and fix — hides
every other table of the module and the messages that would say what is wrong.

**Test blocked.** `tests.ui.webstudio.studio_issues.TestClickOnErrorFromTheBottom` (EPBDS-9309). Its project
carries that table on purpose: the test is about clicking a compilation error in the bottom panel and *not*
getting a server error. It now fails on opening the module, waiting for a tables tree that never appears.
The workbook is left as it is — repairing it would delete the case the test exists for and hide a live 500.

Reproduced on `ghcr.io/openl-tablets/webstudio:6.5.0-b48c86279338`:
`curl -u admin:admin "http://localhost:18099/web/projects/{id}/tables?module=ContextDatatypes"` → 500.

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
| Two versions of one table stand in the rail under the name they share, with the version that was copied from drawn as set aside. The name carries the dimensions a table is told apart by, and a version is not one of them; copying as a new version sets the source aside (EPBDS-16357), so the two are never dispatched together and there is no dimension to write. | `TestVersioningByFolders` counts the tables of that name, checks that exactly one is set aside, and reads each version's own properties from the panel. |
| A module is copied where the project's files are — the workbook is copied beside itself on the Files tab and the copy becomes a module by the folder it lands in — rather than from a dialog on the module screen. | `EditorPage.copyModuleWorkbook` copies the workbook through the Files tab, which is the route the screen offers. |
| A table wears a drawn icon that names itself rather than a small picture file, one chosen to read as the picture the old Editor drew for that kind (`tableIcons.tsx` names the picture each replaces). | `TestTableIcons` expects the glyph each kind wears, one per kind as before. |
| A usage named inside a cell — the table a value comes from — is a button drawn in the colour a link is drawn in, underlined under the pointer, where the old editor drew an underlined anchor. | `TestArrayDeclarationIsLink` counts the buttons and checks that each reads as something to press. |
| A value of a dimension property is offered by the name it is known by — *Québec*, *Washington*, *Yemen, Rials* — where the old list offered the code, and the code is what is still written down. | The tests name the value the way the screen offers it and expect the code in the table, so both halves are checked. |
| The templates and examples the product ships now carry the standard layout — the workbook under `rules/` and a descriptor beside it — so a project made from one has nothing to move and is not offered **Migrate**. Checked through `GET /projects/{id}/migration` for Sample Project, Example 1, Example 2, Example 3 and Tutorial 1; Example 3 still offers the move for its *rules-deploy*, not for its descriptor. | A test that needs a project with a workbook in its root creates it from an archive that has one (`MigrateXlsProject.zip`). |
| The name a table goes by heads the properties panel instead of standing in it as a property of its own, so a table declaring nothing lists nothing. | The tests read the panel's heading for the name and expect no property row where the table declares none. |
| The properties panel offers to keep what was written only once something has been changed; writing a property the value it already holds leaves nothing to keep. | Where a test wrote a value the workbook already carried, it writes one the table does not carry, so the step is the edit it was meant to be. |
| Creating a project from an OpenAPI specification no longer writes an `openapi` block into `rules.xml`: the normalized file in the project root is reconciled against, and nothing is generated again over later edits. Deliberate, with the user guides changed in the same commit — `e3edf4a6e8`, EPBDS-16415, *"Default new OpenAPI projects to reconciliation"* (`repository-editor.md`, `rules-editor.md`). | A freshly created project is expected to read in **Reconciliation** and to name no module to write into; the import dialog starts empty. Where a test drove an overwrite, it now names the modules to write over, as a reader must, so the overwrite itself is still covered. Note the knock-on: a project that declares no module is drawn with its module list read-only even while the card is open for writing (`OverviewPanel.tsx`, `modulesEditable`), which puts the rename and copy steps of the two creation tests under issue 8. |

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
