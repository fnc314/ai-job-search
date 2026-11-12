# Migration Plan: Resume Artifact Migration

**Status:** Proposed  
**Date:** 2026-11-11  
**Target:** tracker  
**Harness:** Junie  
**Model:** claude-4-sonnet  
**Session:** session-261111-224759-11hw  

---

## 1. Executive Summary & Goals

Migrate 67 untracked artifact files from `/Users/fnc314/Documents/Resumes` into the `job-applications/{company}/{role}/{date}/` directory structure, then mirror them to GCS as tracked artifacts. The migration is stage-first: a script produces a JSON manifest for review before any files are copied. After approval, files are copied locally and uploaded to GCS, establishing a reusable mass data migration pipeline.

**Goals:**
- Organize all untracked resume artifacts into the canonical `job-applications/` directory structure
- Cross-reference against Firestore `applications` collection for role resolution
- Produce a reviewable manifest before any file operations
- Create local directories for untracked companies (deferring Firestore records)
- Mirror all migrated files to GCS `applications/{company}/{role}/{date}/` prefix
- Establish a reusable migration pattern for future data massaging and alignment

## 2. Architecture & Design Principles

### Current Implementation

- **`scripts/contract.py`**: `normalize_dir_name()` / `slug()` derive kebab-case slugs from raw names, with alias map (`_COMPANY_SLUG_ALIASES`) and stop-word/corporate-suffix stripping. **Limitation**: does not split camelCase — `CapitalOne` becomes `capitalone` instead of `capital-one`.
- **`scripts/artifacts.py`**: `blobPath()` builds GCS paths; `upload()` / `ensureUpload()` handle GCS uploads with sha256 deduplication.
- **`scripts/alignment.py`**: `update_running_documentation()` creates `job-applications/{company}/{role}/{date}/` dirs and migrates old role folders.
- **`scripts/client.py`**: Single credential boundary; `APPLICATIONS = "applications"` collection name.
- **`job-applications/`**: 90 company folders with `{role-slug}/{YYYY-MM-DD}/` nesting.
- **Firestore**: 98 unique `companyId` values across 116 applications.

### Design Principles

- **Copy, not move**: Source files remain in `/Users/fnc314/Documents/Resumes` until user confirms deletion.
- **Stage-first**: No files are moved until the manifest is reviewed and approved.
- **No Firestore writes**: This migration only creates local directories and GCS blobs; Firestore records for untracked companies are deferred.
- **`_generic/` holding area**: Files with no company go here rather than being lost.
- **Canonical slug invariant**: All company slugs derived via `contract.slug()` after camelCase splitting and alias resolution.
- **GCS mirroring**: After local copy, upload to GCS using `artifacts.ensureUpload()` for idempotent, hash-deduplicated uploads.

### File Inventory (67 files, scanned from `/Users/fnc314/Documents/Resumes`)

#### Tracked companies (in Firestore), role in filename (8 files)
| Source | Company | Role | Firestore companyId |
|---|---|---|---|
| `FrancoNColaizzi_Resume_Netflix_SoftwareEngineer4Android.docx` | Netflix | Software Engineer 4 Android | `netflix` (2 roles — ambiguous) |
| `FrancoNColaizzi_Resume_Slack_StaffPrototypingEngineer.docx` | Slack | Staff Prototyping Engineer | `slack` (not in Firestore — untracked company) |
| `FrancoNColaizzi_Resume_CapitalOne_SeniorLeadAndroidEngineer.docx` | Capital One | Senior Lead Android Engineer | `capital-one` (3 roles — ambiguous) |
| `FrancoNColaizzi_Resume_HomeDepot_StaffSoftwareEngineer.docx` | Home Depot | Staff Software Engineer | `home-depot` (not in Firestore) |
| `FrancoNColaizzi_Resume_Google_YouTube.docx` | Google | YouTube | `google` (3 roles — ambiguous) |
| `FrancoNColaizzi_Resume_ParadigmNatl_AndroidDeveloper.docx` | Paradigm Natl | Android Developer | `paradigm-natl` (not in Firestore) |
| `COLAIZZI_FRANCO_Res_V1-Salesforce_StaffSoftwareEngineer,Android.docx` | Salesforce | Staff Software Engineer, Android | `salesforce` (1 role — match) |
| `Anthropic - Staff Software Engineer, Android - Other answers.gdoc` | Anthropic | Staff Software Engineer, Android | `anthropic` (1 role — match) |

