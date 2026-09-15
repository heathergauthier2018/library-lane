// LIBRARY LANE ENCHANTED INDEX + LIVING QUILL (2026-07-19)
// Keeps the catalog and every selection visually inside the storybook ledger.
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import type { CSSProperties, FocusEvent, MouseEvent } from "react";
import { createPortal } from "react-dom";

import openBookBg from "../../assets/storybook/backgrounds/add-book-background.png";
import closeBookFrame from "../../assets/storybook/frames/close-book-frame.png";
import addStoryFrame from "../../assets/storybook/frames/add-story-frame.png";
import writingQuill from "../../assets/storybook/decor/library-lane-quill-v2.png";

import {
  getLedgerPages,
  ratingIcons,
  type FieldConfig,
  type LedgerSide as LedgerSideConfig,
  type NewBook,
  type Option,
  type RatingIcon,
} from "./bookLedgerConfig";
import {
  catalogApi,
  type CatalogBookFormat,
  type CatalogSearchField,
  type CatalogBookResult,
} from "../../api/libraryLaneApi";

export type { NewBook } from "./bookLedgerConfig";

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

const rejectedCoverUrl =
  /(?:image[_-]?not[_-]?available|no[_-]?image|no[_-]?cover|placeholder|default[_-]?cover|missing[_-]?cover)/i;

function normalizedCatalogText(value?: string | null) {
  return (value || "")
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
}

function sameCatalogWork(
  reference: CatalogBookResult,
  candidate: CatalogBookResult,
) {
  const expectedTitle = normalizedCatalogText(reference.title);
  const candidateTitle = normalizedCatalogText(candidate.title);
  if (
    candidateTitle !== expectedTitle &&
    !candidateTitle.startsWith(`${expectedTitle} `)
  ) return false;

  const expectedAuthors = new Set(
    reference.authors.map(normalizedCatalogText).filter(Boolean),
  );
  const candidateAuthors = candidate.authors
    .map(normalizedCatalogText)
    .filter(Boolean);
  return expectedAuthors.size === 0 || candidateAuthors.length === 0 ||
    candidateAuthors.some((author) => expectedAuthors.has(author));
}

function coverIdentity(url: string) {
  return url
    .replace(/^http:/i, "https:")
    .replace(/([?&])(?:zoom|w|width|height|img)=[^&]*/gi, "$1")
    .replace(/[?&]+$/, "");
}

function preferredCoverUrl(url: string) {
  if (/books\.google/i.test(url)) {
    const upgraded = url
      .replace(/^http:/i, "https:")
      .replace(/([?&])zoom=\d+/i, "$1zoom=2")
      .replace(/([?&])edge=curl&?/i, "$1")
      .replace(/[?&]+$/, "");
    return upgraded.includes("zoom=")
      ? upgraded
      : `${upgraded}${upgraded.includes("?") ? "&" : "?"}zoom=2`;
  }
  return url.replace(/^http:/i, "https:");
}

function validateAddCover(url: string) {
  return new Promise<boolean>((resolve) => {
    const image = new Image();
    image.crossOrigin = "anonymous";
    image.onload = () => {
      if (image.naturalWidth < 180 || image.naturalHeight < 240) {
        resolve(false);
        return;
      }
      try {
        const canvas = document.createElement("canvas");
        canvas.width = 20;
        canvas.height = 30;
        const context = canvas.getContext("2d", { willReadFrequently: true });
        if (!context) return resolve(true);
        context.drawImage(image, 0, 0, 20, 30);
        const pixels = context.getImageData(0, 0, 20, 30).data;
        let white = 0;
        let transparent = 0;
        for (let index = 0; index < pixels.length; index += 4) {
          if (pixels[index] > 232 && pixels[index + 1] > 232 && pixels[index + 2] > 232) white += 1;
          if (pixels[index + 3] < 230) transparent += 1;
        }
        const count = pixels.length / 4;
        resolve(white / count < 0.78 && transparent / count < 0.08);
      } catch {
        resolve(true);
      }
    };
    image.onerror = () => resolve(false);
    image.src = url;
  });
}

function preferredLanguageRank(language?: string | null) {
  const normalized = normalizedCatalogText(language);
  if (["en", "eng", "english"].includes(normalized)) return 0;
  if (!normalized) return 1;
  return 2;
}

