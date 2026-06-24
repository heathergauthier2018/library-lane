const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(
    `${API_BASE_URL}${path}`,
    {
      headers: {
        "Content-Type": "application/json",
      },
      ...options,
    }
  );

  if (!response.ok) {
    throw new Error(
      `API Error ${response.status}: ${response.statusText}`
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
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
   READING EXPERIENCES
=========================== */

export const readingExperienceApi = {
  getAll() {
    return request("/api/reading-experiences");
  },

  getByBook(bookId: number) {
    return request(
      `/api/reading-experiences/book/${bookId}`
    );
  },

  create(experience: unknown) {
    return request(
      "/api/reading-experiences",
      {
        method: "POST",
        body: JSON.stringify(experience),
      }
    );
  },

  update(
    id: number,
    experience: unknown
  ) {
    return request(
      `/api/reading-experiences/${id}`,
      {
        method: "PUT",
        body: JSON.stringify(experience),
      }
    );
  },

  delete(id: number) {
    return request(
      `/api/reading-experiences/${id}`,
      {
        method: "DELETE",
      }
    );
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