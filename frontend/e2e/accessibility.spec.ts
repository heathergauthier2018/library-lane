import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";
import { mixedWinnResults } from "./fixtures/catalog";
import { mockLibraryApis, openAddBook, searchTitle } from "./helpers/libraryLane";

test("Add Book has no critical accessibility violations", async ({ page }) => {
  await mockLibraryApis(page, mixedWinnResults);
  await openAddBook(page);
  await searchTitle(page, "because of winn");

  const results = await new AxeBuilder({ page })
    .include(".add-book-ledger")
    .analyze();
  const critical = results.violations.filter(
    (violation) => violation.impact === "critical",
  );
  expect(critical, JSON.stringify(critical, null, 2)).toEqual([]);
});
