import type { CatalogBookResult } from "../../src/api/libraryLaneApi";

const base = {
  subtitle: null,
  genres: ["Young Adult"],
  description: "A lonely girl adopts a remarkable stray dog.",
  publisher: "Walker Books",
  publicationDate: "2000",
  coverImageUrl: null,
  language: "en",
  isbn10: null,
  isbn13: null,
  seriesName: null,
  seriesNumber: null,
} as const;

export const winnDixiePhysical: CatalogBookResult = {
  ...base,
  provider: "Google Books",
  providerId: "winn-physical",
  title: "Because of Winn-Dixie",
  authors: ["Kate DiCamillo"],
  pageCount: 182,
  audiobookLengthSeconds: null,
  narrators: [],
  editionFormat: "Physical edition",
  format: "PHYSICAL",
};

export const winnDixieEbook: CatalogBookResult = {
  ...winnDixiePhysical,
  providerId: "winn-ebook",
  editionFormat: "E-Book edition",
  format: "EBOOK",
};

export const winnDixieAudiobook: CatalogBookResult = {
  ...base,
  provider: "Apple Audiobooks",
  providerId: "winn-audio",
  title: "Because of Winn-Dixie (Unabridged)",
  authors: ["Kate DiCamillo"],
  pageCount: null,
  audiobookLengthSeconds: 10_860,
  narrators: ["Cherry Jones"],
  editionFormat: "Unabridged audiobook",
  format: "AUDIOBOOK",
};

export const winnDixieWorkbook: CatalogBookResult = {
  ...winnDixiePhysical,
  providerId: "winn-workbook",
  authors: ["Creativity in the Classroom"],
  genres: ["Education"],
  description: "A study guide with chapter analysis and comprehension questions.",
  publisher: "CreateSpace Independent Publishing",
};

export const mixedWinnResults = [
  winnDixiePhysical,
  winnDixieEbook,
  winnDixieAudiobook,
];

export function result(overrides: Partial<CatalogBookResult>): CatalogBookResult {
  return { ...winnDixiePhysical, ...overrides };
}
