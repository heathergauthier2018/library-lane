import { useEffect, useMemo, useState } from "react";
import type { CSSProperties } from "react";
import type { AppPage } from "../App";
import FantasySidebar from "../dashboard/FantasySidebar";
import bookshelfBackground from "../assets/storybook/backgrounds/bookshelf-background.png";
import booksHeaderWall from "../assets/storybook/backgrounds/books-header-wall.png";
import booksBalconyTransition from "../assets/storybook/frames/books-balcony-transition.png";
import booksSearchSortFrame from "../assets/storybook/frames/books-search-sort-frame.png";
import booksFilterFrame from "../assets/storybook/frames/books-filter-frame.png";
import booksAddButtonFrame from "../assets/storybook/frames/books-add-button-frame.png";
import AddBookModal from "../components/books/AddBookModal";
import BookDetailsModal from "../components/books/BookDetailsModal";
import LibrarySelect from "../components/LibrarySelect";
import type { LibrarySelectOption } from "../components/LibrarySelect";
import type { NewBook } from "../components/books/AddBookModal";
import { bookApi, readingExperienceApi } from "../api/libraryLaneApi";

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
  { value: "ALL", label: "All statuses" },
  { value: "TO_READ", label: "Want To Read" },
  { value: "READING", label: "Currently Reading" },
  { value: "COMPLETED", label: "Completed" },
  { value: "DNF", label: "DNF" },
];

const formatOptions: LibrarySelectOption[] = [
  { value: "ALL", label: "All formats" },
  { value: "PHYSICAL", label: "Physical" },
  { value: "EBOOK", label: "E-Book" },
  { value: "AUDIOBOOK", label: "Audiobook" },
  { value: "MIXED", label: "Mixed" },
];

const ownershipOptions: LibrarySelectOption[] = [
  { value: "ALL", label: "All ownership" },
  { value: "OWNED", label: "Owned" },
  { value: "BORROWED", label: "Borrowed" },
  { value: "LIBRARY_COPY", label: "Library Copy" },
  { value: "KINDLE_UNLIMITED", label: "Kindle Unlimited" },
  { value: "AUDIBLE", label: "Audible" },
  { value: "GIFTED", label: "Gifted" },
];

const seriesOptions: LibrarySelectOption[] = [
  { value: "ALL", label: "Series + standalones" },
  { value: "SERIES", label: "Series only" },
  { value: "STANDALONE", label: "Standalones only" },
];

const romanceOptions: LibrarySelectOption[] = [
  { value: "ALL", label: "All romance levels" },
  { value: "NO", label: "No romance" },
  { value: "MINOR", label: "Minor romance" },
  { value: "SIDE", label: "Side romance" },
  { value: "SIGNIFICANT", label: "Significant romance" },
  { value: "PRIMARY", label: "Primary romance plot" },
];

const atmosphereOptions: LibrarySelectOption[] = [
  { value: "ALL", label: "All atmospheres" },
  { value: "COZY", label: "Cozy" },
  { value: "MAGICAL", label: "Magical" },
  { value: "ROMANTIC", label: "Romantic" },
  { value: "HAUNTING", label: "Haunting" },
  { value: "WHIMSICAL", label: "Whimsical" },
  { value: "DARK_ACADEMIA", label: "Dark Academia" },
  { value: "GOTHIC", label: "Gothic" },
  { value: "ADVENTURE", label: "Adventure" },
  { value: "HISTORICAL", label: "Historical" },
  { value: "MYSTERIOUS", label: "Mysterious" },
];