#### Tracked companies (in Firestore), no role in filename (14 files)
| Source | Company | Firestore companyId | Roles in Firestore |
|---|---|---|---|
| `FrancoNColaizzi_Resume_Anthropic.docx` | Anthropic | `anthropic` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_Bitwarden.docx` | Bitwarden | `bitwarden` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_CapitalOne.docx` | Capital One | `capital-one` | 3 roles — ambiguous |
| `FrancoNColaizzi_Resume_EdwardJones.docx` | Edward Jones | `edward-jones` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_EightSleep.docx` | Eight Sleep | `eight-sleep` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_Ford` | Ford | `ford` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_Future` | Future | `future` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_JPMorganChase.docx` | JPMorgan Chase | `jpmorgan-chase` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_Lyft.docx` | Lyft | `lyft` (not in Firestore) | — |
| `FrancoNColaizzi_Resume_Meta.docx` | Meta | `meta` | 3 roles — ambiguous |
| `FrancoNColaizzi_Resume_NPR.docx` | NPR | `npr` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_Nextdoor.docx` | Nextdoor | `nextdoor` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_OpenAI.docx` | OpenAI | `openai` | 2 roles — ambiguous |
| `FrancoNColaizzi_Resume_PNC.pdf` | PNC | `pnc` | 1 role (None) — general |
| `FrancoNColaizzi_Resume_Reddit.docx` | Reddit | `reddit` | 4 roles — ambiguous |
| `FrancoNColaizzi_Resume_Toast.docx` | Toast | `toast` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_Unit21.docx` | Unit21 | `unit21` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_Waymo.docx` | Waymo | `waymo` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_YinzCam.docx` | YinzCam | `yinzcam` | 1 role — auto-match |
| `FrancoNColaizzi_Resume_onX.docx` | onX | `onx` | 1 role — auto-match |

#### Untracked companies (not in Firestore) (16 files)
| Source | Company | Slug |
|---|---|---|
| `FrancoNColaizzi_Resume_1Password.docx` | 1Password | `1password` |
| `FrancoNColaizzi_Resume_ClassList.docx` | ClassList | `classlist` |
| `FrancoNColaizzi_Resume_Colossus.docx` | Colossus | `colossus` |
| `FrancoNColaizzi_Resume_ColossusTechnologies.docx` | Colossus Technologies | `colossus-technologies` |
| `FrancoNColaizzi_Resume_FitnessTech.docx` | FitnessTech | `fitness-tech` |
| `FrancoNColaizzi_Resume_Hightouch.docx` | Hightouch | `hightouch` |
| `FrancoNColaizzi_Resume_Jagger.docx` | Jagger | `jagger` |
| `FrancoNColaizzi_Resume_Motorola.docx` | Motorola | `motorola` |
| `FrancoNColaizzi_Resume_Peleton.docx` | Peleton | `peleton` |
| `FrancoNColaizzi_Resume_PlanetFitness.docx` | Planet Fitness | `planet-fitness` |
| `FrancoNColaizzi_Resume_Redfin.gdoc` | Redfin | `redfin` |
| `FrancoNColaizzi_Resume_Temu.docx` | Temu | `temu` |
| `COLAIZZI_FRANCO_Res_Block.docx` | Block | `block` (in Firestore, 1 role) |
| `COLAIZZI_FRANCO_Res_Duolingo.docx` | Duolingo | `duolingo` (in Firestore, 1 role) |
| `COLAIZZI_FRANCO_Res_Pinterest` | Pinterest | `pinterest` |
| `COLAIZZI_FRANCO_Res_SpotOnStaffSoftwareEngineer.docx` | SpotOn | `spoton` (in Firestore, 1 role) |

