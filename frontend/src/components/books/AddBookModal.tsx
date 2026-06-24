import { useMemo, useState } from "react";
import type { MouseEvent } from "react";

import openBookBg from "../../assets/storybook/backgrounds/add-book-background.png";
import closeBookFrame from "../../assets/storybook/frames/close-book-frame.png";
import addStoryFrame from "../../assets/storybook/frames/add-story-frame.png";

import {
  getLedgerPages,
  ratingIcons,
  type FieldConfig,
  type LedgerSide as LedgerSideConfig,
  type NewBook,
  type Option,
  type RatingIcon,
} from "./bookLedgerConfig";

export type { NewBook } from "./bookLedgerConfig";

type AddBookModalProps = {
  isOpen: boolean;
  onClose: () => void;
  onSave: (book: NewBook) => void;
};

function getBlankBook(): NewBook {
  return {
    title: "",
    author: "",
    genre: "",
    format: "PHYSICAL",
    readingStatus: "TO_READ",
    isSeries: "NO",
    romancePresence: "NO",
    romanceImportance: "",
    wouldRecommend: "",
    hasBookHookedYou: "",
    isPageTurner: "",
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
  };
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

function RenderField({
  field,
  book,
  updateField,
  openPrompt,
}: {
  field: FieldConfig;
  book: NewBook;
  updateField: (field: string, value: string) => void;
  openPrompt: (field: FieldConfig) => void;
}) {
  if (field.showIf && !field.showIf(book)) return null;

  const value = book[field.key] ?? "";

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
}: {
  side: LedgerSideConfig;
  className: string;
  book: NewBook;
  updateField: (field: string, value: string) => void;
  openPrompt: (field: FieldConfig) => void;
}) {
  return (
    <section className={`add-book-page ${className}`}>
      {side.eyebrow && <p className="add-book-eyebrow">{side.eyebrow}</p>}
      {side.title && <h2>{side.title}</h2>}

      {side.fields.map((field) => (
        <RenderField
          key={field.key}
          field={field}
          book={book}
          updateField={updateField}
          openPrompt={openPrompt}
        />
      ))}
    </section>
  );
}

export default function AddBookModal({
  isOpen,
  onClose,
  onSave,
}: AddBookModalProps) {
  const [book, setBook] = useState<NewBook>(getBlankBook);
  const [ledgerPage, setLedgerPage] = useState(0);
  const [focusedPrompt, setFocusedPrompt] = useState<FieldConfig | null>(null);

  const ledgerPages = useMemo(() => {
    return getLedgerPages(book);
  }, [book]);

  if (!isOpen) return null;

  const currentPage = ledgerPages[Math.min(ledgerPage, ledgerPages.length - 1)];
  const isFirstPage = ledgerPage === 0;
  const isLastPage = ledgerPage === ledgerPages.length - 1;

  function updateField(field: string, value: string) {
    setBook((prev) => ({
      ...prev,
      [field]: value,
    }));
  }

  function handleSave() {
    if (!book.title.trim()) return;

    onSave(book);
    setBook(getBlankBook());
    setLedgerPage(0);
    setFocusedPrompt(null);
    onClose();
  }

  function closeModal() {
    setBook(getBlankBook());
    setLedgerPage(0);
    setFocusedPrompt(null);
    onClose();
  }

  if (focusedPrompt) {
    const value = book[focusedPrompt.key] ?? "";

    return (
      <div className="add-book-overlay">
        <div
          className="add-book-ledger add-book-focused-ledger"
          style={{ backgroundImage: `url(${openBookBg})` }}
        >
          <section className="add-book-page add-book-left-page focused-writing-left">
            <p className="add-book-eyebrow">Journal Within The Journal</p>
            <h2>{focusedPrompt.label}</h2>
            <p className="focused-writing-prompt">{focusedPrompt.placeholder}</p>
          </section>

          <section className="add-book-page add-book-right-page focused-writing-right">
            <textarea
              className="focused-writing-area"
              value={value}
              onChange={(event) =>
                updateField(focusedPrompt.key, event.target.value)
              }
              placeholder="Write as much as you want..."
            />
          </section>

          <div className="add-book-actions add-book-actions-focused">
            <button
              type="button"
              className="add-book-frame-button add-story-button"
              onClick={() => setFocusedPrompt(null)}
              style={{ backgroundImage: `url(${addStoryFrame})` }}
            >
              Save & Return
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="add-book-overlay">
      <div
        className="add-book-ledger"
        style={{ backgroundImage: `url(${openBookBg})` }}
      >
        <LedgerSide
          side={currentPage.left}
          className="add-book-left-page"
          book={book}
          updateField={updateField}
          openPrompt={setFocusedPrompt}
        />

        <LedgerSide
          side={currentPage.right}
          className="add-book-right-page"
          book={book}
          updateField={updateField}
          openPrompt={setFocusedPrompt}
        />

        <div className="add-book-page-count">
          Page {ledgerPage + 1} of {ledgerPages.length}
        </div>

        <div className="add-book-actions add-book-actions-left">
          <button
            type="button"
            className="add-book-frame-button close-book-button"
            onClick={closeModal}
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
          <button
            type="button"
            className="add-book-frame-button add-story-button"
            onClick={handleSave}
            style={{ backgroundImage: `url(${addStoryFrame})` }}
          >
            Add Story to Library
          </button>
        </div>
      </div>
    </div>
  );
}