const shelfOptions: LibrarySelectOption[] = [
  { value: "ALL", label: "All shelves" },
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

function normalizeBookText(value: string) {
  return value.trim().toLowerCase().replace(/\s+/g, " ");
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
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [formatFilter, setFormatFilter] = useState("ALL");
  const [ownershipFilter, setOwnershipFilter] = useState("ALL");
  const [romanceFilter, setRomanceFilter] = useState("ALL");
  const [seriesFilter, setSeriesFilter] = useState("ALL");
  const [specialFilter, setSpecialFilter] = useState("ALL");
  const [atmosphereFilter, setAtmosphereFilter] = useState("ALL");
  const [genreFilter, setGenreFilter] = useState("ALL");

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
      } catch (error) {
        console.error("Failed to load books from API", error);
        setBooks([]);
      }
    }

    loadBooksFromApi();
  }, []);

  useEffect(() => {
    if (openAddBookOnLoad) {
      window.scrollTo({ top: 0, behavior: "auto" });
      setAddBookOpen(true);
      onAddBookOpened?.();
    }
  }, [openAddBookOnLoad, onAddBookOpened]);

  useEffect(() => {
    setCurrentBookcasePage(0);
  }, [
    searchTerm,
    sortBy,
    statusFilter,
    formatFilter,
    ownershipFilter,
    romanceFilter,
    seriesFilter,
    specialFilter,
    atmosphereFilter,
    genreFilter,
  ]);

  const genreOptions = useMemo(() => {
    const genres = new Set(
      books
        .map((book) => book.genre.trim())
        .filter((genre) => genre.length > 0),
    );

    return Array.from(genres).sort((a, b) => a.localeCompare(b));
  }, [books]);

  const filteredAndSortedBooks = useMemo(() => {
    const normalizedSearch = normalizeBookText(searchTerm);

    const filtered = books.filter((book) => {
      const searchableText = normalizeBookText(
        [book.title, book.author, book.genre, book.seriesName].join(" "),
      );

      const matchesSearch =
        !normalizedSearch || searchableText.includes(normalizedSearch);

      const matchesStatus =
        statusFilter === "ALL" || book.readingStatus === statusFilter;

      const matchesFormat =
        formatFilter === "ALL" || book.format === formatFilter;

      const matchesOwnership =
        ownershipFilter === "ALL" ||
        book.ownedStatus === ownershipFilter ||
        book.source === ownershipFilter;

      const matchesRomance =
        romanceFilter === "ALL" || book.romancePresence === romanceFilter;

      const matchesSeries =
        seriesFilter === "ALL" ||
        (seriesFilter === "SERIES" && book.isSeries === "YES") ||
        (seriesFilter === "STANDALONE" && book.isSeries !== "YES");

      const matchesAtmosphere =
        atmosphereFilter === "ALL" || book.atmosphere === atmosphereFilter;

      const matchesGenre =
        genreFilter === "ALL" ||
        normalizeBookText(book.genre).includes(normalizeBookText(genreFilter));

      const matchesSpecial =
        specialFilter === "ALL" ||
        (specialFilter === "FAVORITES" && book.favorite === "YES") ||
        (specialFilter === "FIVE_STAR" && ratingValue(book.finalRating) >= 5) ||
        (specialFilter === "REREAD_WORTHY" &&
          ratingValue(book.rereadRating) >= 4) ||
        (specialFilter === "COMFORT_READS" && book.comfortRead === "YES") ||
        (specialFilter === "EMOTIONAL_READS" &&
          ratingValue(book.emotionalDevastationRating) >= 4);

      return (
        matchesSearch &&
        matchesStatus &&
        matchesFormat &&
        matchesOwnership &&
        matchesRomance &&
        matchesSeries &&
        matchesAtmosphere &&
        matchesGenre &&
        matchesSpecial
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
    statusFilter,
    formatFilter,
    ownershipFilter,
    romanceFilter,
    seriesFilter,
    specialFilter,
    atmosphereFilter,
    genreFilter,
  ]);

  const bookcasePages = chunkBooksIntoBookcases(filteredAndSortedBooks);
  const currentBooksForBookcase = bookcasePages[currentBookcasePage] ?? [];
  const shelves = buildShelves(currentBooksForBookcase, shelfZones);

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

        const newBookcasePage = Math.floor(
          (updatedBooks.length - 1) / BOOKS_PER_BOOKCASE,
        );

        setCurrentBookcasePage(newBookcasePage);

        return updatedBooks;
      });

      setAddBookOpen(false);
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
                placeholder="Search title, author, genre, or series..."
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
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
          </section>

          <section
            className="books-filter-bar"
            style={
              {
                "--books-filter-frame": `url(${booksFilterFrame})`,
              } as CSSProperties
            }
          >
            <LibrarySelect
              ariaLabel="Filter by reading status"
              value={statusFilter}
              options={statusOptions}
              onChange={setStatusFilter}
            />
            <LibrarySelect
              ariaLabel="Filter by format"
              value={formatFilter}
              options={formatOptions}
              onChange={setFormatFilter}
            />
            <LibrarySelect
              ariaLabel="Filter by ownership"
              value={ownershipFilter}
              options={ownershipOptions}
              onChange={setOwnershipFilter}
            />
            <LibrarySelect
              ariaLabel="Filter by genre"
              value={genreFilter}
              options={[
                { value: "ALL", label: "All genres" },
                ...genreOptions.map((genre) => ({
                  value: genre,
                  label: genre,
                })),
              ]}
              onChange={setGenreFilter}
            />
            <LibrarySelect
              ariaLabel="Filter by series"
              value={seriesFilter}
              options={seriesOptions}
              onChange={setSeriesFilter}
            />
            <LibrarySelect
              ariaLabel="Filter by romance level"
              value={romanceFilter}
              options={romanceOptions}
              onChange={setRomanceFilter}
            />
            <LibrarySelect
              ariaLabel="Filter by atmosphere"
              value={atmosphereFilter}
              options={atmosphereOptions}
              onChange={setAtmosphereFilter}
            />
            <LibrarySelect
              ariaLabel="Filter by shelf"
              value={specialFilter}
              options={shelfOptions}
              onChange={setSpecialFilter}
            />
          </section>

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
                    className="shelf-book"
                    key={book.id}
                    style={
                      book.spineUrl
                        ? {
                            backgroundImage: `url(${book.spineUrl})`,
                          }
                        : { background: book.color }
                    }
                    title={book.title}
                    onClick={() => setSelectedBook(book)}
                  >
                    <span>{book.title}</span>
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
          onClose={() => setSelectedBook(null)}
          onSave={handleSaveBook}
          onReadingExperiencesChange={handleReadingExperiencesChange}
          onDelete={handleDeleteBook}
        />
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