#### Subdir-based files (role from subfolder or resume-edits header) (17 files)
| Source | Company | Role Source |
|---|---|---|
| `anthropic/Anthropic_FrancoNColaizzi_Resume.pdf` | anthropic | Firestore (1 role) |
| `capital-one/CapitalOne_FrancoNColaizzi_Resume.pdf` | capital-one | Firestore (3 roles — ambiguous) |
| `edward-jones/EdwardJones_FrancoNColaizzi_Resume.pdf` | edward-jones | Firestore (1 role) |
| `ford/Ford_FrancoNColaizzi_Resume.pdf` | ford | Firestore (1 role) |
| `future/Future_FrancoNColaizzi_Resume.pdf` | future | Firestore (1 role) |
| `jp-morgan-chase/JPMorganChase_FrancoNColaizzi_Resume.pdf` | jpmorgan-chase | Firestore (1 role) |
| `lyft/Lyft_FrancoNColaizzi_Resume.pdf` | lyft | not in Firestore |
| `motorola/Motorola_FrancoNColaizzi_Resume.pdf` | motorola | not in Firestore |
| `nextdoor/Nextdoor_FrancoNColaizzi_Resume.pdf` | nextdoor | Firestore (1 role) |
| `npr/SeniorMobileSoftwareArchitect/cover-letter.txt` | npr | subfolder: `senior-mobile-software-architect` |
| `npr/SeniorMobileSoftwareArchitect/resume-edits.txt` | npr | header: `SeniorMobileSoftwareArchitect` |
| `on-x/AndroidEngineerIII/cover-letter.txt` | on-x | subfolder: `android-engineer-iii` |
| `on-x/AndroidEngineerIII/resume-edits.txt` | on-x | header: `AndroidEngineerIII` |
| `open-ai/OpenAI_FrancoNColaizzi_Resume.pdf` | open-ai | Firestore (2 roles — ambiguous) |
| `playlist/SoftwareEngineerIIIAndroid/resume-edits.txt` | playlist | header: `SoftwareEngineerIIIAndroid` |
| `reddit/Reddit_FrancoNColaizzi_Resume.pdf` | reddit | Firestore (4 roles — ambiguous) |
| `redfin/Redfin_FrancoNColaizzi_Resume.pdf` | redfin | not in Firestore |
| `waymo/Waymo_FrancoNColaizzi_Resume.pdf` | waymo | Firestore (1 role) |

#### Ambiguous directories (4 files)
| Source | Issue |
|---|---|
| `salesforce-slack/StaffEngineerMobilePrototyping/resume-edits.txt` | Combined company — likely `salesforce` |
| `salesforce-slack/StaffEngineerPrototypingMobile/resume-edits.txt` | Combined company — likely `salesforce` |
| `consumer-tech/SeniorAndroidDeveloperNewFeatureDevelopment/resume-edits.txt` | Generic placeholder — unknown company |

#### Generic files (no company) (4 files)
| Source | Type |
|---|---|
| `COLAIZZI_FRANCO_LIB_V3.docx` | Generic resume library |
| `COLAIZZI_FRANCO_Res_V3.docx` | Generic resume v3 |
| `Colaizzi_Franco_Resume_2026-05-25.pdf` | Dated generic resume |
| `FrancoNColaizzi_Resume_SoftwareEngineerAndroidTechnicalLeadership_Meta.docx` | Role-before-company pattern → Meta |

### Slug Alias Additions Needed

The following camelCase company names produce slugs that don't match Firestore `companyId`. These need entries in `_COMPANY_SLUG_ALIASES` in both `scripts/contract.py` and `records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/DocumentIdGenerator.kt`:

