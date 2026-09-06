import { expect, test } from "@playwright/test";
import {
  mixedWinnResults,
  result,
  winnDixieWorkbook,
} from "./fixtures/catalog";
import {
  catalogResults,
  mockLibraryApis,
  openAddBook,
  searchTitle,
  titleInput,
} from "./helpers/libraryLane";

test.describe("Add Book catalog search", () => {
  test("partial title presents one physical, e-book, and audiobook lead", async ({ page }) => {
    await mockLibraryApis(page, mixedWinnResults);
    await openAddBook(page);
    await searchTitle(page, "because of winn");

    await expect(catalogResults(page)).toHaveCount(3);
    await expect(catalogResults(page).nth(0)).toContainText("PHYSICAL");
    await expect(catalogResults(page).nth(1)).toContainText("E-BOOK");
    await expect(catalogResults(page).nth(2)).toContainText("AUDIOBOOK");
    for (let index = 0; index < 3; index += 1) {
      await expect(catalogResults(page).nth(index)).toContainText("Kate DiCamillo");
    }
  });

  const variants = [
    "because of winn",
    "Because of Winn-Dixie",
    "BECAUSE OF WINN DIXIE",
    "because   of   winn dixie",
    "Because of Winn Dixie!",
  ];

  for (const query of variants) {
    test(`normalizes reader query: ${query}`, async ({ page }) => {
      await mockLibraryApis(page, mixedWinnResults);
      await openAddBook(page);
      await searchTitle(page, query);
      await expect(catalogResults(page)).toHaveCount(3);
      await expect(catalogResults(page).nth(2)).toContainText("AUDIOBOOK");
    });
  }

  test("renders a large result set without dropping records", async ({ page }) => {
    const many = Array.from({ length: 50 }, (_, index) =>
      result({ providerId: `record-${index}`, title: `Generated Story ${index}` }),
    );
    await mockLibraryApis(page, many);
    await openAddBook(page);
    await searchTitle(page, "generated");
    await expect(catalogResults(page)).toHaveCount(50);
  });

  test("latest query wins when responses arrive out of order", async ({ page }) => {
    let requestNumber = 0;
    await page.route("http://localhost:8080/api/**", async (route) => {
      const url = new URL(route.request().url());
      if (url.pathname === "/api/catalog/books/search") {
        requestNumber += 1;
        if (requestNumber === 1) {
          await new Promise((resolve) => setTimeout(resolve, 1_100));
          await route.fulfill({ status: 200, json: [winnDixieWorkbook] });
        } else {
          await route.fulfill({ status: 200, json: mixedWinnResults });
        }
        return;
      }
      await route.fulfill({ status: 200, json: [] });
    });
    await openAddBook(page);
    await titleInput(page).fill("because");
    await page.waitForTimeout(800);
    await titleInput(page).fill("because of winn");
    await expect(catalogResults(page)).toHaveCount(3);
    await page.waitForTimeout(1_200);
    await expect(catalogResults(page)).toHaveCount(3);
    await expect(catalogResults(page).first()).toContainText("Kate DiCamillo");
  });

  test("catalog failure preserves manual entry", async ({ page }) => {
    await page.route("http://localhost:8080/api/**", async (route) => {
      const url = new URL(route.request().url());
      if (url.pathname === "/api/catalog/books/search") {
        await route.fulfill({ status: 503, json: { message: "Unavailable" } });
        return;
      }
      await route.fulfill({ status: 200, json: [] });
    });
    await openAddBook(page);
    await titleInput(page).fill("Manual Story");
    await expect(page.getByText(/catalog is unavailable/i)).toBeVisible();
    await expect(titleInput(page)).toHaveValue("Manual Story");
  });
});
