import { useEffect, useId, useMemo, useRef, useState } from "react";
import type { KeyboardEvent } from "react";

export type LibrarySelectOption = {
  value: string;
  label: string;
  group?: string;
};

type SingleSelectProps = {
  multiple?: false;
  value: string;
  onChange: (value: string) => void;
  values?: never;
  onValuesChange?: never;
};

type MultiSelectProps = {
  multiple: true;
  values: string[];
  onValuesChange: (values: string[]) => void;
  value?: never;
  onChange?: never;
};

type LibrarySelectProps = (SingleSelectProps | MultiSelectProps) & {
  options: LibrarySelectOption[];
  ariaLabel: string;
  className?: string;
  placeholder?: string;
  searchable?: boolean;
};

export default function LibrarySelect(props: LibrarySelectProps) {
  const {
    options,
    ariaLabel,
    className = "",
    placeholder = "Choose options",
    searchable = false,
  } = props;

  const multiple = props.multiple === true;
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const [optionSearch, setOptionSearch] = useState("");
  const rootRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const listId = useId();

  const selectedValues = props.multiple ? props.values : [props.value];

  const visibleOptions = useMemo(() => {
    const query = optionSearch.trim().toLocaleLowerCase();

    return query
      ? options.filter((option) =>
          option.label.toLocaleLowerCase().includes(query),
        )
      : options;
  }, [optionSearch, options]);

  const selectedLabel = props.multiple
    ? selectedValues.length === 0
      ? placeholder
      : selectedValues.length === 1
        ? options.find((option) => option.value === selectedValues[0])?.label ??
          placeholder
        : `${selectedValues.length} selected`
    : options.find((option) => option.value === props.value)?.label ??
      options[0]?.label ??
      placeholder;

  function selectedIndexFor(candidateOptions: LibrarySelectOption[]) {
    const currentValue = selectedValues[0];
    const selectedIndex = candidateOptions.findIndex(
      (option) => option.value === currentValue,
    );

    return Math.max(0, selectedIndex);
  }

  function openMenu() {
    setOptionSearch("");
    setActiveIndex(selectedIndexFor(options));
    setOpen(true);
  }

  function closeMenu() {
    setOpen(false);
    setOptionSearch("");
  }

  function toggleMenu() {
    if (open) {
      closeMenu();
    } else {
      openMenu();
    }
  }

  useEffect(() => {
    function closeOnOutsideClick(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
        setOptionSearch("");
      }
    }

    document.addEventListener("mousedown", closeOnOutsideClick);

    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick);
    };
  }, []);

  useEffect(() => {
    if (!open || !searchable) return;

    const focusTimer = window.setTimeout(() => {
      searchRef.current?.focus();
    }, 0);

    return () => window.clearTimeout(focusTimer);
  }, [open, searchable]);

  function choose(index: number) {
    const option = visibleOptions[index];
    if (!option) return;

    if (props.multiple) {
      props.onValuesChange(
        props.values.includes(option.value)
          ? props.values.filter((value) => value !== option.value)
          : [...props.values, option.value],
      );
      return;
    }

    props.onChange(option.value);
    closeMenu();
  }

  function handleSearchChange(value: string) {
    setOptionSearch(value);

    const query = value.trim().toLocaleLowerCase();
    const nextVisibleOptions = query
      ? options.filter((option) =>
          option.label.toLocaleLowerCase().includes(query),
        )
      : options;

    setActiveIndex(selectedIndexFor(nextVisibleOptions));
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === "Escape") {
      closeMenu();
      return;
    }

    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();

      if (open) {
        choose(activeIndex);
      } else {
        openMenu();
      }

      return;
    }

    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();

      const direction = event.key === "ArrowDown" ? 1 : -1;

      if (!open) {
        openMenu();
        return;
      }

      setActiveIndex((current) =>
        visibleOptions.length
          ? (current + direction + visibleOptions.length) %
            visibleOptions.length
          : 0,
      );

      return;
    }

    if (event.key === "Home" || event.key === "End") {
      event.preventDefault();

      if (!open) openMenu();

      setActiveIndex(
        event.key === "Home" ? 0 : Math.max(visibleOptions.length - 1, 0),
      );
    }
  }

  return (
    <div
      ref={rootRef}
      className={`library-select ${className} ${open ? "is-open" : ""} ${
        multiple ? "is-multiple" : ""
      }`}
    >
      <button
        type="button"
        className="library-select-trigger"
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={toggleMenu}
        onKeyDown={handleKeyDown}
      >
        <span>{selectedLabel}</span>
        <span className="library-select-chevron" aria-hidden="true">
          ⌄
        </span>
      </button>

      {open && (
        <div
          id={listId}
          className="library-select-menu"
          role="listbox"
          aria-multiselectable={multiple || undefined}
        >
          {searchable && (
            <label className="library-select-search">
              <span aria-hidden="true">⌕</span>
              <input
                ref={searchRef}
                value={optionSearch}
                onChange={(event) => handleSearchChange(event.target.value)}
                placeholder={`Search ${ariaLabel.toLocaleLowerCase()}…`}
                aria-label={`Search options for ${ariaLabel}`}
                onKeyDown={(event) => {
                  if (event.key === "Escape") closeMenu();
                }}
              />
            </label>
          )}

          {visibleOptions.length === 0 && (
            <p className="library-select-empty">No matching options</p>
          )}

          {visibleOptions.map((option, index) => {
            const selected = selectedValues.includes(option.value);
            const previousOption = visibleOptions[index - 1];
            const showGroup = Boolean(
              option.group && option.group !== previousOption?.group,
            );

            return (
              <div key={option.value}>
                {showGroup && (
                  <div className="library-select-group">{option.group}</div>
                )}

                <button
                  type="button"
                  role="option"
                  aria-selected={selected}
                  className={`${selected ? "selected" : ""} ${
                    index === activeIndex ? "active" : ""
                  }`}
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => choose(index)}
                >
                  <span className="library-select-option-label">
                    {multiple && (
                      <span
                        className={`library-select-checkbox ${
                          selected ? "checked" : ""
                        }`}
                        aria-hidden="true"
                      >
                        {selected ? "✓" : ""}
                      </span>
                    )}
                    {option.label}
                  </span>

                  {!multiple && selected && (
                    <span className="library-select-check" aria-hidden="true">
                      ◆
                    </span>
                  )}
                </button>
              </div>
            );
          })}

          {multiple && selectedValues.length > 0 && (
            <button
              type="button"
              className="library-select-clear"
              onClick={() => {
                if (props.multiple) props.onValuesChange([]);
              }}
            >
              Clear this filter
            </button>
          )}
        </div>
      )}
    </div>
  );
}