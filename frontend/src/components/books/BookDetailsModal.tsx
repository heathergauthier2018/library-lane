import { useEffect, useMemo, useState } from "react";
import openBookBg from "../../assets/storybook/backgrounds/add-book-background.png";
import closeBookFrame from "../../assets/storybook/frames/close-book-frame.png";
import addStoryFrame from "../../assets/storybook/frames/add-story-frame.png";
import ReadingExperienceModal from "./ReadingExperienceModal";
import type { ReadingExperience, ShelfBook } from "../../pages/BooksPage";
import { ratingIcons } from "./bookLedgerConfig";
import RatingDisplay from "./RatingDisplay";
import { readingExperienceApi } from "../../api/libraryLaneApi";

type Option = {
  label: string;
  value: string;
};

type BookDetailsModalProps = {
  isOpen: boolean;
  book: ShelfBook | null;
  onClose: () => void;
  onSave: (book: ShelfBook) => void;
  onReadingExperiencesChange: (
    bookId: string,
    experiences: ReadingExperience[]
  ) => void;
  onDelete: (bookId: string) => void;
};

const statusOptions: Option[] = [
  { label: "Want To Read", value: "TO_READ" },
  { label: "Currently Reading", value: "READING" },
  { label: "Completed", value: "COMPLETED" },
  { label: "Did Not Finish", value: "DNF" },
];

const formatOptions: Option[] = [
  { label: "Physical Book", value: "PHYSICAL" },
  { label: "E-Book", value: "EBOOK" },
  { label: "Audiobook", value: "AUDIOBOOK" },
  { label: "Mixed Formats", value: "MIXED" },
];

type ReaderHandwritingStyle =
  | "cedarville"
  | "labelle"
  | "caveat"
  | "shadows"
  | "nothing"
  | "sue"
  | "reenie"
  | "kalam"
  | "architects"
  | "handlee"
  | "ballet"
  | "tangerine"
  | "marck"
  | "qwigley"
  | "pinyon"
  | "parisienne"
  | "imfell"
  | "medieval";

const handwritingOptions: {
  label: string;
  value: ReaderHandwritingStyle;
}[] = [
  { label: "Cedarville Cursive", value: "cedarville" },
  { label: "La Belle Aurore", value: "labelle" },
  { label: "Caveat", value: "caveat" },
  { label: "Shadows Into Light", value: "shadows" },
  { label: "Nothing You Could Do", value: "nothing" },
  { label: "Sue Ellen Francisco", value: "sue" },
  { label: "Reenie Beanie", value: "reenie" },
  { label: "Kalam", value: "kalam" },
  { label: "Architects Daughter", value: "architects" },
  { label: "Handlee", value: "handlee" },
  { label: "Ballet", value: "ballet" },
  { label: "Tangerine", value: "tangerine" },
  { label: "Marck Script", value: "marck" },
  { label: "Qwigley", value: "qwigley" },
  { label: "Pinyon Script", value: "pinyon" },
  { label: "Parisienne", value: "parisienne" },
  { label: "IM Fell English SC", value: "imfell" },
  { label: "MedievalSharp", value: "medieval" },
];

function ratingValue(value?: string) {
  const parsed = Number(value || "0");
  return Number.isNaN(parsed) ? 0 : parsed;
}

function average(values: number[]) {
  const validValues = values.filter((value) => value > 0);
  if (validValues.length === 0) return "Not rated yet";

  return (
    validValues.reduce((sum, value) => sum + value, 0) / validValues.length
  ).toFixed(1);
}

function formatDisplay(value: string) {
  return (
    formatOptions.find((option) => option.value === value)?.label ||
    value ||
    "Format not selected"
  );
}

function statusDisplay(value: string) {
  return (
    statusOptions.find((option) => option.value === value)?.label ||
    value ||
    "Status not selected"
  );
}

type BookDetailsModalContentProps = Omit<
  BookDetailsModalProps,
  "isOpen" | "book"
> & {
  book: ShelfBook;
};

