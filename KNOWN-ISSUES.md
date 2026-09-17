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

**Filed as EPBDS-16665.**

**What changed.** The JSF tables tree had a filter dialog with a *Hide Utility Tables* checkbox, on by
default. `EPBDS-16599` deleted the dialog together with the tree, and the React module screen offers no
replacement: its extended search filters by scope, kind, name, header, text and properties, and carries no
such option. The server kept the behaviour and the flag but not a way to ask for it —
`ProjectTableCriteriaQuery` still has `includeOther`, and `WorkspaceProjectService.buildTableSelector` lets
`XLS_OTHER` through when it is set, but nothing binds it from a request: the one place that sets it is an
internal search by name. So utility tables are hidden and nobody can ask to see them.

**Why it matters.** A rule author can no longer see utility tables at all. The information exists in the
model; only the way to ask for it was removed.

**Blocked tests** (the scenario stops half way, having run the rest).
- `tests.ui.webstudio.rules_editor.TestOrderingModeTableList#testTableListOrdering2` — the first half
  (ordering with utility tables hidden) still runs and passes; the scenario then stops with `SkipException`,
  because the second half needs the filter turned off.

---

## 2. The "Vocabulary" group disappeared from the tables tree

**Filed as EPBDS-16654** (*Grouping by table type doesn't work properly for Vocabularies*, 17 Sep 2026).
`TestOrderingModeDefaults#testDefaultOrderForMultiUser` asks the Type view for a Vocabulary group again and
carries `@KnownIssue("EPBDS-16654")`.

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

**Coverage dropped; the tests run.**
- `tests.ui.webstudio.rules_editor.TestOrderingModeDefaults#testDefaultOrderForMultiUser` — step 2.7 asked for
  a `Vocabulary` group in the Type view; it asks for the groups the view draws instead.
- `tests.ui.webstudio.rules_editor.TestRangeDataTypes` — reaches `Vocabulary1` under the group the tree puts it
  in now, in place of a `Vocabulary` folder.
- `tests.ui.webstudio.studio_smoke.TestTableIcons` — expects the icon a vocabulary table wears now, which is
  the datatype icon.
What is not asked any more, and is the coverage to restore with the fix: that a vocabulary is told apart from
a datatype anywhere in the tree.

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

**Tests that report this as a known issue (they run and fail against it).**
- `tests.ui.webstudio.repository.TestProjectNameValidationUi#testSlashNameShowsSpecificValidationMessage` —
  already carries `@KnownIssue("EPBDS-16439")`.

---

## 5. Deploy notifications still name the removed "Deploy Configuration" feature

**What changed.** The deploy notification reads `Deploy Configuration added / The deployment configuration has
been successfully added.`, although the feature it names was removed from the product. A deployment refused
because of a forbidden character in its name answers with an empty message.

**Tests that report this as a known issue (they run and fail against it).**
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

**Filed as EPBDS-16664.**

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
  Tags and ID among others. The scenario stops with `SkipException` after the properties that can still be
  added; the Category part runs before it.

**Coverage dropped; the tests run.**
- `tests.ui.webstudio.studio_issues.TestAddProperty` and
  `tests.ui.webstudio.studio_issues.TestAddPropertyExtraStateAppears` — both added Description; both now add
  Category, which the panel still offers. What each guards — the write on a two-column table, and that only
  the property added is written — is unchanged; that it is Description is the coverage to restore.
- `tests.ui.webstudio.rules_editor.TestSearchOnProjectLevel#testAdvancedSearchOnProjectLevel` — the three
  cases that narrowed the search by Description and by Tags are gone from it; everything else it asked — the
  scopes, the table kinds, the name, the header, the counts, opening a table from the results — still runs.
  The three are the coverage to restore when the search reads the dictionary for a table.

---

## 8. A module is added, renamed and removed where it is declared — not from one dialog

**Resolved, and kept here for the record.** What the JSF page offered as "Add Module", "Edit" and "Remove"
beside the module list is offered in two places now, by how the project leads to the module:

- A module the descriptor **declares by name** is written on the project's card: open it with **Edit** and
  the module rows become a name, a path and *Compile this module only*, with **Add** under them and a bin
  beside each (`OverviewPanel.tsx`, `modulesEditable = hasRulesXml && !modulesDefault`). A workbook two
  modules would read is refused there — *The path 'X' is already read by another module.*
- A module the standard layout **finds by itself** — the case for every project whose descriptor declares
  nothing — is named after its workbook, so it is added, renamed and copied by adding, renaming and copying
  that workbook on the **Files** tab.

Its own method filter is declared in the descriptor beside the module and shown on the card
(`module-filter-<path>`); the screen offers no form of its own for it, and **Migrate** lifts such a filter to
the project's `<exposed-methods>` where every pattern of it reduces to a glob
(`RulesXmlMigrations.methodFilter`, which keeps the filter where one does not).

**Tests.** `TestAddModuleWithPathExistingModule` writes the duplicate path on the card and reads the refusal;
`TestCreateProjectFromOpenApiJsonFile` renames and copies its modules by their workbooks;
`TestCreateProjectFromOpenApiYamlWithCustomModuleNames` renames the declared module on the card and the
pattern-matched one by its workbook, and takes the declaration out to remove it;
`TestMigratedMethodFilterReloadUi` writes the filter into the descriptor and asks what it always asked — that
the project reloads once and settles, with the table still there.

---

## 9. "Compare Excel files" no longer opens the comparison of two Excel files

**Filed as EPBDS-16655** (*The feature "Compare Excel Files" disappeared*, 17 Sep 2026). The two scenarios
are no longer stopped: each asks the action to open the comparison of two uploaded workbooks and carries
`@KnownIssue("EPBDS-16655")`, so they report the ticket until the way in returns.

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
- `tests.ui.webstudio.studio_issues.TestDisplayChangedRowsCompareScreens#testDisplayChangedRowsUploadedFilesCompareScreen`

**Not this issue.** The comparison of a project against its own revisions is a different screen and still
reachable, so `TestGitSortingExcelFilesInComparePopUp`, which uses that one, runs.

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

## 13. A project is renamed by its descriptor, not from a dialog of its own

**Resolved, and kept here for the record.** The JSF page renamed a project through the Edit Project dialog.
A project is named by its descriptor now: write another `<name>` into `rules.xml` — on the Files tab, where
the file is updated or edited — and the working copy is listed under that name at once, while the repository
still holds the project under the name it was published with (`repositories.ts:509-511` says as much of the
history call, which is asked by id for exactly this reason). Saving the project publishes the new name.

**Tests.** `TestProjectDeleteUnsavedEditUi` writes the new name into the descriptor and deletes the project
while the rename is unsaved; `TestRenameProjectFromOldRevisionConflictUi` renames it, saves, opens an older
revision — which brings the descriptor of that revision back, so the project is listed under its first name
again — renames it a second time and asks for the conflict.

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

**Coverage dropped; the tests run.** `tests.ui.webstudio.rules_editor.TestOrderingModeTableList` — both
scenarios are about that order (the second of them also stops half way, for issue 1). What each still asks
is kept: that the view opens on Excel Sheet, that every sheet of the
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

**Coverage dropped; the test runs.**
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

**Filed as EPBDS-16662.**

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

## 20. Migrating a project that declares nothing always fails, and leaves it broken

**Filed as EPBDS-16666.**

**Of the same family as EPBDS-16638, EPBDS-16639 and EPBDS-16641** — a project left without a descriptor
while *Detect projects by Excel files* is off also answers 404 on its Management tab, refuses a copy to a
branch and is not deployed. This one is the migrate.

**What happens.** For a project with no `rules.xml` — workbooks lying in its root, which is what a migration
is for — `POST /web/projects/{id}/migrate?scope=rulesXml` answers HTTP 400
`openl.error.file.descriptor.name.required.message` (*The project name is required.*) **after** it has already
moved every workbook into `rules/`. Nothing is put back: the root is left holding neither the workbooks nor a
descriptor, so the project stops resolving as a rules project at all and lists no modules.

**Where it comes from.** `ProjectMigrationService.rulesXmlForMovedWorkbooks`
(`STUDIO/org.openl.rules.webstudio/.../projects/service/ProjectMigrationService.java:175-188`) builds a
`ProjectDescriptor` and never sets its `<name>`; writing it goes through `ProjectDescriptorValidator`
(`.../projects/validator/file/ProjectDescriptorValidator.java:104-111`), which refuses a blank name because,
with no `rules.xml` on disk, there is no stored name to match it against. The three other places that write a
descriptor for such a project all set the name (`TableCreatorService:488`, `ProjectOpenApiService:193`,
`ZipProjectSaveStrategy:165`), and the class comment of the migration service
(`ProjectMigrationService.java:47-50`) says Studio never drops the project name — the code does.

The move and the write are not one act: `ProjectMigrationService.java:154-166` moves every workbook in a loop
and only then writes the descriptor, with nothing putting the moves back when the write is refused.

`GET /web/projects/{id}/migration` still answers `migratable: true` for such a project, so the screen offers a
migration that cannot succeed for any input.

**Why it is not caught.** The unit tests mock `ProjectFilesService` whole
(`ProjectMigrationServiceTest.java:40`), so the validator never runs; and the end-to-end case that covered
this branch (`ITEST/itest.studio/repos/test-resources/task_EPBDS-16364-xls-loss`) was rewritten to expect
`migratable: false` when EPBDS-16415 made the import write a descriptor of its own, so nothing exercises the
branch any more.

**Test blocked.** `TestMigrateLegacyProjectUi.testMigrateMovesRootWorkbookOfAProjectWithoutDescriptor` — the
half of the scenario where the workbooks are moved. The other half, the rewrite of a legacy descriptor, runs.

Reproduced on `ghcr.io/openl-tablets/webstudio:6.5.0-b48c86279338`: upload `MigrateXlsProject.zip`, open it,
`DELETE /web/projects/{id}/files/rules.xml`, then `POST /web/projects/{id}/migrate?scope=rulesXml` → 400, and
`GET /web/projects/{id}/files` → `rules/` alone.

---

## 21. The OpenAPI section keeps a specification that has been deleted

**Filed as EPBDS-16656** (*The message "The project declares no OpenAPI specification." is displayed when
openAPI file was generated*, 17 Sep 2026) — the same section reading its files once.
`TestGenerateOpenApiDefaultDate#testCardNamesTheSpecificationJustGenerated` asks the card to name the
specification a generation has just written and carries `@KnownIssue("EPBDS-16656")`.

**What happens.** A project made from a specification declares none in its descriptor: the file lying in the
project under the name its format reads as is what is read against, and the card names it, marked
`openapi-by-default`. Delete that file on the project's Files tab and the OpenAPI section goes on naming it —
FILE `openapi.yaml`, MODE Reconciliation — until the card is drawn again from the start.

**Where it comes from.** The section reads the files of the project once, on a dependency list of
`[editing, projectId]` (`STUDIO/studio-ui/src/containers/projects/OverviewPanel.tsx:1282-1297`), and the
token the card bumps when something about the project has changed reaches the descriptor
(`OverviewPanel.tsx:828`) and the migration (`:906-910`) but not this section. The Overview tab stays mounted
while the reader is on Files (`ProjectDetail.tsx:425-441`), so nothing remounts it either.

**Why it matters.** The card says the project reads against a specification it no longer holds, which is the
one thing that section is there to answer.

**Test changed rather than blocked.** `TestDeleteOpenApiFileRemovesProperties` draws the card again from the
start before reading the section. What is not asked, and is the coverage to restore with the fix, is that the
section answers the deletion by itself.

## 22. The values of an alias datatype never reach the Run/Trace launcher

**What happens.** A parameter whose type is an alias datatype — `myType`, declaring bla1, bla2, bla3 — is
offered as a plain text box in the Run and Trace launchers, where the old launcher offered the three values
to pick from. It holds for the datatype itself and for an array of it: growing `my (myType[])` by one and
folding the row open gives `input-my[0]`, an `ant-input`, not a list.

**Where it comes from.** The launcher draws its form from the JSON schema of the parameter, and the schema is
generated from the erased Java class: `ExecutionValueMapper.java:182-186` calls
`SafeSchemaGenerator.generate(..., type.getInstanceClass())`, and `DomainOpenClass.getInstanceClass()`
(`DEV/org.openl.rules/src/org/openl/types/impl/DomainOpenClass.java:100-102`) answers `String.class` for
`myType` and `String[].class` for `myType[]`. The domain is dropped there, so the schema carries no `enum`,
and the front end picks `'string'` (`studio-ui/src/components/schemaForm/schema.ts:97-122`) and draws an
`Input` (`ScalarEditor.tsx:181-194`) in place of the `Select` it draws for `enum` (`:127-142`). The domain is
still at hand where the schema is asked for — `TableInputServiceImpl.java:119-137` passes the declared
`IOpenClass`, and an array of an alias type is itself a `DomainOpenClass` carrying the element's domain
(`AOpenClass.java:90-106`) — and the front end is ready to draw a list from `items`
(`SchemaTree.tsx:256-271`, covered by `SchemaForm.test.tsx`). For an array the `enum` belongs in `items`, the
domain being the element's.

This is a regression of what EPBDS-6497 gave the Run page (`d9e15d6feb`, *"Show appropriate input elements
for input parameters in Run Table page"*): the JSF tree held the `IOpenClass` itself and answered `selection`
for an alias (deleted `STUDIO/org.openl.rules.webstudio/src/org/openl/rules/webstudio/web/test/SimpleParameterTreeNode.java:47-81`),
with a collection's elements built from `getComponentClass()` (`CollectionParameterTreeNode.java:163-170`).
The new route came in with EPBDS-16560 (`1929d67c7b`), the JSF nodes were deleted in `ba11551ebe`. The test
that traces this is filed under EPBDS-7796, which is the ticket the array case was raised as.

**Why it matters.** The whole point of an alias datatype is that only its values are allowed; the launcher
now takes anything typed into the box.

**Blocked tests.**
**Filed as EPBDS-16660.**

- `tests.ui.webstudio.studio_issues.TestArrayOfAliasValuesInRunTrace#testArrayOfAliasValuesInRunTrace` — the
  scenario stops with `SkipException` before it runs. Its assertion `containsExactly("bla1", "bla2", "bla3")`
  is what catches the defect and is kept in the source word for word, but it is not carried out while the
  test is blocked, so nothing in the suite asks the launcher for the values of an alias datatype until the
  fix lands.

**Tests changed rather than blocked.** `TestSimpleLookupSimpleRules` runs its rules with a `Gender` and a
`Marital_Status`, both alias datatypes. It now types those values into the boxes the launcher offers instead
of picking them from lists; what the run returns is still checked in full. Picking them from a list is the
coverage to restore with the fix.

## 23. Generating tables lays a second module beside the one the project already has

**Filed as EPBDS-16661.**

**What happens.** A project whose modules come from patterns — every project made from a template; the
descriptor of *Example 3 - Auto Policy Calculation* is `<project/>`, so the defaults `rules/**/*.xlsx` and
`tests/**/*.xlsx` apply — is asked to generate tables into a module it already has. `AutoPolicyTests` reads
`tests/AutoPolicyTests.xlsx`, and the plan answers
`{"name":"AutoPolicyTests","path":"rules/AutoPolicyTests.xlsx","declared":true}`: a workbook that does not
exist, announced as one about to be replaced. The generation writes it, and the project is left with two
modules named `AutoPolicyTests`, one under `rules/` and one under `tests/`. Read off the running application:
`GET /projects/{id}/openapi/generation`, then the generation, then `GET /projects/{id}/modules`.

**Where it comes from.** `ProjectOpenApiGenerationService.targetOf` looks for the module among the
declarations that are **not** patterns (`.filter(module -> !module.isModuleWithWildcard())`, line 121),
although the documentation directly above it promises the opposite — *"where a wildcard already matches the
workbook and names the module after it. The generation writes over the workbook such a module reads rather
than laying a second one beside it"*. It also reads the declared `rules.xml` rather than the resolved
descriptor, so the patterns are never expanded into the files they matched. The `declared` flag then comes
from `ProjectDescriptorManager.isNamedByWildcard`, which answers about the path the generation invented
rather than about a module that exists: `rules/AutoPolicyTests.xlsx` falls under `rules/**/*.xlsx` and its
base name is the module name, so the answer is yes. The screen sends that path straight back
(`openApiActions.tsx`), so nothing downstream can correct it.

**Why it matters.** Two modules of one name are one module to the engine — a module is told by
`<project>/<name>`, the two dependency loaders compare equal and the set keeps the first — so only the
workbook whose path sorts first, the generated one under `rules/`, is compiled. The project's own module is
dropped from every compilation with nothing said about it. The product refuses this state everywhere else:
the descriptor validator answers *More than one module is named 'X'.* and adding a module answers *The
module 'X' already exists.*; the generation is the one writer that never asks. Afterwards every lookup of a
module by name reaches only one of the two, so Local Changes, a restore and a comparison address the wrong
workbook or none.

The old wizard did not do this: it found the module through the resolved descriptor, showed its real path
read-only, said *Import and overwrite*, deleted that workbook and wrote the generated one in its place
(`ProjectBean.getModulesInfo` and `regenerateOpenAPI`, deleted in `7ab8ab2570`). Nothing in the commit that
replaced it records a decision to change this.

**Blocked tests.**
- `tests.ui.webstudio.rules_editor.TestLocalChangesAfterReImportForTemplateProject` — the scenario stops with
  `SkipException` where it turns to `AutoPolicyTests`: the module it lands on was created rather than
  replaced, so it has no local history at all, and the tree now offers two modules of that name. What runs
  before it — the re-import itself and the local change it leaves on the module of rules — still runs.

---

## Defects reported on 16-17 September, covered by tests of their own

| Ticket | Test | State |
|---|---|---|
| EPBDS-16639 | `TestProjectWithoutDescriptorUi#testCopyToBranchWorksForAProjectWithoutDescriptor` | fails: *Failed to update project status.* |
| EPBDS-16641 | `TestProjectWithoutDescriptorUi#testDeployWorksForAProjectWithoutDescriptor` | fails: the project is not offered to be deployed |
| EPBDS-16652 | `TestProjectCreatedMessageUi#testProjectCreationSaysSo` | fails: creating a project says nothing |
| EPBDS-16657 | `TestMigrateAfterDeployConfigEditUi#testMigrateIsNotOfferedAgainAfterEditingTheDeployConfig` | fails: Migrate is offered again after the deploy configuration is written through the studio |
| EPBDS-16638 | `TestProjectWithoutDescriptorUi#testManagementTabOpensForAProjectWithoutDescriptor` | passes here: the 404 does not reproduce on the pinned build, so the test guards the behaviour |
| EPBDS-16653 | `TestWithinCurrentModuleOnlyAfterModuleSwitch` | passes here: the box stays offered on this build, so the test guards the behaviour |

**Not covered, and why.** EPBDS-16635 (an empty result from running a Run table) is reported against a
project attached to the ticket, and a Run table cannot be built from the templates without guessing what
that project holds. EPBDS-16650 needs a JDBC or S3 design repository, which this suite does not raise.
EPBDS-16636 (409s in the browser console) and EPBDS-16634 (an XML parsing error in the browser log, raised
against a JSF endpoint this build no longer has) are about what the browser logs rather than what the screen
does. EPBDS-16651 (the theme not reaching the pop-up messages) needs the themes, which this build does not
have: nothing in `studio-ui` knows of a night theme.

---

## Open tickets the suite already carries

Two scenarios fail against bugs raised before this migration and carry the ticket, so they report as known
issues rather than as failures. They are listed here so the count of what is red and why is complete.

| Ticket | Test | What it is about |
|---|---|---|
| EPBDS-15703 | `tests.ui.webstudio.studio_smoke.TestAdminNotifications` | the notification sent to every user |
| EPBDS-15704 | `tests.ui.webstudio.studio_smoke.TestAdminSystemSettings` | the refusal a thread count that is not a positive number should be given |

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
| **Migrate** moves the workbooks under `rules/` only for a project that declares nothing; a project that already carries a `rules.xml` is migrated by rewriting that file into its minimal modern form, and its workbooks stay where the descriptor names them (`ProjectMigrationService.java:41-47,89-101,126-132`, EPBDS-16327 — the same as the `openl:migrate` goal, which moves nothing at all). Since EPBDS-16415 an archive taken in without a descriptor is given one on the spot, so a project that declares nothing is now made by deleting its `rules.xml`. | `TestMigrateLegacyProjectUi` asks each of the two: the rewrite keeps the workbook where it is and stops offering the migration, and the move is asked of a project whose descriptor was deleted first. |
| The settings point the project at a specification it already **holds**, chosen from the ones it holds: there is no box to write a path into. A project holding none is offered none, and two modules named the same name one workbook between them, and that is what is refused when the generation is asked for (the paths are compared, and a name that is not written out becomes `rules/<name>.xlsx`) — *The rules and the data types need a workbook each; one workbook cannot hold both.* — rather than while the names are being written. A specification is also kept under the name its format reads as, so the characters the old wizard refused in a file name never reach the repository. | `TestImportOpenApiDialogDefaultStateForNonOpenApiProject` asks that a project holding no specification is offered none, in place of naming a missing file to be refused; `TestImportOpenApiModuleNamesValidation` reads the refusal off the generation it confirmed; `TestCreateProjectFromOpenApiFormValidation` asks that an oddly named file is taken and kept as `openapi.yaml`. |
| A row or a column is laid down **after or before the cell the reader is standing on**, so the buttons that lay one down are withheld until a cell is picked; and a line laid down and left empty is not kept — the screen refuses to keep a table carrying a blank line (`tableEdits.ts`, `blankLine`). | The tests pick a cell before asking for a row or a column, and write into the new line before keeping the table. |
| A cell whose type is a list of allowed values is written by picking from that list: the cell offers the list and nothing else, so the value cannot be typed into it as text. The old editor let a reader switch such a cell to a plain box and write into it. | `TestEditingCommaSeparatedArrayValues` writes the original value back by picking it from the list, in place of typing it, and still checks that the cell reads as it did before. |
| The **Show equal rows** box of the comparison now does what it says: with it off only the rows that differ are drawn. A table whose column was added shows eight of its nine rows as differing — the ninth is the heading, one cell banked across the table, which reads the same on both sides. The old screen drew all nine either way, so the box changed nothing. | `TestDisplayChangedRowsTableStructure` expects eight rows with the box off and nine with it on, which is the difference the box is there to make. |
| A row added at the **end** of a table is drawn by the comparison as one row more on the side it was added to, with that row marked as the difference; both sides are drawn whole rather than reduced to the one row that differs. | `TestDisplayChangedRowsResolveConflicts` asks that the side with the addition holds one row more, that the last row of it carries what was written and is marked, and that the row before it reads the same on both sides. |
| The window a run opens holds one row — the values the table ran with, a column each, and the value it returned (`RunResultModal.tsx:30-42`) — without the case number the old window numbered the row with. A value is drawn the way a debugger draws a literal, so a string stands in quotation marks and a value that was not given reads `null` (`valueTree.ts:20-41`). | `TestSimpleLookupSimpleRules` reads the row as the window draws it: the inputs and the result, with the strings quoted and the parameter left empty read as `null`. |
| A cell is drawn with the spaces the workbook holds, where the old editor let the browser fold them together, and a cell holding nothing reads as nothing rather than as a space. | `TestSimpleLookupSimpleRules` expects the header of `SimpleLEx2` with the two spaces its workbook carries, and the empty cell as empty. |
| The editor of a cell opens as a plain box and becomes the list, the calendar or the number field the cell asks for once the cell has said what it takes (`CellValueEditor.tsx`), and a value picked from a list is kept by Enter, as a value typed is. | `TableComponent.editCell` waits for the editor to settle before writing, picks from the list where one is offered and presses Enter to keep it. |
| The Create Table dialog holds its body under a spinner while it reads the modules, the skeleton or the sheets (`CreateTableModal.tsx:1232,1258`), and a body under the spinner takes no press at all. | `CreateTableDialogComponent` waits for the dialog to stop loading before every press, and picks a value from the list that belongs to the cell it is writing, named `<id of the cell's input>_list`. |
| A cell holding several values keeps them in the order they were picked, the new one written at the end, where the old editor wrote the whole cell out in the order of the vocabulary every time it was edited (`CellValueEditor.tsx:209-231` against the deleted `MultiselectEditor.js:149-153`). A condition of a Decision table matches such a cell as a set — `containsCtr` (`AContainsInArrayIndexedEvaluator.java:36`, `ContainsInArraySelector.java:19-33`) — so which order it is written in changes nothing it decides; where the array is a value, the order the author wrote is now kept rather than rewritten. Worth noting for whoever reads this next: **Select All** still writes the whole vocabulary in its own order (`CellValueEditor.tsx:211`), so the same set is written two different ways depending on how it was gathered. | `TestEditingCommaSeparatedArrayValues` expects each cell in the order its values were picked, and the whole value after Select All in the order of the vocabulary. |
| A cell is written into the table being edited and kept by **Save**; it no longer reaches the server the moment the cell is left, so leaving the page with a cell edited raises **Discard changes**. | `TestEditingCommaSeparatedArrayValues#testVerifyNoInfiniteLoading` keeps the table before it goes to the repository, which is what puts the value with the empty element in front of the server — the thing the scenario is about. |
| A cell holding several lines takes Enter for a line of its own, so what was written into it is kept by **Ctrl+Enter** — as the old editor took it (`CellValueEditor.tsx`, the `keys` handler). | `TableComponent.editCell` presses Ctrl+Enter where the editor is a field of several lines and Enter everywhere else, so the cell is kept rather than left open. |
| **Within Current Module Only** is asked in the launcher, before the tests are run, and the window of results no longer repeats it (`TestsResultModal`, `TestsLaunchHost.tsx:129-138`). | `TestWorkWithDuplicateTables` reads the box in the launcher it opened — the one the tests launcher carries, `tests-module-only`, rather than the one the input launcher carries — and no longer looks for it among the results. |
| Every comparison is drawn on the one comparison screen, in a window of its own — the comparison against the repository and the one between the two versions of a conflicted file alike (`compare.ts`, `openConflictCompareWindow`) — where the conflict comparison used to be a dialog inside the dialog. | `ResolveConflictsDialogComponent.clickCompareLinkInCurrentPage` takes the window the press opens and reads the comparison there. |
| The comparison against the repository opens with the rows that read the same left out, and draws them once **Show equal rows** is asked for. | `TestDisplayChangedRowsCompareScreens` asks for them before it reads the table line by line, and with them left out asks only that the differences are still marked. |
| The comparison screen offers the workbook to be compared from a list on each side, and the lists are ordered the way the files API answers: case is not told apart, so `aa_CD.xlsx` heads the list, and `Main (9).xlsx` stands before `Main.xlsx`. | `TestGitSortingExcelFilesInComparePopUp` reads the two lists the window offers, in place of the tree a started comparison draws, and expects that order. |
| A project made from a specification declares no module of its own either: the descriptor holds what the reader wrote into it and nothing else, and the workbooks are found where they lie. | `TestDeleteOpenApiFileRemovesProperties` asks that the two workbooks still stand in the exported project after the specification is deleted, in place of reading the module names out of `rules.xml`. |
| Writing the specification of a project's rules is offered beside the OpenAPI heading of the card as it is read (`openApiActions.tsx`, `openapi-write`); the settings the card is written through hold no such action. | `TestGenerateOpenApiDefaultDate` presses it on the card it is reading, without opening the card for writing. |
| The wizard stays open on a name it refuses, holding what it was given, so the refusal can be read off it. | `CreateNewProjectComponent.submitExpectingRefusal` presses Create where a refusal is what the scenario is about, and `TestProjectNameValidationUi` reads the refusal from there. |

## Class names of the component library, for whoever writes the next locator

Ant Design 6 renamed containers that a great many locators were written against. Each was confirmed in the
running application, not only in the sources:

| Ant Design 5 (≤ 6.4.0) | Ant Design 6 (6.5.0) |
|---|---|
| `ant-modal-content` | `ant-modal-container` |
| `rc-virtual-list-holder-inner` (the box a Select's options are drawn in) | `ant-select-dropdown-list-holder-inner` |
| `ant-tooltip-inner` (the box a tooltip's words are drawn in) | `ant-tooltip-container`, marked `role="tooltip"` |

`ant-select-content` (not `ant-select-selector`) is the box a Select shows its value in. An option is
`div.ant-select-item-option` and carries the value as its `title`; the text inside it sits in a box whose
class reads as the option's own, so a locator must ask for the `title` to count each option once.
