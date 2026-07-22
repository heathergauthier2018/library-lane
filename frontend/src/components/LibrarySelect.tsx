import { useEffect, useId, useRef, useState } from "react";
import type { KeyboardEvent } from "react";

export type LibrarySelectOption = {
  value: string;
  label: string;
  group?: string;
};

type LibrarySelectProps = {
  value: string;
  options: LibrarySelectOption[];
  onChange: (value: string) => void;
  ariaLabel: string;
  className?: string;
};

export default function LibrarySelect({
  value,
  options,
  onChange,
  ariaLabel,
  className = "",
}: LibrarySelectProps) {
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(() =>
    Math.max(0, options.findIndex((option) => option.value === value))
  );
  const rootRef = useRef<HTMLDivElement>(null);
  const listId = useId();
  const selectedOption =
    options.find((option) => option.value === value) ?? options[0];

  useEffect(() => {
    setActiveIndex(
      Math.max(0, options.findIndex((option) => option.value === value))
    );
  }, [options, value]);

  useEffect(() => {
    function closeOnOutsideClick(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    }

    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, []);

  function choose(index: number) {
    const option = options[index];
    if (!option) return;
    onChange(option.value);
    setActiveIndex(index);
    setOpen(false);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === "Escape") {
      setOpen(false);
      return;
    }

    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      if (open) choose(activeIndex);
      else setOpen(true);
      return;
    }

    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      const direction = event.key === "ArrowDown" ? 1 : -1;
      setOpen(true);
      setActiveIndex((current) =>
        (current + direction + options.length) % options.length
      );
      return;
    }

    if (event.key === "Home") {
      event.preventDefault();
      setOpen(true);
      setActiveIndex(0);
    }

    if (event.key === "End") {
      event.preventDefault();
      setOpen(true);
      setActiveIndex(options.length - 1);
    }
  }

  let previousGroup: string | undefined;

  return (
    <div
      ref={rootRef}
      className={`library-select ${className} ${open ? "is-open" : ""}`}
    >
      <button
        type="button"
        className="library-select-trigger"
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((current) => !current)}
        onKeyDown={handleKeyDown}
      >
        <span>{selectedOption?.label}</span>
        <span className="library-select-chevron" aria-hidden="true">⌄</span>
      </button>

      {open && (
        <div id={listId} className="library-select-menu" role="listbox">
          {options.map((option, index) => {
            const showGroup = Boolean(
              option.group && option.group !== previousGroup
            );
            previousGroup = option.group;

            return (
              <div key={option.value}>
                {showGroup && (
                  <div className="library-select-group">{option.group}</div>
                )}
                <button
                  type="button"
                  role="option"
                  aria-selected={option.value === value}
                  className={`${option.value === value ? "selected" : ""} ${
                    index === activeIndex ? "active" : ""
                  }`}
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => choose(index)}
                >
                  <span>{option.label}</span>
                  {option.value === value && (
                    <span className="library-select-check" aria-hidden="true">◆</span>
                  )}
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}