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

## 7. Half the table properties cannot be added: the panel asks for the wrong dictionary

**What changed.** The table details panel offers "Add a property" from a dictionary it reads with
`getProjectProperties(projectId)` — without a table type (`TableDetailsPanel.tsx`, the effect that fills
`dictionary`). The endpoint answers differently depending on that argument: with a table type it returns the
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

**Effect:** `description`, `tags`, `id` and `active` cannot be set on a table through the properties panel.

**Blocked tests.**
- `tests.ui.webstudio.rules_editor.TestAddAndDeleteProperty#testAddAndDeleteProperty` — adds Description,
  Tags and ID among others. The scenario now stops with `SkipException` after the properties that can still
  be added; the Category part still runs and passes.

---

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
