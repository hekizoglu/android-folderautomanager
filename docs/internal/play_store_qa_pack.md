# Play Store QA Pack

> Generated: 2026-07-01
> Sources: docs/store_listing.md, docs/competitor_user_research_2026-06-30.md, docs/privacy_policy.html

## Positioning

Primary message:

- AppOrganizer is a privacy-first launcher that automatically organizes app clutter.
- It should feel less complex than classic custom launchers and more useful than a stock launcher.
- The pitch should consistently combine automatic organization, fast search, easy correction, and on-device control.

Do not lead with:

- Generic "Android launcher" copy.
- AI as the first promise.
- Permissions without a plain reason.

## Store Copy Gates

| Area | Required check | Status |
|---|---|---|
| Short description | Mentions automatic organization or smart launcher in <= 80 chars | Ready |
| Long description | Explains auto folders, fast search, customization, privacy, backup | Ready |
| Privacy-first claim | Copy must say data stays on device unless optional AI classification is enabled | Needs final review |
| QUERY_ALL_PACKAGES | Store declaration must say launcher needs installed-app visibility for organizing and launching apps | Needs Play Console entry |
| Contacts/files search | If enabled in release, privacy policy and data safety form must mention optional local indexing | Needs policy check |

## Screenshot Matrix

Capture each core screen in light and dark mode, using the same device size where possible.

| # | Screen | Purpose | Required state |
|---|---|---|---|
| 1 | Home with folders | Shows the main product promise | At least 5 organized folders, no debug overlays |
| 2 | All Apps search | Shows fast command-center search | Query with app, category, contact/file sections visible if available |
| 3 | Folder detail | Shows manual control | Folder with apps, edit affordance visible |
| 4 | Search settings | Shows opt-in sources | Apps locked on, contacts/files optional |
| 5 | Privacy/permissions | Builds trust | Permission rationale visible, no scary unexplained toggle |
| 6 | Dashboard/report | Shows value beyond icons | Usage or organization summary visible |
| 7 | Customization | Shows folder color/name/icon control | Edit dialog or customization section |
| 8 | Backup/restore | Reduces switching anxiety | Backup option visible |

Minimum deliverable:

- 4 phone screenshots light mode.
- 4 matching phone screenshots dark mode.
- No personal contact names, phone numbers, real file names, private notifications, or emails.

## Data Safety And Permission Mapping

| Feature | Permission/data | User-facing reason | Store/Privacy wording |
|---|---|---|---|
| Installed app organization | QUERY_ALL_PACKAGES / package list | Categorize and launch installed apps | Required for launcher core function; stored locally |
| Usage insights | PACKAGE_USAGE_STATS | Recent apps and usage reports | Optional system permission; stored locally |
| Notification badges | Notification listener | Badge counts on icons/folders | Counts only; notification content is not stored |
| Contacts search | READ_CONTACTS | Optional contact results in search | Optional, local index; user can disable |
| File-name search | MediaStore/SAF metadata | Optional file-name search | Names/paths only; file contents are not read |
| Backup | User-selected local file | Export/import preferences | Local file only |
| AI classification | Network/API key if enabled | Classify unknown apps | Optional; sends app/package names only |

## QUERY_ALL_PACKAGES Declaration Draft

Use case:

AppOrganizer is a launcher. It needs to see the installed application list to organize apps into folders, show all apps, search installed apps, and launch selected apps from the home screen.

Why narrower APIs are not enough:

The app must categorize and display the full installed app set, including apps the user expects to find in a launcher. Package visibility limited to explicit intents would not provide a complete launcher app drawer or automatic folder organization.

Data handling:

Installed app names, package names, category assignments, and user preferences are stored locally on the device. They are not sold or shared. Optional AI classification can be disabled and only applies to unknown apps.

## Pre-Submit Checklist

- [ ] Privacy policy URL is live and reachable from Play Console.
- [ ] Data safety form matches optional contacts/files/search behavior.
- [ ] QUERY_ALL_PACKAGES declaration uses launcher core functionality language.
- [ ] Screenshots hide personal data and demonstrate the real app UI.
- [ ] Store listing does not overpromise AI, cloud sync, or contact/file behavior.
- [ ] Release notes mention privacy-first local processing.
- [ ] Final APK/AAB is built from a clean commit.
- [ ] Telegram/shared QA build matches the commit used for screenshots.

## Pass Criteria

The Play Store package is ready when the listing, privacy policy, screenshots, permission declarations, and data safety form all tell the same story: automatic organization, fast local search, optional sensitive sources, and user control.
