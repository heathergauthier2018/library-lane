import { expect, test } from "@playwright/test";
import { mixedWinnResults } from "./fixtures/catalog";
import { mockLibraryApis, openAddBook, searchTitle, titleInput } from "./helpers/libraryLane";

test("search debounce limits provider requests", async ({ page }) => {
  let searches = 0;
  await mockLibraryApis(page, mixedWinnResults);
  page.on("request", (request) => {
    if (request.url().includes("/api/catalog/books/search")) searches += 1;
  });
  await openAddBook(page);
  await titleInput(page).pressSequentially("because of winn", { delay: 30 });
  await expect(page.locator(".catalog-search-result").first()).toBeVisible();
  expect(searches).toBeLessThanOrEqual(1);
});

test("reopening Add Book starts with clean state", async ({ page }) => {
  await mockLibraryApis(page, mixedWinnResults);
  await openAddBook(page);
  await searchTitle(page, "because of winn");
  await page.getByRole("button", { name: "Close" }).click();
  await page.getByRole("button", { name: /add book/i }).click();
  await expect(titleInput(page)).toHaveValue("");
  await expect(page.locator(".catalog-search-result")).toHaveCount(0);
});

test("no console errors or unhandled page errors during search", async ({ page }) => {
  const errors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
  });
  page.on("pageerror", (error) => errors.push(error.message));
  await mockLibraryApis(page, mixedWinnResults);
  await openAddBook(page);
  await searchTitle(page, "because of winn");
  expect(errors).toEqual([]);
});
