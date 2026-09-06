Library Lane automated testing

Run everything from the project root

For the first run on a machine:

powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\test-all.ps1 -Install

For later runs:

powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\test-all.ps1

The bypass applies only to the new PowerShell process and does not change the
permanent system-wide execution policy.

test-all.ps1 must remain in the project root beside the frontend and
backend folders. It validates that structure and reports a specific error
when the files are in the wrong location.

The complete deterministic runner performs:

Spring Boot and PostgreSQL-backed Maven tests

ESLint

TypeScript compilation and the Vite production build

Deterministic Playwright tests in Chromium

Frontend checks

Run the complete deterministic frontend suite:

cd frontend
npm run test:all

Run individual checks:

npm run lint
npm run build
npm run test:e2e

The Playwright suite uses mocked catalog responses for deterministic search,
selection, metadata-isolation, debounce, out-of-order response, failure
recovery, accessibility, console-error, large-result-set, modal-state, and
book-motion checks. It does not depend on external catalog availability.

Backend suite

cd backend
.\mvnw.cmd test

The backend suite includes controller contracts, catalog service tests,
provider-failure resilience, audiobook metadata tests, combined-format
ordering, enrichment tests, and the generated search matrix in
BookCatalogAutomationMatrixTest.

The PostgreSQL development database must be available for the Spring Boot
application-context test.

Live provider smoke checks

Start the backend, then run:

cd frontend
$env:LIBRARY_LANE_API_URL="http://localhost:8080"
npm run test:live-catalog

Live checks are intentionally separate because third-party catalog
availability and provider credentials can vary even when Library Lane is
working correctly.

Generated reports

Playwright HTML: frontend/playwright-report/index.html

Playwright JSON: frontend/test-results/results.json

Maven reports: backend/target/surefire-reports

Generated Playwright and Maven report directories are not committed.