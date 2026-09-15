import { expect, test, type Page } from "@playwright/test";

// These tests animate multiple full-screen, high-resolution storybook layers.
// Keep this visual journey serial even when the wider Playwright project uses
// parallel workers; concurrent copies can starve Chromium and detach otherwise
// stable controls during actionability checks.
test.describe.configure({ mode: "serial" });

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
  shelfPosition: 1,
};

const neighboringBook = {
  ...animatedBook,
  id: 42,
  title: "The Lantern Archive",
  createdAt: "2026-07-28T20:00:00Z",
  shelfPosition: 2,
};

const alternateCover =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='600' height='900'%3E%3Crect width='600' height='900' fill='%236d294f'/%3E%3Ccircle cx='300' cy='300' r='120' fill='%23d8ae62'/%3E%3C/svg%3E";

const magicTreeHouseBook = {
  ...animatedBook,
  id: 43,
  title: "Magic Tree House 1: Valley of the Dinosaurs",
  authors: [{ firstName: "Mary Pope", lastName: "Osborne" }],
};

type CatalogFixture = {
  provider: string;
  providerId: string;
  title: string;
  authors: string[];
  coverImageUrl: string;
  format: string;
  language?: string;
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
  fixture?: { books?: typeof animatedBook[]; catalog?: CatalogFixture[] },
) {
  const books = fixture?.books || [animatedBook, neighboringBook];
  await page.route(
    (url) => url.pathname.startsWith("/api/"),
    async (route) => {
      const request = route.request();
      const url = new URL(request.url());

      if (url.pathname === "/api/books" && request.method() === "GET") {
        await route.fulfill({
          status: 200,
          json: addBook ? books : [],
        });
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
      if (url.pathname === "/api/catalog/books/search") {
        const format = url.searchParams.get("format");
        await route.fulfill({
          status: 200,
          json: fixture?.catalog || [{
            provider: "GOOGLE_BOOKS",
            providerId: format === "EBOOK" ? "clockwork-ebook" : "alternate-clockwork-cover",
            title: "The Clockwork Garden",
            authors: ["Mara Vale"],
            genres: ["Fantasy"],
            narrators: [],
            coverImageUrl: alternateCover,
            publisher: format === "EBOOK" ? "Moonlit Digital" : "Library Lane Press",
            pageCount: format === "EBOOK" ? 444 : 336,
            language: "en",
            description: "A magical garden wakes beneath an old library.",
            format: format === "EBOOK" ? "EBOOK" : "PHYSICAL",
          }],
        });
        return;
      }
      if (url.pathname === "/api/catalog/books/resolve") {
        await route.fulfill({ status: 200, json: request.postDataJSON() });
        return;
      }
      if (/^\/api\/books\/\d+$/.test(url.pathname) && request.method() === "PUT") {
        const id = Number(url.pathname.split("/").at(-1));
        await route.fulfill({ status: 200, json: books.find((book) => book.id === id) || animatedBook });
        return;
      }
      await route.fulfill({ status: 200, json: {} });
    },
  );

  await page.goto("/", { waitUntil: "domcontentloaded" });
  await page.getByRole("button", { name: "Books", exact: true }).click();
  await expect(page.locator(".books-page")).toBeVisible();

  if (!addBook) {
    await expect(
      page.getByRole("button", { name: /^\+?\s*add book$/i }),
    ).toBeVisible();
    return;
  }

  await expect(page.locator(`button.shelf-book[title="${books[0].title}"]`))
    .toBeVisible({ timeout: 15_000 });
}

test("loading the Books page never replaces saved cover art", async ({ page }) => {
  let coverUpdates = 0;
  page.on("request", (request) => {
    if (request.method() === "PUT" && /\/api\/books\/\d+$/.test(new URL(request.url()).pathname)) {
      coverUpdates += 1;
    }
  });

  await openLibraryWithBook(page);
  await page.waitForTimeout(500);

  expect(coverUpdates).toBe(0);
});

test("a reader can save a personal shelf arrangement without changing sort views", async ({ page }) => {
  let savedArrangement: { bookId: number; position: number }[] = [];
  page.on("request", (request) => {
    if (request.method() === "PUT" &&
        new URL(request.url()).pathname === "/api/books/arrangement") {
      savedArrangement = request.postDataJSON();
    }
  });

  await openLibraryWithBook(page);
  await page.getByRole("button", { name: "Arrange My Books" }).click();
  await expect(page.getByRole("button", { name: "Done Arranging" }))
    .toHaveAttribute("aria-pressed", "true");

  await animatedShelfBook(page).focus();
  await page.keyboard.press("ArrowRight");

  await expect.poll(() => savedArrangement.length).toBe(2);
  expect(savedArrangement).toEqual(expect.arrayContaining([
    { bookId: 41, position: 2 },
    { bookId: 42, position: 3 },
  ]));
  await expect(page.getByText("Your bookshelf arrangement is saved."))
    .toBeVisible();

  await page.getByLabel("Sort books").click();
  await page.getByRole("option", { name: "Title A–Z" }).click();
  await expect(page.getByRole("button", { name: "Arrange My Books" }))
    .toHaveAttribute("aria-pressed", "false");
});

test("a spine can be placed into any empty position including slot 207", async ({ page }) => {
  let savedArrangement: { bookId: number; position: number }[] = [];
  page.on("request", (request) => {
    if (request.method() === "PUT" &&
        new URL(request.url()).pathname === "/api/books/arrangement") {
      savedArrangement = request.postDataJSON();
    }
  });

  await openLibraryWithBook(page);
  await page.getByRole("button", { name: "Arrange My Books" }).click();
  const source = animatedShelfBook(page);
  await source.focus();
  await page.keyboard.press("End");
  await expect.poll(() =>
    savedArrangement.find((item) => item.bookId === 41)?.position,
  ).toBe(207);
  await page.getByTestId("shelf-slot-207").scrollIntoViewIfNeeded();
  await expect(animatedShelfBook(page)).toBeVisible();
});

test("a cover can be placed into the final fixed cover slot", async ({ page }) => {
  let savedPosition: number | undefined;
  page.on("request", (request) => {
    if (request.method() === "PUT" &&
        new URL(request.url()).pathname === "/api/books/cover-arrangement") {
      const placements = request.postDataJSON() as { bookId: number; position: number }[];
      savedPosition = placements.find((item) => item.bookId === 41)?.position;
    }
  });

  await openLibraryWithBook(page);
  await page.getByRole("button", { name: "Show covers" }).click();
  await page.getByRole("button", { name: "Arrange My Books" }).click();
  await animatedShelfBook(page).focus();
  await page.keyboard.press("End");
  await expect.poll(() => savedPosition).toBe(100);
  await page.getByTestId("shelf-slot-100").scrollIntoViewIfNeeded();
  await expect(animatedShelfBook(page)).toBeVisible();
});

test("cover mode keeps the approved 100-position physical shelf map", async ({ page }) => {
  await openLibraryWithBook(page);
  await page.getByRole("button", { name: "Show covers" }).click();

  const slots = page.locator("[data-shelf-slot]");
  await expect(slots).toHaveCount(100);
  await expect(page.getByTestId("shelf-slot-1")).toBeAttached();
  await expect(page.getByTestId("shelf-slot-100")).toBeAttached();
  await expect(page.getByTestId("shelf-slot-101")).toHaveCount(0);

  const capacities = await page.locator(".bookshelf-shelf-zone").evaluateAll(
    (zones) => zones.map((zone) => zone.querySelectorAll(":scope > .bookshelf-slot").length),
  );
  expect(capacities).toHaveLength(35);
  expect(capacities.slice(0, 5)).toEqual([2, 2, 2, 2, 2]);
  expect(capacities.slice(5)).toEqual(Array(30).fill(3));
});

test("cover arranging exposes empty shelf positions as pointer targets", async ({ page }) => {
  let savedPosition: number | undefined;
  page.on("request", (request) => {
    if (request.method() === "PUT" &&
        new URL(request.url()).pathname === "/api/books/cover-arrangement") {
      const placements = request.postDataJSON() as { bookId: number; position: number }[];
      savedPosition = placements.find((item) => item.bookId === 41)?.position;
    }
  });

  await openLibraryWithBook(page);
  await page.getByRole("button", { name: "Show covers" }).click();
  await page.getByRole("button", { name: "Arrange My Books" }).click();
  const source = animatedShelfBook(page);
  const target = page.getByTestId("shelf-slot-4");
  const sourceBox = await source.boundingBox();
  const targetBox = await target.boundingBox();
  expect(sourceBox).not.toBeNull();
  expect(targetBox).not.toBeNull();

  await page.mouse.move(sourceBox!.x + sourceBox!.width / 2, sourceBox!.y + sourceBox!.height / 2);
  await page.mouse.down();
  await page.mouse.move(targetBox!.x + targetBox!.width / 2, targetBox!.y + targetBox!.height / 2, { steps: 8 });
  await expect(page.getByTestId("arrangement-floating-book")).toBeVisible();
  await expect(target).toHaveClass(/bookshelf-slot-target/);
  await page.mouse.up();

  await expect.poll(() => savedPosition).toBe(4);
  const shelf = target.locator("xpath=..");
  await expect.poll(async () => {
    const [bookBox, shelfBox] = await Promise.all([
      animatedShelfBook(page).boundingBox(),
      shelf.boundingBox(),
    ]);
    if (!bookBox || !shelfBox) return false;
    return bookBox.x + bookBox.width / 2 > shelfBox.x + shelfBox.width * 0.58;
  }).toBe(true);
});

test("arranged covers remain distinct instead of overlapping logical slots", async ({ page }) => {
  await openLibraryWithBook(page);
  await page.getByRole("button", { name: "Show covers" }).click();

  const covers = page.locator("button.shelf-book-cover");
  await expect(covers).toHaveCount(2);
  const boxes = await covers.evaluateAll((elements) =>
    elements.map((element) => {
      const box = element.getBoundingClientRect();
      return { left: box.left, right: box.right };
    }),
  );

  expect(boxes[0].right).toBeLessThanOrEqual(boxes[1].left);
});

test("a cover changes only after the reader selects a validated candidate", async ({ page }) => {
  let coverUpdates = 0;
  const savedCoverUrls: string[] = [];
  page.on("request", (request) => {
    if (request.method() === "PUT" && new URL(request.url()).pathname === "/api/books/41") {
      coverUpdates += 1;
      const payload = request.postDataJSON() as { coverImageUrl?: string };
      savedCoverUrls.push(payload.coverImageUrl || "");
    }
  });

  await openLibraryWithBook(page);
  await animatedShelfBook(page).click();
  await expect(page.locator(".add-book-ledger")).toBeVisible({ timeout: 5_000 });
  await page.getByRole("button", { name: "Choose Cover" }).click();
  await expect(page.getByRole("dialog", { name: /Choose a cover/ })).toBeVisible();
  expect(coverUpdates).toBe(0);

  await page.getByRole("button", { name: /Original cover/i }).click();
  await expect.poll(() => coverUpdates).toBe(1);
  await expect(page.getByRole("dialog", { name: /Choose a cover/ })).toHaveCount(0);
  await expect(page.getByRole("status")).toContainText("No additional save is needed");

  // The ledger must receive the selected URL too. Saving other edits later
  // cannot restore the stale pre-picker cover.
  await page.getByRole("button", { name: "Save Changes" }).click();
  await expect.poll(() => coverUpdates).toBe(2);
  expect(savedCoverUrls).toEqual([alternateCover, alternateCover]);
});

test("cover choices reject other series books and accept the verified Book 1 title", async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await openLibraryWithBook(page, true, {
    books: [magicTreeHouseBook],
    catalog: [
      {
        provider: "GOOGLE_BOOKS",
        providerId: "wrong-series-book",
        title: "The Knight at Dawn",
        authors: ["Mary Pope Osborne"],
        coverImageUrl: alternateCover,
        format: "PHYSICAL",
        language: "en",
      },
      {
        provider: "AUDIBLE",
        providerId: "branded-audio",
        title: "Magic Tree House 1: Valley of the Dinosaurs",
        authors: ["Mary Pope Osborne"],
        coverImageUrl: `${alternateCover}#audible`,
        format: "AUDIOBOOK",
        language: "en",
      },
      {
        provider: "GOOGLE_BOOKS",
        providerId: "verified-us-title",
        title: "Dinosaurs Before Dark",
        authors: ["Mary Pope Osborne"],
        coverImageUrl: `${alternateCover}#book-one`,
        format: "PHYSICAL",
        language: "en",
      },
    ],
  });

  await page.locator(`button.shelf-book[title="${magicTreeHouseBook.title}"]`).click();
  await page.getByRole("button", { name: "Choose Cover" }).click();
  const gallery = page.getByRole("dialog", { name: /Choose a cover/ });
  await expect(gallery).toBeVisible();
  await expect(gallery.getByAltText("Dinosaurs Before Dark cover from GOOGLE_BOOKS"))
    .toBeVisible();
  await expect(gallery.getByAltText("The Knight at Dawn cover from GOOGLE_BOOKS"))
    .toHaveCount(0);
  await expect(gallery.getByAltText(/AUDIBLE/)).toHaveCount(0);
});