| Raw Name | Current Slug | Canonical Slug | Firestore companyId |
|---|---|---|---|
| `CapitalOne` | `capitalone` | `capital-one` | `capital-one` |
| `EdwardJones` | `edwardjones` | `edward-jones` | `edward-jones` |
| `EightSleep` | `eightsleep` | `eight-sleep` | `eight-sleep` |
| `JPMorganChase` | `jpmorganchase` | `jpmorgan-chase` | `jpmorgan-chase` |
| `HomeDepot` | `homedepot` | `home-depot` | `home-depot` (not in Firestore) |
| `ThompsonReuters` | `thompsonreuters` | `thomson-reuters` | `thomson-reuters` (spelling fix) |
| `Unit21` | `unit21` | `unit21` | `unit21` (ok) |
| `YinzCam` | `yinzcam` | `yinzcam` | `yinzcam` (ok) |
| `ColossusTechnologies` | `colossustechnologies` | `colossus-technologies` | not in Firestore |
| `FitnessTech` | `fitnesstech` | `fitness-tech` | not in Firestore |
| `PlanetFitness` | `planetfitness` | `planet-fitness` | not in Firestore |
| `ParadigmNatl` | `paradigmnatl` | `paradigm-natl` | not in Firestore |

## 3. Proposed Changes & Implementation Steps

### New Script: `scripts/resume_migration.py`

A single Python script that operates in three modes:

1. **`--scan` (default)**: Scans `/Users/fnc314/Documents/Resumes`, extracts metadata, cross-references Firestore, and outputs:
   - `generated/resume-migration-manifest.json` — full mapping of source → target paths with confidence levels
   - `UNRESOLVED.md` in repo root — human-readable table of files needing manual company/role assignment

2. **`--apply`**: Reads the manifest, creates directories, copies files into `job-applications/`.

3. **`--upload`**: Reads the manifest, uploads all copied files to GCS using `artifacts.ensureUpload()` for idempotent, hash-deduplicated uploads.

### CamelCase Splitting

The script must split camelCase company names before passing to `contract.slug()`. Example: `CapitalOne` → `Capital One` → `capital-one`. Implementation:
```python
def split_camel_case(text: str) -> str:
    """Split camelCase/PascalCase into space-separated words.
    CapitalOne -> Capital One, JPMorganChase -> JP Morgan Chase
    """
    # Insert space before uppercase letters that follow lowercase
    result = re.sub(r'([a-z])([A-Z])', r'\1 \2', text)
    # Insert space before uppercase letters that precede lowercase (for acronyms)
    result = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1 \2', result)
    return result
```

### Company Extraction Strategies (in priority order)

1. **Subdirectory name**: `anthropic/Anthropic_FrancoNColaizzi_Resume.pdf` → company from subdir slug
2. **Resume-edits header**: `RESUME EDITS: {Company} - {Role}` → parse both company and role
3. **Filename pattern**: `FrancoNColaizzi_Resume_{Company}[_{Role}].ext` → after camelCase split
4. **Filename pattern**: `COLAIZZI_FRANCO_Res_{Company}[_{Role}].ext` → after camelCase split and tokenization
5. **Filename pattern**: `COLAIZZI_FRANCO_Res_V{N}-{Company}_{Role}.ext` → explicit versioned pattern
6. **Filename pattern**: `{Company} - {Role} - ...` → dash-separated (e.g., Anthropic answers.gdoc)
7. **Role-before-company**: `FrancoNColaizzi_Resume_{Role}_{Company}.ext` → detect when last token is a known company

### Role Resolution (in priority order)

1. Role extracted from filename (after camelCase split)
2. Role from subfolder name (e.g., `npr/SeniorMobileSoftwareArchitect/`)
3. Role from resume-edits.txt header: `RESUME EDITS: {Company} - {Role}`
4. Firestore lookup: if company has exactly 1 application, use that role; if multiple, flag as `ambiguous`
5. Default to `general` if no role can be determined

### Date Extraction

Use file creation timestamp (`st_birthtime` on macOS, fallback to `st_ctime`), formatted as `YYYY-MM-DD`.

### GCS Upload Strategy

After `--apply` copies files locally, `--upload` mirrors them to GCS:
- Use `artifacts.ensureUpload()` which is idempotent — skips if blob already exists with matching sha256.
- Artifact kind: `resume` (matches `contract.enums("artifactKind")`).
- For files without a Firestore application record, upload to GCS but don't create artifact documents (defer until Firestore records exist).
- For files with a Firestore match, upload and create artifact documents linked to the application.

### Ambiguous Company Resolution