function BookDetailsModalContent({
  book,
  onClose,
  onSave,
  onReadingExperiencesChange,
  onDelete,
}: BookDetailsModalContentProps) {
  const [draftBook, setDraftBook] = useState<ShelfBook | null>(book);
  const [ledgerPage, setLedgerPage] = useState(0);
  const [experienceModalOpen, setExperienceModalOpen] = useState(false);
  const [selectedExperience, setSelectedExperience] =
    useState<ReadingExperience | null>(null);

    
  const [readerHandwritingStyle, setReaderHandwritingStyle] =
  useState<ReaderHandwritingStyle>(() => {
    return (
      (localStorage.getItem(
        "libraryLaneReaderHandwritingStyle"
      ) as ReaderHandwritingStyle) || "cedarville"
    );
  });

  useEffect(() => {
  localStorage.setItem(
    "libraryLaneReaderHandwritingStyle",
    readerHandwritingStyle
  );

  document.documentElement.className = document.documentElement.className
    .split(" ")
    .filter(
      (className) =>
        !className.startsWith("reader-handwriting-style-")
    )
    .join(" ");

  document.documentElement.classList.add(
    `reader-handwriting-style-${readerHandwritingStyle}`
  );
}, [readerHandwritingStyle]);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !experienceModalOpen) {
        event.preventDefault();
        onClose();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [experienceModalOpen, onClose]);

  const experiences = useMemo(
    () => draftBook?.readingExperiences ?? [],
    [draftBook?.readingExperiences]
  );

  const readingStats = useMemo(() => {
    const finalRatings = experiences.map((experience) =>
      ratingValue(experience.finalRating)
    );

    const rereadRatings = experiences.map((experience) =>
      ratingValue(experience.rereadRating)
    );

    const emotionalRatings = experiences.map((experience) =>
      ratingValue(experience.emotionalDevastationRating)
    );

    const formatsUsed = Array.from(
      new Set(experiences.map((experience) => experience.format).filter(Boolean))
    );

    return {
      total: experiences.length,
      averageFinal: average(finalRatings),
      averageReread: average(rereadRatings),
      averageEmotional: average(emotionalRatings),
      formatsUsed: formatsUsed.length
        ? formatsUsed.map(formatDisplay).join(", ")
        : "None yet",
    };
  }, [experiences]);

  if (!draftBook) return null;

  const currentBook = draftBook;
  const pageCount = 4;
  const isFirstPage = ledgerPage === 0;
  const isLastPage = ledgerPage === pageCount - 1;

  function updateBookField(field: keyof ShelfBook, value: string) {
    setDraftBook((prev) =>
      prev
        ? {
            ...prev,
            [field]: value,
          }
        : prev
    );
  }

  function handleOpenExperience(experience: ReadingExperience) {
    setSelectedExperience(experience);
    setExperienceModalOpen(true);
  }

  function handleAddExperience() {
    setSelectedExperience(null);
    setExperienceModalOpen(true);
  }

 function mapReadingStatusToBackend(status?: string) {
  switch (status) {
    case "TO_READ":
      return "TBR";
    case "READING":
      return "CURRENTLY_READING";
    case "COMPLETED":
      return "COMPLETED";
    case "DNF":
      return "DNF";
    default:
      return "TBR";
  }
}

function mapFormatToBackend(format?: string) {
  switch (format) {
    case "PHYSICAL":
      return "PHYSICAL_BOOK";
    case "EBOOK":
      return "E_BOOK";
    case "AUDIOBOOK":
      return "AUDIO_BOOK";
    case "MIXED":
      return "MIXED_FORMATS";
    default:
      return "PHYSICAL_BOOK";
  }
}

