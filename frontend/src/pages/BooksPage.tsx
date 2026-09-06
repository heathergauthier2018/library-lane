import { useEffect, useMemo, useRef, useState } from "react";
import type { CSSProperties } from "react";
import type { AppPage } from "../App";
import FantasySidebar from "../dashboard/FantasySidebar";
import bookshelfBackground from "../assets/storybook/backgrounds/bookshelf-background.png";
import booksHeaderWall from "../assets/storybook/backgrounds/books-header-wall.png";
import openBookBackground from "../assets/storybook/backgrounds/add-book-background.png";
import booksBalconyTransition from "../assets/storybook/frames/books-balcony-transition.png";
import booksSearchSortFrame from "../assets/storybook/frames/books-search-sort-frame.png";
import booksFilterFrame from "../assets/storybook/frames/books-filter-frame.png";
import booksAddButtonFrame from "../assets/storybook/frames/books-add-button-frame.png";
import AddBookModal from "../components/books/AddBookModal";
import BookDetailsModal from "../components/books/BookDetailsModal";
import LibrarySelect from "../components/LibrarySelect";
import type { LibrarySelectOption } from "../components/LibrarySelect";
import type { NewBook } from "../components/books/AddBookModal";
import {
  bookApi,
  catalogApi,
  readingExperienceApi,
} from "../api/libraryLaneApi";
import type {
  CatalogBookFormat,
  CatalogBookResult,
} from "../api/libraryLaneApi";

type BooksPageProps = {
  currentPage: AppPage;
  setCurrentPage: (page: AppPage) => void;
  openAddBookOnLoad?: boolean;
  onAddBookOpened?: () => void;
};

const sortOptions: LibrarySelectOption[] = [
  { value: "added-new", label: "Recently added", group: "Added to Library" },
  { value: "added-old", label: "Oldest added", group: "Added to Library" },
  { value: "title-az", label: "Title A–Z", group: "Alphabetical" },
  { value: "title-za", label: "Title Z–A", group: "Alphabetical" },
  { value: "author-az", label: "Author A–Z", group: "Alphabetical" },
  { value: "author-za", label: "Author Z–A", group: "Alphabetical" },
  { value: "series-order", label: "Series reading order", group: "Alphabetical" },
  { value: "status", label: "Reading status", group: "Reading Journey" },
  {
    value: "recent-read",
    label: "Most recently finished",
    group: "Reading Journey",
  },
  { value: "oldest-read", label: "Oldest finished", group: "Reading Journey" },
  { value: "rating-high", label: "Highest rated", group: "Ratings" },
  { value: "rating-low", label: "Lowest rated", group: "Ratings" },
  { value: "emotional-high", label: "Most emotional", group: "Ratings" },
  { value: "romance-high", label: "Most romantic", group: "Ratings" },
  { value: "spice-high", label: "Spiciest", group: "Ratings" },
  { value: "horror-high", label: "Scariest", group: "Ratings" },
  { value: "reread-high", label: "Most rereadable", group: "Ratings" },
  { value: "pages-high", label: "Most pages", group: "Book Details" },
  { value: "pages-low", label: "Fewest pages", group: "Book Details" },
  {
    value: "publication-new",
    label: "Publication date: newest",
    group: "Book Details",
  },
  {
    value: "publication-old",
    label: "Publication date: oldest",
    group: "Book Details",
  },
];

const statusOptions: LibrarySelectOption[] = [
  { value: "TO_READ", label: "Want To Read" },
  { value: "READING", label: "Currently Reading" },
  { value: "COMPLETED", label: "Completed" },
  { value: "DNF", label: "DNF" },
];

const formatOptions: LibrarySelectOption[] = [
  { value: "PHYSICAL", label: "Physical" },
  { value: "EBOOK", label: "E-Book" },
  { value: "AUDIOBOOK", label: "Audiobook" },
  { value: "MIXED", label: "Mixed" },
];

const seriesOptions: LibrarySelectOption[] = [
  { value: "SERIES", label: "Series only" },
  { value: "STANDALONE", label: "Standalones only" },
];

const romanceOptions: LibrarySelectOption[] = [
  { value: "NO", label: "No romance" },
  { value: "MINOR", label: "Minor romance" },
  { value: "SIDE", label: "Side romance" },
  { value: "SIGNIFICANT", label: "Significant romance" },
  { value: "PRIMARY", label: "Primary romance plot" },
];

const shelfOptions: LibrarySelectOption[] = [
  { value: "FAVORITES", label: "Favorites only" },
  { value: "FIVE_STAR", label: "Five-star books" },
  { value: "REREAD_WORTHY", label: "Reread-worthy" },
  { value: "COMFORT_READS", label: "Comfort reads" },
  { value: "EMOTIONAL_READS", label: "Heartbreak books" },
];

type BackendBook = {
  id?: number | string;
  title?: string;
  subtitle?: string | null;
  description?: string | null;
  publisher?: string | null;
  publicationDate?: string | null;
  pageCount?: number | null;
  audiobookLengthSeconds?: number | null;
  coverImageUrl?: string | null;
  language?: string | null;
  seriesName?: string | null;
  seriesNumber?: number | string | null;
  editionFormat?: string | null;
  narrator?: string | null;
  catalogProvider?: string | null;
  catalogProviderId?: string | null;
  personalNotes?: string | null;
  favorite?: boolean;
  owned?: boolean;
  wishlist?: boolean;
  dnf?: boolean;
  primaryFormat?: string | null;
  currentStatus?: string | null;
  createdAt?: string;
  updatedAt?: string;
  dateAdded?: string;
  authors?: {
    name?: string;
    firstName?: string;
    lastName?: string;
    penName?: string;
  }[];
  genres?: { name?: string }[];
  readingExperiences?: BackendReadingExperience[];
  [key: string]: unknown;
};

type BackendReadingExperience = {
  id?: number | string;
  label?: string | null;
  format?: string | null;
  status?: string | null;
  startDate?: string | null;
  finishDate?: string | null;
  currentPage?: number | string | null;
  totalPages?: number | string | null;
  currentListeningSeconds?: number | string | null;
  totalListeningSeconds?: number | string | null;
  listeningSpeed?: number | string | null;
  predictedRating?: string | null;
  currentRating?: string | null;
  initialRating?: string | null;
  finalRating?: string | null;
  rereadRating?: string | null;
  emotionalDevastationRating?: string | null;
  excitementRating?: string | null;
  currentExcitementRating?: string | null;
  excitementWhileReading?: string | null;
  romancePresence?: string | null;
  romanceImportance?: string | null;
  romanceRating?: string | null;
  spiceRating?: string | null;
  horrorRating?: string | null;
  romanceNotes?: string | null;
  reviewText?: string | null;
  dnfReason?: string | null;
  promptResponsesJson?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  [key: string]: unknown;
};

export type ReadingExperience = {
  id: string;
  label: string;

  title?: string;
  author?: string;
  genre?: string;
  seriesName?: string;

  format: string;
  readingStatus: string;
  startDate?: string;
  finishDate?: string;
  currentPage?: string;
  pageCount?: string;
  audioLength?: string;
  currentListeningPosition?: string;
  listeningSpeed?: string;
  narrator?: string;
  currentRating?: string;
  finalRating?: string;
  rereadRating?: string;
  emotionalDevastationRating?: string;
  romancePresence?: string;
  romanceImportance?: string;
  romanceNotes?: string;
  romanceRating?: string;
  spiceRating?: string;
  horrorRating?: string;
  notes?: string;
  createdAt: string;
  promptResponsesJson?: string;

  [key: string]: string | undefined;
};

export type ShelfBook = {
  id: string;
  title: string;
  author: string;
  genre: string;
  seriesName: string;
  seriesNumber?: string;
  subtitle?: string;
  description?: string;
  publisher?: string;
  publisherOther?: string;
  editionFormat?: string;
  narrator?: string;
  language?: string;
  isbn10?: string;
  isbn13?: string;
  catalogProvider?: string;
  catalogProviderId?: string;
  readingStatus: string;
  format: string;
  ownedStatus: string;
  source: string;
  atmosphere: string;
  romancePresence: string;
  romanceImportance?: string;
  isSeries: string;
  favorite: string;
  comfortRead: string;
  color: string;
  coverUrl?: string;
  spineUrl?: string;
  addedAt: string;
  startDate?: string;
  finishDate?: string;
  publicationYear?: string;
  pageCount?: string;
  audioLength?: string;
  finalRating?: string;
  rereadRating?: string;
  emotionalDevastationRating?: string;
  romanceRating?: string;
  spiceRating?: string;
  horrorRating?: string;
  readingExperiences: ReadingExperience[];
};

type ShelfZone = {
  className: string;
  capacity: number;
};

type MotionRect = {
  left: number;
  top: number;
  width: number;
  height: number;
};

type BookMotion = {
  kind: "arrival" | "open" | "close";
  stage:
    | "preview"
    | "lift"
    | "travel"
    | "reveal"
    | "opening"
    | "handoff"
    | "closing"
    | "covering"
    | "return";
  book: ShelfBook;
  shelfRect: MotionRect;
};

const LIBRARY_LANE_BOOKS_KEY = "libraryLaneBooks";

const mockColors = [
  "#355f86",
  "#6f4a2e",
  "#87436a",
  "#447a4f",
  "#b07b35",
  "#4d6d96",
  "#7f3646",
  "#3e6a8f",
  "#5f7c46",
  "#9b6840",
  "#4b3d70",
  "#2f6f69",
];

type SpineCssProperties = CSSProperties & {
  "--spine-accent": string;
  "--spine-base": string;
  "--spine-secondary": string;
  "--spine-ink": string;
  "--spine-image": string;
  "--spine-title-size": string;
  "--spine-title-top": string;
  "--spine-title-bottom": string;
  "--spine-texture-strength": string;
  "--spine-visible-width": string;
  "--spine-visible-height": string;
  "--spine-lean": string;
};

type SpinePalette = {
  base: string;
  secondary: string;
  accent: string;
  ink: string;
};

const coverPaletteCache = new Map<string, Promise<SpinePalette | null>>();

const spineAssetModules = import.meta.glob(
  "../assets/storybook/book-spines/*.png",
  {
    eager: true,
    import: "default",
    query: "?url",
  },
) as Record<string, string>;

const spineAssets = Object.entries(spineAssetModules)
  .sort(([left], [right]) => left.localeCompare(right))
  .map(([, assetUrl]) => assetUrl);

const spineBaseColors = [
  "#e9dfc7", "#d8a3a8", "#9cbfd0", "#c86f62", "#26364b",
  "#a694bd", "#5fa9a5", "#e7dbc0", "#b78186", "#ddd2c4",
  "#176b70", "#a9c2d1", "#b44167", "#e4ddd1", "#3a3434",
  "#e4d8bd", "#aa7141", "#75b5bd", "#91a58c", "#8996c4",
  "#743a43", "#168f9b", "#c58a35", "#a9d3d2", "#e5dac4",
  "#b8822f", "#b84468", "#e7e1d5", "#17656a", "#6b3e69",
  "#4a3027", "#9a8977", "#b9684f", "#e8d49a", "#8d7180",
];

const spiceOptions: LibrarySelectOption[] = [
  { value: "0", label: "No spice" },
  { value: "1", label: "1 — Glimpse" },
  { value: "2", label: "2 — Mild" },
  { value: "3", label: "3 — Open door" },
  { value: "4", label: "4 — Explicit" },
  { value: "5", label: "5 — Very explicit" },
  { value: "UNRATED", label: "Not rated" },
];

const genreAliases: Record<string, string> = {
  "sci fi": "Science Fiction",
  scifi: "Science Fiction",
  "science fiction": "Science Fiction",
  ya: "Young Adult",
  "young adult": "Young Adult",
  romantasy: "Romantasy",
  fantasy: "Fantasy",
  romance: "Romance",
  thriller: "Thriller",
  suspense: "Thriller",
  mystery: "Mystery",
  crime: "Crime",
  horror: "Horror",
  gothic: "Gothic",
  historical: "Historical Fiction",
  "historical fiction": "Historical Fiction",
  contemporary: "Contemporary",
  literary: "Literary Fiction",
  "literary fiction": "Literary Fiction",
  memoir: "Memoir",
  biography: "Biography",
  nonfiction: "Nonfiction",
  "non fiction": "Nonfiction",
  dystopian: "Dystopian",
  speculative: "Speculative Fiction",
  "magical realism": "Magical Realism",
  "middle grade": "Middle Grade",
  childrens: "Children’s",
  children: "Children’s",
  poetry: "Poetry",
  classics: "Classics",
};

