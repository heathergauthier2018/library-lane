import { useEffect, useMemo, useState } from "react";
import type { AppPage } from "../App";
import FantasySidebar from "../dashboard/FantasySidebar";
import bookshelfBackground from "../assets/storybook/backgrounds/bookshelf-background.png";
import AddBookModal from "../components/books/AddBookModal";
import BookDetailsModal from "../components/books/BookDetailsModal";
import type { NewBook } from "../components/books/AddBookModal";
import { bookApi } from "../api/libraryLaneApi";

type BooksPageProps = {
  currentPage: AppPage;
  setCurrentPage: (page: AppPage) => void;
  openAddBookOnLoad?: boolean;
  onAddBookOpened?: () => void;
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

  [key: string]: string | undefined;
};

export type ShelfBook = {
  id: string;
  title: string;
  author: string;
  genre: string;
  seriesName: string;
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
  extra: Partial<ShelfBook> = {}
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
  makeStarterBook(`Book ${9 + i}`, mockColors[i % mockColors.length])
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
  0
);

function migrateReadingExperience(
  experience: Partial<ReadingExperience>
): ReadingExperience {
  return {
    id: experience.id || crypto.randomUUID(),
    label: experience.label || "Reading Experience",

    title: experience.title,
    author: experience.author,
    genre: experience.genre,
    seriesName: experience.seriesName,

    format: experience.format || "PHYSICAL",
    readingStatus: experience.readingStatus || "TO_READ",
    startDate: experience.startDate,
    finishDate: experience.finishDate,
    currentPage: experience.currentPage,
    pageCount: experience.pageCount,
    audioLength: experience.audioLength,
    currentListeningPosition: experience.currentListeningPosition,
    listeningSpeed: experience.listeningSpeed,
    narrator: experience.narrator,

    excitementRating: experience.excitementRating,
    predictedRating: experience.predictedRating,
    currentRating: experience.currentRating,
    currentExcitementRating: experience.currentExcitementRating,
    initialRating: experience.initialRating,
    finalRating: experience.finalRating,
    rereadRating: experience.rereadRating,
    emotionalDevastationRating: experience.emotionalDevastationRating,

    romancePresence: experience.romancePresence,
    romanceImportance: experience.romanceImportance,
    romanceRating: experience.romanceRating,
    spiceRating: experience.spiceRating,
    romanceNotes: experience.romanceNotes,

    horrorRating: experience.horrorRating,

    notes: experience.notes,
    createdAt: experience.createdAt || new Date().toISOString(),

    ...experience,
  };
}

