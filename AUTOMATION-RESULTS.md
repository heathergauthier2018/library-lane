Library Lane automation results

Verified local checkpoint

The complete deterministic Library Lane automation suite passed on
September 6, 2026.

ESLint completed with zero errors and zero warnings.

TypeScript compilation passed.

The Vite production build passed.

All 77 Spring Boot backend tests passed.

Spring Boot connected successfully to PostgreSQL 17.10.

All 21 deterministic Playwright tests passed in Chromium.

Four live-provider scenarios remain isolated behind the @live tag.

Total deterministic automated tests: 98 passed, 0 failed, 0 skipped.

Automated coverage

Partial and complete title variants

Capitalization, punctuation, whitespace, and hyphen variants

Mixed physical/e-book/audiobook lead ordering

Large 50-result rendering

Out-of-order response protection

Catalog and resolve failure recovery

Search debounce request limits

Metadata isolation by edition

Audiobook narrator and duration integrity

Physical/e-book page-count integrity

Clean modal state after reopen

Critical accessibility scanning

Console and unhandled page-error monitoring

Book arrival, opening, closing, and reduced-motion behavior

Unicode API contract behavior

Null-field API contract behavior

Provider-failure resilience

Generated backend title-normalization matrix

Ancillary workbook and teacher-guide filtering

Study-guide synopsis and genre-contamination regression protection

Optional real-provider smoke checks

Expected non-failing warnings

The backend test logs currently include development warnings for:

Spring Security's generated development password

Spring JPA Open Session in View

Mockito's dynamically attached Java agent

These warnings do not fail the current suite, but they should be addressed
before a production deployment.

Run the complete deterministic suite

From the project root:

powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\test-all.ps1

The one-process execution-policy bypass does not modify the permanent Windows
execution policy.

For first-time dependency and browser installation:

powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\test-all.ps1 -Install