const quietSpineNumbers = new Set([
  1, 3, 5, 8, 10, 12, 17, 19, 21, 24, 25, 27, 29, 31, 32, 35,
]);

type SelectedSpine = {
  url: string;
  ink: string;
  number: number;
  score: number;
};

const spineFamilyByNumber: Record<number, string> = {
  1: "luminous", 2: "romanticized", 3: "luminous", 4: "romanticized",
  5: "dark-regal", 6: "romanticized", 7: "enchanted", 8: "antique",
  9: "romanticized", 10: "luminous", 11: "dark-regal", 12: "luminous",
  13: "romanticized", 14: "luminous", 15: "dark-regal", 16: "antique",
  17: "warm-antique", 18: "enchanted", 19: "luminous", 20: "romanticized",
  21: "dark-regal", 22: "enchanted", 23: "warm-antique", 24: "luminous",
  25: "antique", 26: "warm-antique", 27: "romanticized", 28: "luminous",
  29: "dark-regal", 30: "dark-regal", 31: "warm-antique",
  32: "warm-antique", 33: "warm-antique", 34: "luminous",
  35: "romanticized",
};

function parseHexColor(value: string) {
  const match = value.match(/^#([\da-f]{2})([\da-f]{2})([\da-f]{2})$/i);
  if (!match) return null;
  return [
    Number.parseInt(match[1], 16),
    Number.parseInt(match[2], 16),
    Number.parseInt(match[3], 16),
  ] as const;
}

function colorDistance(left: string, right: string) {
  const leftRgb = parseHexColor(left);
  const rightRgb = parseHexColor(right);
  if (!leftRgb || !rightRgb) return 0;
  return Math.sqrt(
    leftRgb.reduce(
      (total, channel, index) =>
        total + Math.pow(channel - rightRgb[index], 2),
      0,
    ),
  );
}

function spineMoodNumbers(book: ShelfBook) {
  const description = `${book.genre} ${book.atmosphere} ${
    book.description || ""
  }`.toLowerCase();

  if (/(dragon|war|battle|epic|military|deadly|violent|assassin|kingdom)/.test(description)) {
    return new Set([5, 11, 15, 21, 22, 23, 29, 30, 31]);
  }
  if (/(horror|gothic|haunt|dark|thriller|mystery|suspense|crime)/.test(description)) {
    return new Set([5, 11, 15, 21, 29, 30, 31, 35]);
  }
  if (/(romance|romantic|tender|emotional|dreamy)/.test(description)) {
    return new Set([2, 4, 6, 9, 10, 13, 14, 20, 27, 28, 30, 35]);
  }
  if (/(fantasy|magic|fairy|whimsical|adventure|myth)/.test(description)) {
    return new Set([1, 3, 5, 7, 11, 13, 18, 20, 22, 23, 24, 29, 30, 34]);
  }
  if (/(histor|classic|memoir|biograph|literary|nonfiction)/.test(description)) {
    return new Set([8, 16, 17, 21, 23, 25, 26, 31, 32, 33]);
  }
  if (/(cozy|comfort|nature|wellness|contemporary)/.test(description)) {
    return new Set([1, 3, 7, 8, 10, 12, 14, 18, 19, 24, 25, 28, 32, 34]);
  }

  return new Set<number>();
}

function normalizeBookText(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim()
    .replace(/\s+/g, " ");
}

function stableBookHash(book: Pick<ShelfBook, "id" | "title" | "author">) {
  // Title and author keep the same book binding consistent when the book is
  // reused on another Library Lane shelf or inside a collection.
  const identity = `${normalizeBookText(book.title)}|${normalizeBookText(
    book.author,
  )}`;
  let hash = 2166136261;

  for (let index = 0; index < identity.length; index += 1) {
    hash ^= identity.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }

  return hash >>> 0;
}

function canonicalGenres(value: string) {
  const sourceGenres = value
    .split(/[,;/|]+/)
    .map((genre) => genre.trim())
    .filter(Boolean);

  return Array.from(
    new Set(
      sourceGenres.map((genre) => {
        const normalized = normalizeBookText(genre);
        return (
          genreAliases[normalized] ||
          genre
            .toLocaleLowerCase()
            .replace(/\b\w/g, (letter) => letter.toLocaleUpperCase())
        );
      }),
    ),
  );
}

function matchesAny(selected: string[], values: string[]) {
  return (
    selected.length === 0 ||
    selected.some((selection) => values.includes(selection))
  );
}

function spiceBucket(value?: string) {
  if (value === undefined || value === null || value.trim() === "") {
    return "UNRATED";
  }
  return String(Math.max(0, Math.min(5, Math.round(ratingValue(value)))));
}

function stableTextHash(identity: string) {
  let hash = 2166136261;

  for (let index = 0; index < identity.length; index += 1) {
    hash ^= identity.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }

  return hash >>> 0;
}

function bindingIdentityHash(book: ShelfBook) {
  const series = normalizeBookText(book.seriesName || "");
  if (series) {
    return stableTextHash(
      `series|${series}|${normalizeBookText(book.author || "")}`,
    );
  }
  return stableBookHash(book);
}

function rankedSpinesForBook(
  book: ShelfBook,
  palette?: SpinePalette,
): SelectedSpine[] {
  if (!spineAssets.length) {
    return [{ url: "", ink: "#f3e6c5", number: 1, score: 0 }];
  }

  const hash = bindingIdentityHash(book);
  const moodNumbers = spineMoodNumbers(book);
  const longTitle = book.title.trim().length > 22;
  const isSeriesBook = Boolean(normalizeBookText(book.seriesName || ""));
  // `book.color` is an older decorative fallback, not a verified cover color.
  // Use it for the legacy generated spine only; never let it misrepresent the
  // cover when choosing one of the illustrated bindings.
  const paletteBase = palette?.base;

  return spineAssets
    .map((url, index) => {
      const spineNumber = index + 1;
      // Cover color remains meaningful for series books too. Series cohesion is
      // handled separately through binding families, so it should not erase
      // the identity of each volume's actual cover.
      let score = paletteBase
        ? colorDistance(paletteBase, spineBaseColors[index] || paletteBase) * 0.82
        : 105;

      if (moodNumbers.has(spineNumber)) score -= 68;
      if (!isSeriesBook && longTitle && quietSpineNumbers.has(spineNumber)) {
        score -= 32;
      }
      if (!isSeriesBook && longTitle && !quietSpineNumbers.has(spineNumber)) {
        score += 22;
      }

      // A small deterministic variation prevents books with nearly identical
      // covers from all receiving the same binding.
      score += ((hash >>> (index % 16)) + index * 17) % 23;
      const baseRgb = parseHexColor(spineBaseColors[index] || "#5b4638");
      const luminance = baseRgb
        ? 0.2126 * baseRgb[0] +
          0.7152 * baseRgb[1] +
          0.0722 * baseRgb[2]
        : 90;

      return {
        url,
        number: spineNumber,
        score,
        ink: luminance > 150 ? "#3c281c" : "#f8e9c5",
      };
    })
    .sort((left, right) => left.score - right.score);
}

function spineAssetForBook(book: ShelfBook, palette?: SpinePalette) {
  return rankedSpinesForBook(book, palette)[0];
}

function normalizedSeriesKey(book: ShelfBook) {
  const explicitSeries = normalizeBookText(book.seriesName || "");
  const description = normalizeBookText(book.description || "");
  const describedSeries =
    description.match(
      /(?:in|of|from)\s+(?:the\s+)?([a-z0-9'’ -]{3,42}?)\s+(?:book\s+)?series\b/i,
    )?.[1] || "";

  const rawSeries = explicitSeries || describedSeries;
  if (!rawSeries) {
    return book.isSeries === "YES" && normalizeBookText(book.author || "")
      ? `author ${normalizeBookText(book.author)}`
      : "";
  }

  return rawSeries
    .replace(/^the\s+/, "")
    .replace(
      /\s+(series|saga|trilogy|duology|quartet|cycle|books|novels|universe)$/,
      "",
    )
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim();
}

function preferredSeriesFamilies(
  books: ShelfBook[],
  palettes: Record<string, SpinePalette>,
) {
  const groups = new Map<string, ShelfBook[]>();
  books.forEach((book) => {
    const key = normalizedSeriesKey(book);
    if (!key) return;
    groups.set(key, [...(groups.get(key) || []), book]);
  });

  const preferred = new Map<string, string>();
  groups.forEach((seriesBooks, key) => {
    if (seriesBooks.length < 2) return;

    const familyScores = new Map<string, number>();
    seriesBooks.forEach((book) => {
      rankedSpinesForBook(book, palettes[book.id]).forEach((candidate) => {
        const family = spineFamilyByNumber[candidate.number] || "luminous";
        const current = familyScores.get(family);
        if (current === undefined || candidate.score < current) {
          familyScores.set(family, candidate.score);
        }
      });
    });

    const familyTotals = [...new Set(Object.values(spineFamilyByNumber))]
      .map((family) => {
        const familyAssets = Object.entries(spineFamilyByNumber)
          .filter(([, value]) => value === family)
          .map(([number]) => Number(number));

        const total = seriesBooks.reduce((sum, book) => {
          const best = rankedSpinesForBook(book, palettes[book.id])
            .filter((candidate) => familyAssets.includes(candidate.number))
            .sort((left, right) => left.score - right.score)[0];
          return sum + (best?.score ?? 500);
        }, 0);

        // Avoid choosing a family that cannot provide a distinct binding for
        // every volume currently present in the series.
        const capacityPenalty =
          familyAssets.length < seriesBooks.length ? 1000 : 0;
        return { family, total: total + capacityPenalty };
      })
      .sort((left, right) => left.total - right.total);

    if (familyTotals[0]) preferred.set(key, familyTotals[0].family);
  });

  return preferred;
}

function minimumCostAssignment(costs: number[][]) {
  const rowCount = costs.length;
  const columnCount = costs[0]?.length || 0;
  if (!rowCount || !columnCount) return [];

  const rowPotential = Array(rowCount + 1).fill(0);
  const columnPotential = Array(columnCount + 1).fill(0);
  const matchedRow = Array(columnCount + 1).fill(0);
  const path = Array(columnCount + 1).fill(0);

  for (let row = 1; row <= rowCount; row += 1) {
    matchedRow[0] = row;
    let column = 0;
    const minimum = Array(columnCount + 1).fill(Number.POSITIVE_INFINITY);
    const used = Array(columnCount + 1).fill(false);

    do {
      used[column] = true;
      const currentRow = matchedRow[column];
      let delta = Number.POSITIVE_INFINITY;
      let nextColumn = 0;

      for (
        let candidateColumn = 1;
        candidateColumn <= columnCount;
        candidateColumn += 1
      ) {
        if (used[candidateColumn]) continue;
        const reducedCost =
          costs[currentRow - 1][candidateColumn - 1] -
          rowPotential[currentRow] -
          columnPotential[candidateColumn];

        if (reducedCost < minimum[candidateColumn]) {
          minimum[candidateColumn] = reducedCost;
          path[candidateColumn] = column;
        }
        if (minimum[candidateColumn] < delta) {
          delta = minimum[candidateColumn];
          nextColumn = candidateColumn;
        }
      }

      for (
        let candidateColumn = 0;
        candidateColumn <= columnCount;
        candidateColumn += 1
      ) {
        if (used[candidateColumn]) {
          rowPotential[matchedRow[candidateColumn]] += delta;
          columnPotential[candidateColumn] -= delta;
        } else {
          minimum[candidateColumn] -= delta;
        }
      }
      column = nextColumn;
    } while (matchedRow[column] !== 0);

    do {
      const previousColumn = path[column];
      matchedRow[column] = matchedRow[previousColumn];
      column = previousColumn;
    } while (column !== 0);
  }

  const assignment = Array(rowCount).fill(-1);
  for (let column = 1; column <= columnCount; column += 1) {
    if (matchedRow[column] > 0) {
      assignment[matchedRow[column] - 1] = column - 1;
    }
  }
  return assignment;
}

function assignUniqueSpines(
  books: ShelfBook[],
  palettes: Record<string, SpinePalette>,
) {
  const assignments: Record<string, SelectedSpine> = {};
  const stableBooks = [...books].sort((left, right) => {
    const dateDifference =
      new Date(left.addedAt || "").getTime() -
      new Date(right.addedAt || "").getTime();
    return dateDifference || stableBookHash(left) - stableBookHash(right);
  });
  const seriesFamilies = preferredSeriesFamilies(stableBooks, palettes);

  // Run one global optimization for each complete set of 35 designs. This
  // guarantees full design rotation before repetition while still producing
  // the best total fit for all books in the batch.
  for (let offset = 0; offset < stableBooks.length; offset += spineAssets.length) {
    const batch = stableBooks.slice(offset, offset + spineAssets.length);
    const candidateRows = batch.map((book) =>
      rankedSpinesForBook(book, palettes[book.id]),
    );

    const costs = batch.map((book, rowIndex) => {
      const candidatesByNumber = new Map(
        candidateRows[rowIndex].map((candidate) => [
          candidate.number,
          candidate,
        ]),
      );
      const seriesKey = normalizedSeriesKey(book);
      const preferredFamily = seriesKey
        ? seriesFamilies.get(seriesKey)
        : undefined;

      return spineAssets.map((_, assetIndex) => {
        const spineNumber = assetIndex + 1;
        const candidate = candidatesByNumber.get(spineNumber);
        const compatibilityRank = candidateRows[rowIndex].findIndex(
          (rankedCandidate) => rankedCandidate.number === spineNumber,
        );
        // Uniqueness may choose among several good matches, but it should not
        // force an unrelated binding simply to complete the rotation.
        const compatibilityPenalty =
          compatibilityRank >= 14
            ? 650 + (compatibilityRank - 13) * 42
            : Math.max(0, compatibilityRank) * 2;
        const familyPenalty =
          preferredFamily &&
          spineFamilyByNumber[spineNumber] !== preferredFamily
            ? 170
            : 0;
        return (
          (candidate?.score ?? 500) +
          compatibilityPenalty +
          familyPenalty
        );
      });
    });

    const selectedColumns = minimumCostAssignment(costs);
    batch.forEach((book, rowIndex) => {
      const selectedNumber = selectedColumns[rowIndex] + 1;
      const selected =
        candidateRows[rowIndex].find(
          (candidate) => candidate.number === selectedNumber,
        ) || candidateRows[rowIndex][0];
      assignments[book.id] = selected;
    });
  }

  return assignments;
}

function motionRect(element: HTMLElement): MotionRect {
  const rect = element.getBoundingClientRect();
  return {
    left: rect.left,
    top: rect.top,
    width: rect.width,
    height: rect.height,
  };
}

function prefersReducedMotion() {
  return window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
}

function waitForMotion(milliseconds: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, milliseconds);
  });
}