test("a successfully saved cover floats into its real shelf slot", async ({
  page,
}) => {
  await openLibraryWithBook(page, false);
  await page.getByRole("button", { name: /^\+?\s*add book$/i }).click();
  await page.locator(".ledger-field-author input").fill("Mara Vale");
  await page.locator(".ledger-field-title input").fill("The Clockwork Garden");
  await expect(page.locator(".ledger-field-title input"))
    .toHaveValue("The Clockwork Garden");
  await page
    .getByRole("button", { name: "Add Story to Library", exact: true })
    .click();

  const motion = page.getByTestId("book-motion-layer");
  await expect(motion).toHaveAttribute("data-motion-kind", "arrival");
  await expect(motion).toHaveAttribute(
    "data-motion-stage",
    /preview|travel/,
  );
  await expect
    .poll(() => motion.getAttribute("data-motion-stage"))
    .toBe("travel");
  await expect(motion).toHaveCount(0, { timeout: 3_000 });
  await expect(animatedShelfBook(page)).toBeVisible();
});

test("catalog format changes refresh the edition without emptying story details", async ({ page }) => {
  await openLibraryWithBook(page, false);
  await page.getByRole("button", { name: /^\+?\s*add book$/i }).click();
  await page.locator(".ledger-field-title input").fill("The Clockwork Garden");
  await page.locator(".catalog-search-result").first().click();

  await expect(page.locator(".ledger-field-author input")).toHaveValue("Mara Vale");
  await expect(page.getByRole("button", {
    name: "Choose the cover saved with this book",
  })).toBeVisible();

  await page.getByRole("button", { name: "Physical Book" }).click();
  await page.getByRole("option", { name: "E-Book" }).click();

  await expect(page.locator(".ledger-field-title input")).toHaveValue("The Clockwork Garden");
  await expect(page.locator(".ledger-field-author input")).toHaveValue("Mara Vale");
  await expect(page.locator(".ledger-field-pageCount input")).toHaveValue("444");
});

