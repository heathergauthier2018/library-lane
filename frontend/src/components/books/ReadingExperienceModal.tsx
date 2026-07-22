import {
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import type { CSSProperties, FocusEvent, MouseEvent } from "react";

import openBookBg from "../../assets/storybook/backgrounds/add-book-background.png";
import closeBookFrame from "../../assets/storybook/frames/close-book-frame.png";
import addStoryFrame from "../../assets/storybook/frames/add-story-frame.png";
import writingQuill from "../../assets/storybook/decor/library-lane-quill-v2.png";

import type { ReadingExperience, ShelfBook } from "../../pages/BooksPage";
import RatingDisplay from "./RatingDisplay";
import {
  catalogApi,
  type CatalogBookFormat,
  type CatalogBookResult,
  type CatalogSearchField,
} from "../../api/libraryLaneApi";

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
    promptResponsesJson?: string;
    [key: string]: string | undefined;
  };

type ReadingExperienceModalProps = {
  isOpen: boolean;
  book: ShelfBook;
  experience: ReadingExperience | null;
  onClose: () => void;
  onSave: (experience: ReadingExperience) => Promise<void>;
};

function catalogSearchField(field: string): CatalogSearchField {
  if (field === "author") return "AUTHOR";
  if (field === "seriesName") return "SERIES";
  return "TITLE";
}

function usefulGenres(genres: string[]) {
  const cleaned = genres
    .map((genre) => genre.replaceAll("_", " ").trim())
    .filter(
      (genre) =>
        genre.length > 0 &&
        !/^nyt:/i.test(genre) &&
        !/^serie?s?:/i.test(genre) &&
        !/new york times bestseller/i.test(genre) &&
        !/combined[- ]print/i.test(genre)
    );
  const specific = cleaned.filter(
    (genre) => !["fiction", "juvenile fiction"].includes(genre.toLowerCase())
  );
  return (specific.length ? specific : cleaned).join(", ");
}

function displaySeriesName(value?: string | null) {
  if (!value) return "";
  return value
    .replaceAll("_", " ")
    .replace(/^serie?s?:\s*/i, "")
    .replace(/\s+/g, " ")
    .trim()
    .split(" ")
    .map((word) =>
      /^(and|of|the|in|on|a|an)$/i.test(word)
        ? word.toLowerCase()
        : word.charAt(0).toUpperCase() + word.slice(1)
    )
    .join(" ")
    .replace(/^the\b/i, "The");
}

function frontendFormat(format?: CatalogBookFormat) {
  if (format === "EBOOK") return "EBOOK";
  if (format === "AUDIOBOOK") return "AUDIOBOOK";
  return "PHYSICAL";
}

function durationLabel(totalSeconds?: number | null) {
  if (!totalSeconds) return "";
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.round((totalSeconds % 3600) / 60);
  return [hours ? `${hours}h` : "", minutes ? `${minutes}m` : ""]
    .filter(Boolean)
    .join(" ");
}