function toNumberOrNull(value?: string) {
  if (!value) return null;

  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

async function handleSaveExperience(
  savedExperience: ReadingExperience
) {
  try {
    const bookId = Number(currentBook.id);

    if (!Number.isFinite(bookId) || bookId <= 0) {
      throw new Error(
        "This book does not have a valid database ID, so its reading experience cannot be saved yet."
      );
    }

    const backendId = Number(savedExperience.id);

    const isExistingBackendExperience =
      Number.isFinite(backendId) && backendId > 0;

    const payload = {
      label: savedExperience.label,

      format: mapFormatToBackend(savedExperience.format),
      status: mapReadingStatusToBackend(savedExperience.readingStatus),

      startDate: savedExperience.startDate || null,
      finishDate: savedExperience.finishDate || null,

      currentPage: toNumberOrNull(savedExperience.currentPage),
      totalPages: toNumberOrNull(savedExperience.pageCount),

      listeningSpeed: toNumberOrNull(savedExperience.listeningSpeed),

      predictedRating: savedExperience.predictedRating,
      currentRating: savedExperience.currentRating,
      initialRating: savedExperience.initialRating,
      finalRating: savedExperience.finalRating,
      rereadRating: savedExperience.rereadRating,
      emotionalDevastationRating:
        savedExperience.emotionalDevastationRating,

      excitementRating: savedExperience.excitementRating,
      currentExcitementRating:
        savedExperience.currentExcitementRating,
      excitementWhileReading:
        savedExperience.excitementWhileReading,

      romancePresence: savedExperience.romancePresence,
      romanceImportance: savedExperience.romanceImportance,
      romanceRating: savedExperience.romanceRating,
      spiceRating: savedExperience.spiceRating,
      horrorRating: savedExperience.horrorRating,

      romanceNotes: savedExperience.romanceNotes,
      reviewText: savedExperience.notes,
      dnfReason: savedExperience.dnfReason,
      promptResponsesJson: savedExperience.promptResponsesJson,

      currentExperience: savedExperience.readingStatus === "READING",

      book: {
        id: bookId,
      },
    };

    const savedFromBackend = isExistingBackendExperience
      ? await readingExperienceApi.update(backendId, payload)
      : await readingExperienceApi.create(payload);

    const updatedExperience = {
      ...savedExperience,
      id: String(savedFromBackend.id),
      createdAt:
        savedExperience.createdAt ||
        savedFromBackend.createdAt ||
        new Date().toISOString(),
    };

    setSelectedExperience(updatedExperience);

    const oldId = savedExperience.id;
    const newId = updatedExperience.id;
    const existingExperiences = currentBook.readingExperiences ?? [];
    const alreadyExists = existingExperiences.some(
      (experience) =>
        experience.id === oldId || experience.id === newId
    );

    const updatedExperiences = alreadyExists
      ? existingExperiences.map((experience) =>
          experience.id === oldId || experience.id === newId
            ? updatedExperience
            : experience
        )
      : [...existingExperiences, updatedExperience];

    // The first experience spread also displays shared Book metadata. If the
    // reader edits those fields there, carry them into the parent book draft
    // so the parent Save Changes button can persist them through /api/books.
    const updatedBookMetadata = {
      title: savedExperience.title?.trim() || currentBook.title,
      author:
        savedExperience.author !== undefined
          ? savedExperience.author.trim()
          : currentBook.author,
      genre:
        savedExperience.genre !== undefined
          ? savedExperience.genre.trim()
          : currentBook.genre,
      seriesName:
        savedExperience.seriesName !== undefined
          ? savedExperience.seriesName.trim()
          : currentBook.seriesName,
      seriesNumber:
        savedExperience.seriesNumber !== undefined
          ? savedExperience.seriesNumber.trim()
          : currentBook.seriesNumber,
      isSeries:
        savedExperience.isSeries !== undefined
          ? savedExperience.isSeries.trim()
          : currentBook.isSeries,
      subtitle:
        savedExperience.subtitle !== undefined
          ? savedExperience.subtitle.trim()
          : currentBook.subtitle,
      description:
        savedExperience.description !== undefined
          ? savedExperience.description.trim()
          : currentBook.description,
      publisher:
        savedExperience.publisher !== undefined
          ? savedExperience.publisher.trim()
          : currentBook.publisher,
      publisherOther:
        savedExperience.publisherOther !== undefined
          ? savedExperience.publisherOther.trim()
          : currentBook.publisherOther,
      publicationYear:
        savedExperience.publicationYear !== undefined
          ? savedExperience.publicationYear.trim()
          : currentBook.publicationYear,
      editionFormat:
        savedExperience.editionFormat !== undefined
          ? savedExperience.editionFormat.trim()
          : currentBook.editionFormat,
      narrator:
        savedExperience.narrator !== undefined
          ? savedExperience.narrator.trim()
          : currentBook.narrator,
      coverUrl:
        savedExperience.coverUrl !== undefined
          ? savedExperience.coverUrl.trim()
          : currentBook.coverUrl,
      language:
        savedExperience.language !== undefined
          ? savedExperience.language.trim()
          : currentBook.language,
      isbn10:
        savedExperience.isbn10 !== undefined
          ? savedExperience.isbn10.trim()
          : currentBook.isbn10,
      isbn13:
        savedExperience.isbn13 !== undefined
          ? savedExperience.isbn13.trim()
          : currentBook.isbn13,
      catalogProvider:
        savedExperience.catalogProvider !== undefined
          ? savedExperience.catalogProvider.trim()
          : currentBook.catalogProvider,
      catalogProviderId:
        savedExperience.catalogProviderId !== undefined
          ? savedExperience.catalogProviderId.trim()
          : currentBook.catalogProviderId,
      pageCount:
        savedExperience.pageCount !== undefined
          ? savedExperience.pageCount.trim()
          : currentBook.pageCount,
      audioLength:
        savedExperience.audioLength !== undefined
          ? savedExperience.audioLength.trim()
          : currentBook.audioLength,
    };

    setDraftBook((prev) =>
      prev
        ? {
            ...prev,
            ...updatedBookMetadata,
            readingExperiences: updatedExperiences,
          }
        : prev
    );

    // Update BooksPage state without issuing a second PUT /api/books/{id}.
    onReadingExperiencesChange(currentBook.id, updatedExperiences);
  } catch (error) {
    console.error("Failed to save reading experience", error);
    const message =
      error instanceof Error
        ? error.message
        : "The reading experience could not be saved.";
    window.alert(message);
    throw error;
  }
}

function handleSave() {
  const savedBook: ShelfBook = {
    ...currentBook,
    title: currentBook.title.trim(),
    author: currentBook.author.trim(),
    genre: currentBook.genre.trim(),
    seriesName: currentBook.seriesName.trim(),
  };

  if (!savedBook.title) return;

  onSave(savedBook);
  setDraftBook(savedBook);
}

  function handleDelete() {
    const confirmed = window.confirm(
      `Remove "${currentBook.title}" from your library?`
    );

    if (confirmed) {
      onDelete(currentBook.id);
    }
  }

function renderBookRecordPage() {
  const firstExperience = experiences[0];
  const latestExperience = experiences[experiences.length - 1];

  const formatsExperienced = Array.from(
    new Set(experiences.map((experience) => formatDisplay(experience.format)))
  ).join(", ");

  const firstJourneyDate = firstExperience?.createdAt
    ? new Date(firstExperience.createdAt).toLocaleDateString("en-US", {
        month: "long",
        day: "numeric",
        year: "numeric",
      })
    : "Not started";

  const latestJourneyDate = latestExperience?.createdAt
    ? new Date(latestExperience.createdAt).toLocaleDateString("en-US", {
        month: "long",
        day: "numeric",
        year: "numeric",
      })
    : "Not started";

  const averageRomance = average(
    experiences.map((experience) => ratingValue(experience.romanceRating))
  );

  const averageSpice = average(
    experiences.map((experience) => ratingValue(experience.spiceRating))
  );

  const averageHorror = average(
    experiences.map((experience) => ratingValue(experience.horrorRating))
  );

  const hasOverallRating = readingStats.averageFinal !== "Not rated yet";
  const hasRereadRating = readingStats.averageReread !== "Not rated yet";
  const hasEmotionalRating = readingStats.averageEmotional !== "Not rated yet";
  const hasRomanceRating = averageRomance !== "Not rated yet";
  const hasSpiceRating = averageSpice !== "Not rated yet";
  const hasHorrorRating = averageHorror !== "Not rated yet";

  return (
    <>
      <section className="add-book-page add-book-left-page story-overview-page story-title-page">
        <p className="add-book-eyebrow">Library Lane Ledger</p>

        <div className="story-title-page-content">
          <h2 className="story-overview-title">
            {currentBook.title || "Untitled Story"}
          </h2>

          {currentBook.author && (
            <p className="story-overview-author">by {currentBook.author}</p>
          )}

          {hasOverallRating && (
            <div className="story-overview-main-rating">
              <span>Overall Rating</span>
              {renderRatingIcons(readingStats.averageFinal, "overall")}
            </div>
          )}
        </div>
      </section>

      <section className="add-book-page add-book-right-page story-overview-page story-record-page">
        <p className="add-book-eyebrow">Story Overview</p>
        <h2>Reading Record</h2>

        <div className="story-overview-fact">
          <span>Status</span>
          <strong>{statusDisplay(currentBook.readingStatus)}</strong>
        </div>

        <div className="story-overview-fact">
          <span>First Journey</span>
          <strong>{firstJourneyDate}</strong>
        </div>

        <div className="story-overview-fact">
          <span>Latest Journey</span>
          <strong>{latestJourneyDate}</strong>
        </div>

        <div className="story-overview-fact">
          <span>Formats Experienced</span>
          <strong>
            {formatsExperienced || formatDisplay(currentBook.format)}
          </strong>
        </div>

        {currentBook.publicationYear && (
          <div className="story-overview-fact">
            <span>Published</span>
            <strong>{currentBook.publicationYear}</strong>
          </div>
        )}

        {currentBook.seriesName && (
          <div className="story-overview-fact">
            <span>Series</span>
            <strong>{currentBook.seriesName}</strong>
          </div>
        )}

        <div className="story-overview-journey-count story-record-journey-count">
          <span>Journey Through This Story</span>
          <strong>
            {experiences.length}{" "}
            {experiences.length === 1 ? "Experience" : "Experiences"} Logged
          </strong>
        </div>

        {(hasRereadRating ||
          hasEmotionalRating ||
          hasRomanceRating ||
          hasSpiceRating ||
          hasHorrorRating) && (
          <div className="story-record-ratings">
            {hasRereadRating && (
              <div className="story-record-rating-row">
                <span>Re-readability</span>
                {renderRatingIcons(readingStats.averageReread, "reread")}
              </div>
            )}

            {hasEmotionalRating && (
              <div className="story-record-rating-row">
                <span>Emotional Devastation</span>
                {renderRatingIcons(readingStats.averageEmotional, "devastated")}
              </div>
            )}

            {hasRomanceRating && (
              <div className="story-record-rating-row">
                <span>Romance</span>
                {renderRatingIcons(averageRomance, "romance")}
              </div>
            )}

            {hasSpiceRating && (
              <div className="story-record-rating-row">
                <span>Spice</span>
                {renderRatingIcons(averageSpice, "spice")}
              </div>
            )}

            {hasHorrorRating && (
              <div className="story-record-rating-row">
                <span>Scare Factor</span>
                {renderRatingIcons(averageHorror, "horror")}
              </div>
            )}
          </div>
        )}
      </section>
    </>
  );
}

 function renderRatingIcons(
  rating?: string,
  ratingIcon: keyof typeof ratingIcons = "overall"
) {
  return (
    <RatingDisplay
      value={rating}
      icon={ratingIcons[ratingIcon]}
      className={`reading-experience-rating-icons rating-display-${ratingIcon}`}
    />
  );
}

  function renderExperiencesPage() {
  return (
    <>
      <section className="add-book-page add-book-left-page">
        <p className="add-book-eyebrow">Reading Experiences</p>
        <h2>Experience Log</h2>

        {experiences.length === 0 ? (
          <p className="focused-writing-prompt">
            No reading experiences yet. Add the first read, listen, reread, or
            special edition experience for this story.
          </p>
        ) : (
          experiences.map((experience, index) => (
            <button
              type="button"
              className="reading-experience-card"
              key={experience.id}
              onClick={() => handleOpenExperience(experience)}
            >
              <strong>
                {experience.label ||
                  (index === 0
                    ? "Original Reading Experience"
                    : `Reading Experience ${index + 1}`)}
              </strong>

              <span className="reading-experience-meta">
                {formatDisplay(experience.format)} •{" "}
                {statusDisplay(experience.readingStatus)}
              </span>

              <span className="reading-experience-rating-row">
                {renderRatingIcons(experience.finalRating)}
              </span>

              <span className="reading-experience-date">
                Began{" "}
                {new Date(experience.createdAt).toLocaleDateString("en-US", {
                  month: "long",
                  day: "numeric",
                  year: "numeric",
                })}
              </span>
            </button>
          ))
        )}
      </section>

      <section className="add-book-page add-book-right-page">
        <p className="add-book-eyebrow">Add Another Journey</p>
        <h2>New Read</h2>

        <p className="focused-writing-prompt">
          Track every version of your relationship with this book: first read,
          reread, audiobook listen, Kindle read, physical copy, annotated copy,
          or book club read.
        </p>

       <button
  type="button"
  className="magical-script-action"
  onClick={handleAddExperience}
>
  Add Experience
</button>
      </section>
    </>
  );
}

  function renderStatsPage() {
    return (
      <>
        <section className="add-book-page add-book-left-page">
          <p className="add-book-eyebrow">Reading History</p>
          <h2>Summary</h2>

          <label>
            <span>Total Reading Experiences</span>
            <input value={String(readingStats.total)} readOnly />
          </label>

          <label>
            <span>Average Final Rating</span>
            <input value={readingStats.averageFinal} readOnly />
          </label>

          <label>
            <span>Average Rereadability</span>
            <input value={readingStats.averageReread} readOnly />
          </label>

          <label>
            <span>Average Emotional Devastation</span>
            <input value={readingStats.averageEmotional} readOnly />
          </label>
        </section>

        <section className="add-book-page add-book-right-page">
          <p className="add-book-eyebrow">Across Every Read</p>
          <h2>Patterns</h2>

          <label>
            <span>Formats Used</span>
            <input value={readingStats.formatsUsed} readOnly />
          </label>

          <label>
            <span>Book-Level Final Rating</span>
            <input
              value={currentBook.finalRating ?? ""}
              onChange={(event) =>
                updateBookField("finalRating", event.target.value)
              }
              placeholder="Overall book rating..."
            />
          </label>

          <label>
            <span>Book-Level Rereadability</span>
            <input
              value={currentBook.rereadRating ?? ""}
              onChange={(event) =>
                updateBookField("rereadRating", event.target.value)
              }
              placeholder="0 - 5"
            />
          </label>

          <label>
            <span>Book-Level Emotional Devastation</span>
            <input
              value={currentBook.emotionalDevastationRating ?? ""}
              onChange={(event) =>
                updateBookField(
                  "emotionalDevastationRating",
                  event.target.value
                )
              }
              placeholder="0 - 5"
            />
          </label>
        </section>
      </>
    );
  }

  function renderJournalSummaryPage() {
  return (
    <>
      <section className="add-book-page add-book-left-page">
        <p className="add-book-eyebrow">Book Journal</p>
        <h2>Saved Pages</h2>

        <label>
          <span>Quotes Saved</span>
          <input value="Coming soon" readOnly />
        </label>

        <label>
          <span>Characters Tracked</span>
          <input value="Coming soon" readOnly />
        </label>

        <label>
          <span>Locations Logged</span>
          <input value="Coming soon" readOnly />
        </label>

        <label>
          <span>Timeline Events</span>
          <input value="Coming soon" readOnly />
        </label>
      </section>

      <section className="add-book-page add-book-right-page">
        <p className="add-book-eyebrow">Future Ledger Links</p>
        <h2>Story Worlds</h2>

        <p className="focused-writing-prompt">
          This page will eventually link to quotes, characters, locations,
          timeline events, fan casting, music, collections, book weather, and
          other saved journal pages for this specific story.
        </p>

        <div className="reader-handwriting-selector">
          <span>Reader Handwriting Style</span>

          <select
            value={readerHandwritingStyle}
            onChange={(event) =>
              setReaderHandwritingStyle(
                event.target.value as ReaderHandwritingStyle
              )
            }
          >
            {handwritingOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <p className="reader-handwriting-preview reader-handwriting">
            The stars were brighter than usual tonight.
          </p>
        </div>
      </section>
    </>
  );
}

  function renderCurrentPage() {
    switch (ledgerPage) {
      case 0:
        return renderBookRecordPage();
      case 1:
        return renderExperiencesPage();
      case 2:
        return renderStatsPage();
      case 3:
        return renderJournalSummaryPage();
      default:
        return renderBookRecordPage();
    }
  }

  return (
    <>
      <div
        className="add-book-overlay"
        role="dialog"
        aria-modal="true"
        aria-label={`${currentBook.title} reading record`}
        onMouseDown={(event) => {
          if (event.target === event.currentTarget) onClose();
        }}
      >
        <div
          className="add-book-ledger"
          style={{ backgroundImage: `url(${openBookBg})` }}
        >
          {renderCurrentPage()}

          <div className="add-book-actions add-book-actions-left">
            <button
              type="button"
              className="add-book-frame-button close-book-button"
              onClick={onClose}
              style={{ backgroundImage: `url(${closeBookFrame})` }}
            >
              Close
            </button>

            <div className="add-book-nav-slot">
              {!isFirstPage && (
                <button
                  type="button"
                  className="add-book-frame-button close-book-button"
                  onClick={() =>
                    setLedgerPage((prev) => Math.max(prev - 1, 0))
                  }
                  style={{ backgroundImage: `url(${closeBookFrame})` }}
                >
                  Previous Page
                </button>
              )}
            </div>

            <div className="add-book-nav-slot">
              {!isLastPage && (
                <button
                  type="button"
                  className="add-book-frame-button close-book-button"
                  onClick={() =>
                    setLedgerPage((prev) => Math.min(prev + 1, pageCount - 1))
                  }
                  style={{ backgroundImage: `url(${closeBookFrame})` }}
                >
                  Turn Page
                </button>
              )}
            </div>
          </div>

          <div className="add-book-actions add-book-actions-right">
            <button
              type="button"
              className="add-book-frame-button close-book-button"
              onClick={handleDelete}
              style={{ backgroundImage: `url(${closeBookFrame})` }}
            >
              Delete
            </button>

            <button
              type="button"
              className="add-book-frame-button add-story-button"
              onClick={handleSave}
              style={{ backgroundImage: `url(${addStoryFrame})` }}
            >
              Save Changes
            </button>
          </div>
        </div>
      </div>

      {experienceModalOpen && (
        <ReadingExperienceModal
          isOpen={experienceModalOpen}
          book={currentBook}
          experience={selectedExperience}
          onClose={() => {
            setExperienceModalOpen(false);
            setSelectedExperience(null);
          }}
          onSave={handleSaveExperience}
        />
      )}
    </>
  );
}

export default function BookDetailsModal(props: BookDetailsModalProps) {
  if (!props.isOpen || !props.book) return null;

  return (
    <BookDetailsModalContent
      key={props.book.id}
      book={props.book}
      onClose={props.onClose}
      onSave={props.onSave}
      onReadingExperiencesChange={props.onReadingExperiencesChange}
      onDelete={props.onDelete}
    />
  );
}