function migrateBook(book: Partial<ShelfBook>): ShelfBook {
  return {
    id: book.id || crypto.randomUUID(),
    title: book.title || "",
    author: book.author || "",
    genre: book.genre || "",
    seriesName: book.seriesName || "",
    readingStatus: book.readingStatus || "TO_READ",
    format: book.format || "PHYSICAL",
    ownedStatus: book.ownedStatus || "",
    source: book.source || "",
    atmosphere: book.atmosphere || "",
    romancePresence: book.romancePresence || "NO",
    isSeries: book.isSeries || "NO",
    favorite: book.favorite || "NO",
    comfortRead: book.comfortRead || "NO",
    color: book.color || mockColors[0],
    coverUrl: book.coverUrl,
    spineUrl: book.spineUrl,
    addedAt: book.addedAt || new Date().toISOString(),
    startDate: book.startDate,
    finishDate: book.finishDate,
    publicationYear: book.publicationYear,
    pageCount: book.pageCount,
    audioLength: book.audioLength,
    finalRating: book.finalRating,
    rereadRating: book.rereadRating,
    emotionalDevastationRating: book.emotionalDevastationRating,
    romanceRating: book.romanceRating,
    spiceRating: book.spiceRating,
    horrorRating: book.horrorRating,
    readingExperiences: Array.isArray(book.readingExperiences)
      ? book.readingExperiences.map(migrateReadingExperience)
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

function saveBooksToLocalStorage(books: ShelfBook[]) {
  localStorage.setItem(LIBRARY_LANE_BOOKS_KEY, JSON.stringify(books));
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
    const booksForZone = books.slice(currentIndex, currentIndex + zone.capacity);
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
    readingStatus: newBook.readingStatus || "TO_READ",
    format: newBook.format || "PHYSICAL",
    ownedStatus: newBook.ownedStatus || "",
    source: newBook.source || "",
    atmosphere: newBook.atmosphere || "",
    romancePresence: newBook.romancePresence || "NO",
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
      setBooks((apiBooks as Partial<ShelfBook>[]).map(migrateBook));
    } catch (error) {
      console.error("Failed to load books from API", error);
      setBooks(loadSavedBooks());
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
      books.map((book) => book.genre.trim()).filter((genre) => genre.length > 0)
    );

    return Array.from(genres).sort((a, b) => a.localeCompare(b));
  }, [books]);

  const filteredAndSortedBooks = useMemo(() => {
    const normalizedSearch = normalizeBookText(searchTerm);

    const filtered = books.filter((book) => {
      const searchableText = normalizeBookText(
        [book.title, book.author, book.genre, book.seriesName].join(" ")
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
          return numberValue(b.publicationYear) - numberValue(a.publicationYear);
        case "publication-old":
          return numberValue(a.publicationYear) - numberValue(b.publicationYear);
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

  function handleAddBook(newBook: NewBook) {
    const normalizedNewTitle = normalizeBookText(newBook.title);

    if (!normalizedNewTitle) return;

    const alreadyExists = books.some(
      (book) => normalizeBookText(book.title) === normalizedNewTitle
    );

    if (alreadyExists) {
      alert("This book already exists in your library.");
      return;
    }

    const nextBookColor = mockColors[books.length % mockColors.length];
    const shelfBook = makeShelfBookFromNewBook(newBook, nextBookColor);
    const updatedBooks = [...books, shelfBook];

    setBooks(updatedBooks);
    saveBooksToLocalStorage(updatedBooks);

    const newBookcasePage = Math.floor(
      (updatedBooks.length - 1) / BOOKS_PER_BOOKCASE
    );

    setCurrentBookcasePage(newBookcasePage);
  }

  function handleSaveBook(updatedBook: ShelfBook) {
    const updatedBooks = books.map((book) =>
      book.id === updatedBook.id ? updatedBook : book
    );

    setBooks(updatedBooks);
    saveBooksToLocalStorage(updatedBooks);
    setSelectedBook(updatedBook);
  }

  function handleDeleteBook(bookId: string) {
    const updatedBooks = books.filter((book) => book.id !== bookId);

    setBooks(updatedBooks);
    saveBooksToLocalStorage(updatedBooks);
    setSelectedBook(null);

    const maxPage = Math.max(
      0,
      Math.ceil(updatedBooks.length / BOOKS_PER_BOOKCASE) - 1
    );

    setCurrentBookcasePage((page) => Math.min(page, maxPage));
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
        <section className="books-page-header">
          <div>
            <p>Your Complete Library</p>
            <h1>Books</h1>
            <span>Every story you have added to Library Lane.</span>
          </div>

          <button
            className="books-add-button"
            onClick={() => setAddBookOpen(true)}
          >
            + Add Book
          </button>
        </section>

        <section className="books-toolbar">
          <input
            placeholder="Search title, author, genre, or series..."
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
          />

          <select
            value={sortBy}
            onChange={(event) => setSortBy(event.target.value)}
          >
            <option value="added-new">Sort by recently added</option>
            <option value="added-old">Sort by oldest added</option>
            <option value="title-az">Title A–Z</option>
            <option value="title-za">Title Z–A</option>
            <option value="author-az">Author A–Z</option>
            <option value="author-za">Author Z–A</option>
            <option value="status">Reading status</option>
            <option value="rating-high">Highest rated</option>
            <option value="rating-low">Lowest rated</option>
            <option value="emotional-high">Most emotional</option>
            <option value="romance-high">Most romantic</option>
            <option value="spice-high">Spiciest</option>
            <option value="horror-high">Scariest</option>
            <option value="reread-high">Most rereadable</option>
            <option value="pages-high">Most pages</option>
            <option value="pages-low">Fewest pages</option>
            <option value="publication-new">Publication date: newest</option>
            <option value="publication-old">Publication date: oldest</option>
            <option value="recent-read">Most recently finished</option>
            <option value="oldest-read">Oldest finished</option>
          </select>
        </section>

        <section className="books-filter-bar">
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="ALL">All statuses</option>
            <option value="TO_READ">Want To Read</option>
            <option value="READING">Currently Reading</option>
            <option value="COMPLETED">Completed</option>
            <option value="DNF">DNF</option>
          </select>

          <select
            value={formatFilter}
            onChange={(event) => setFormatFilter(event.target.value)}
          >
            <option value="ALL">All formats</option>
            <option value="PHYSICAL">Physical</option>
            <option value="EBOOK">E-Book</option>
            <option value="AUDIOBOOK">Audiobook</option>
            <option value="MIXED">Mixed</option>
          </select>

          <select
            value={ownershipFilter}
            onChange={(event) => setOwnershipFilter(event.target.value)}
          >
            <option value="ALL">All ownership</option>
            <option value="OWNED">Owned</option>
            <option value="BORROWED">Borrowed</option>
            <option value="LIBRARY_COPY">Library Copy</option>
            <option value="KINDLE_UNLIMITED">Kindle Unlimited</option>
            <option value="AUDIBLE">Audible</option>
            <option value="GIFTED">Gifted</option>
          </select>

          <select
            value={genreFilter}
            onChange={(event) => setGenreFilter(event.target.value)}
          >
            <option value="ALL">All genres</option>
            {genreOptions.map((genre) => (
              <option key={genre} value={genre}>
                {genre}
              </option>
            ))}
          </select>

          <select
            value={seriesFilter}
            onChange={(event) => setSeriesFilter(event.target.value)}
          >
            <option value="ALL">Series + standalones</option>
            <option value="SERIES">Series only</option>
            <option value="STANDALONE">Standalones only</option>
          </select>

          <select
            value={romanceFilter}
            onChange={(event) => setRomanceFilter(event.target.value)}
          >
            <option value="ALL">All romance levels</option>
            <option value="NO">No romance</option>
            <option value="MINOR">Minor romance</option>
            <option value="SIDE">Side romance</option>
            <option value="SIGNIFICANT">Significant romance</option>
            <option value="PRIMARY">Primary romance plot</option>
          </select>

          <select
            value={atmosphereFilter}
            onChange={(event) => setAtmosphereFilter(event.target.value)}
          >
            <option value="ALL">All atmospheres</option>
            <option value="COZY">Cozy</option>
            <option value="MAGICAL">Magical</option>
            <option value="ROMANTIC">Romantic</option>
            <option value="HAUNTING">Haunting</option>
            <option value="WHIMSICAL">Whimsical</option>
            <option value="DARK_ACADEMIA">Dark Academia</option>
            <option value="GOTHIC">Gothic</option>
            <option value="ADVENTURE">Adventure</option>
            <option value="HISTORICAL">Historical</option>
            <option value="MYSTERIOUS">Mysterious</option>
          </select>

          <select
            value={specialFilter}
            onChange={(event) => setSpecialFilter(event.target.value)}
          >
            <option value="ALL">All shelves</option>
            <option value="FAVORITES">Favorites only</option>
            <option value="FIVE_STAR">Five-star books</option>
            <option value="REREAD_WORTHY">Reread-worthy</option>
            <option value="COMFORT_READS">Comfort reads</option>
            <option value="EMOTIONAL_READS">Heartbreak books</option>
          </select>
        </section>

        <section className="bookcase-view">
          <section
            className="bookshelf-page-background"
            style={{ backgroundImage: `url(${bookshelfBackground})` }}
          >
            {shelves.map((shelf) => (
              <div
                className={`bookshelf-shelf-zone ${shelf.className} ${
                  shelf.books.length < shelf.capacity ? "shelf-zone-partial" : ""
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
          onDelete={handleDeleteBook}
        />
      </main>
    </div>
  );
}