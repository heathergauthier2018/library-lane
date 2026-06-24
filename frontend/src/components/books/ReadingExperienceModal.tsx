import { useEffect, useMemo, useState } from "react";
import type { MouseEvent } from "react";

import openBookBg from "../../assets/storybook/backgrounds/add-book-background.png";
import closeBookFrame from "../../assets/storybook/frames/close-book-frame.png";
import addStoryFrame from "../../assets/storybook/frames/add-story-frame.png";

import type { ReadingExperience, ShelfBook } from "../../pages/BooksPage";
import RatingDisplay from "./RatingDisplay";

import {
  getLedgerPages,
  ratingIcons,
  type FieldConfig,
  type LedgerSide as LedgerSideConfig,
  type NewBook,
  type Option,
  type RatingIcon,
} from "./bookLedgerConfig";

type ExperienceDraft = ReadingExperience &
  NewBook & {
    id: string;
    label: string;
    createdAt: string;
    [key: string]: string;
  };

type ReadingExperienceModalProps = {
  isOpen: boolean;
  book: ShelfBook;
  experience: ReadingExperience | null;
  onClose: () => void;
  onSave: (experience: ReadingExperience) => void;
};

function createBlankExperience(book: ShelfBook): ExperienceDraft {
  return {
    id: crypto.randomUUID(),
    label: "New Reading Experience",

    title: book.title,
    author: book.author,
    genre: book.genre,
    seriesName: book.seriesName,

    format: book.format || "PHYSICAL",
    readingStatus: "TO_READ",
    romancePresence: book.romancePresence || "NO",
    isSeries: book.isSeries || "NO",

    startDate: "",
    finishDate: "",
    currentPage: "",
    pageCount: book.pageCount || "",
    audioLength: book.audioLength || "",
    currentListeningPosition: "",
    listeningSpeed: "",
    narrator: "",

    excitementRating: "0",
    predictedRating: "0",
    currentRating: "0",
    currentExcitementRating: "0",
    initialRating: "0",
    finalRating: "0",
    rereadRating: "0",
    emotionalDevastationRating: "0",
    romanceRating: "0",
    spiceRating: "0",
    horrorRating: "0",

    notes: "",
    createdAt: new Date().toISOString(),
  };
}

function experienceToDraft(
  book: ShelfBook,
  experience: ReadingExperience | null
): ExperienceDraft {
  return {
    ...createBlankExperience(book),
    ...(experience ?? {}),
    title: book.title,
    author: book.author,
    genre: book.genre,
    seriesName: book.seriesName,
    romancePresence:
      experience?.romancePresence || book.romancePresence || "NO",
    isSeries:
      experience?.isSeries || book.isSeries || "NO",
  };
}

function hasReadableValue(value: string | undefined) {
  if (!value) return false;
  if (value === "0") return false;
  if (value === "NO") return false;
  if (value === "Not selected") return false;
  return value.trim().length > 0;
}

function formatReadonlyValue(field: FieldConfig, value: string) {
  if (field.type === "dropdown") {
    return field.options?.find((option) => option.value === value)?.label || value;
  }

  return value;
}

