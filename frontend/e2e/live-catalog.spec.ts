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
    acceptableFormats: [["AUDIOBOOK", "EBOOK", "PHYSICAL"]],
  },
  {
    query: "bfg",
    title: /^(?:the )?bfg(?:\s|\(|$)/i,
    author: /roald dahl/i,
    acceptableFormats: [["AUDIOBOOK", "EBOOK", "PHYSICAL"]],
  },
  {
    query: "not your perfect mexican daughter",
    title: /not your perfect mexican daughter/i,
    author: /erika/i,
    acceptableFormats: [["AUDIOBOOK", "PHYSICAL"]],
  },
  {
    query: "anne of green",
    title: /anne of green gables/i,
    author: /montgomery/i,
    acceptableFormats: [
      ["AUDIOBOOK", "EBOOK", "PHYSICAL"],
      ["AUDIOBOOK", "PHYSICAL"],
    ],
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
            if (!response.ok()) return false;

            const results = (await response.json()) as LiveCatalogResult[];
            for (const formats of catalogCase.acceptableFormats) {
              const candidate = results.slice(0, formats.length);
              const candidateFormats = candidate
                .map((entry) => entry.format)
                .filter((format): format is string => Boolean(format))
                .sort();
              if (candidateFormats.join("|") === formats.join("|")) {
                leadResults = candidate;
                return true;
              }
            }
            return false;
          },
          {
            message:
              `The live catalog should lead with ${catalogCase.acceptableFormats
                .map((formats) => formats.join(", "))
                .join(" or ")}`,
            timeout: 20_000,
            intervals: [1_000, 2_000, 3_000],
          },
        )
        .toBe(true);

      expect(
        catalogCase.acceptableFormats.some(
          (formats) => formats.length === leadResults.length,
        ),
      ).toBe(true);
      for (const result of leadResults) {
        expect(result.title).toMatch(catalogCase.title);
        expect((result.authors ?? []).join(" ")).toMatch(catalogCase.author);
      }
    });
  }
});