function BookMotionLayer({ motion }: { motion: BookMotion | null }) {
  if (!motion) return null;

  const spreadWidth = Math.min(
    1420,
    window.innerWidth * 0.96,
    window.innerHeight * 0.96 * 1.5,
  );
  const spreadHeight = spreadWidth / 1.5;
  const closedHeight = Math.min(spreadHeight * 0.96, window.innerHeight * 0.9);
  const closedWidth = closedHeight * (2 / 3);
  const closedRect = {
    left: window.innerWidth / 2 - closedWidth / 2,
    top: window.innerHeight / 2 - closedHeight / 2,
    width: closedWidth,
    height: closedHeight,
  };
  const spreadRect = {
    left: window.innerWidth / 2 - spreadWidth / 2,
    top: window.innerHeight / 2 - spreadHeight / 2,
    width: spreadWidth,
    height: spreadHeight,
  };
  const movingToShelf =
    motion.kind === "arrival" && motion.stage === "travel";
  const returning =
    motion.kind === "close" && motion.stage === "return";
  const onShelf = motion.stage === "lift" || movingToShelf || returning;
  const openSpread =
    motion.stage === "opening" ||
    motion.stage === "handoff" ||
    motion.stage === "closing";

  const box = onShelf
    ? motion.shelfRect
    : openSpread
      ? spreadRect
      : closedRect;

  const style = {
    left: `${box.left}px`,
    top: `${box.top}px`,
    width: `${box.width}px`,
    height: `${box.height}px`,
    "--motion-shelf-width": `${motion.shelfRect.width}px`,
    "--motion-shelf-height": `${motion.shelfRect.height}px`,
  } as CSSProperties & {
    "--motion-shelf-width": string;
    "--motion-shelf-height": string;
  };

  return (
    <div
      className={`book-motion-layer book-motion-${motion.kind} book-motion-${motion.stage}`}
      data-testid="book-motion-layer"
      data-motion-kind={motion.kind}
      data-motion-stage={motion.stage}
      aria-hidden="true"
    >
      <div className="book-motion-dust">
        {Array.from({ length: 12 }, (_, index) => (
          <i key={index} />
        ))}
      </div>
      <div className="book-motion-glow" />
      <div className="book-motion-object" style={style}>
        <div className="book-motion-floor-shadow" />
        <div
          className="book-motion-spread"
          style={{ backgroundImage: `url("${openBookBackground}")` }}
        >
          <span className="book-motion-spread-seam" />
          <span className="book-motion-proxy-copy book-motion-proxy-left">
            <strong>{motion.book.title}</strong>
            {motion.book.author && <small>by {motion.book.author}</small>}
          </span>
          <span className="book-motion-proxy-copy book-motion-proxy-right">
            <strong>Reading Record</strong>
            <small>{motion.book.readingStatus.replaceAll("_", " ")}</small>
            <small>{motion.book.format.replaceAll("_", " ")}</small>
            {motion.book.publicationYear && (
              <small>{motion.book.publicationYear}</small>
            )}
          </span>
        </div>
        <div className="book-motion-volume">
          <div className="book-motion-page-edge" />
          <div
            className="book-motion-cover"
            style={
              motion.book.coverUrl
                ? { backgroundImage: `url("${motion.book.coverUrl}")` }
                : undefined
            }
          >
            {!motion.book.coverUrl && (
              <span className="book-motion-fallback">
                <strong>{motion.book.title}</strong>
                <small>{motion.book.author}</small>
              </span>
            )}
          </div>
          <div className="book-motion-spine">
            <span>{spineTitleForBook(motion.book)}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function catalogFormatForShelfBook(format: string): CatalogBookFormat {
  if (format === "EBOOK") return "EBOOK";
  if (format === "AUDIOBOOK") return "AUDIOBOOK";
  return "PHYSICAL";
}

function normalizedPersonNames(value: string) {
  return splitList(value)
    .map((name) => normalizeBookText(name).replace(/[^\p{L}\p{N}]/gu, ""))
    .filter(Boolean);
}

function coverMatchScore(book: ShelfBook, candidate: CatalogBookResult) {
  const bookTitle = normalizeBookText(book.title);
  const candidateTitle = normalizeBookText(candidate.title || "");

  if (!bookTitle || candidateTitle !== bookTitle) return 0;

  let score = 45;
  const bookIsbn13 = book.isbn13?.replace(/\D/g, "");
  const bookIsbn10 = book.isbn10?.replace(/\D/g, "");
  const candidateIsbn13 = candidate.isbn13?.replace(/\D/g, "");
  const candidateIsbn10 = candidate.isbn10?.replace(/\D/g, "");

  if (
    (bookIsbn13 && candidateIsbn13 && bookIsbn13 === candidateIsbn13) ||
    (bookIsbn10 && candidateIsbn10 && bookIsbn10 === candidateIsbn10)
  ) {
    score += 100;
  }

  const expectedAuthors = normalizedPersonNames(book.author);
  const candidateAuthors = candidate.authors.flatMap(normalizedPersonNames);
  const authorMatches = expectedAuthors.some((expected) =>
    candidateAuthors.some(
      (candidateAuthor) =>
        candidateAuthor === expected ||
        candidateAuthor.includes(expected) ||
        expected.includes(candidateAuthor),
    ),
  );

  if (authorMatches) score += 35;
  if (candidate.format === catalogFormatForShelfBook(book.format)) score += 10;

  const expectedPublisher = normalizeBookText(book.publisher || "");
  const candidatePublisher = normalizeBookText(candidate.publisher || "");
  if (
    expectedPublisher &&
    candidatePublisher &&
    (expectedPublisher.includes(candidatePublisher) ||
      candidatePublisher.includes(expectedPublisher))
  ) {
    score += 10;
  }

  if (
    book.publicationYear &&
    candidate.publicationDate?.startsWith(book.publicationYear)
  ) {
    score += 5;
  }

  return score;
}

function bestCoverMatch(
  book: ShelfBook,
  results: CatalogBookResult[],
): CatalogBookResult | undefined {
  return results
    .filter((result) => Boolean(result.coverImageUrl))
    .map((result) => ({ result, score: coverMatchScore(book, result) }))
    .filter(({ score }) => score >= 80)
    .sort((left, right) => right.score - left.score)[0]?.result;
}

function displayCoverScore(book: ShelfBook, candidate: CatalogBookResult) {
  let score = coverMatchScore(book, candidate);
  const provider = candidate.provider.toLowerCase();
  const coverUrl = (candidate.coverImageUrl || "").toLowerCase();

  // The shelf animation represents a bound volume, even when the reader owns
  // an e-book or audiobook. Prefer a clean, front-facing physical edition for
  // that visual while retaining the selected edition's metadata.
  if (candidate.format === "PHYSICAL") score += 28;
  if (provider.includes("google_books") || provider.includes("google books")) {
    score += 14;
  }
  if (coverUrl.includes("books.google")) score += 10;
  if (provider === "open_library" || provider === "open library") score -= 12;
  if (coverUrl.includes("openlibrary") || coverUrl.includes("archive.org")) {
    score -= 16;
  }

  return score;
}

function bestDisplayCoverMatch(
  book: ShelfBook,
  results: CatalogBookResult[],
): CatalogBookResult | undefined {
  return results
    .filter((result) => Boolean(result.coverImageUrl))
    .map((result) => ({ result, score: displayCoverScore(book, result) }))
    .filter(({ score }) => score >= 80)
    .sort((left, right) => right.score - left.score)[0]?.result;
}

function coverNeedsRefinement(book: ShelfBook) {
  if (!book.coverUrl) return true;
  const provider = (book.catalogProvider || "").toLowerCase();
  const coverUrl = book.coverUrl.toLowerCase();

  return (
    provider.includes("open_library") ||
    provider.includes("open library") ||
    provider.includes("apple audiobook") ||
    provider.includes("spotify audiobook") ||
    coverUrl.includes("openlibrary") ||
    coverUrl.includes("archive.org")
  );
}

function hexColor(red: number, green: number, blue: number) {
  return `#${[red, green, blue]
    .map((channel) => Math.max(0, Math.min(255, channel)).toString(16).padStart(2, "0"))
    .join("")}`;
}

function readableSpineRgb(red: number, green: number, blue: number) {
  const luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
  if (luminance < 52) {
    const lift = 52 - luminance;
    return {
      red: red + lift * 0.86 + 5,
      green: green + lift * 0.86 + 5,
      blue: blue + lift * 0.86 + 5,
    };
  }
  if (luminance > 158) {
    const scale = 158 / luminance;
    return { red: red * scale, green: green * scale, blue: blue * scale };
  }
  return { red, green, blue };
}

function coverPalette(url: string): Promise<SpinePalette | null> {
  const cached = coverPaletteCache.get(url);
  if (cached) return cached;

  const pending = new Promise<SpinePalette | null>((resolve) => {
    const image = new Image();
    image.crossOrigin = "anonymous";
    image.decoding = "async";

    image.onload = () => {
      try {
        const canvas = document.createElement("canvas");
        canvas.width = 24;
        canvas.height = 36;
        const context = canvas.getContext("2d", { willReadFrequently: true });
        if (!context) return resolve(null);

        context.drawImage(image, 0, 0, canvas.width, canvas.height);
        const pixels = context.getImageData(
          0,
          0,
          canvas.width,
          canvas.height,
        ).data;
        const buckets = new Map<
          string,
          { count: number; red: number; green: number; blue: number }
        >();

        for (let index = 0; index < pixels.length; index += 16) {
          if (pixels[index + 3] < 180) continue;
          const red = pixels[index];
          const green = pixels[index + 1];
          const blue = pixels[index + 2];
          const key = `${red >> 5}-${green >> 5}-${blue >> 5}`;
          const bucket = buckets.get(key) || {
            count: 0,
            red: 0,
            green: 0,
            blue: 0,
          };
          bucket.count += 1;
          bucket.red += red;
          bucket.green += green;
          bucket.blue += blue;
          buckets.set(key, bucket);
        }

        const colors = [...buckets.values()]
          .map((bucket) => {
            const red = Math.round(bucket.red / bucket.count);
            const green = Math.round(bucket.green / bucket.count);
            const blue = Math.round(bucket.blue / bucket.count);
            const maximum = Math.max(red, green, blue);
            const minimum = Math.min(red, green, blue);
            return {
              ...bucket,
              red,
              green,
              blue,
              luminance: 0.2126 * red + 0.7152 * green + 0.0722 * blue,
              saturation: maximum - minimum,
            };
          })
          .filter(
            (color) =>
              color.luminance >= 24 &&
              color.luminance <= 230 &&
              (color.saturation >= 16 || color.luminance >= 48),
          )
          .sort(
            (left, right) =>
              right.count * (1 + right.saturation / 150) -
              left.count * (1 + left.saturation / 150),
          );

        const first = colors[0];
        const coverageLeader = [...colors].sort(
          (left, right) => right.count - left.count,
        )[0];
        // Dark covers frequently use a small bright title or emblem. Preserve
        // the broad black/charcoal field instead of allowing that accent to
        // become the apparent cover color.
        const dominant =
          coverageLeader &&
          coverageLeader.luminance < 78 &&
          coverageLeader.count >= (first?.count || 0) * 0.55
            ? coverageLeader
            : first;
        if (!dominant) return resolve(null);
        const accent =
          colors.find(
            (candidate) =>
              Math.abs(candidate.luminance - dominant.luminance) > 65 ||
              candidate.saturation > dominant.saturation + 55,
          ) || colors[1] || dominant;
        const adjusted = readableSpineRgb(
          dominant.red,
          dominant.green,
          dominant.blue,
        );
        const base = hexColor(adjusted.red, adjusted.green, adjusted.blue);
        const adjustedLuminance =
          0.2126 * adjusted.red +
          0.7152 * adjusted.green +
          0.0722 * adjusted.blue;

        resolve({
          base,
          secondary: hexColor(
            adjusted.red * 0.68,
            adjusted.green * 0.68,
            adjusted.blue * 0.68,
          ),
          accent: hexColor(accent.red, accent.green, accent.blue),
          ink: adjustedLuminance > 148 ? "#21150e" : "#f6e9c9",
        });
      } catch {
        resolve(null);
      }
    };
    image.onerror = () => resolve(null);
    image.src = url;
  });

  coverPaletteCache.set(url, pending);
  return pending;
}

function spineStyleForBook(
  book: ShelfBook,
  palette?: SpinePalette,
  assignedSpine?: SelectedSpine,
): SpineCssProperties {
  const hash = stableBookHash(book);
  const accentColors = ["#e1bd68", "#d6a754", "#d9caa6", "#b98b54"];
  const accent = accentColors[(hash >>> 11) % accentColors.length];
  const fallbackColors = [
    "#234c69",
    "#703a35",
    "#4a315f",
    "#315d4b",
    "#86512f",
    "#243b65",
    "#743a57",
    "#4b5c2e",
    "#6a3f28",
    "#2d5d62",
    "#543e72",
    "#7a5b25",
    "#5c2d37",
    "#365276",
    "#52643b",
    "#684b39",
  ];
  const fallbackColor = fallbackColors[(hash >>> 15) % fallbackColors.length];
  const selectedSpine =
    assignedSpine || spineAssetForBook(book, palette);
  const quietDesign = quietSpineNumbers.has(selectedSpine.number);
  const titleLength = book.title.trim().length;

  return {
    "--spine-accent": palette?.accent || accent,
    "--spine-base": palette?.base || book.color || fallbackColor,
    "--spine-secondary": palette?.secondary || fallbackColor,
    "--spine-ink": selectedSpine.ink,
    "--spine-image": `url("${selectedSpine.url}")`,
    "--spine-title-size":
      titleLength > 27 ? "8.9px" : titleLength > 15 ? "10.1px" : "11.5px",
    "--spine-title-top": quietDesign ? "9px" : "15px",
    "--spine-title-bottom": quietDesign ? "24px" : "28px",
    "--spine-texture-strength": `${0.025 + ((hash >>> 6) % 4) * 0.012}`,
    "--spine-visible-width": "40px",
    "--spine-visible-height": "122px",
    "--spine-lean": "0deg",
  };
}

function spineVariantForBook(book: ShelfBook) {
  return stableBookHash(book) % 5;
}

function spineBindingForBook(book: ShelfBook) {
  const edition = `${book.editionFormat || ""} ${book.format || ""}`.toLowerCase();
  if (/paperback|softcover/.test(edition)) return "paperback";
  if (/cloth/.test(edition)) return "clothbound";
  if (/hardcover|hardback/.test(edition)) return "hardcover";
  return ["hardcover", "clothbound", "dust-jacket", "paperback"][
    stableBookHash(book) % 4
  ];
}

function spineTypographyForBook(book: ShelfBook) {
  const genres = `${book.genre || ""}`.toLowerCase();
  if (/(romance|contemporary)/.test(genres)) return "soft-serif";
  if (/(thriller|horror|mystery|suspense)/.test(genres)) return "condensed";
  if (/(young adult|adventure)/.test(genres)) return "small-caps";
  if (/(memoir|biograph|nonfiction|history)/.test(genres)) return "literary";
  return "classic";
}

function spineTitleLayoutForBook(book: ShelfBook) {
  const length = book.title.trim().length;
  if (length <= 12) return "short";
  if (length >= 25) return "long";
  return "standard";
}

function spineTitleForBook(book: ShelfBook) {
  const fullTitle = book.title.trim();
  let mainTitle = fullTitle.includes(":")
    ? fullTitle.split(":")[0].trim()
    : fullTitle;
  if (mainTitle.length > 19) {
    mainTitle = mainTitle
      .replace(/^(the|a|an)\s+/i, "")
      .replace(/\s+and\s+/gi, " & ");
  }
  const words = mainTitle.split(/\s+/).filter(Boolean);
  let display = "";

  for (const word of words) {
    const next = display ? `${display} ${word}` : word;
    if (next.length > 21) break;
    display = next;
  }

  if (!display) display = mainTitle.slice(0, 19);
  return display.length < mainTitle.length ? `${display}…` : display;
}

function spineAuthorForBook(book: ShelfBook) {
  const firstAuthor = splitList(book.author)[0] || "";
  const parts = firstAuthor.split(/\s+/).filter(Boolean);
  return parts.at(-1) || firstAuthor;
}

function makeStarterBook(
  title: string,
  color: string,
  extra: Partial<ShelfBook> = {},
): ShelfBook {
  return {
    id: crypto.randomUUID(),
    title,
    author: "",
    genre: "",
    seriesName: "",
    readingStatus: "TO_READ",
    format: "PHYSICAL",
    ownedStatus: "",
    source: "",
    atmosphere: "",
    romancePresence: "NO",
    isSeries: "NO",
    favorite: "NO",
    comfortRead: "NO",
    color,
    addedAt: new Date().toISOString(),
    readingExperiences: [],
    ...extra,
  };
}

const starterBooks: ShelfBook[] = [
  makeStarterBook("The Midnight Library", "#1f4968", {
    author: "Matt Haig",
    genre: "Contemporary",
    readingStatus: "COMPLETED",
  }),
  makeStarterBook("The Night Circus", "#3b243f", {
    author: "Erin Morgenstern",
    genre: "Fantasy",
    readingStatus: "COMPLETED",
  }),
  makeStarterBook("The Four Winds", "#9b5f35", {
    author: "Kristin Hannah",
    genre: "Historical Fiction",
  }),
  makeStarterBook("Book Lovers", "#b06f78", {
    author: "Emily Henry",
    genre: "Romance",
    romancePresence: "PRIMARY",
  }),
  makeStarterBook("Project Hail Mary", "#1b6b78", {
    author: "Andy Weir",
    genre: "Sci-Fi",
  }),
  makeStarterBook("Demon Copperhead", "#6f4a2e", {
    author: "Barbara Kingsolver",
    genre: "Contemporary",
  }),
  makeStarterBook("Fourth Wing", "#6d2438", {
    author: "Rebecca Yarros",
    genre: "Romantasy",
    seriesName: "The Empyrean",
    isSeries: "YES",
    romancePresence: "SIGNIFICANT",
  }),
  makeStarterBook("Iron Flame", "#2f2f35", {
    author: "Rebecca Yarros",
    genre: "Romantasy",
    seriesName: "The Empyrean",
    isSeries: "YES",
    romancePresence: "SIGNIFICANT",
  }),
];

const generatedMockBooks: ShelfBook[] = Array.from({ length: 199 }, (_, i) =>
  makeStarterBook(`Book ${9 + i}`, mockColors[i % mockColors.length]),
);

const initialBooks: ShelfBook[] = [...starterBooks, ...generatedMockBooks];

const shelfZones: ShelfZone[] = [
  { className: "shelf-zone-r1-c1", capacity: 4 },
  { className: "shelf-zone-r1-c2", capacity: 4 },
  { className: "shelf-zone-r1-c3", capacity: 5 },
  { className: "shelf-zone-r1-c4", capacity: 4 },
  { className: "shelf-zone-r1-c5", capacity: 4 },

  { className: "shelf-zone-r2-c1", capacity: 6 },
  { className: "shelf-zone-r2-c2", capacity: 6 },
  { className: "shelf-zone-r2-c3", capacity: 7 },
  { className: "shelf-zone-r2-c4", capacity: 6 },
  { className: "shelf-zone-r2-c5", capacity: 6 },

  { className: "shelf-zone-r3-c1", capacity: 6 },
  { className: "shelf-zone-r3-c2", capacity: 6 },
  { className: "shelf-zone-r3-c3", capacity: 7 },
  { className: "shelf-zone-r3-c4", capacity: 6 },
  { className: "shelf-zone-r3-c5", capacity: 6 },

  { className: "shelf-zone-r4-c1", capacity: 6 },
  { className: "shelf-zone-r4-c2", capacity: 6 },
  { className: "shelf-zone-r4-c3", capacity: 7 },
  { className: "shelf-zone-r4-c4", capacity: 6 },
  { className: "shelf-zone-r4-c5", capacity: 6 },

  { className: "shelf-zone-r5-c1", capacity: 6 },
  { className: "shelf-zone-r5-c2", capacity: 6 },
  { className: "shelf-zone-r5-c3", capacity: 7 },
  { className: "shelf-zone-r5-c4", capacity: 6 },
  { className: "shelf-zone-r5-c5", capacity: 6 },

  { className: "shelf-zone-r6-c1", capacity: 6 },
  { className: "shelf-zone-r6-c2", capacity: 6 },
  { className: "shelf-zone-r6-c3", capacity: 7 },
  { className: "shelf-zone-r6-c4", capacity: 6 },
  { className: "shelf-zone-r6-c5", capacity: 6 },

  { className: "shelf-zone-r7-c1", capacity: 6 },
  { className: "shelf-zone-r7-c2", capacity: 6 },
  { className: "shelf-zone-r7-c3", capacity: 7 },
  { className: "shelf-zone-r7-c4", capacity: 6 },
  { className: "shelf-zone-r7-c5", capacity: 6 },
];

const BOOKS_PER_BOOKCASE = shelfZones.reduce(
  (total, zone) => total + zone.capacity,
  0,
);

function frontendStatusFromBackend(status?: string | null, dnf?: boolean) {
  if (dnf) return "DNF";

  switch (status) {
    case "CURRENTLY_READING":
      return "READING";
    case "COMPLETED":
      return "COMPLETED";
    case "DNF":
      return "DNF";
    case "PAUSED":
      return "READING";
    case "TBR":
    case "TO_READ":
    default:
      return "TO_READ";
  }
}

function backendStatusFromFrontend(status: string) {
  switch (status) {
    case "READING":
      return "CURRENTLY_READING";
    case "COMPLETED":
      return "COMPLETED";
    case "DNF":
      return "DNF";
    case "TO_READ":
    default:
      return "TBR";
  }
}

function frontendFormatFromBackend(format?: string | null) {
  switch (format) {
    case "EBOOK":
    case "E_BOOK":
      return "EBOOK";
    case "AUDIOBOOK":
    case "AUDIO_BOOK":
      return "AUDIOBOOK";
    case "MIXED":
    case "MIXED_FORMATS":
      return "MIXED";
    case "PHYSICAL":
    case "PHYSICAL_BOOK":
    default:
      return "PHYSICAL";
  }
}

function backendFormatFromFrontend(format: string) {
  switch (format) {
    case "EBOOK":
      return "E_BOOK";
    case "AUDIOBOOK":
      return "AUDIO_BOOK";
    case "MIXED":
      // The current backend BookFormat enum does not contain a mixed value.
      // Individual Reading Experiences still retain their own formats.
      return "PHYSICAL_BOOK";
    case "PHYSICAL":
    default:
      return "PHYSICAL_BOOK";
  }
}

function stringFromUnknown(value: unknown, fallback = "") {
  if (typeof value === "string") return value;
  if (typeof value === "number") return String(value);
  if (typeof value === "boolean") return value ? "YES" : "NO";
  return fallback;
}

function boolFromYesNo(value?: string) {
  return value === "YES";
}

function numberOrNull(value?: string) {
  if (!value) return null;

  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

function yearToPublicationDate(year?: string) {
  if (!year) return null;

  const parsedYear = Number(year);
  if (Number.isNaN(parsedYear)) return null;

  return `${parsedYear}-01-01`;
}

function publicationYearFromDate(value?: string | null) {
  if (!value) return undefined;

  return value.slice(0, 4);
}

function authorNamesFromList(
  authors?: {
    name?: string;
    firstName?: string;
    lastName?: string;
    penName?: string;
  }[],
) {
  return (
    authors
      ?.map((author) => {
        const penName = author.penName?.trim();
        if (penName) return penName;

        return (
          [author.firstName, author.lastName]
            .filter(Boolean)
            .join(" ")
            .trim() ||
          author.name?.trim() ||
          ""
        );
      })
      .filter(Boolean)
      .join(", ") || ""
  );
}

function genreNamesFromList(genres?: { name?: string }[]) {
  return (
    genres
      ?.map((genre) => genre.name)
      .filter(Boolean)
      .join(", ") || ""
  );
}

function migrateReadingExperience(
  experience: Partial<ReadingExperience> | BackendReadingExperience,
): ReadingExperience {
  const backendExperience = experience as BackendReadingExperience;
  const frontendExperience = experience as Partial<ReadingExperience>;

  const isBackendExperience =
    "status" in backendExperience ||
    "totalPages" in backendExperience ||
    "reviewText" in backendExperience;

  return {
    id: stringFromUnknown(experience.id, crypto.randomUUID()),
    label:
      stringFromUnknown(backendExperience.label) ||
      frontendExperience.label ||
      "Reading Experience",

    title: frontendExperience.title,
    author: frontendExperience.author,
    genre: frontendExperience.genre,
    seriesName: frontendExperience.seriesName,

    format: isBackendExperience
      ? frontendFormatFromBackend(backendExperience.format)
      : frontendExperience.format || "PHYSICAL",
    readingStatus: isBackendExperience
      ? frontendStatusFromBackend(backendExperience.status)
      : frontendExperience.readingStatus || "TO_READ",

    startDate:
      stringFromUnknown(backendExperience.startDate) ||
      frontendExperience.startDate,
    finishDate:
      stringFromUnknown(backendExperience.finishDate) ||
      frontendExperience.finishDate,

    currentPage:
      stringFromUnknown(backendExperience.currentPage) ||
      frontendExperience.currentPage,
    pageCount:
      stringFromUnknown(backendExperience.totalPages) ||
      frontendExperience.pageCount,
    audioLength:
      stringFromUnknown(backendExperience.totalListeningSeconds) ||
      frontendExperience.audioLength,
    currentListeningPosition:
      stringFromUnknown(backendExperience.currentListeningSeconds) ||
      frontendExperience.currentListeningPosition,
    listeningSpeed:
      stringFromUnknown(backendExperience.listeningSpeed) ||
      frontendExperience.listeningSpeed,
    narrator: frontendExperience.narrator,

    excitementRating:
      stringFromUnknown(backendExperience.excitementRating) ||
      frontendExperience.excitementRating,
    predictedRating:
      stringFromUnknown(backendExperience.predictedRating) ||
      frontendExperience.predictedRating,
    currentRating:
      stringFromUnknown(backendExperience.currentRating) ||
      frontendExperience.currentRating,
    currentExcitementRating:
      stringFromUnknown(backendExperience.currentExcitementRating) ||
      frontendExperience.currentExcitementRating,
    initialRating:
      stringFromUnknown(backendExperience.initialRating) ||
      frontendExperience.initialRating,
    finalRating:
      stringFromUnknown(backendExperience.finalRating) ||
      frontendExperience.finalRating,
    rereadRating:
      stringFromUnknown(backendExperience.rereadRating) ||
      frontendExperience.rereadRating,
    emotionalDevastationRating:
      stringFromUnknown(backendExperience.emotionalDevastationRating) ||
      frontendExperience.emotionalDevastationRating,

    romancePresence:
      stringFromUnknown(backendExperience.romancePresence) ||
      frontendExperience.romancePresence,
    romanceImportance:
      stringFromUnknown(backendExperience.romanceImportance) ||
      frontendExperience.romanceImportance,
    romanceRating:
      stringFromUnknown(backendExperience.romanceRating) ||
      frontendExperience.romanceRating,
    spiceRating:
      stringFromUnknown(backendExperience.spiceRating) ||
      frontendExperience.spiceRating,
    romanceNotes:
      stringFromUnknown(backendExperience.romanceNotes) ||
      frontendExperience.romanceNotes,

    horrorRating:
      stringFromUnknown(backendExperience.horrorRating) ||
      frontendExperience.horrorRating,

    notes:
      stringFromUnknown(backendExperience.reviewText) ||
      frontendExperience.notes,
    dnfReason:
      stringFromUnknown(backendExperience.dnfReason) ||
      frontendExperience.dnfReason,
    promptResponsesJson:
      stringFromUnknown(backendExperience.promptResponsesJson) ||
      frontendExperience.promptResponsesJson,

    createdAt:
      stringFromUnknown(backendExperience.createdAt) ||
      frontendExperience.createdAt ||
      new Date().toISOString(),
  };
}

function migrateBook(book: Partial<ShelfBook> | BackendBook): ShelfBook {
  const backendBook = book as BackendBook;
  const shelfBook = book as Partial<ShelfBook>;

  const isBackendBook =
    "currentStatus" in backendBook ||
    "primaryFormat" in backendBook ||
    "authors" in backendBook ||
    "genres" in backendBook;

  return {
    id: stringFromUnknown(book.id, crypto.randomUUID()),
    title: stringFromUnknown(book.title),
    author: isBackendBook
      ? authorNamesFromList(backendBook.authors)
      : shelfBook.author || "",
    genre: isBackendBook
      ? genreNamesFromList(backendBook.genres)
      : shelfBook.genre || "",
    seriesName:
      stringFromUnknown(backendBook.seriesName) || shelfBook.seriesName || "",
    seriesNumber:
      stringFromUnknown(backendBook.seriesNumber) || shelfBook.seriesNumber,
    subtitle: stringFromUnknown(backendBook.subtitle) || shelfBook.subtitle,
    description:
      stringFromUnknown(backendBook.description) || shelfBook.description,
    publisher: stringFromUnknown(backendBook.publisher) || shelfBook.publisher,
    publisherOther: shelfBook.publisherOther,
    editionFormat:
      stringFromUnknown(backendBook.editionFormat) || shelfBook.editionFormat,
    narrator: stringFromUnknown(backendBook.narrator) || shelfBook.narrator,
    language: stringFromUnknown(backendBook.language) || shelfBook.language,
    isbn10: stringFromUnknown(backendBook.isbn10) || shelfBook.isbn10,
    isbn13: stringFromUnknown(backendBook.isbn13) || shelfBook.isbn13,
    catalogProvider:
      stringFromUnknown(backendBook.catalogProvider) ||
      shelfBook.catalogProvider,
    catalogProviderId:
      stringFromUnknown(backendBook.catalogProviderId) ||
      shelfBook.catalogProviderId,
    readingStatus: isBackendBook
      ? frontendStatusFromBackend(backendBook.currentStatus, backendBook.dnf)
      : shelfBook.readingStatus || "TO_READ",
    format: isBackendBook
      ? frontendFormatFromBackend(backendBook.primaryFormat)
      : shelfBook.format || "PHYSICAL",
    ownedStatus: backendBook.owned ? "OWNED" : shelfBook.ownedStatus || "",
    source: shelfBook.source || "",
    atmosphere: shelfBook.atmosphere || "",
    romancePresence: shelfBook.romancePresence || "NO",
    romanceImportance: shelfBook.romanceImportance,
    isSeries:
      stringFromUnknown(backendBook.seriesName || shelfBook.seriesName).trim()
        .length > 0
        ? "YES"
        : shelfBook.isSeries || "NO",
    favorite: backendBook.favorite ? "YES" : shelfBook.favorite || "NO",
    comfortRead: shelfBook.comfortRead || "NO",
    color: shelfBook.color || mockColors[0],
    coverUrl:
      stringFromUnknown(backendBook.coverImageUrl) || shelfBook.coverUrl,
    spineUrl: shelfBook.spineUrl,
    addedAt:
      stringFromUnknown(backendBook.dateAdded) ||
      stringFromUnknown(backendBook.createdAt) ||
      shelfBook.addedAt ||
      new Date().toISOString(),
    startDate: shelfBook.startDate,
    finishDate: shelfBook.finishDate,
    publicationYear:
      publicationYearFromDate(backendBook.publicationDate || null) ||
      shelfBook.publicationYear,
    pageCount: stringFromUnknown(backendBook.pageCount) || shelfBook.pageCount,
    audioLength:
      durationFromSeconds(backendBook.audiobookLengthSeconds) ||
      shelfBook.audioLength,
    finalRating: shelfBook.finalRating,
    rereadRating: shelfBook.rereadRating,
    emotionalDevastationRating: shelfBook.emotionalDevastationRating,
    romanceRating: shelfBook.romanceRating,
    spiceRating: shelfBook.spiceRating,
    horrorRating: shelfBook.horrorRating,
    readingExperiences: Array.isArray(backendBook.readingExperiences)
      ? backendBook.readingExperiences.map(migrateReadingExperience)
      : Array.isArray(shelfBook.readingExperiences)
        ? shelfBook.readingExperiences.map(migrateReadingExperience)
        : [],
  };
}

function loadSavedBooks(): ShelfBook[] {
  const savedBooks = localStorage.getItem(LIBRARY_LANE_BOOKS_KEY);

  if (!savedBooks) return initialBooks;

  try {
    const parsedBooks = JSON.parse(savedBooks);

    if (!Array.isArray(parsedBooks)) return initialBooks;

    return parsedBooks.map(migrateBook);
  } catch {
    return initialBooks;
  }
}

function chunkBooksIntoBookcases(books: ShelfBook[]) {
  const pages: ShelfBook[][] = [];

  for (let i = 0; i < books.length; i += BOOKS_PER_BOOKCASE) {
    pages.push(books.slice(i, i + BOOKS_PER_BOOKCASE));
  }

  return pages.length > 0 ? pages : [[]];
}

function buildShelves(books: ShelfBook[], zones: ShelfZone[]) {
  let currentIndex = 0;

  return zones.map((zone) => {
    const booksForZone = books.slice(
      currentIndex,
      currentIndex + zone.capacity,
    );
    currentIndex += zone.capacity;

    return {
      ...zone,
      books: booksForZone,
    };
  });
}

function ratingValue(value?: string) {
  return Number(value || "0");
}

function numberValue(value?: string) {
  const parsed = Number(value || "0");
  return Number.isNaN(parsed) ? 0 : parsed;
}

function makeShelfBookFromNewBook(newBook: NewBook, color: string): ShelfBook {
  return {
    id: crypto.randomUUID(),
    title: newBook.title.trim(),
    author: newBook.author?.trim() || "",
    genre: newBook.genre?.trim() || "",
    seriesName: newBook.seriesName?.trim() || "",
    seriesNumber: newBook.seriesNumber,
    subtitle: newBook.subtitle,
    description: newBook.description,
    publisher: newBook.publisher,
    publisherOther: newBook.publisherOther,
    editionFormat: newBook.editionFormat,
    narrator: newBook.narrator,
    language: newBook.language,
    isbn10: newBook.isbn10,
    isbn13: newBook.isbn13,
    catalogProvider: newBook.catalogProvider,
    catalogProviderId: newBook.catalogProviderId,
    readingStatus: newBook.readingStatus || "TO_READ",
    format: newBook.format || "PHYSICAL",
    ownedStatus: newBook.ownedStatus || "",
    source: newBook.source || "",
    atmosphere: newBook.atmosphere || "",
    romancePresence: newBook.romancePresence || "NO",
    romanceImportance: newBook.romanceImportance,
    isSeries: newBook.isSeries || "NO",
    favorite: newBook.favorite || "NO",
    comfortRead: newBook.comfortRead || "NO",
    color,
    coverUrl: newBook.coverUrl?.trim() || undefined,
    addedAt: new Date().toISOString(),
    startDate: newBook.startDate,
    finishDate: newBook.finishDate,
    publicationYear: newBook.publicationYear,
    pageCount: newBook.pageCount,
    audioLength: newBook.audioLength,
    finalRating: newBook.finalRating,
    rereadRating: newBook.rereadRating,
    emotionalDevastationRating: newBook.emotionalDevastationRating,
    romanceRating: newBook.romanceRating,
    spiceRating: newBook.spiceRating,
    horrorRating: newBook.horrorRating,
    readingExperiences: [
      {
        id: crypto.randomUUID(),
        label: "Original Reading Experience",
        ...newBook,
        title: newBook.title.trim(),
        author: newBook.author?.trim() || "",
        genre: newBook.genre?.trim() || "",
        seriesName: newBook.seriesName?.trim() || "",
        format: newBook.format || "PHYSICAL",
        readingStatus: newBook.readingStatus || "TO_READ",
        createdAt: new Date().toISOString(),
      },
    ],
  };
}

function mapShelfBookToBackendBook(book: ShelfBook): BackendBook {
  return {
    id: book.id ? Number(book.id) : undefined,

    title: book.title,
    subtitle: book.subtitle || null,
    description: book.description || null,

    isbn10: book.isbn10 || null,
    isbn13: book.isbn13 || null,

    publisher:
      book.publisher === "OTHER"
        ? book.publisherOther || null
        : book.publisher || null,
    publicationDate: yearToPublicationDate(book.publicationYear),

    pageCount: numberOrNull(book.pageCount),
    audiobookLengthSeconds: durationToSeconds(book.audioLength),

    coverImageUrl: book.coverUrl || null,
    language: book.language || null,

    seriesName: book.seriesName || null,
    seriesNumber: numberOrNull(book.seriesNumber),
    editionFormat: book.editionFormat || null,
    narrator: book.narrator || null,
    catalogProvider: book.catalogProvider || null,
    catalogProviderId: book.catalogProviderId || null,

    personalNotes: null,

    favorite: boolFromYesNo(book.favorite),
    owned: book.ownedStatus === "OWNED",
    wishlist: false,
    dnf: book.readingStatus === "DNF",

    primaryFormat: backendFormatFromFrontend(book.format),
    currentStatus: backendStatusFromFrontend(book.readingStatus),

    authors: splitList(book.author).map((authorName) => ({
      penName: authorName,
      firstName: authorName.split(" ")[0],
      lastName: authorName.split(" ").slice(1).join(" ") || "",
    })),

    genres: splitList(book.genre).map((genreName) => ({ name: genreName })),

    // Don't overwrite these from the frontend
    library: undefined,
    readingExperiences: undefined,
  };
}

function mapReadingExperienceToBackend(
  experience: ReadingExperience,
  bookId: number,
) {
  return {
    label: experience.label || "Reading Experience",

    format: backendFormatFromFrontend(experience.format || "PHYSICAL"),
    status: backendStatusFromFrontend(experience.readingStatus || "TO_READ"),

    startDate: experience.startDate || null,
    finishDate: experience.finishDate || null,

    currentPage: numberOrNull(experience.currentPage),
    totalPages: numberOrNull(experience.pageCount),

    currentListeningSeconds: numberOrNull(experience.currentListeningPosition),
    totalListeningSeconds: durationToSeconds(experience.audioLength),
    listeningSpeed: numberOrNull(experience.listeningSpeed),

    predictedRating: experience.predictedRating || null,
    currentRating: experience.currentRating || null,
    initialRating: experience.initialRating || null,
    finalRating: experience.finalRating || null,
    rereadRating: experience.rereadRating || null,
    emotionalDevastationRating: experience.emotionalDevastationRating || null,

    excitementRating: experience.excitementRating || null,
    currentExcitementRating: experience.currentExcitementRating || null,
    excitementWhileReading: experience.excitementWhileReading || null,

    romancePresence: experience.romancePresence || null,
    romanceImportance: experience.romanceImportance || null,
    romanceRating: experience.romanceRating || null,
    spiceRating: experience.spiceRating || null,
    horrorRating: experience.horrorRating || null,

    romanceNotes: experience.romanceNotes || null,
    reviewText: experience.notes || null,
    dnfReason: experience.dnfReason || null,
    promptResponsesJson: experience.promptResponsesJson || null,

    currentExperience: experience.readingStatus === "READING",

    book: {
      id: bookId,
    },
  };
}

export default function BooksPage({
  currentPage,
  setCurrentPage,
  openAddBookOnLoad = false,
  onAddBookOpened,
}: BooksPageProps) {
  const [books, setBooks] = useState<ShelfBook[]>([]);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [addBookOpen, setAddBookOpen] = useState(false);
  const [selectedBook, setSelectedBook] = useState<ShelfBook | null>(null);
  const [currentBookcasePage, setCurrentBookcasePage] = useState(0);

  const [searchTerm, setSearchTerm] = useState("");
  const [sortBy, setSortBy] = useState("added-new");
  const [filtersOpen, setFiltersOpen] = useState(true);
  const [statusFilters, setStatusFilters] = useState<string[]>([]);
  const [formatFilters, setFormatFilters] = useState<string[]>([]);
  const [romanceFilters, setRomanceFilters] = useState<string[]>([]);
  const [spiceFilters, setSpiceFilters] = useState<string[]>([]);
  const [seriesFilters, setSeriesFilters] = useState<string[]>([]);
  const [specialFilters, setSpecialFilters] = useState<string[]>([]);
  const [genreFilters, setGenreFilters] = useState<string[]>([]);
  const [spinePalettes, setSpinePalettes] = useState<
    Record<string, SpinePalette>
  >({});
  const [bookMotion, setBookMotion] = useState<BookMotion | null>(null);
  const [pendingArrivalId, setPendingArrivalId] = useState<string | null>(null);
  const [hiddenShelfBookId, setHiddenShelfBookId] = useState<string | null>(
    null,
  );
  const shelfBookRefs = useRef(new Map<string, HTMLButtonElement>());
  const motionSequence = useRef(0);
  const bookMotionRef = useRef<BookMotion | null>(null);

  useEffect(() => {
    bookMotionRef.current = bookMotion;
  }, [bookMotion]);

  useEffect(() => {
    async function loadBooksFromApi() {
      try {
        const apiBooks = await bookApi.getAll();

        const booksWithExperiences = await Promise.all(
          (apiBooks as BackendBook[]).map(async (backendBook) => {
            const migratedBook = migrateBook(backendBook);
            const backendBookId = Number(migratedBook.id);

            if (!Number.isFinite(backendBookId) || backendBookId <= 0) {
              return migratedBook;
            }

            try {
              const experiences =
                await readingExperienceApi.getByBook(backendBookId);

              return {
                ...migratedBook,
                readingExperiences: Array.isArray(experiences)
                  ? (experiences as BackendReadingExperience[]).map(
                      migrateReadingExperience,
                    )
                  : [],
              };
            } catch (error) {
              console.warn(
                "Failed to load reading experiences for book",
                migratedBook.id,
                error,
              );

              return {
                ...migratedBook,
                // Do not make an existing history look erased because one
                // request temporarily failed.
                readingExperiences: migratedBook.readingExperiences ?? [],
              };
            }
          }),
        );

        setBooks(booksWithExperiences);

        // Recover missing covers and replace scan-oriented catalog art with a
        // clean, front-facing exact-title/author physical cover. Metadata for
        // the reader's selected edition remains untouched.
        for (const book of booksWithExperiences) {
          if (!coverNeedsRefinement(book)) continue;

          const backendBookId = Number(book.id);
          if (!Number.isFinite(backendBookId) || backendBookId <= 0) continue;

          try {
            const results = await catalogApi.searchBooks(
              book.title,
              undefined,
              "TITLE",
            );
            const match = book.coverUrl
              ? bestDisplayCoverMatch(book, results)
              : bestCoverMatch(book, results);
            if (!match?.coverImageUrl) continue;
            if (match.coverImageUrl === book.coverUrl) continue;

            const recoveredBook = {
              ...book,
              coverUrl: match.coverImageUrl,
            };

            await bookApi.update(
              backendBookId,
              mapShelfBookToBackendBook(recoveredBook),
            );

            setBooks((currentBooks) =>
              currentBooks.map((currentBook) =>
                currentBook.id === book.id ? recoveredBook : currentBook,
              ),
            );
          } catch (error) {
            console.warn("Could not recover cover for", book.title, error);
          }
        }
      } catch (error) {
        console.error("Failed to load books from API", error);
        setBooks([]);
      }
    }

    loadBooksFromApi();
  }, []);

  useEffect(() => {
    let cancelled = false;
    const missingPalettes = books.filter(
      (book) => book.coverUrl && !spinePalettes[book.id],
    );
    if (missingPalettes.length === 0) return;

    Promise.all(
      missingPalettes.map(async (book) => ({
        id: book.id,
        palette: await coverPalette(book.coverUrl!),
      })),
    ).then((resolved) => {
      if (cancelled) return;
      setSpinePalettes((current) => {
        const next = { ...current };
        let changed = false;

        resolved.forEach(({ id, palette }) => {
          if (palette && current[id] !== palette) {
            next[id] = palette;
            changed = true;
          }
        });

        return changed ? next : current;
      });
    });

    return () => {
      cancelled = true;
    };
  }, [books, spinePalettes]);

  useEffect(() => {
    if (!openAddBookOnLoad) return;

    const frame = window.requestAnimationFrame(() => {
      window.scrollTo({ top: 0, behavior: "auto" });
      setAddBookOpen(true);
      onAddBookOpened?.();
    });

    return () => window.cancelAnimationFrame(frame);
  }, [openAddBookOnLoad, onAddBookOpened]);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      setCurrentBookcasePage(0);
    });

    return () => window.cancelAnimationFrame(frame);
  }, [
    searchTerm,
    sortBy,
    statusFilters,
    formatFilters,
    romanceFilters,
    spiceFilters,
    seriesFilters,
    specialFilters,
    genreFilters,
  ]);

  useEffect(() => {
    if (!pendingArrivalId || bookMotionRef.current) return;
    const arrivalId = pendingArrivalId;
    const arrivingBook = books.find((book) => book.id === arrivalId);
    if (!arrivingBook) return;
    const arrivalBook = arrivingBook;

    let cancelled = false;
    const sequence = ++motionSequence.current;

    async function animateArrival() {
      // Let React place the permanent destination spine before measuring it.
      await new Promise<void>((resolve) =>
        window.requestAnimationFrame(() =>
          window.requestAnimationFrame(() => resolve()),
        ),
      );
      if (cancelled || sequence !== motionSequence.current) return;

      const destination = shelfBookRefs.current.get(arrivalId);
      if (!destination || prefersReducedMotion()) {
        setPendingArrivalId(null);
        return;
      }

      setHiddenShelfBookId(arrivalId);
      setBookMotion({
        kind: "arrival",
        stage: "preview",
        book: arrivalBook,
        shelfRect: motionRect(destination),
      });
      await waitForMotion(480);
      if (cancelled || sequence !== motionSequence.current) return;

      setBookMotion((current) =>
        current ? { ...current, stage: "travel" } : current,
      );
      await waitForMotion(780);
      if (cancelled || sequence !== motionSequence.current) return;

      setBookMotion(null);
      setHiddenShelfBookId(null);
      setPendingArrivalId(null);
      destination.focus({ preventScroll: true });
    }

    void animateArrival();
    return () => {
      cancelled = true;
    };
  }, [pendingArrivalId, books]);

  const genreOptions = useMemo(() => {
    return Array.from(
      new Set(books.flatMap((book) => canonicalGenres(book.genre))),
    ).sort((a, b) => a.localeCompare(b));
  }, [books]);

  const filteredAndSortedBooks = useMemo(() => {
    const searchTokens = normalizeBookText(searchTerm)
      .split(/\s+/)
      .filter(Boolean);

    const filtered = books.filter((book) => {
      const searchableText = normalizeBookText(
        [
          book.title,
          book.subtitle,
          book.author,
          canonicalGenres(book.genre).join(" "),
          book.seriesName,
          book.publisher,
          book.publisherOther,
          book.narrator,
          book.description,
        ]
          .filter(Boolean)
          .join(" "),
      );
      const bookShelves = [
        book.favorite === "YES" ? "FAVORITES" : "",
        ratingValue(book.finalRating) >= 5 ? "FIVE_STAR" : "",
        ratingValue(book.rereadRating) >= 4 ? "REREAD_WORTHY" : "",
        book.comfortRead === "YES" ? "COMFORT_READS" : "",
        ratingValue(book.emotionalDevastationRating) >= 4
          ? "EMOTIONAL_READS"
          : "",
      ].filter(Boolean);

      return (
        searchTokens.every((token) => searchableText.includes(token)) &&
        matchesAny(statusFilters, [book.readingStatus]) &&
        matchesAny(formatFilters, [book.format]) &&
        matchesAny(romanceFilters, [book.romancePresence || "NO"]) &&
        matchesAny(spiceFilters, [spiceBucket(book.spiceRating)]) &&
        matchesAny(seriesFilters, [
          book.isSeries === "YES" ? "SERIES" : "STANDALONE",
        ]) &&
        matchesAny(genreFilters, canonicalGenres(book.genre)) &&
        matchesAny(specialFilters, bookShelves)
      );
    });

    return [...filtered].sort((a, b) => {
      switch (sortBy) {
        case "added-old":
          return new Date(a.addedAt).getTime() - new Date(b.addedAt).getTime();
        case "title-az":
          return a.title.localeCompare(b.title);
        case "title-za":
          return b.title.localeCompare(a.title);
        case "author-az":
          return a.author.localeCompare(b.author);
        case "author-za":
          return b.author.localeCompare(a.author);
        case "series-order":
          return (
            (a.seriesName || a.title).localeCompare(b.seriesName || b.title) ||
            numberValue(a.seriesNumber) - numberValue(b.seriesNumber) ||
            a.title.localeCompare(b.title)
          );
        case "status":
          return a.readingStatus.localeCompare(b.readingStatus);
        case "rating-high":
          return ratingValue(b.finalRating) - ratingValue(a.finalRating);
        case "rating-low":
          return ratingValue(a.finalRating) - ratingValue(b.finalRating);
        case "emotional-high":
          return (
            ratingValue(b.emotionalDevastationRating) -
            ratingValue(a.emotionalDevastationRating)
          );
        case "romance-high":
          return ratingValue(b.romanceRating) - ratingValue(a.romanceRating);
        case "spice-high":
          return ratingValue(b.spiceRating) - ratingValue(a.spiceRating);
        case "horror-high":
          return ratingValue(b.horrorRating) - ratingValue(a.horrorRating);
        case "reread-high":
          return ratingValue(b.rereadRating) - ratingValue(a.rereadRating);
        case "pages-high":
          return numberValue(b.pageCount) - numberValue(a.pageCount);
        case "pages-low":
          return numberValue(a.pageCount) - numberValue(b.pageCount);
        case "publication-new":
          return (
            numberValue(b.publicationYear) - numberValue(a.publicationYear)
          );
        case "publication-old":
          return (
            numberValue(a.publicationYear) - numberValue(b.publicationYear)
          );
        case "recent-read":
          return (
            new Date(b.finishDate || "").getTime() -
            new Date(a.finishDate || "").getTime()
          );
        case "oldest-read":
          return (
            new Date(a.finishDate || "").getTime() -
            new Date(b.finishDate || "").getTime()
          );
        case "added-new":
        default:
          return new Date(b.addedAt).getTime() - new Date(a.addedAt).getTime();
      }
    });
  }, [
    books,
    searchTerm,
    sortBy,
    statusFilters,
    formatFilters,
    romanceFilters,
    spiceFilters,
    seriesFilters,
    specialFilters,
    genreFilters,
  ]);

  const filterGroups = [
    { label: "Status", placeholder: "All statuses", values: statusFilters, setValues: setStatusFilters, options: statusOptions },
    { label: "Format", placeholder: "All formats", values: formatFilters, setValues: setFormatFilters, options: formatOptions },
    { label: "Genre", placeholder: "All genres", values: genreFilters, setValues: setGenreFilters, options: genreOptions.map((genre) => ({ value: genre, label: genre })) },
    { label: "Series", placeholder: "Series + standalones", values: seriesFilters, setValues: setSeriesFilters, options: seriesOptions },
    { label: "Romance", placeholder: "All romance levels", values: romanceFilters, setValues: setRomanceFilters, options: romanceOptions },
    { label: "Spice", placeholder: "All spice levels", values: spiceFilters, setValues: setSpiceFilters, options: spiceOptions },
    { label: "Shelf", placeholder: "All shelves", values: specialFilters, setValues: setSpecialFilters, options: shelfOptions },
  ];
  const activeFilterCount = filterGroups.reduce(
    (total, group) => total + group.values.length,
    0,
  );

  function clearAllFilters() {
    filterGroups.forEach((group) => group.setValues([]));
    setSearchTerm("");
  }

  const spineAssignments = useMemo(
    () => assignUniqueSpines(books, spinePalettes),
    [books, spinePalettes],
  );

  const bookcasePages = chunkBooksIntoBookcases(filteredAndSortedBooks);
  const currentBooksForBookcase = bookcasePages[currentBookcasePage] ?? [];
  const shelves = buildShelves(currentBooksForBookcase, shelfZones);

  async function handleShelfBookOpen(
    book: ShelfBook,
    element: HTMLButtonElement,
  ) {
    if (bookMotion) return;
    if (prefersReducedMotion()) {
      setSelectedBook(book);
      return;
    }

    const sequence = ++motionSequence.current;
    setHiddenShelfBookId(book.id);
    setBookMotion({
      kind: "open",
      stage: "lift",
      book,
      shelfRect: motionRect(element),
    });
    await waitForMotion(240);
    if (sequence !== motionSequence.current) return;

    setBookMotion((current) =>
      current ? { ...current, stage: "reveal" } : current,
    );
    await waitForMotion(900);
    if (sequence !== motionSequence.current) return;

    setBookMotion((current) =>
      current ? { ...current, stage: "opening" } : current,
    );
    await waitForMotion(800);
    if (sequence !== motionSequence.current) return;

    setSelectedBook(book);
    setBookMotion((current) =>
      current ? { ...current, stage: "handoff" } : current,
    );
    await waitForMotion(280);
    if (sequence !== motionSequence.current) return;

    setBookMotion(null);
    setHiddenShelfBookId(null);
  }

  async function handleBookDetailsClose() {
    const closingBook = selectedBook;
    if (!closingBook) return;

    const destination = shelfBookRefs.current.get(closingBook.id);
    if (!destination || prefersReducedMotion()) {
      setSelectedBook(null);
      return;
    }

    const sequence = ++motionSequence.current;
    setHiddenShelfBookId(closingBook.id);
    setBookMotion({
      kind: "close",
      stage: "closing",
      book: closingBook,
      shelfRect: motionRect(destination),
    });
    // Paint the matching open-book proxy over the ledger before removing the
    // interactive modal. This prevents a blank or single-cover flash.
    await new Promise<void>((resolve) =>
      window.requestAnimationFrame(() =>
        window.requestAnimationFrame(() => resolve()),
      ),
    );
    if (sequence !== motionSequence.current) return;
    setSelectedBook(null);
    await waitForMotion(720);
    if (sequence !== motionSequence.current) return;

    setBookMotion((current) =>
      current ? { ...current, stage: "covering" } : current,
    );
    // Pages finish closing before the outer cover is revealed. This prevents
    // the cover from floating in front of a still-open ledger.
    await waitForMotion(800);
    if (sequence !== motionSequence.current) return;

    setBookMotion((current) =>
      current ? { ...current, stage: "preview" } : current,
    );
    await waitForMotion(420);
    if (sequence !== motionSequence.current) return;

    setBookMotion((current) =>
      current ? { ...current, stage: "return" } : current,
    );
    await waitForMotion(780);
    if (sequence !== motionSequence.current) return;

    setBookMotion(null);
    setHiddenShelfBookId(null);
    destination.focus({ preventScroll: true });
  }

  async function handleAddBook(newBook: NewBook) {
    const normalizedNewTitle = normalizeBookText(newBook.title);

    if (!normalizedNewTitle) return;

    const alreadyExists = books.some((book) => {
      if (normalizeBookText(book.title) !== normalizedNewTitle) return false;

      const incomingIsbn = newBook.isbn13?.trim() || newBook.isbn10?.trim();
      const existingIsbn = book.isbn13?.trim() || book.isbn10?.trim();

      if (incomingIsbn && existingIsbn) return incomingIsbn === existingIsbn;

      return (
        book.format === (newBook.format || "PHYSICAL") &&
        normalizeBookText(book.author) ===
          normalizeBookText(newBook.author || "")
      );
    });

    if (alreadyExists) {
      alert("This exact edition already exists in your library.");
      return;
    }

    const nextBookColor = mockColors[books.length % mockColors.length];
    const shelfBook = makeShelfBookFromNewBook(newBook, nextBookColor);

    try {
      const savedBackendBook = await bookApi.create(
        mapShelfBookToBackendBook(shelfBook),
      );

      const savedBookId = Number((savedBackendBook as BackendBook).id);

      if (!Number.isFinite(savedBookId) || savedBookId <= 0) {
        throw new Error("Backend did not return a valid book id.");
      }

      const savedExperiences = await Promise.all(
        shelfBook.readingExperiences.map(async (experience) => {
          const savedExperience = await readingExperienceApi.create(
            mapReadingExperienceToBackend(experience, savedBookId),
          );

          return migrateReadingExperience(
            savedExperience as BackendReadingExperience,
          );
        }),
      );

      const savedBook: ShelfBook = {
        ...migrateBook(savedBackendBook as BackendBook),
        // Preserve frontend-only fields that are not returned by the backend.
        ...shelfBook,
        id: String(savedBookId),
        readingExperiences: savedExperiences,
      };

      setBooks((prev) => {
        const updatedBooks = [...prev, savedBook];
        return updatedBooks;
      });

      // The arrival moment must always have a real, visible shelf destination.
      // A freshly added story therefore returns to the newest-first library
      // view rather than attempting to land behind an active filter.
      setSearchTerm("");
      setSortBy("added-new");
      setStatusFilters([]);
      setFormatFilters([]);
      setRomanceFilters([]);
      setSpiceFilters([]);
      setSeriesFilters([]);
      setSpecialFilters([]);
      setGenreFilters([]);
      setCurrentBookcasePage(0);
      setAddBookOpen(false);
      setPendingArrivalId(String(savedBookId));
    } catch (error) {
      console.error("Failed to create book", error);
      alert("The book could not be saved to the database.");
    }
  }

  function handleReadingExperiencesChange(
    bookId: string,
    readingExperiences: ReadingExperience[],
  ) {
    setBooks((previousBooks) =>
      previousBooks.map((book) =>
        book.id === bookId ? { ...book, readingExperiences } : book,
      ),
    );

    setSelectedBook((previousBook) =>
      previousBook?.id === bookId
        ? { ...previousBook, readingExperiences }
        : previousBook,
    );
  }

  async function handleSaveBook(updatedBook: ShelfBook) {
    try {
      const backendBookId = Number(updatedBook.id);

      if (!Number.isFinite(backendBookId) || backendBookId <= 0) {
        alert("This book does not have a valid database ID yet.");
        return;
      }

      const savedBackendBook = await bookApi.update(
        backendBookId,
        mapShelfBookToBackendBook(updatedBook),
      );

      const migratedBackendBook = migrateBook(savedBackendBook as BackendBook);
      const savedBook: ShelfBook = {
        ...migratedBackendBook,
        // Keep frontend-only display and reading-history fields intact.
        ...updatedBook,
        id: String(backendBookId),
        readingExperiences: updatedBook.readingExperiences,
      };

      setBooks((previousBooks) =>
        previousBooks.map((book) =>
          book.id === String(backendBookId) ? savedBook : book,
        ),
      );
      setSelectedBook(savedBook);
    } catch (error) {
      console.error("Failed to update book", error);
      alert("The book changes could not be saved to the database.");
    }
  }

  async function handleDeleteBook(bookId: string) {
    const numericBookId = Number(bookId);
    const hasValidBackendId =
      Number.isFinite(numericBookId) && numericBookId > 0;

    try {
      if (hasValidBackendId) {
        await bookApi.delete(numericBookId);
      }
    } catch (error) {
      console.error("Failed to delete book", error);
      alert("The book could not be deleted from the database.");
      return;
    }

    setBooks((previousBooks) => {
      const updatedBooks = previousBooks.filter((book) => book.id !== bookId);
      const maxPage = Math.max(
        0,
        Math.ceil(updatedBooks.length / BOOKS_PER_BOOKCASE) - 1,
      );

      setCurrentBookcasePage((page) => Math.min(page, maxPage));
      return updatedBooks;
    });

    setSelectedBook(null);
  }

  return (
    <div className="books-layout">
      <button
        className="books-menu-button"
        onClick={() => setSidebarOpen(true)}
        aria-label="Open menu"
      >
        ☰
      </button>

      {sidebarOpen && (
        <div
          className="books-sidebar-backdrop"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <div className={`books-sidebar-drawer ${sidebarOpen ? "open" : ""}`}>
        <FantasySidebar
          currentPage={currentPage}
          setCurrentPage={(page) => {
            setCurrentPage(page);
            setSidebarOpen(false);
          }}
        />
      </div>

      <main className="books-page">
        <section
          className="books-gallery-header"
          style={{ backgroundImage: `url(${booksHeaderWall})` }}
        >
          <section className="books-page-header">
            <div>
              <p>My Complete Library</p>
              <h1>Books</h1>
              <span>Every story I have added to Library Lane.</span>
            </div>

            <button
              className="books-add-button"
              onClick={() => setAddBookOpen(true)}
              style={{ backgroundImage: `url(${booksAddButtonFrame})` }}
            >
              + Add Book
            </button>
          </section>

          <section className="books-toolbar">
            <div
              className="books-search-frame"
              style={{ backgroundImage: `url(${booksSearchSortFrame})` }}
            >
              <span className="books-search-icon" aria-hidden="true">
                ⌕
              </span>
              <input
                placeholder="Search title, author, series, genre, publisher, narrator..."
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                aria-label="Search books"
              />
              {searchTerm && (
                <button
                  type="button"
                  className="books-search-clear"
                  onClick={() => setSearchTerm("")}
                  aria-label="Clear search"
                >
                  ×
                </button>
              )}
            </div>

            <div
              className="books-sort-frame"
              style={{ backgroundImage: `url(${booksSearchSortFrame})` }}
            >
              <LibrarySelect
                className="books-sort-select"
                ariaLabel="Sort books"
                value={sortBy}
                options={sortOptions}
                onChange={setSortBy}
              />
            </div>

            <button
              type="button"
              className={`books-filter-toggle ${filtersOpen ? "active" : ""}`}
              style={{ backgroundImage: `url(${booksFilterFrame})` }}
              onClick={() => setFiltersOpen((current) => !current)}
              aria-expanded={filtersOpen}
              aria-controls="books-filter-panel"
            >
              Filters
              {activeFilterCount > 0 && (
                <span>{activeFilterCount}</span>
              )}
              <b className="books-filter-chevron" aria-hidden="true">
                {filtersOpen ? "⌃" : "⌄"}
              </b>
            </button>
          </section>

          <div className="books-toolbar-meta" aria-live="polite">
            {activeFilterCount > 0 || searchTerm ? (
              <>
                Showing <strong>{filteredAndSortedBooks.length}</strong> of{" "}
                <strong>{books.length}</strong> books
              </>
            ) : (
              <>
                <strong>{books.length}</strong>{" "}
                {books.length === 1 ? "book" : "books"}
              </>
            )}
          </div>

          {filtersOpen && (
            <section
              id="books-filter-panel"
              className="books-filter-panel"
              style={
                {
                  "--books-filter-frame": `url(${booksFilterFrame})`,
                } as CSSProperties
              }
            >
              <p className="books-filter-instructions">
                Choose multiple options within a filter. Different filters work
                together.
              </p>

              <div className="books-filter-bar">
                {filterGroups.map((group) => (
                  <LibrarySelect
                    key={group.label}
                    multiple
                    ariaLabel={`Filter by ${group.label}`}
                    placeholder={group.placeholder}
                    values={group.values}
                    options={group.options}
                    searchable={group.label === "Genre"}
                    onValuesChange={group.setValues}
                  />
                ))}
              </div>

              {(activeFilterCount > 0 || searchTerm) && (
              <div className="books-filter-summary" aria-live="polite">
                {activeFilterCount > 0 && (
                  <div className="books-active-filters">
                    {filterGroups.flatMap((group) =>
                      group.values.map((value) => {
                        const option = group.options.find(
                          (candidate) => candidate.value === value,
                        );
                        return (
                          <button
                            type="button"
                            key={`${group.label}-${value}`}
                            onClick={() =>
                              group.setValues(
                                group.values.filter(
                                  (selected) => selected !== value,
                                ),
                              )
                            }
                            aria-label={`Remove ${option?.label || value} filter`}
                          >
                            <small>{group.label}</small>
                            {option?.label || value}
                            <span aria-hidden="true">×</span>
                          </button>
                        );
                      }),
                    )}
                  </div>
                )}
                {(activeFilterCount > 0 || searchTerm) && (
                  <button
                    type="button"
                    className="books-clear-all"
                    onClick={clearAllFilters}
                  >
                    Clear all
                  </button>
                )}
              </div>
              )}
            </section>
          )}

          <img
            className="books-balcony-transition"
            src={booksBalconyTransition}
            alt=""
            aria-hidden="true"
          />
        </section>

        <section className="bookcase-view">
          <section
            className="bookshelf-page-background"
            style={{ backgroundImage: `url(${bookshelfBackground})` }}
          >
            {shelves.map((shelf) => (
              <div
                className={`bookshelf-shelf-zone ${shelf.className} ${
                  shelf.books.length < shelf.capacity
                    ? "shelf-zone-partial"
                    : ""
                }`}
                key={`${currentBookcasePage + 1}-${shelf.className}`}
              >
                {shelf.books.map((book) => (
                  <button
                    className={`shelf-book shelf-book-illustrated spine-variant-${spineVariantForBook(
                      book,
                    )} spine-binding-${spineBindingForBook(
                      book,
                    )} spine-type-${spineTypographyForBook(
                      book,
                    )} spine-title-${spineTitleLayoutForBook(book)} ${
                      hiddenShelfBookId === book.id
                        ? "shelf-book-in-motion"
                        : ""
                    }`}
                    key={book.id}
                    ref={(element) => {
                      if (element) shelfBookRefs.current.set(book.id, element);
                      else shelfBookRefs.current.delete(book.id);
                    }}
                    style={spineStyleForBook(
                      book,
                      spinePalettes[book.id],
                      spineAssignments[book.id],
                    )}
                    title={book.title}
                    onClick={(event) =>
                      void handleShelfBookOpen(book, event.currentTarget)
                    }
                    disabled={bookMotion !== null}
                    aria-label={`Open ${book.title}${
                      book.author ? ` by ${book.author}` : ""
                    }`}
                  >
                    <span className="shelf-book-shell" aria-hidden="true">
                      <span className="shelf-book-edge" />
                      <span className="shelf-book-band shelf-book-band-top" />
                      <span className="shelf-book-band shelf-book-band-bottom" />
                      <span className="shelf-book-spine-title">
                        {spineTitleForBook(book)}
                      </span>
                      {book.author && (
                        <span className="shelf-book-spine-author">
                          {spineAuthorForBook(book)}
                        </span>
                      )}
                    </span>
                    <span className="shelf-book-tooltip" role="tooltip">
                      <strong>{book.title}</strong>
                      {book.author && <small>{book.author}</small>}
                    </span>
                  </button>
                ))}
              </div>
            ))}
          </section>

          {bookcasePages.length > 1 && (
            <div className="bookcase-pagination">
              <button
                disabled={currentBookcasePage === 0}
                onClick={() => setCurrentBookcasePage((page) => page - 1)}
              >
                ← Previous Shelf
              </button>

              <span>
                Bookcase {currentBookcasePage + 1} of {bookcasePages.length}
              </span>

              <button
                disabled={currentBookcasePage === bookcasePages.length - 1}
                onClick={() => setCurrentBookcasePage((page) => page + 1)}
              >
                Next Shelf →
              </button>
            </div>
          )}
        </section>

        <AddBookModal
          isOpen={addBookOpen}
          onClose={() => setAddBookOpen(false)}
          onSave={handleAddBook}
        />

        <BookDetailsModal
          isOpen={selectedBook !== null}
          book={selectedBook}
          onClose={() => void handleBookDetailsClose()}
          onSave={handleSaveBook}
          onReadingExperiencesChange={handleReadingExperiencesChange}
          onDelete={handleDeleteBook}
        />
        <BookMotionLayer motion={bookMotion} />
      </main>
    </div>
  );
}

function durationToSeconds(value?: string) {
  if (!value?.trim()) return null;
  if (/^\d+$/.test(value.trim())) return Number(value.trim());

  const hours = Number(value.match(/(\d+(?:\.\d+)?)\s*h/i)?.[1] || 0);
  const minutes = Number(value.match(/(\d+(?:\.\d+)?)\s*m/i)?.[1] || 0);
  const seconds = Number(value.match(/(\d+(?:\.\d+)?)\s*s/i)?.[1] || 0);
  const total = Math.round(hours * 3600 + minutes * 60 + seconds);
  return total > 0 ? total : null;
}

function durationFromSeconds(value?: number | null) {
  if (!value) return undefined;
  const hours = Math.floor(value / 3600);
  const minutes = Math.round((value % 3600) / 60);
  return [hours ? `${hours}h` : "", minutes ? `${minutes}m` : ""]
    .filter(Boolean)
    .join(" ");
}

function splitList(value?: string) {
  return (value || "")
    .split(/[,;|]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

// Retained for a future offline/local-storage fallback.
void loadSavedBooks;