- **`salesforce-slack/`**: Map to `salesforce` (the resume-edits headers reference Salesforce/Slack combined work; Salesforce is the primary employer). Role from subfolder: `staff-engineer-mobile-prototyping`.
- **`consumer-tech/`**: Flag as `unresolved` — user must identify the actual company. Role from subfolder: `senior-android-developer-new-feature-development`.
- **`COLAIZZI_FRANCO_Res_SpotOnStaffSoftwareEngineer`**: Tokenize as company=`SpotOn`, role=`StaffSoftwareEngineer`.
- **`FrancoNColaizzi_Resume_SoftwareEngineerAndroidTechnicalLeadership_Meta.docx`**: Detect `Meta` as the trailing company token; role=`SoftwareEngineerAndroidTechnicalLeadership`.

### Manifest JSON Structure

```json
{
  "source": "/Users/fnc314/Documents/Resumes/FrancoNColaizzi_Resume_Meta.docx",
  "company": "meta",
  "role": "software-engineer-android-technical-leadership",
  "role_source": "firestore",
  "date": "2026-08-25",
  "target": "job-applications/meta/software-engineer-android-technical-leadership/2026-08-25/FrancoNColaizzi_Resume_Meta.docx",
  "confidence": "high",
  "in_tracker": true,
  "notes": ""
}
```

Confidence levels: `high` (auto-detected, single match), `medium` (role from Firestore with multiple options, picked first), `low` (generic fallback), `unresolved` (needs manual input)

### Alias Updates

Add the following to `_COMPANY_SLUG_ALIASES` in both `scripts/contract.py` and `DocumentIdGenerator.kt`:
```python
"capitalone": "capital-one",
"edwardjones": "edward-jones",
"eightsleep": "eight-sleep",
"jpmorganchase": "jpmorgan-chase",
"homedepot": "home-depot",
"thompsonreuters": "thomson-reuters",
"colossustechnologies": "colossus-technologies",
"fitnesstech": "fitness-tech",
"planetfitness": "planet-fitness",
"paradigmnatl": "paradigm-natl",
```

## 4. Verification Plan & Testing Checklist

### Validation Approach

- Run `--scan` mode and review the generated manifest for accuracy
- Verify all 67 files are accounted for in the manifest
- Check that company slugs match canonical slugs via `contract.slug()` after camelCase splitting
- Verify role resolution for single-role companies matches Firestore
- Confirm ambiguous entries are flagged with `medium` or `unresolved` confidence
- After `--apply`: verify directory structure matches manifest
- After `--upload`: verify GCS blobs exist at correct kebab-case paths

### Key Scenarios

- **Tracked company, role in filename**: Should auto-resolve with `high` confidence
- **Tracked company, no role**: Should resolve via Firestore lookup (single app = `high`, multiple = `medium`)
- **Untracked company**: Should create local directories, `in_tracker: false`
- **Subdir with resume-edits**: Should parse header for company and role
- **Generic files**: Should go to `_generic/{date}/` with `low` confidence
- **Ambiguous companies**: Should be flagged as `unresolved` in UNRESOLVED.md
- **CamelCase companies**: `CapitalOne` → `capital-one`, `EdwardJones` → `edward-jones`
- **Role-before-company**: `SoftwareEngineerAndroidTechnicalLeadership_Meta` → company=`meta`

### Edge Cases

- `salesforce-slack/` combined company directory → mapped to `salesforce`
- `consumer-tech/` generic placeholder → `unresolved`
- `COLAIZZI_FRANCO_Res_*` concatenated company+role names → tokenized
- `1Password` starting with digit → handled by slug function
- Files with no creation timestamp (fallback to `st_ctime`)
- Duplicate files (same company, same role, different dates or sources)
- `ThompsonReuters` spelling → aliased to `thomson-reuters`

### Test Changes

- No automated tests for migration script (one-time tool)
- Manual verification via manifest review
- Post-apply: verify directory structure matches manifest
- Post-upload: `gsutil ls` or GCS console to verify blobs at kebab-case paths

## 5. Bookkeeping
- [ ] Plan persisted at `.agents/plans/YYYY-MM-DD/HHMMSS_resume-artifact-migration.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`