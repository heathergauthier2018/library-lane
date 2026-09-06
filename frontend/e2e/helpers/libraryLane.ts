import { expect, type Page } from "@playwright/test";
import type { CatalogBookResult } from "../../src/api/libraryLaneApi";

export async function mockLibraryApis(
  page: Page,
  searchResults: CatalogBookResult[],
  resolveResult?: CatalogBookResult,
) {
  await page.route("http://localhost:8080/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());

    if (url.pathname === "/api/catalog/books/search") {
      await route.fulfill({ status: 200, json: searchResults });
      return;
    }
    if (url.pathname === "/api/catalog/books/resolve") {
      const selected = request.postDataJSON() as CatalogBookResult;
      await route.fulfill({ status: 200, json: resolveResult ?? selected });
      return;
    }
    if (url.pathname === "/api/books" && request.method() === "GET") {
      await route.fulfill({ status: 200, json: [] });
      return;
    }
    if (url.pathname.startsWith("/api/reading-experiences")) {
      await route.fulfill({ status: 200, json: [] });
      return;
    }
    await route.fulfill({ status: 200, json: {} });
  });
}

export async function openAddBook(page: Page) {
  // Library Lane intentionally uses many large, high-resolution storybook
  // images, so wait for the DOM rather than the final decorative image load.
  // Do not abort image URLs: Vite uses requests such as image.png?import as
  // JavaScript modules while constructing the application dependency graph.
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const addBookButton = page
    .getByRole("button", { name: /^\+?\s*add book$/i })
    .first();
  await expect(addBookButton).toBeVisible({ timeout: 15_000 });
  await addBookButton.click();
  await expect(page.locator(".add-book-ledger")).toBeVisible();
}

export function titleInput(page: Page) {
  return page.locator(".ledger-field-title input");
}

export function catalogResults(page: Page) {
  return page.locator(".catalog-search-result");
}

export function field(page: Page, key: string) {
  return page.locator(`.ledger-field-${key} input, .ledger-field-${key} textarea`).first();
}

export async function searchTitle(page: Page, query: string) {
  await titleInput(page).fill(query);
  await expect(catalogResults(page).first()).toBeVisible({ timeout: 5_000 });
}