test("the same work cannot be added again under another edition", async ({
  page,
}) => {
  await openLibraryWithBook(page);
  let createRequests = 0;
  page.on("request", (request) => {
    if (request.method() === "POST" && new URL(request.url()).pathname === "/api/books") {
      createRequests += 1;
    }
  });

  await page.getByRole("button", { name: /^\+?\s*add book$/i }).click();
  await page.locator(".ledger-field-author input").fill("Mara Vale");
  await page.locator(".ledger-field-title input").fill("The Clockwork Garden");

  let duplicateMessage = "";
  page.once("dialog", async (dialog) => {
    duplicateMessage = dialog.message();
    await dialog.accept();
  });
  await page
    .getByRole("button", { name: "Add Story to Library", exact: true })
    .click();

  expect(duplicateMessage).toContain("already in your library");
  expect(duplicateMessage).toContain("create a new reading experience");
  expect(createRequests).toBe(0);
  await expect(animatedShelfBook(page)).toHaveCount(1);
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
  await expect
    .poll(() => motion.getAttribute("data-motion-stage"), {
      intervals: [50],
      timeout: 4_500,
    })
    .toBe("return");
  await expect(motion).toHaveCount(0, { timeout: 3_000 });
  await expect(animatedShelfBook(page)).toBeVisible();
});