function editionCompleteness(result: CatalogBookResult) {
  return [
    result.coverImageUrl,
    result.description,
    result.publisher,
    result.publicationDate,
    result.pageCount,
    result.audiobookLengthSeconds,
    result.seriesName,
    result.seriesNumber,
    result.isbn13,
  ].filter((value) => value !== null && value !== undefined && value !== "").length;
}

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

function clearImportedMetadata(book: NewBook): NewBook {
  return {
    ...book,
    subtitle: "",
    genre: "",
    description: "",
    publisher: "",
    publisherOther: "",
    publicationYear: "",
    editionFormat: "",
    pageCount: "",
    audioLength: "",
    narrator: "",
    coverUrl: "",
    language: "",
    isbn10: "",
    isbn13: "",
    catalogProvider: "",
    catalogProviderId: "",
    seriesName: "",
    seriesNumber: "",
    isSeries: "",
  };
}

function prefersReducedMotion() {
  return (
    typeof window !== "undefined" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

function EnchantedTextInput({
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

function EnchantedTextarea({
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

    const lineHeight = Number.parseFloat(style.lineHeight) ||
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
  const dropdownWrapRef = useRef<HTMLDivElement>(null);
  const intervalRef = useRef<number | null>(null);
  const timeoutRefs = useRef<number[]>([]);
  const [writingPosition, setWritingPosition] = useState({ x: 5, y: 18 });
  const [menuPosition, setMenuPosition] = useState({
    top: 0,
    left: 0,
    width: 226,
    maxHeight: 310,
    opensUp: false,
  });

  useEffect(() => {
    const timers = timeoutRefs.current;

    return () => {
      if (intervalRef.current !== null) {
        window.clearInterval(intervalRef.current);
      }

      timers.forEach((timer) => window.clearTimeout(timer));
    };
  }, []);

  useLayoutEffect(() => {
    const text = writingTextRef.current;
    const button = dropdownButtonRef.current;
    if (!text || !button) return;

    /* Use the same line-height baseline calculation as the typed inputs so
       the dropdown quill's nib sits on the handwriting instead of below it. */
    const style = window.getComputedStyle(text);
    const fontSize = Number.parseFloat(style.fontSize) || 20;
    const lineHeight = Number.parseFloat(style.lineHeight) || fontSize * 1.2;

    setWritingPosition({
      x: text.offsetLeft + text.offsetWidth,
      y: text.offsetTop + lineHeight * 0.8,
    });
  }, [typedSelection, writingTarget]);

  useLayoutEffect(() => {
    if (!isOpen) return;

    function placeMenu() {
      const button = dropdownButtonRef.current;
      if (!button) return;

      const bounds = button.getBoundingClientRect();
      const viewportPadding = 12;
      const gap = 4;
      const naturalHeight = Math.min(310, options.length * 39 + 16);
      const roomBelow = window.innerHeight - bounds.bottom - viewportPadding;
      const roomAbove = bounds.top - viewportPadding;
      const opensUp = roomBelow < naturalHeight && roomAbove > roomBelow;
      const availableHeight = Math.max(
        82,
        Math.min(310, (opensUp ? roomAbove : roomBelow) - gap),
      );
      const width = Math.min(
        Math.max(226, bounds.width * 0.78),
        window.innerWidth - viewportPadding * 2,
      );
      const left = Math.min(
        Math.max(viewportPadding, bounds.left),
        window.innerWidth - width - viewportPadding,
      );
      const top = opensUp
        ? Math.max(viewportPadding, bounds.top - Math.min(naturalHeight, availableHeight) - gap)
        : bounds.bottom + gap;

      setMenuPosition({ top, left, width, maxHeight: availableHeight, opensUp });
    }

    placeMenu();
    window.addEventListener("resize", placeMenu);
    window.addEventListener("scroll", placeMenu, true);
    return () => {
      window.removeEventListener("resize", placeMenu);
      window.removeEventListener("scroll", placeMenu, true);
    };
  }, [isOpen, options.length]);

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
      ref={dropdownWrapRef}
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

      {isOpen && !writingTarget && createPortal(
        <div
          className={`fantasy-dropdown-menu fantasy-dropdown-portal ${
            menuPosition.opensUp ? "opens-up" : "opens-down"
          }`}
          role="listbox"
          style={{
            top: `${menuPosition.top}px`,
            left: `${menuPosition.left}px`,
            width: `${menuPosition.width}px`,
            maxHeight: `${menuPosition.maxHeight}px`,
          }}
        >
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
        </div>,
        document.body,
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

  const resultLabel = results.length === 1 ? "1 catalog match" : `${results.length} catalog matches`;

  return (
    <aside
      className={`catalog-search-panel ${
        results.length > 0 ? "catalog-search-panel-results" : "catalog-search-panel-message"
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
          <span className="catalog-state-ornament" aria-hidden="true">⌁</span>
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
          <span className="catalog-state-ornament" aria-hidden="true">⌁</span>
          <small>From the Archive of Stories</small>
          <p>{message}</p>
        </div>
      )}

      {!searching && results.length > 0 && (
        <div className="catalog-results-heading" aria-hidden="true">
          <span>From the catalog</span>
          <small>{resultLabel}</small>
        </div>
      )}

      {!searching && results.map((result) => (
        <button
          type="button"
          className="catalog-search-result"
          key={`${result.provider}-${result.providerId}`}
          onMouseDown={(event) => event.preventDefault()}
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
            {result.provider.toLowerCase().includes("apple audiobook") && (
              <small className="catalog-result-source">
                Audiobook catalog record from Apple Books
              </small>
            )}
            {result.provider.toLowerCase().includes("spotify audiobook album") && (
              <small className="catalog-result-source">
                Legacy audiobook edition available through Spotify
              </small>
            )}
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
  book: NewBook;
  updateField: (field: string, value: string) => void;
  openPrompt: (field: FieldConfig) => void;
  activeCatalogField: string;
  setActiveCatalogField: (field: string) => void;
  catalogSearching: boolean;
  catalogMessage: string;
  catalogResults: CatalogBookResult[];
  applyCatalogResult: (result: CatalogBookResult) => void;
  openDropdown: string;
  toggleDropdown: (dropdownId: string) => void;
}) {
  if (field.showIf && !field.showIf(book)) return null;

  const value = book[field.key] ?? "";

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

        <EnchantedTextarea
          rows={field.rows ?? 3}
          placeholder={field.placeholder}
          value={value}
          onChange={(nextValue) => updateField(field.key, nextValue)}
        />
      </label>
    );
  }

  // Audiobooks can have a principal narrator, guest narrators, and bonus-
  // chapter performers. Keep the complete credited list visible and let the
  // reader open it on the full writing page instead of clipping it inside a
  // single-line input.
  if (field.key === "narrator") {
    return (
      <label className="ledger-long-prompt ledger-field-narrator">
        <button
          type="button"
          className="ledger-prompt-title-button"
          onClick={() => openPrompt(field)}
        >
          {field.label}
        </button>

        <EnchantedTextarea
          rows={2}
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
      } ${isCatalogField && activeCatalogField === field.key
        ? "catalog-search-field-active"
        : ""
      }`}
    >
      <label>
        <span>{field.label}</span>
        <EnchantedTextInput
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
  book: NewBook;
  updateField: (field: string, value: string) => void;
  openPrompt: (field: FieldConfig) => void;
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
  const seriesLayoutClass =
    book.isSeries === "YES" ? "ledger-has-series" : "ledger-no-series";

  return (
    <section
      className={`add-book-page ${className} ${seriesLayoutClass}`}
    >
      <div className="ledger-page-scroll-content" key={pageNumber}>
        {side.eyebrow && <p className="add-book-eyebrow">{side.eyebrow}</p>}
        {side.title && <h2>{side.title}</h2>}

        {side.fields.map((field) => (
          <RenderField
            key={field.key}
            field={field}
            book={book}
            updateField={updateField}
            openPrompt={openPrompt}
            activeCatalogField={activeCatalogField}
            setActiveCatalogField={setActiveCatalogField}
            catalogSearching={catalogSearching}
            catalogMessage={catalogMessage}
            catalogResults={catalogResults}
            applyCatalogResult={applyCatalogResult}
            openDropdown={openDropdown}
            toggleDropdown={toggleDropdown}
          />
        ))}
      </div>

      <span className="ledger-page-number">
        {pageNumber} of {totalPages}
      </span>
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
  const [catalogQuery, setCatalogQuery] = useState("");
  const [activeCatalogField, setActiveCatalogField] = useState("");
  const [catalogResults, setCatalogResults] = useState<CatalogBookResult[]>([]);
  const [catalogSearching, setCatalogSearching] = useState(false);
  const [catalogMessage, setCatalogMessage] = useState("");
  const [openDropdown, setOpenDropdown] = useState("");
  const [selectedCatalogWork, setSelectedCatalogWork] =
    useState<CatalogBookResult | null>(null);
  const [formatResolving, setFormatResolving] = useState(false);
  const [addCoverCandidates, setAddCoverCandidates] =
    useState<CatalogBookResult[]>([]);
  const [coverPickerOpen, setCoverPickerOpen] = useState(false);
  const [coverSearchMessage, setCoverSearchMessage] = useState("");

  const ledgerPages = useMemo(() => {
    return getLedgerPages(book);
  }, [book]);

  useEffect(() => {
    const query = catalogQuery.trim();

    if (query.length < 3 || !activeCatalogField) return;

    let active = true;
    const timeout = window.setTimeout(async () => {
      setCatalogSearching(true);
      setCatalogMessage("");

      try {
        // Search every available format. The chosen result, not the form's
        // default, decides whether the saved experience is print, e-book, or
        // audiobook.
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
      // A slightly longer pause prevents a separate Apple request for nearly
      // every partial word while leaving the established results unchanged.
    }, 750);

    return () => {
      active = false;
      window.clearTimeout(timeout);
    };
  }, [catalogQuery, activeCatalogField]);

  useEffect(() => {
    if (!selectedCatalogWork) return;
    const requestedFormat = book.format as CatalogBookFormat;
    if (frontendFormat(selectedCatalogWork.format) === requestedFormat) return;

    let active = true;
    void (async () => {
      try {
        const matches = await catalogApi.searchBooks(
          selectedCatalogWork.title,
          requestedFormat,
          "TITLE",
        );
        const matchingEdition = matches
          .filter((candidate) => sameCatalogWork(selectedCatalogWork, candidate))
          .sort((left, right) =>
            preferredLanguageRank(left.language) - preferredLanguageRank(right.language) ||
            editionCompleteness(right) - editionCompleteness(left)
          )[0];
        if (!matchingEdition || !active) {
          setCatalogMessage(
            `No ${requestedFormat.toLowerCase().replace("ebook", "e-book")} edition was found. Your story details were preserved for manual entry.`,
          );
          return;
        }
        let resolved = matchingEdition;
        try {
          resolved = await catalogApi.resolveBook(matchingEdition);
        } catch (error) {
          console.warn("Format-specific catalog enrichment failed", error);
        }
        if (!active) return;
        const publisher = resolved.publisher?.trim() || "";
        setBook((previous) => ({
          ...previous,
          format: requestedFormat,
          publisher: publisher ? "OTHER" : previous.publisher,
          publisherOther: publisher || previous.publisherOther,
          publicationYear:
            resolved.publicationDate?.slice(0, 4) || previous.publicationYear,
          pageCount:
            requestedFormat === "AUDIOBOOK"
              ? ""
              : resolved.pageCount
                ? String(resolved.pageCount)
                : previous.pageCount,
          audioLength:
            requestedFormat === "AUDIOBOOK"
              ? durationLabel(resolved.audiobookLengthSeconds)
              : "",
          narrator:
            requestedFormat === "AUDIOBOOK"
              ? resolved.narrators.join(", ")
              : "",
          description: resolved.description || previous.description,
          coverUrl: resolved.coverImageUrl || previous.coverUrl,
          language: resolved.language || previous.language,
          isbn10: resolved.isbn10 || "",
          isbn13: resolved.isbn13 || "",
          catalogProvider: resolved.provider,
          catalogProviderId: resolved.providerId,
        }));
        setSelectedCatalogWork(resolved);
        setCatalogMessage("");
      } catch (error) {
        console.warn("Format-specific catalog lookup failed", error);
        if (active) {
          setCatalogMessage(
            "That format could not be refreshed. Your existing story details were preserved.",
          );
        }
      } finally {
        if (active) setFormatResolving(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [book.format, selectedCatalogWork]);

  useEffect(() => {
    if (!selectedCatalogWork) return;
    let active = true;
    void Promise.allSettled(
      ([undefined, "PHYSICAL", "EBOOK", "AUDIOBOOK"] as const).map((format) =>
        catalogApi.searchBooks(selectedCatalogWork.title, format, "TITLE"),
      ),
    ).then(async (groups) => {
      if (!active) return;
      const allResults = groups.flatMap((group) =>
        group.status === "fulfilled" ? group.value : [],
      );
      const candidates = allResults.filter((candidate) =>
        Boolean(candidate.coverImageUrl) &&
        !rejectedCoverUrl.test(candidate.coverImageUrl || "") &&
        sameCatalogWork(selectedCatalogWork, candidate),
      ).map((candidate) => ({
        ...candidate,
        coverImageUrl: preferredCoverUrl(candidate.coverImageUrl || ""),
      }));
      const inspectedCandidates = (
        await Promise.all(candidates.slice(0, 60).map(async (candidate) => ({
          candidate,
          usable: await validateAddCover(candidate.coverImageUrl || ""),
        })))
      ).filter(({ usable }) => usable).map(({ candidate }) => candidate);
      if (!active) return;
      const unique = new Map<string, CatalogBookResult>();
      inspectedCandidates.forEach((candidate) => {
        if (!candidate.coverImageUrl || rejectedCoverUrl.test(candidate.coverImageUrl)) return;
        const identity = coverIdentity(candidate.coverImageUrl);
        if (!unique.has(identity)) unique.set(identity, candidate);
      });
      const choices = [...unique.values()]
        .sort((left, right) =>
          preferredLanguageRank(left.language) - preferredLanguageRank(right.language) ||
          editionCompleteness(right) - editionCompleteness(left)
        )
        .slice(0, 16);
      setAddCoverCandidates(choices);
      setCoverSearchMessage(
        choices.length
          ? ""
          : "No validated catalog cover was found. A solid Library Lane binding will be used.",
      );
      const matchingResults = allResults.filter((candidate) =>
        sameCatalogWork(selectedCatalogWork, candidate),
      );
      const workDescription = matchingResults
        .filter((candidate) => Boolean(candidate.description?.trim()))
        .sort((left, right) =>
          preferredLanguageRank(left.language) - preferredLanguageRank(right.language) ||
          editionCompleteness(right) - editionCompleteness(left)
        )[0]?.description;
      const factualSupplement = matchingResults
        .filter((candidate) => frontendFormat(candidate.format) === book.format)
        .sort((left, right) =>
          preferredLanguageRank(left.language) - preferredLanguageRank(right.language) ||
          editionCompleteness(right) - editionCompleteness(left)
        )[0];
      const currentCoverIsValidated = choices.some(
        (candidate) => candidate.coverImageUrl === book.coverUrl,
      );
      setBook((previous) => ({
        ...previous,
        coverUrl: currentCoverIsValidated
          ? previous.coverUrl
          : choices[0]?.coverImageUrl || "",
        // A synopsis describes the work, so it is safe to borrow from another
        // matching edition when the selected record omitted it.
        description: previous.description || workDescription || "",
        pageCount: previous.pageCount ||
          (factualSupplement?.pageCount ? String(factualSupplement.pageCount) : ""),
        seriesName: previous.seriesName ||
          displaySeriesName(factualSupplement?.seriesName),
        seriesNumber: previous.seriesNumber ||
          (factualSupplement?.seriesNumber != null
            ? String(factualSupplement.seriesNumber)
            : ""),
      }));
    });
    return () => {
      active = false;
    };
  }, [book.coverUrl, book.format, selectedCatalogWork]);

  useEffect(() => {
    if (!activeCatalogField) return;

    function dismissCatalog(event: globalThis.MouseEvent) {
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

    // Dismiss after the target's click has completed. Using pointerdown here
    // could rerender the ledger between press and release, detaching action
    // buttons before their click handlers had a chance to run.
    document.addEventListener("click", dismissCatalog);
    document.addEventListener("keydown", dismissWithEscape);

    return () => {
      document.removeEventListener("click", dismissCatalog);
      document.removeEventListener("keydown", dismissWithEscape);
    };
  }, [activeCatalogField]);

  useEffect(() => {
    if (!openDropdown) return;

    function dismissDropdown(event: PointerEvent) {
      const target = event.target;
      if (
        target instanceof Element &&
        target.closest(".fantasy-dropdown-wrap, .fantasy-dropdown-portal")
      ) {
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

  if (!isOpen) return null;

  const currentPage = ledgerPages[Math.min(ledgerPage, ledgerPages.length - 1)];
  const isFirstPage = ledgerPage === 0;
  const isLastPage = ledgerPage === ledgerPages.length - 1;
  const totalLedgerPages = ledgerPages.length * 2;
  const leftPageNumber = ledgerPage * 2 + 1;
  const rightPageNumber = leftPageNumber + 1;

  function toggleDropdown(dropdownId: string) {
    setActiveCatalogField("");
    setCatalogResults([]);
    setCatalogMessage("");
    setOpenDropdown((current) =>
      dropdownId && current !== dropdownId ? dropdownId : ""
    );
  }

  function updateField(field: string, value: string) {
    if (field === "format" && book.format !== value) {
      setFormatResolving(Boolean(selectedCatalogWork));
    }
    setBook((previous) => {
      let next = previous;

      // Once the reader changes a selected catalog identity, metadata from
      // that former result is no longer safe to display with the new search.
      if (
        (field === "title" || field === "author") &&
        previous.catalogProviderId &&
        previous[field] !== value
      ) {
        next = clearImportedMetadata(previous);
      }

      next = { ...next, [field]: value };

      if (field === "seriesName") {
        next = {
          ...next,
          catalogProvider: "",
          catalogProviderId: "",
          seriesNumber: previous.seriesName === value ? next.seriesNumber : "",
          isSeries: value.trim() ? "YES" : "",
        };
      }

      if (field === "isSeries" && value !== "YES") {
        next = { ...next, seriesName: "", seriesNumber: "" };
      }

      if (field === "format" && previous.format !== value) {
        next = {
          ...next,
          editionFormat: "",
          pageCount: value === "AUDIOBOOK" ? "" : next.pageCount,
          audioLength: value === "AUDIOBOOK" ? next.audioLength : "",
          narrator: value === "AUDIOBOOK" ? next.narrator : "",
        };
      }

      return next;
    });

    if (field === "title" || field === "author" || field === "seriesName") {
      if (value.trim().length < 3) {
        setCatalogResults([]);
        setCatalogMessage("");
        setCatalogSearching(false);
      }

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
      // The backend owns cross-source resolution. Performing another
      // title-only merge here previously mixed unrelated editions and authors.
      result = await catalogApi.resolveBook(selectedResult);
    } catch (error) {
      // The selected catalog record is still usable when enrichment fails.
      console.warn("Catalog detail enrichment failed", error);
    }

    const publisher = result.publisher?.trim() || "";
    const seriesName = displaySeriesName(
      result.seriesName || seriesSearchName
    );

    setBook((previous) => ({
      ...clearImportedMetadata(previous),
      title: result.title || previous.title,
      subtitle: result.subtitle || "",
      author: result.authors.join(", "),
      genre: usefulGenres(result.genres),
      description: result.description || "",
      format: frontendFormat(result.format),
      publisher: publisher ? "OTHER" : "",
      publisherOther: publisher,
      publicationYear: result.publicationDate?.slice(0, 4) || "",
      // Catalog labels such as "Book catalog record" and "E-Book edition"
      // do not identify the reader's actual hardcover, paperback, sprayed-edge,
      // signed, or special edition. Leave this reader-owned field blank.
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
      // Missing provider evidence does not prove that a book is standalone.
      // Leave the field unselected so the reader can confirm it.
      isSeries: seriesName ? "YES" : "",
      readingStatus: previous.readingStatus,
    }));
    setSelectedCatalogWork(result);
    setCoverSearchMessage("Searching for trustworthy covers…");

    setCatalogQuery("");
    setActiveCatalogField("");
    setCatalogResults([]);
    setCatalogMessage("");
    setCatalogSearching(false);
    setOpenDropdown("");
  }

  function handleSave() {
    if (!book.title.trim()) return;

    onSave(book);
    setBook(getBlankBook());
    setLedgerPage(0);
    setFocusedPrompt(null);
    setOpenDropdown("");
    setSelectedCatalogWork(null);
    setAddCoverCandidates([]);
    setCoverPickerOpen(false);
    onClose();
  }

  function closeModal() {
    setBook(getBlankBook());
    setLedgerPage(0);
    setFocusedPrompt(null);
    setActiveCatalogField("");
    setCatalogResults([]);
    setCatalogMessage("");
    setCatalogSearching(false);
    setCatalogQuery("");
    setOpenDropdown("");
    setSelectedCatalogWork(null);
    setAddCoverCandidates([]);
    setCoverPickerOpen(false);
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
            <EnchantedTextarea
              className="focused-writing-area"
              value={value}
              onChange={(nextValue) => updateField(focusedPrompt.key, nextValue)}
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
          activeCatalogField={activeCatalogField}
          setActiveCatalogField={setActiveCatalogField}
          catalogSearching={catalogSearching}
          catalogMessage={catalogMessage}
          catalogResults={catalogResults}
          applyCatalogResult={applyCatalogResult}
          openDropdown={openDropdown}
          toggleDropdown={toggleDropdown}
          pageNumber={leftPageNumber}
          totalPages={totalLedgerPages}
        />

        <LedgerSide
          side={currentPage.right}
          className="add-book-right-page"
          book={book}
          updateField={updateField}
          openPrompt={setFocusedPrompt}
          activeCatalogField={activeCatalogField}
          setActiveCatalogField={setActiveCatalogField}
          catalogSearching={catalogSearching}
          catalogMessage={catalogMessage}
          catalogResults={catalogResults}
          applyCatalogResult={applyCatalogResult}
          openDropdown={openDropdown}
          toggleDropdown={toggleDropdown}
          pageNumber={rightPageNumber}
          totalPages={totalLedgerPages}
        />

        <div className="add-book-actions add-book-actions-left">
          <button
            type="button"
            className="add-book-frame-button close-book-button"
            onClick={closeModal}
            style={{ backgroundImage: `url(${closeBookFrame})` }}
          >
            Close
          </button>

          {selectedCatalogWork && (
            <button
              type="button"
              className="add-book-frame-button close-book-button choose-cover-action"
              onClick={() => setCoverPickerOpen(true)}
              style={{ backgroundImage: `url(${closeBookFrame})` }}
              aria-label="Choose the cover saved with this book"
            >
              Choose Cover
              {formatResolving && <span>Refreshing…</span>}
            </button>
          )}

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

      {coverPickerOpen && (
        <div className="add-cover-picker-overlay" role="dialog" aria-modal="true" aria-label="Choose the cover saved with this book">
          <section className="add-cover-picker">
            <header>
              <span>
                <small>Before this story joins the shelf</small>
                <h3>Choose the edition that feels like yours</h3>
              </span>
              <button type="button" aria-label="Close cover choices" onClick={() => setCoverPickerOpen(false)}>×</button>
            </header>
            {coverSearchMessage && <p>{coverSearchMessage}</p>}
            <div className="add-cover-picker-grid">
              {addCoverCandidates.map((candidate) => {
                const selected = candidate.coverImageUrl === book.coverUrl;
                return (
                  <button
                    type="button"
                    className={selected ? "selected" : ""}
                    aria-pressed={selected}
                    key={`${candidate.provider}-${candidate.providerId}-${candidate.coverImageUrl}`}
                    onClick={() => {
                      if (candidate.coverImageUrl) {
                        setBook((current) => ({ ...current, coverUrl: candidate.coverImageUrl || "" }));
                      }
                    }}
                  >
                    <img src={candidate.coverImageUrl || ""} alt={`Cover of ${candidate.title}`} />
                    <strong>{candidate.editionFormat || candidate.format}</strong>
                    <small>{[candidate.language, candidate.publisher, candidate.publicationDate?.slice(0, 4)].filter(Boolean).join(" • ")}</small>
                    {selected && <em>Selected</em>}
                  </button>
                );
              })}
            </div>
            <button type="button" className="add-cover-picker-done" onClick={() => setCoverPickerOpen(false)}>
              Use this cover
            </button>
          </section>
        </div>
      )}
    </div>
  );
}
