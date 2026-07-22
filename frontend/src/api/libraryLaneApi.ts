const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  details?: string;

  constructor(status: number, message: string, details?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  const text = response.status === 204 ? "" : await response.text();

  if (!response.ok) {
    let backendMessage = "";

    if (text) {
      try {
        const body = JSON.parse(text) as {
          message?: string;
          error?: string;
          detail?: string;
        };
        backendMessage = body.message || body.detail || body.error || "";
      } catch {
        backendMessage = text;
      }
    }

    throw new ApiError(
      response.status,
      backendMessage || `Request failed with status ${response.status}`,
      text || undefined
    );
  }

  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

/* ===========================
   BOOKS
=========================== */

export const bookApi = {
  getAll() {
    return request("/api/books");
  },

  getById(id: number) {
    return request(`/api/books/${id}`);
  },

  create(book: unknown) {
    return request("/api/books", {
      method: "POST",
      body: JSON.stringify(book),
    });
  },

  update(id: number, book: unknown) {
    return request(`/api/books/${id}`, {
      method: "PUT",
      body: JSON.stringify(book),
    });
  },

  delete(id: number) {
    return request(`/api/books/${id}`, {
      method: "DELETE",
    });
  },
};

/* ===========================
   EXTERNAL BOOK CATALOG
=========================== */

export type CatalogBookFormat = "PHYSICAL" | "EBOOK" | "AUDIOBOOK";
export type CatalogSearchField = "TITLE" | "AUTHOR" | "SERIES";

export type CatalogBookResult = {
  provider: string;
  providerId: string;
  title: string;
  subtitle?: string | null;
  authors: string[];
  genres: string[];
  description?: string | null;
  publisher?: string | null;
  publicationDate?: string | null;
  pageCount?: number | null;
  audiobookLengthSeconds?: number | null;
  narrators: string[];
  coverImageUrl?: string | null;
  language?: string | null;
  isbn10?: string | null;
  isbn13?: string | null;
  seriesName?: string | null;
  seriesNumber?: number | null;
  editionFormat?: string | null;
  format: CatalogBookFormat;
};

export const catalogApi = {
  searchBooks(
    query: string,
    format?: CatalogBookFormat,
    searchBy: CatalogSearchField = "TITLE"
  ) {
    const params = new URLSearchParams({ q: query.trim() });

    if (format) params.set("format", format);
    params.set("searchBy", searchBy);

    return request<CatalogBookResult[]>(
      `/api/catalog/books/search?${params.toString()}`
    );
  },

  resolveBook(result: CatalogBookResult) {
    return request<CatalogBookResult>("/api/catalog/books/resolve", {
      method: "POST",
      body: JSON.stringify(result),
    });
  },
};

/* ===========================
   READING EXPERIENCES
=========================== */

export type BackendReadingExperience = {
  id: number;
  createdAt?: string;
  updatedAt?: string;
};

export const readingExperienceApi = {
  getAll() {
    return request<BackendReadingExperience[]>("/api/reading-experiences");
  },

  getByBook(bookId: number) {
    return request<BackendReadingExperience[]>(
      `/api/reading-experiences/book/${bookId}`
    );
  },

  create(experience: unknown) {
    return request<BackendReadingExperience>("/api/reading-experiences", {
      method: "POST",
      body: JSON.stringify(experience),
    });
  },

  update(id: number, experience: unknown) {
    return request<BackendReadingExperience>(
      `/api/reading-experiences/${id}`,
      {
        method: "PUT",
        body: JSON.stringify(experience),
      }
    );
  },

  delete(id: number) {
    return request<void>(`/api/reading-experiences/${id}`, {
      method: "DELETE",
    });
  },
};

/* ===========================
   DASHBOARD
=========================== */

export const dashboardApi = {
  get() {
    return request("/api/dashboard");
  },
};