test("cover display is remembered and uses the same book journey", async ({
  page,
}) => {
  await openLibraryWithBook(page);

  await page.getByRole("button", { name: "Show covers" }).click();
  await expect(page.locator(".bookshelf-display-cover")).toBeVisible();
  await expect(animatedShelfBook(page).locator(".shelf-book-cover-shell"))
    .toBeVisible();

  await animatedShelfBook(page).click();
  await expect(page.getByTestId("book-motion-layer"))
    .toHaveClass(/book-motion-from-cover/);
  await expect(page.locator(".add-book-ledger")).toBeVisible({
    timeout: 4_000,
  });

  await page.reload();
  await page.getByRole("button", { name: "Books", exact: true }).click();
  await expect(page.getByRole("button", { name: "Show spines" })).toBeVisible();
});

test("Escape closes an open reading record", async ({ page }) => {
  await openLibraryWithBook(page);
  await animatedShelfBook(page).click();
  await expect(page.locator(".add-book-ledger")).toBeVisible({ timeout: 4_000 });

  await page.keyboard.press("Escape");
  await expect(page.locator(".add-book-ledger")).toHaveCount(0);
  await expect(page.getByTestId("book-motion-layer")).toHaveCount(0, {
    // The physical reverse journey intentionally takes about four seconds:
    // pages close, the cover reforms, then the volume returns to its shelf.
    timeout: 6_000,
  });
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
