import { expect, test, type Page } from "@playwright/test";

const animatedBook = {
  id: 41,
  title: "The Clockwork Garden",
  description: "A magical garden wakes beneath an old library.",
  publisher: "Library Lane Press",
  publicationDate: "2024-04-02",
  pageCount: 336,
  coverImageUrl:
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='600'%3E%3Crect width='400' height='600' fill='%23294d55'/%3E%3C/svg%3E",
  primaryFormat: "PHYSICAL_BOOK",
  currentStatus: "TBR",
  createdAt: "2026-07-29T20:00:00Z",
  authors: [{ firstName: "Mara", lastName: "Vale" }],
  genres: [{ name: "Fantasy" }],
};

function animatedShelfBook(page: Page) {
  // The motion contract needs the actual shelf element. Its title attribute is
  // stable across backend author migrations, while the full accessible name
  // may legitimately omit or reformat edition-specific author metadata.
  return page.locator('button.shelf-book[title="The Clockwork Garden"]');
}

async function openLibraryWithBook(
  page: Page,
  addBook = true,
) {
  await page.route(
    (url) => url.pathname.startsWith("/api/"),
    async (route) => {
      const request = route.request();
      const url = new URL(request.url());

      if (url.pathname === "/api/books" && request.method() === "GET") {
        await route.fulfill({ status: 200, json: [] });
        return;
      }
      if (url.pathname === "/api/books" && request.method() === "POST") {
        await route.fulfill({ status: 200, json: animatedBook });
        return;
      }
      if (url.pathname.startsWith("/api/reading-experiences/book/")) {
        await route.fulfill({ status: 200, json: [] });
        return;
      }
      await route.fulfill({ status: 200, json: {} });
    },
  );

  await page.goto("/", { waitUntil: "domcontentloaded" });
  if (!addBook) return;

  await page.getByRole("button", { name: /^\+?\s*add book$/i }).click();
  await page.locator(".ledger-field-title input").fill("The Clockwork Garden");
  await page.locator(".ledger-field-author input").fill("Mara Vale");
  await page
    .getByRole("button", { name: "Add Story to Library", exact: true })
    .click();

  await expect(animatedShelfBook(page)).toBeVisible({ timeout: 15_000 });
}

test("a successfully saved cover floats into its real shelf slot", async ({
  page,
}) => {
  await openLibraryWithBook(page, false);
  await page.getByRole("button", { name: /^\+?\s*add book$/i }).click();
  await page.locator(".ledger-field-title input").fill("The Clockwork Garden");
  await page.locator(".ledger-field-author input").fill("Mara Vale");
  await page
    .getByRole("button", { name: "Add Story to Library", exact: true })
    .click();

  const motion = page.getByTestId("book-motion-layer");
  await expect(motion).toHaveAttribute("data-motion-kind", "arrival");
  await expect(motion).toHaveAttribute("data-motion-stage", "preview");
  await expect
    .poll(() => motion.getAttribute("data-motion-stage"))
    .toBe("travel");
  await expect(motion).toHaveCount(0, { timeout: 3_000 });
  await expect(animatedShelfBook(page)).toBeVisible();
});

test("a shelf spine reveals its cover before opening the book ledger", async ({
  page,
}) => {
  await openLibraryWithBook(page);

  const shelfBook = animatedShelfBook(page);
  await shelfBook.click();

  const motion = page.getByTestId("book-motion-layer");
  await expect(motion).toBeVisible();
  await expect(motion).toHaveAttribute("data-motion-kind", "open");
  await expect
    .poll(() => motion.getAttribute("data-motion-stage"))
    .toMatch(/reveal|opening/);
  await expect(page.locator(".add-book-ledger")).toBeVisible({
    timeout: 4_000,
  });
  await expect(motion).toHaveCount(0);
});

test("closing the ledger returns the book to its measured shelf position", async ({
  page,
}) => {
  await openLibraryWithBook(page);
  await animatedShelfBook(page).click();
  await expect(page.locator(".add-book-ledger")).toBeVisible({
    timeout: 4_000,
  });

  await page.getByRole("button", { name: "Close", exact: true }).click();
  const motion = page.getByTestId("book-motion-layer");
  await expect(motion).toHaveAttribute("data-motion-kind", "close");
  await expect(motion).toHaveAttribute("data-motion-stage", "closing");
  await expect(motion.locator(".book-motion-cover")).toHaveCSS("opacity", "0");
  await expect
    .poll(() => motion.getAttribute("data-motion-stage"), {
      intervals: [50],
      timeout: 2_000,
    })
    .toBe("covering");
  await expect(motion.locator(".book-motion-spread")).toHaveCSS("opacity", "0");
  await expect
    .poll(() => motion.getAttribute("data-motion-stage"), {
      intervals: [50],
      timeout: 3_500,
    })
    .toBe("return");
  await expect(motion).toHaveCount(0, { timeout: 3_000 });
  await expect(animatedShelfBook(page)).toBeVisible();
});

test("reduced-motion readers open the ledger without a flight animation", async ({
  page,
}) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await openLibraryWithBook(page);

  await animatedShelfBook(page).click();

  await expect(page.getByTestId("book-motion-layer")).toHaveCount(0);
  await expect(page.locator(".add-book-ledger")).toBeVisible();
});
