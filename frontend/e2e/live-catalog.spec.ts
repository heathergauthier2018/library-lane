import { expect, test } from "@playwright/test";

const liveBase = process.env.LIBRARY_LANE_API_URL;

type LiveCatalogResult = {
  title?: string;
  authors?: string[];
  format?: string;
};

const cases = [
  {
    query: "because of winn",
    title: /because of winn[- ]dixie/i,
    author: /kate dicamillo/i,
  },
  {
    query: "bfg",
    title: /^(?:the )?bfg(?:\s|\(|$)/i,
    author: /roald dahl/i,
  },
  {
    query: "not your perfect mexican daughter",
    title: /not your perfect mexican daughter/i,
    author: /erika/i,
  },
  {
    query: "anne of green",
    title: /anne of green gables/i,
    author: /montgomery/i,
  },
];

test.describe("live provider smoke checks @live", () => {
  test.skip(!liveBase, "Set LIBRARY_LANE_API_URL to run live provider checks.");
  // These requests reach rate-limited third-party catalogs. Running them in
  // parallel can make one provider temporarily disappear from an otherwise
  // healthy combined result.
  test.describe.configure({ mode: "serial" });

  for (const catalogCase of cases) {
    test(`${catalogCase.query} returns a trustworthy lead group`, async ({
      request,
    }) => {
      let leadResults: LiveCatalogResult[] = [];

      await expect
        .poll(
          async () => {
            const response = await request.get(
              `${liveBase}/api/catalog/books/search?q=${encodeURIComponent(
                catalogCase.query,
              )}&searchBy=TITLE`,
            );
            if (!response.ok()) return [];

            const results = (await response.json()) as LiveCatalogResult[];
            leadResults = results.slice(0, 3);
            return leadResults
              .map((entry) => entry.format)
              .filter((format): format is string => Boolean(format))
              .sort();
          },
          {
            message:
              "The live catalog should lead with one physical, e-book, and audiobook result",
            timeout: 20_000,
            intervals: [1_000, 2_000, 3_000],
          },
        )
        .toEqual(["AUDIOBOOK", "EBOOK", "PHYSICAL"]);

      expect(leadResults).toHaveLength(3);
      for (const result of leadResults) {
        expect(result.title).toMatch(catalogCase.title);
        expect((result.authors ?? []).join(" ")).toMatch(catalogCase.author);
      }
    });
  }
});