function FantasyDropdown({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: Option[];
  onChange: (value: string) => void;
}) {
  const [open, setOpen] = useState(false);

  const selected =
    options.find((option) => option.value === value)?.label ?? options[0]?.label;

  return (
    <div className="fantasy-dropdown-wrap">
      <span>{label}</span>

      <button
        type="button"
        className="fantasy-dropdown-button"
        onClick={() => setOpen((prev) => !prev)}
      >
        {selected}
        <span className="fantasy-dropdown-arrow">⌄</span>
      </button>

      {open && (
        <div className="fantasy-dropdown-menu">
          {options.map((option) => (
            <button
              type="button"
              key={option.value || option.label}
              className={option.value === value ? "selected" : ""}
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function IconRating({
  label,
  value,
  icon,
  ratingIcon,
  onChange,
}: {
  label: string;
  value: string;
  icon: string;
  ratingIcon: RatingIcon;
  onChange: (value: string) => void;
}) {
  const numericValue = Number(value || "0");

  function handleClick(rating: number, event: MouseEvent<HTMLButtonElement>) {
    const rect = event.currentTarget.getBoundingClientRect();
    const clickX = event.clientX - rect.left;
    const isHalf = clickX < rect.width / 2;

    onChange(String(isHalf ? rating - 0.5 : rating));
  }

  return (
    <div className={`ledger-icon-rating ledger-icon-rating-${ratingIcon}`}>
      <span>{label}</span>

      <div className="ledger-rating-icons">
        {[1, 2, 3, 4, 5].map((rating) => {
          const isFull = numericValue >= rating;
          const isHalf = numericValue === rating - 0.5;

          return (
            <button
              type="button"
              key={rating}
              className={`${isFull ? "selected" : ""} ${
                isHalf ? "half-selected" : ""
              }`}
              onClick={(event) => handleClick(rating, event)}
              aria-label={`${label} ${rating}`}
            >
              <img src={icon} alt="" />
            </button>
          );
        })}
      </div>
    </div>
  );
}

function ReadonlyRating({
  label,
  value,
  icon,
  ratingIcon,
}: {
  label: string;
  value: string;
  icon: string;
  ratingIcon: RatingIcon;
}) {
  const numericValue = Number(value || "0");

  if (!numericValue || Number.isNaN(numericValue)) return null;

  return (
    <div className={`ledger-icon-rating ledger-icon-rating-${ratingIcon}`}>
      <span>{label}</span>

      <RatingDisplay
        value={value}
        icon={icon}
        className="readonly-rating-icons"
      />
    </div>
  );
}

function RenderField({
  field,
  book,
  updateField,
  openPrompt,
  isEditing,
}: {
  field: FieldConfig;
  book: ExperienceDraft;
  updateField: (field: string, value: string) => void;
  openPrompt: (field: FieldConfig) => void;
  isEditing: boolean;
}) {
  if (field.showIf && !field.showIf(book as NewBook)) return null;

  const value = book[field.key] ?? "";

  if (!isEditing) {
    if (!hasReadableValue(value)) return null;

    if (field.type === "rating") {
      const ratingIcon = field.ratingIcon ?? "overall";

      return (
        <ReadonlyRating
          label={field.label}
          value={value}
          icon={ratingIcons[ratingIcon]}
          ratingIcon={ratingIcon}
        />
      );
    }

    if (field.type === "textarea" || field.type === "longPrompt") {
      return (
        <div className={`ledger-readonly-field ledger-field-${field.key}`}>
          <button
            type="button"
            className="ledger-prompt-title-button"
            onClick={() => openPrompt(field)}
          >
            {field.label}
          </button>

          <p className="ledger-readonly-text reader-handwriting">
            {formatReadonlyValue(field, value)}
          </p>
        </div>
      );
    }

    return (
      <div className={`ledger-readonly-field ledger-field-${field.key}`}>
        <span>{field.label}</span>
        <p className="ledger-readonly-text reader-handwriting">
          {formatReadonlyValue(field, value)}
        </p>
      </div>
    );
  }

  if (field.type === "dropdown") {
    return (
      <FantasyDropdown
        label={field.label}
        value={value}
        options={field.options ?? [{ label: "Not selected", value: "" }]}
        onChange={(nextValue) => updateField(field.key, nextValue)}
      />
    );
  }

  if (field.type === "rating") {
    const ratingIcon = field.ratingIcon ?? "overall";

    return (
      <IconRating
        label={field.label}
        value={value}
        icon={ratingIcons[ratingIcon]}
        ratingIcon={ratingIcon}
        onChange={(nextValue) => updateField(field.key, nextValue)}
      />
    );
  }

  if (field.type === "textarea" || field.type === "longPrompt") {
    return (
      <label
        className={`${
          field.type === "longPrompt" ? "ledger-long-prompt" : ""
        } ledger-field-${field.key}`}
      >
        <button
          type="button"
          className="ledger-prompt-title-button"
          onClick={() => openPrompt(field)}
        >
          {field.label}
        </button>

        <textarea
          rows={field.rows ?? 3}
          placeholder={field.placeholder}
          value={value}
          onChange={(event) => updateField(field.key, event.target.value)}
        />
      </label>
    );
  }

  return (
    <label>
      <span>{field.label}</span>
      <input
        placeholder={field.placeholder}
        value={value}
        onChange={(event) => updateField(field.key, event.target.value)}
      />
    </label>
  );
}

function LedgerSide({
  side,
  className,
  book,
  updateField,
  openPrompt,
  isEditing,
}: {
  side: LedgerSideConfig;
  className: string;
  book: ExperienceDraft;
  updateField: (field: string, value: string) => void;
  openPrompt: (field: FieldConfig) => void;
  isEditing: boolean;
}) {
  const visibleFields = side.fields.filter((field) => {
    if (field.showIf && !field.showIf(book as NewBook)) return false;

    if (isEditing) return true;

    const value = book[field.key] ?? "";
    return hasReadableValue(value);
  });

  return (
    <section className={`add-book-page ${className}`}>
      {side.eyebrow && <p className="add-book-eyebrow">{side.eyebrow}</p>}
      {side.title && <h2>{side.title}</h2>}

      {visibleFields.length === 0 && !isEditing ? (
        <p className="focused-writing-prompt">
          No notes have been added to this page yet.
        </p>
      ) : (
        visibleFields.map((field) => (
          <RenderField
            key={field.key}
            field={field}
            book={book}
            updateField={updateField}
            openPrompt={openPrompt}
            isEditing={isEditing}
          />
        ))
      )}
    </section>
  );
}

export default function ReadingExperienceModal({
  isOpen,
  book,
  experience,
  onClose,
  onSave,
}: ReadingExperienceModalProps) {
  const [draftExperience, setDraftExperience] =
    useState<ExperienceDraft | null>(null);
  const [ledgerPage, setLedgerPage] = useState(0);
  const [focusedPrompt, setFocusedPrompt] = useState<FieldConfig | null>(null);
  const [isEditing, setIsEditing] = useState(!experience);

  useEffect(() => {
  setDraftExperience(experienceToDraft(book, experience));
  setFocusedPrompt(null);
  setIsEditing(!experience);

  if (!experience) {
    setLedgerPage(0);
  }
}, [book, experience]);

  const ledgerPages = useMemo(() => {
    if (!draftExperience) return [];
    return getLedgerPages(draftExperience as NewBook);
  }, [draftExperience]);

  if (!isOpen || !draftExperience || ledgerPages.length === 0) return null;

  const currentPage = ledgerPages[Math.min(ledgerPage, ledgerPages.length - 1)];
  const isFirstPage = ledgerPage === 0;
  const isLastPage = ledgerPage === ledgerPages.length - 1;

  function updateField(field: string, value: string) {
    setDraftExperience((prev) =>
      prev
        ? {
            ...prev,
            [field]: value,
          }
        : prev
    );
  }

  function handleSave() {
    const currentExperience = draftExperience;
    if (!currentExperience) return;

    const savedExperience: ReadingExperience = {
      ...currentExperience,
      label:
        currentExperience.label?.trim() ||
        experience?.label ||
        "Reading Experience",
      format: currentExperience.format || "PHYSICAL",
      readingStatus: currentExperience.readingStatus || "TO_READ",
      createdAt: currentExperience.createdAt || new Date().toISOString(),
    };

    setDraftExperience(savedExperience as ExperienceDraft);
setIsEditing(false);
onSave(savedExperience);
  }

  if (focusedPrompt) {
    const value = draftExperience[focusedPrompt.key] ?? "";

    return (
      <div className="add-book-overlay">
        <div
          className="add-book-ledger add-book-focused-ledger reader-handwriting"
          style={{ backgroundImage: `url(${openBookBg})` }}
        >
          <section className="add-book-page add-book-left-page focused-writing-left">
            <p className="add-book-eyebrow">Experience Journal</p>
            <h2>{focusedPrompt.label}</h2>
            <p className="focused-writing-prompt">{focusedPrompt.placeholder}</p>
          </section>

          <section className="add-book-page add-book-right-page focused-writing-right">
            {isEditing ? (
              <textarea
                className="focused-writing-area reader-handwriting"
                value={value}
                onChange={(event) =>
                  updateField(focusedPrompt.key, event.target.value)
                }
                placeholder="Write as much as you want..."
              />
            ) : (
              <div className="focused-writing-area focused-writing-readonly reader-handwriting">
                {value}
              </div>
            )}
          </section>

          <div className="add-book-actions add-book-actions-focused">
            <button
              type="button"
              className="add-book-frame-button add-story-button"
              onClick={() => setFocusedPrompt(null)}
              style={{ backgroundImage: `url(${addStoryFrame})` }}
            >
              Return
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="add-book-overlay">
      <div
        className={`add-book-ledger reader-handwriting ${
          isEditing ? "ledger-edit-mode" : "ledger-read-mode"
        }`}
        style={{ backgroundImage: `url(${openBookBg})` }}
      >
        <LedgerSide
          side={currentPage.left}
          className="add-book-left-page"
          book={draftExperience}
          updateField={updateField}
          openPrompt={setFocusedPrompt}
          isEditing={isEditing}
        />

        <LedgerSide
          side={currentPage.right}
          className="add-book-right-page"
          book={draftExperience}
          updateField={updateField}
          openPrompt={setFocusedPrompt}
          isEditing={isEditing}
        />

        <div className="add-book-page-count">
          Page {ledgerPage + 1} of {ledgerPages.length}
        </div>

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
                onClick={() => setLedgerPage((prev) => Math.max(prev - 1, 0))}
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
                  setLedgerPage((prev) =>
                    Math.min(prev + 1, ledgerPages.length - 1)
                  )
                }
                style={{ backgroundImage: `url(${closeBookFrame})` }}
              >
                Turn Page
              </button>
            )}
          </div>
        </div>

        <div className="add-book-actions add-book-actions-right">
          {isEditing ? (
            <button
              type="button"
              className="add-book-frame-button add-story-button"
              onClick={handleSave}
              style={{ backgroundImage: `url(${addStoryFrame})` }}
            >
              Save Changes
            </button>
          ) : (
            <button
              type="button"
              className="add-book-frame-button add-story-button"
              onClick={() => setIsEditing(true)}
              style={{ backgroundImage: `url(${addStoryFrame})` }}
            >
              Edit Experience
            </button>
          )}
        </div>
      </div>
    </div>
  );
}