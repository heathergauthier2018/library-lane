import { expect, test } from "@playwright/test";
import type { CatalogBookResult } from "../src/api/libraryLaneApi";
import {
  mixedWinnResults,
  winnDixieAudiobook,
  winnDixieEbook,
  winnDixiePhysical,
} from "./fixtures/catalog";
import {
  catalogResults,
  field,
  mockLibraryApis,
  openAddBook,
  searchTitle,
} from "./helpers/libraryLane";

test.describe("catalog selection and metadata isolation", () => {
  test("changing format resolves a real edition and removes former edition facts", async ({ page }) => {
    const physical = {
      ...winnDixiePhysical,
      publisher: "Candlewick Press",
      publicationDate: "2000",
      pageCount: 182,
      description: "The print edition description.",
    };
    const audio = {
      ...winnDixieAudiobook,
      publisher: "Listening Library",
      publicationDate: "2014",
      description: "The complete audiobook description.",
    };

    await page.emulateMedia({ reducedMotion: "reduce" });
    await page.route("http://localhost:8080/api/**", async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (url.pathname === "/api/catalog/books/search") {
        const format = url.searchParams.get("format");
        await route.fulfill({
          status: 200,
          json: format === "AUDIOBOOK" ? [audio] : [physical],
        });
        return;
      }
      if (url.pathname === "/api/catalog/books/resolve") {
        const selected = request.postDataJSON() as CatalogBookResult;
        await route.fulfill({ status: 200, json: selected });
        return;
      }
      await route.fulfill({ status: 200, json: [] });
    });

    await openAddBook(page);
    await searchTitle(page, "because of winn");
    await catalogResults(page).first().click();
    await expect(field(page, "pageCount")).toHaveValue("182");

    await page
      .getByText("Format *", { exact: true })
      .locator("..")
      .getByRole("button")
      .click();
    await page.getByRole("option", { name: "Audiobook", exact: true }).click();

    await expect(field(page, "narrator")).toHaveValue("Cherry Jones");
    await expect(field(page, "audioLength")).toHaveValue("3h 1m");
    await expect(page.locator(".ledger-field-pageCount")).toHaveCount(0);
    await expect(page.locator(".add-book-ledger")).toContainText(
      "The complete audiobook description.",
    );
    await expect(page.locator(".add-book-ledger")).not.toContainText(
      "The print edition description.",
    );
    await expect(page.locator(".add-book-ledger")).not.toContainText(
      "Candlewick Press",
    );
  });

  test("audiobook keeps edition-specific metadata", async ({ page }) => {
    await mockLibraryApis(page, mixedWinnResults, winnDixieAudiobook);
    await openAddBook(page);
    await searchTitle(page, "because of winn");
    await catalogResults(page).nth(2).click();

    await expect(field(page, "author")).toHaveValue("Kate DiCamillo");
    await expect(field(page, "narrator")).toHaveValue("Cherry Jones");
    await expect(field(page, "audioLength")).toHaveValue("3h 1m");
    await expect(page.locator(".ledger-field-pageCount")).toHaveCount(0);
    await expect(page.locator(".add-book-ledger")).not.toContainText("Ann Patchett");
    await expect(page.locator(".add-book-ledger")).not.toContainText("Jenna Lamia");
    await expect(page.locator(".add-book-ledger")).not.toContainText("study guide");
  });

  test("physical selection has pages and no audio metadata", async ({ page }) => {
    await mockLibraryApis(page, mixedWinnResults, winnDixiePhysical);
    await openAddBook(page);
    await searchTitle(page, "because of winn");
    await catalogResults(page).first().click();

    await expect(field(page, "pageCount")).toHaveValue("182");
    await expect(page.locator(".ledger-field-narrator")).toHaveCount(0);
    await expect(page.locator(".ledger-field-audioLength")).toHaveCount(0);
  });

  test("e-book selection has pages and no audio metadata", async ({ page }) => {
    await mockLibraryApis(page, mixedWinnResults, winnDixieEbook);
    await openAddBook(page);
    await searchTitle(page, "because of winn");
    await catalogResults(page).nth(1).click();

    await expect(field(page, "pageCount")).toHaveValue("182");
    await expect(page.locator(".ledger-field-narrator")).toHaveCount(0);
  });

  test("resolve failure falls back to the selected record", async ({ page }) => {
    await page.route("http://localhost:8080/api/**", async (route) => {
      const url = new URL(route.request().url());
      if (url.pathname === "/api/catalog/books/search") {
        await route.fulfill({ status: 200, json: mixedWinnResults });
      } else if (url.pathname === "/api/catalog/books/resolve") {
        await route.fulfill({ status: 503, json: { message: "Resolve failed" } });
      } else {
        await route.fulfill({ status: 200, json: [] });
      }
    });
    await openAddBook(page);
    await searchTitle(page, "because of winn");
    await catalogResults(page).nth(2).click();
    await expect(field(page, "narrator")).toHaveValue("Cherry Jones");
  });
});