function prefersReducedMotion() {
  return (
    typeof window !== "undefined" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

function safeParsePromptResponses(value?: string) {
  if (!value) return {};

  try {
    const parsed = JSON.parse(value);

    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }

    return parsed as Record<string, string>;
  } catch {
    return {};
  }
}

function createBlankExperience(book: ShelfBook): ExperienceDraft {
  return {
    id: "",
    label: "New Reading Experience",

    title: book.title,
    author: book.author,
    genre: book.genre,
    seriesName: book.seriesName,
    seriesNumber: book.seriesNumber || "",
    subtitle: book.subtitle || "",
    description: book.description || "",
    publisher: book.publisher || "",
    publisherOther: book.publisherOther || "",
    publicationYear: book.publicationYear || "",
    editionFormat: book.editionFormat || "",
    narrator: book.narrator || "",
    language: book.language || "",
    isbn10: book.isbn10 || "",
    isbn13: book.isbn13 || "",
    coverUrl: book.coverUrl || "",
    catalogProvider: book.catalogProvider || "",
    catalogProviderId: book.catalogProviderId || "",

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
    promptResponsesJson: "",
    createdAt: new Date().toISOString(),
  };
}

function experienceToDraft(
  book: ShelfBook,
  experience: ReadingExperience | null
): ExperienceDraft {
  const promptResponses = safeParsePromptResponses(
    experience?.promptResponsesJson
  );

  return {
    ...createBlankExperience(book),
    ...promptResponses,
    ...(experience ?? {}),
    title: book.title,
    author: book.author,
    genre: book.genre,
    seriesName: book.seriesName,
    romancePresence:
      experience?.romancePresence || book.romancePresence || "NO",
    isSeries: experience?.isSeries || book.isSeries || "NO",
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
    return (
      field.options?.find((option) => option.value === value)?.label || value
    );
  }

  return value;
}

function collectPromptResponses(draft: ExperienceDraft) {
  const pages = getLedgerPages(draft as NewBook);
  const promptKeys = new Set<string>();

  pages.forEach((page) => {
    [...page.left.fields, ...page.right.fields].forEach((field) => {
      if (field.type === "textarea" || field.type === "longPrompt") {
        promptKeys.add(field.key);
      }
    });
  });

  const responses: Record<string, string> = {};

  promptKeys.forEach((key) => {
    const value = draft[key];

    if (typeof value === "string" && value.trim().length > 0) {
      responses[key] = value;
    }
  });

  return responses;
}

function LivingQuillInput({
  value,
  placeholder,
  onChange,
  onFocus,
}: {
  value: string;
  placeholder?: string;
  onChange: (value: string) => void;
  onFocus?: (event: FocusEvent<HTMLInputElement>) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [focused, setFocused] = useState(false);
  const [quillPosition, setQuillPosition] = useState({ x: 5, y: 18 });
  const [quillAtRightEdge, setQuillAtRightEdge] = useState(false);
  const [isWriting, setIsWriting] = useState(false);
  const writingTimerRef = useRef<number | null>(null);

  function keepQuillWriting() {
    setIsWriting(true);
    if (writingTimerRef.current !== null) {
      window.clearTimeout(writingTimerRef.current);
    }
    writingTimerRef.current = window.setTimeout(() => {
      setIsWriting(false);
      writingTimerRef.current = null;
    }, 420);
  }

  function positionQuill() {
    const input = inputRef.current;
    if (!input) return;

    const caret = input.selectionStart ?? input.value.length;
    const style = window.getComputedStyle(input);
    const mirror = document.createElement("span");
    const paddingLeft = Number.parseFloat(style.paddingLeft) || 0;

    mirror.style.position = "fixed";
    mirror.style.visibility = "hidden";
    mirror.style.pointerEvents = "none";
    mirror.style.top = "-9999px";
    mirror.style.left = "0";
    mirror.style.whiteSpace = "pre";
    mirror.style.fontFamily = style.fontFamily;
    mirror.style.fontSize = style.fontSize;
    mirror.style.fontStyle = style.fontStyle;
    mirror.style.fontWeight = style.fontWeight;
    mirror.style.letterSpacing = style.letterSpacing;
    mirror.style.wordSpacing = style.wordSpacing;
    mirror.style.textTransform = style.textTransform;
    mirror.textContent = input.value.slice(0, caret);
    document.body.appendChild(mirror);
    const textWidth = mirror.getBoundingClientRect().width;
    mirror.remove();

    const fontSize = Number.parseFloat(style.fontSize) || 20;
    const lineHeight = Number.parseFloat(style.lineHeight) || fontSize * 1.2;
    const paddingTop = Number.parseFloat(style.paddingTop) || 0;
    const caretX = paddingLeft + textWidth - input.scrollLeft;

    setQuillAtRightEdge(caretX > input.clientWidth - 118);
    setQuillPosition({
      x: Math.min(Math.max(caretX, 2), input.clientWidth - 2),
      y: paddingTop + lineHeight * 0.8,
    });
  }

  useLayoutEffect(() => {
    if (!focused) return;
    positionQuill();
  }, [focused, value]);

  useEffect(() => {
    return () => {
      if (writingTimerRef.current !== null) {
        window.clearTimeout(writingTimerRef.current);
      }
    };
  }, []);

  return (
    <span
      className={`ledger-quill-input-shell ${focused ? "is-focused" : ""}`}
      style={
        {
          "--quill-caret-x": `${quillPosition.x}px`,
          "--quill-caret-y": `${quillPosition.y}px`,
        } as CSSProperties
      }
    >
      <input
        ref={inputRef}
        placeholder={placeholder}
        value={value}
        autoComplete="off"
        onFocus={(event) => {
          setFocused(true);
          window.requestAnimationFrame(positionQuill);
          onFocus?.(event);
        }}
        onBlur={() => setFocused(false)}
        onClick={positionQuill}
        onKeyUp={positionQuill}
        onSelect={positionQuill}
        onChange={(event) => {
          onChange(event.target.value);
          keepQuillWriting();
          window.requestAnimationFrame(positionQuill);
        }}
      />

      {focused && value.length > 0 && !prefersReducedMotion() && (
        <img
          className={`ledger-caret-quill ${
            quillAtRightEdge ? "is-near-right-edge" : ""
          } ${isWriting ? "is-writing" : ""}`}
          src={writingQuill}
          alt=""
          aria-hidden="true"
        />
      )}
    </span>
  );
}

function LivingQuillTextarea({
  value,
  placeholder,
  rows,
  className,
  onChange,
}: {
  value: string;
  placeholder?: string;
  rows?: number;
  className?: string;
  onChange: (value: string) => void;
}) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const [focused, setFocused] = useState(false);
  const [quillPosition, setQuillPosition] = useState({ x: 5, y: 24 });
  const [quillAtRightEdge, setQuillAtRightEdge] = useState(false);
  const [isWriting, setIsWriting] = useState(false);
  const writingTimerRef = useRef<number | null>(null);

  function keepQuillWriting() {
    setIsWriting(true);
    if (writingTimerRef.current !== null) {
      window.clearTimeout(writingTimerRef.current);
    }
    writingTimerRef.current = window.setTimeout(() => {
      setIsWriting(false);
      writingTimerRef.current = null;
    }, 420);
  }

  function positionQuill() {
    const textarea = textareaRef.current;
    if (!textarea) return;

    const style = window.getComputedStyle(textarea);
    const caret = textarea.selectionStart ?? textarea.value.length;
    const mirror = document.createElement("div");
    const marker = document.createElement("span");
    const properties = [
      "boxSizing",
      "width",
      "height",
      "paddingTop",
      "paddingRight",
      "paddingBottom",
      "paddingLeft",
      "borderTopWidth",
      "borderRightWidth",
      "borderBottomWidth",
      "borderLeftWidth",
      "fontFamily",
      "fontSize",
      "fontStyle",
      "fontWeight",
      "lineHeight",
      "letterSpacing",
      "textAlign",
      "textTransform",
      "wordSpacing",
      "tabSize",
    ] as const;

    mirror.style.position = "fixed";
    mirror.style.visibility = "hidden";
    mirror.style.pointerEvents = "none";
    mirror.style.top = "-9999px";
    mirror.style.left = "0";
    mirror.style.whiteSpace = "pre-wrap";
    mirror.style.overflowWrap = "break-word";
    properties.forEach((property) => {
      mirror.style[property] = style[property];
    });

    mirror.textContent = textarea.value.slice(0, caret);
    marker.textContent = textarea.value.slice(caret, caret + 1) || "\u200b";
    mirror.appendChild(marker);
    document.body.appendChild(mirror);

    const lineHeight =
      Number.parseFloat(style.lineHeight) ||
      Number.parseFloat(style.fontSize) * 1.2;
    const x = marker.offsetLeft - textarea.scrollLeft;
    const y = marker.offsetTop - textarea.scrollTop + lineHeight * 0.78;
    mirror.remove();

    setQuillAtRightEdge(x > textarea.clientWidth - 118);
    setQuillPosition({
      x: Math.min(Math.max(x, 5), textarea.clientWidth - 5),
      y: Math.min(Math.max(y, 10), textarea.clientHeight - 7),
    });
  }

  useLayoutEffect(() => {
    if (!focused) return;
    positionQuill();
  }, [focused, value]);

  useEffect(() => {
    return () => {
      if (writingTimerRef.current !== null) {
        window.clearTimeout(writingTimerRef.current);
      }
    };
  }, []);

  return (
    <span
      className={`ledger-quill-input-shell ledger-quill-textarea-shell ${
        focused ? "is-focused" : ""
      }`}
      style={
        {
          "--quill-caret-x": `${quillPosition.x}px`,
          "--quill-caret-y": `${quillPosition.y}px`,
        } as CSSProperties
      }
    >
      <textarea
        ref={textareaRef}
        className={className}
        rows={rows}
        placeholder={placeholder}
        value={value}
        onFocus={() => {
          setFocused(true);
          window.requestAnimationFrame(positionQuill);
        }}
        onBlur={() => setFocused(false)}
        onClick={positionQuill}
        onKeyUp={positionQuill}
        onSelect={positionQuill}
        onScroll={positionQuill}
        onChange={(event) => {
          onChange(event.target.value);
          keepQuillWriting();
          window.requestAnimationFrame(positionQuill);
        }}
      />

      {focused && value.length > 0 && !prefersReducedMotion() && (
        <img
          className={`ledger-caret-quill ${
            quillAtRightEdge ? "is-near-right-edge" : ""
          } ${isWriting ? "is-writing" : ""}`}
          src={writingQuill}
          alt=""
          aria-hidden="true"
        />
      )}
    </span>
  );
}

function FantasyDropdown({
  dropdownId,
  label,
  value,
  options,
  onChange,
  isOpen,
  onToggle,
}: {
  dropdownId: string;
  label: string;
  value: string;
  options: Option[];
  onChange: (value: string) => void;
  isOpen: boolean;
  onToggle: (dropdownId: string) => void;
}) {
  const selected =
    options.find((option) => option.value === value)?.label ?? "Not selected";
  const [writingTarget, setWritingTarget] = useState<Option | null>(null);
  const [typedSelection, setTypedSelection] = useState("");
  const [writingPhase, setWritingPhase] = useState<"idle" | "turning" | "writing">(
    "idle"
  );
  const writingTextRef = useRef<HTMLSpanElement>(null);
  const dropdownButtonRef = useRef<HTMLButtonElement>(null);
  const intervalRef = useRef<number | null>(null);
  const timeoutRefs = useRef<number[]>([]);
  const [writingPosition, setWritingPosition] = useState({ x: 5, y: 18 });

  useEffect(() => {
    return () => {
      if (intervalRef.current !== null) window.clearInterval(intervalRef.current);
      timeoutRefs.current.forEach((timer) => window.clearTimeout(timer));
    };
  }, []);

  useLayoutEffect(() => {
    const text = writingTextRef.current;
    const button = dropdownButtonRef.current;
    if (!text || !button) return;

    const style = window.getComputedStyle(text);
    const fontSize = Number.parseFloat(style.fontSize) || 20;
    const lineHeight = Number.parseFloat(style.lineHeight) || fontSize * 1.2;

    setWritingPosition({
      x: text.offsetLeft + text.offsetWidth,
      y: text.offsetTop + lineHeight * 0.8,
    });
  }, [typedSelection, writingTarget]);

  function chooseOption(option: Option) {
    if (prefersReducedMotion()) {
      onChange(option.value);
      onToggle(dropdownId);
      return;
    }

    setWritingTarget(option);
    setTypedSelection("");
    setWritingPhase("turning");

    const turnTimer = window.setTimeout(() => {
      setWritingPhase("writing");
      let character = 0;

      intervalRef.current = window.setInterval(() => {
        character += 1;
        setTypedSelection(option.label.slice(0, character));

        if (character >= option.label.length) {
          if (intervalRef.current !== null) {
            window.clearInterval(intervalRef.current);
            intervalRef.current = null;
          }

          onChange(option.value);
          const finishTimer = window.setTimeout(() => {
            setWritingTarget(null);
            setTypedSelection("");
            setWritingPhase("idle");
            onToggle(dropdownId);
          }, 420);
          timeoutRefs.current.push(finishTimer);
        }
      }, 205);
    }, 190);

    timeoutRefs.current.push(turnTimer);
  }

  return (
    <div
      className={`fantasy-dropdown-wrap ${
        writingTarget ? "quill-writing-selection" : ""
      }`}
    >
      <span>{label}</span>

      <button
        ref={dropdownButtonRef}
        type="button"
        className={`fantasy-dropdown-button quill-phase-${writingPhase}`}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        onClick={() => {
          if (!writingTarget) onToggle(dropdownId);
        }}
        style={
          {
            "--quill-writing-x": `${writingPosition.x}px`,
            "--quill-writing-y": `${writingPosition.y}px`,
          } as CSSProperties
        }
      >
        <span ref={writingTextRef} className="fantasy-dropdown-display">
          {writingTarget ? typedSelection || "\u00a0" : selected}
        </span>

        {writingTarget && (
          <img
            className={`dropdown-writing-quill ${
              writingPhase === "writing" ? "is-writing" : ""
            }`}
            src={writingQuill}
            alt=""
            aria-hidden="true"
          />
        )}
        <span className="fantasy-dropdown-arrow">⌄</span>
      </button>

      {isOpen && !writingTarget && (
        <div className="fantasy-dropdown-menu" role="listbox">
          {options.map((option) => (
            <button
              type="button"
              key={option.value || option.label}
              className={option.value === value ? "selected" : ""}
              role="option"
              aria-selected={option.value === value}
              onClick={() => chooseOption(option)}
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

function CatalogResults({
  searching,
  message,
  results,
  query,
  searchLabel,
  onSelect,
}: {
  searching: boolean;
  message: string;
  results: CatalogBookResult[];
  query: string;
  searchLabel: CatalogSearchField;
  onSelect: (result: CatalogBookResult) => void;
}) {
  if (!searching && !message && results.length === 0) return null;

  const resultLabel =
    results.length === 1
      ? "1 catalog match"
      : `${results.length} catalog matches`;

  return (
    <aside
      className={`catalog-search-panel ${
        results.length > 0
          ? "catalog-search-panel-results"
          : "catalog-search-panel-message"
      }`}
      aria-live="polite"
    >
      <header className="catalog-index-masthead">
        <span>Library Lane Card Catalog</span>
        <small>
          {searchLabel}: <b>{query}</b>
        </small>
      </header>

      {searching && (
        <div className="catalog-search-state catalog-search-state-loading">
          <span className="catalog-state-ornament" aria-hidden="true">
            ⌁
          </span>
          <p>Consulting the catalog…</p>
          <small>Searching the shelves for matching records</small>
          <span className="catalog-search-dots" aria-hidden="true">
            <i />
            <i />
            <i />
          </span>
        </div>
      )}

      {!searching && message && (
        <div className="catalog-search-state catalog-search-state-empty">
          <span className="catalog-state-ornament" aria-hidden="true">
            ⌁
          </span>
          <p>{message}</p>
        </div>
      )}

      {!searching && results.length > 0 && (
        <div className="catalog-results-heading" aria-hidden="true">
          <span>From the catalog</span>
          <small>{resultLabel}</small>
        </div>
      )}

      {!searching &&
        results.map((result) => (
          <button
            type="button"
            className="catalog-search-result"
            key={`${result.provider}-${result.providerId}-${result.format}`}
            onClick={() => onSelect(result)}
          >
            <span className="catalog-cover-frame" aria-hidden="true">
              {result.coverImageUrl ? (
                <img src={result.coverImageUrl} alt="" />
              ) : (
                <span className="catalog-cover-placeholder" />
              )}
            </span>

            <span className="catalog-result-copy">
              <strong>{result.title}</strong>
              <span>{result.authors.join(", ") || "Unknown author"}</span>
              <small>
                {[result.editionFormat, result.publisher, result.publicationDate]
                  .filter(Boolean)
                  .join(" • ")}
              </small>
              <em>{result.format.replace("EBOOK", "E-BOOK")}</em>
            </span>
          </button>
        ))}

      <div className="catalog-index-signature" aria-hidden="true">
        <span>LL</span>
        <small>Records summoned for this ledger</small>
      </div>
    </aside>
  );
}

function RenderField({
  field,
  book,
  updateField,
  openPrompt,
  isEditing,
  activeCatalogField,
  setActiveCatalogField,
  catalogSearching,
  catalogMessage,
  catalogResults,
  applyCatalogResult,
  openDropdown,
  toggleDropdown,
}: {
  field: FieldConfig;
  book: ExperienceDraft;
  updateField: (field: string, value: string) => void;
  openPrompt: (field: FieldConfig) => void;
  isEditing: boolean;
  activeCatalogField: string;
  setActiveCatalogField: (field: string) => void;
  catalogSearching: boolean;
  catalogMessage: string;
  catalogResults: CatalogBookResult[];
  applyCatalogResult: (result: CatalogBookResult) => void;
  openDropdown: string;
  toggleDropdown: (dropdownId: string) => void;
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
        dropdownId={field.key}
        label={field.label}
        value={value}
        options={field.options ?? [{ label: "Not selected", value: "" }]}
        onChange={(nextValue) => updateField(field.key, nextValue)}
        isOpen={openDropdown === field.key}
        onToggle={toggleDropdown}
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

        <LivingQuillTextarea
          rows={field.rows ?? 3}
          placeholder={field.placeholder}
          value={value}
          onChange={(nextValue) => updateField(field.key, nextValue)}
        />
      </label>
    );
  }

  const isCatalogField = ["title", "author", "seriesName"].includes(field.key);

  return (
    <div
      className={`ledger-input-field ledger-field-${field.key} ${
        isCatalogField ? "catalog-search-field" : ""
      } ${
        isCatalogField && activeCatalogField === field.key
          ? "catalog-search-field-active"
          : ""
      }`}
    >
      <label>
        <span>{field.label}</span>
        <LivingQuillInput
          placeholder={field.placeholder}
          value={value}
          onFocus={() => {
            if (isCatalogField) {
              toggleDropdown("");
              setActiveCatalogField(field.key);
              updateField(field.key, value);
            }
          }}
          onChange={(nextValue) => updateField(field.key, nextValue)}
        />
      </label>

      {isCatalogField && activeCatalogField === field.key && (
        <CatalogResults
          searching={catalogSearching}
          message={catalogMessage}
          results={catalogResults}
          query={value}
          searchLabel={catalogSearchField(field.key)}
          onSelect={applyCatalogResult}
        />
      )}
    </div>
  );
}

function LedgerSide({
  side,
  className,
  book,
  updateField,
  openPrompt,
  isEditing,
  activeCatalogField,
  setActiveCatalogField,
  catalogSearching,
  catalogMessage,
  catalogResults,
  applyCatalogResult,
  openDropdown,
  toggleDropdown,
  pageNumber,
  totalPages,
}: {
  side: LedgerSideConfig;
  className: string;
  book: ExperienceDraft;
  updateField: (field: string, value: string) => void;
  openPrompt: (field: FieldConfig) => void;
  isEditing: boolean;
  activeCatalogField: string;
  setActiveCatalogField: (field: string) => void;
  catalogSearching: boolean;
  catalogMessage: string;
  catalogResults: CatalogBookResult[];
  applyCatalogResult: (result: CatalogBookResult) => void;
  openDropdown: string;
  toggleDropdown: (dropdownId: string) => void;
  pageNumber: number;
  totalPages: number;
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
            activeCatalogField={activeCatalogField}
            setActiveCatalogField={setActiveCatalogField}
            catalogSearching={catalogSearching}
            catalogMessage={catalogMessage}
            catalogResults={catalogResults}
            applyCatalogResult={applyCatalogResult}
            openDropdown={openDropdown}
            toggleDropdown={toggleDropdown}
          />
        ))
      )}

      <span className="ledger-page-number">
        {pageNumber} of {totalPages}
      </span>
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
  const [catalogQuery, setCatalogQuery] = useState("");
  const [activeCatalogField, setActiveCatalogField] = useState("");
  const [catalogResults, setCatalogResults] = useState<CatalogBookResult[]>([]);
  const [catalogSearching, setCatalogSearching] = useState(false);
  const [catalogMessage, setCatalogMessage] = useState("");
  const [openDropdown, setOpenDropdown] = useState("");

  useEffect(() => {
    setDraftExperience(experienceToDraft(book, experience));
    setFocusedPrompt(null);
    setIsEditing(!experience);
    setCatalogQuery("");
    setActiveCatalogField("");
    setCatalogResults([]);
    setCatalogSearching(false);
    setCatalogMessage("");
    setOpenDropdown("");

    if (!experience) {
      setLedgerPage(0);
    }
  }, [book, experience]);

  useEffect(() => {
    if (!isEditing) return;

    const query = catalogQuery.trim();
    if (query.length < 3 || !activeCatalogField) {
      setCatalogResults([]);
      setCatalogMessage("");
      setCatalogSearching(false);
      return;
    }

    let active = true;
    const timeout = window.setTimeout(async () => {
      setCatalogSearching(true);
      setCatalogMessage("");

      try {
        const results = await catalogApi.searchBooks(
          query,
          undefined,
          catalogSearchField(activeCatalogField)
        );

        if (!active) return;
        setCatalogResults(results);
        setCatalogMessage(
          results.length === 0
            ? catalogSearchField(activeCatalogField) === "SERIES"
              ? "We couldn't find a matching series. You can enter it manually."
              : catalogSearchField(activeCatalogField) === "AUTHOR"
                ? "No matching authors or stories were found. You can continue with manual entry."
                : "No matching stories were found. You can continue with manual entry."
            : ""
        );
      } catch (error) {
        if (!active) return;
        console.error("Catalog search failed", error);
        setCatalogResults([]);
        setCatalogMessage(
          "The catalog is unavailable. Manual entry is still available."
        );
      } finally {
        if (active) setCatalogSearching(false);
      }
    }, 450);

    return () => {
      active = false;
      window.clearTimeout(timeout);
    };
  }, [catalogQuery, activeCatalogField, isEditing]);

  useEffect(() => {
    if (!activeCatalogField) return;

    function dismissCatalog(event: PointerEvent) {
      const target = event.target;
      if (!(target instanceof Element)) return;
      if (target.closest(".catalog-search-field-active")) return;

      setActiveCatalogField("");
      setCatalogResults([]);
      setCatalogMessage("");
      setCatalogSearching(false);
    }

    function dismissWithEscape(event: KeyboardEvent) {
      if (event.key !== "Escape") return;
      setActiveCatalogField("");
      setCatalogResults([]);
      setCatalogMessage("");
      setCatalogSearching(false);
    }

    document.addEventListener("pointerdown", dismissCatalog);
    document.addEventListener("keydown", dismissWithEscape);
    return () => {
      document.removeEventListener("pointerdown", dismissCatalog);
      document.removeEventListener("keydown", dismissWithEscape);
    };
  }, [activeCatalogField]);

  useEffect(() => {
    if (!openDropdown) return;

    function dismissDropdown(event: PointerEvent) {
      const target = event.target;
      if (target instanceof Element && target.closest(".fantasy-dropdown-wrap")) {
        return;
      }
      setOpenDropdown("");
    }

    function dismissDropdownWithEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setOpenDropdown("");
    }

    document.addEventListener("pointerdown", dismissDropdown);
    document.addEventListener("keydown", dismissDropdownWithEscape);
    return () => {
      document.removeEventListener("pointerdown", dismissDropdown);
      document.removeEventListener("keydown", dismissDropdownWithEscape);
    };
  }, [openDropdown]);

  const ledgerPages = useMemo(() => {
    if (!draftExperience) return [];
    return getLedgerPages(draftExperience as NewBook);
  }, [draftExperience]);

  useEffect(() => {
    if (ledgerPages.length === 0) return;

    setLedgerPage((prev) =>
      Math.min(prev, Math.max(ledgerPages.length - 1, 0))
    );
  }, [ledgerPages.length]);

  if (!isOpen || !draftExperience || ledgerPages.length === 0) return null;

  const safeLedgerPage = Math.min(ledgerPage, ledgerPages.length - 1);
  const currentPage = ledgerPages[safeLedgerPage];
  const isFirstPage = safeLedgerPage === 0;
  const isLastPage = safeLedgerPage === ledgerPages.length - 1;
  const totalDisplayedPages = ledgerPages.length * 2;
  const leftPageNumber = safeLedgerPage * 2 + 1;
  const rightPageNumber = safeLedgerPage * 2 + 2;

  function toggleDropdown(dropdownId: string) {
    setActiveCatalogField("");
    setCatalogResults([]);
    setCatalogMessage("");
    setOpenDropdown((current) =>
      dropdownId && current !== dropdownId ? dropdownId : ""
    );
  }

  function updateField(field: string, value: string) {
    setDraftExperience((prev) =>
      prev
        ? {
            ...prev,
            [field]: value,
            ...(field === "seriesName"
              ? {
                  seriesNumber: prev.seriesName === value ? prev.seriesNumber : "",
                  isSeries: value.trim() ? "YES" : "",
                }
              : {}),
            ...(field === "isSeries" && value !== "YES"
              ? { seriesName: "", seriesNumber: "" }
              : {}),
          }
        : prev
    );

    if (field === "title" || field === "author" || field === "seriesName") {
      setActiveCatalogField(field);
      setCatalogQuery(value);
    }
  }

  async function applyCatalogResult(selectedResult: CatalogBookResult) {
    const seriesSearchName =
      activeCatalogField === "seriesName"
        ? displaySeriesName(catalogQuery)
        : "";

    setCatalogSearching(true);

    let result = selectedResult;
    try {
      result = await catalogApi.resolveBook(selectedResult);
    } catch (error) {
      console.warn("Catalog detail enrichment failed", error);
    }

    const publisher = result.publisher?.trim() || "";
    const seriesName = displaySeriesName(result.seriesName || seriesSearchName);

    setDraftExperience((previous) =>
      previous
        ? {
            ...previous,
            title: result.title || previous.title,
            subtitle: result.subtitle || "",
            author: result.authors.join(", "),
            genre: usefulGenres(result.genres),
            description: result.description || "",
            format: frontendFormat(result.format),
            publisher: publisher ? "OTHER" : "",
            publisherOther: publisher,
            publicationYear: result.publicationDate?.slice(0, 4) || "",
            editionFormat: "",
            pageCount: result.pageCount ? String(result.pageCount) : "",
            audioLength: durationLabel(result.audiobookLengthSeconds),
            narrator: result.narrators.join(", "),
            coverUrl: result.coverImageUrl || "",
            language: result.language || "",
            isbn10: result.isbn10 || "",
            isbn13: result.isbn13 || "",
            catalogProvider: result.provider,
            catalogProviderId: result.providerId,
            seriesName,
            seriesNumber:
              result.seriesNumber !== null && result.seriesNumber !== undefined
                ? String(result.seriesNumber)
                : "",
            isSeries: seriesName ? "YES" : "",
            readingStatus: previous.readingStatus,
          }
        : previous
    );

    setCatalogQuery("");
    setActiveCatalogField("");
    setCatalogResults([]);
    setCatalogMessage("");
    setCatalogSearching(false);
    setOpenDropdown("");
  }

  async function handleSave() {
    const currentExperience = draftExperience;
    if (!currentExperience) return;

    const promptResponses = collectPromptResponses(currentExperience);

    const savedExperience: ReadingExperience = {
      ...currentExperience,
      label:
        currentExperience.label?.trim() ||
        experience?.label ||
        "Reading Experience",
      format: currentExperience.format || "PHYSICAL",
      readingStatus: currentExperience.readingStatus || "TO_READ",
      promptResponsesJson: JSON.stringify(promptResponses),
      createdAt: currentExperience.createdAt || new Date().toISOString(),
    };

    try {
      await onSave(savedExperience);

      // Remain on this exact spread and show the newly saved values in
      // read-only mode. The Reading Experience should not close or return the
      // reader to the first page of the parent book.
      setDraftExperience(savedExperience as ExperienceDraft);
      setIsEditing(false);
      setActiveCatalogField("");
      setCatalogResults([]);
      setCatalogMessage("");
      setCatalogSearching(false);
      setCatalogQuery("");
      setOpenDropdown("");
    } catch {
      // The parent displays the detailed API error. Stay in edit mode so the
      // reader's unsaved work remains available to correct and retry.
    }
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
            <p className="focused-writing-prompt">
              {focusedPrompt.placeholder}
            </p>
          </section>

          <section className="add-book-page add-book-right-page focused-writing-right">
            {isEditing ? (
              <LivingQuillTextarea
                className="focused-writing-area reader-handwriting"
                value={value}
                onChange={(nextValue) =>
                  updateField(focusedPrompt.key, nextValue)
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
          activeCatalogField={activeCatalogField}
          setActiveCatalogField={setActiveCatalogField}
          catalogSearching={catalogSearching}
          catalogMessage={catalogMessage}
          catalogResults={catalogResults}
          applyCatalogResult={applyCatalogResult}
          openDropdown={openDropdown}
          toggleDropdown={toggleDropdown}
          pageNumber={leftPageNumber}
          totalPages={totalDisplayedPages}
        />

        <LedgerSide
          side={currentPage.right}
          className="add-book-right-page"
          book={draftExperience}
          updateField={updateField}
          openPrompt={setFocusedPrompt}
          isEditing={isEditing}
          activeCatalogField={activeCatalogField}
          setActiveCatalogField={setActiveCatalogField}
          catalogSearching={catalogSearching}
          catalogMessage={catalogMessage}
          catalogResults={catalogResults}
          applyCatalogResult={applyCatalogResult}
          openDropdown={openDropdown}
          toggleDropdown={toggleDropdown}
          pageNumber={rightPageNumber}
          totalPages={totalDisplayedPages}
        />

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