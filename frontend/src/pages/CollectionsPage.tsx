import { useState } from "react";
import type { AppPage } from "../App";
import FantasySidebar from "../dashboard/FantasySidebar";
import "./collections-page.css";

type CollectionsPageProps = {
  currentPage: AppPage;
  setCurrentPage: (page: AppPage) => void;
};

type PermanentCollection = {
  id: string;
  label: string;
  bookCount: number;
  placement: string;
};

type CustomCollection = {
  id: string;
  label: string;
  bookCount: number;
  color: string;
};

const permanentCollections: PermanentCollection[] = [
  {
    id: "currently-reading",
    label: "Currently Reading",
    bookCount: 0,
    placement: "left-tall",
  },
  {
    id: "tbr",
    label: "TBR",
    bookCount: 0,
    placement: "left-tall",
  },
  {
    id: "wishlist",
    label: "Wishlist",
    bookCount: 0,
    placement: "right-low",
  },
  {
    id: "favorites",
    label: "Favorites",
    bookCount: 0,
    placement: "right-low",
  },
  {
    id: "completed",
    label: "Completed",
    bookCount: 0,
    placement: "right-tall",
  },
  {
    id: "dnf",
    label: "DNF",
    bookCount: 0,
    placement: "right-tall",
  },
];

const customCollections: CustomCollection[] = [
  {
    id: "fae-and-folklore",
    label: "Fae & Folklore",
    bookCount: 17,
    color: "#6f315f",
  },
  {
    id: "enchanted-escapes",
    label: "My Enchanted Escapes",
    bookCount: 24,
    color: "#224e68",
  },
  {
    id: "slow-burn",
    label: "Slow Burn",
    bookCount: 19,
    color: "#8c493d",
  },
];

function EmptyBookcaseFrame() {
  return (
    <div className="collection-bookcase-frame" aria-hidden="true">
      <span />
      <span />
      <span />
      <span />
    </div>
  );
}

function CollectionBookcase({
  collection,
  onSelect,
}: {
  collection: PermanentCollection;
  onSelect: (collection: PermanentCollection) => void;
}) {
  return (
    <button
      type="button"
      className={`collection-bookcase ${collection.placement}`}
      onClick={() => onSelect(collection)}
      aria-label={`Open ${collection.label}, ${collection.bookCount} books`}
    >
      <EmptyBookcaseFrame />

      <span className="collection-bookcase-copy">
        <strong>{collection.label}</strong>
        <small>
          {collection.bookCount}{" "}
          {collection.bookCount === 1 ? "book" : "books"}
        </small>
      </span>
    </button>
  );
}

export default function CollectionsPage({
  currentPage,
  setCurrentPage,
}: CollectionsPageProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeCustomIndex, setActiveCustomIndex] = useState(0);
  const [selectedCollection, setSelectedCollection] = useState<string | null>(
    null,
  );

  function rotateCarousel(direction: number) {
    setActiveCustomIndex((current) => {
      const next = current + direction;
      return (next + customCollections.length) % customCollections.length;
    });
  }

  return (
    <div className="collections-layout">
      <button
        type="button"
        className="collections-menu-button"
        onClick={() => setSidebarOpen(true)}
        aria-label="Open menu"
      >
        ☰
      </button>

      {sidebarOpen && (
        <button
          type="button"
          className="collections-sidebar-backdrop"
          onClick={() => setSidebarOpen(false)}
          aria-label="Close menu"
        />
      )}

      <div
        className={`collections-sidebar-drawer ${
          sidebarOpen ? "open" : ""
        }`}
      >
        <FantasySidebar
          currentPage={currentPage}
          setCurrentPage={(page) => {
            setCurrentPage(page);
            setSidebarOpen(false);
          }}
        />
      </div>

      <main className="collections-page">
        <header className="collections-heading">
          <p>Rooms Within Your Library</p>
          <h1>Collections</h1>
          <span>Every shelf holds a different kind of story.</span>
        </header>

        <section
          className="collections-scene"
          aria-label="Collection room"
        >
          <div className="collections-left-wall">
            {permanentCollections.slice(0, 2).map((collection) => (
              <CollectionBookcase
                key={collection.id}
                collection={collection}
                onSelect={(item) => setSelectedCollection(item.label)}
              />
            ))}
          </div>

          <section
            className="collection-carousel"
            aria-label="Custom collections"
          >
            <div className="collection-carousel-canopy" aria-hidden="true">
              <span className="collection-carousel-night" />
            </div>

            <div className="collection-carousel-bay">
              <button
                type="button"
                className="custom-collection"
                style={{
                  borderColor:
                    customCollections[activeCustomIndex].color,
                }}
                onClick={() =>
                  setSelectedCollection(
                    customCollections[activeCustomIndex].label,
                  )
                }
              >
                <span className="custom-collection-shelves" aria-hidden="true">
                  <i />
                  <i />
                  <i />
                </span>

                <strong>
                  {customCollections[activeCustomIndex].label}
                </strong>

                <small>
                  {customCollections[activeCustomIndex].bookCount} books
                </small>
              </button>

              <button
                type="button"
                className="create-collection-bay"
                onClick={() => setSelectedCollection("Create Collection")}
              >
                <span aria-hidden="true">＋</span>
                Create Collection
              </button>
            </div>

            <div className="collection-carousel-base" aria-hidden="true" />
          </section>

          <div className="collections-right-wall">
            {permanentCollections.slice(2).map((collection) => (
              <CollectionBookcase
                key={collection.id}
                collection={collection}
                onSelect={(item) => setSelectedCollection(item.label)}
              />
            ))}
          </div>

          <button
            type="button"
            className="collections-previous"
            onClick={() => rotateCarousel(-1)}
            aria-label="Previous custom collection"
          >
            ‹
          </button>

          <button
            type="button"
            className="collections-next"
            onClick={() => rotateCarousel(1)}
            aria-label="Next custom collection"
          >
            ›
          </button>
        </section>

        <p className="collections-selection" aria-live="polite">
          {selectedCollection
            ? `${selectedCollection} selected`
            : "Choose a collection to step closer."}
        </p>
      </main>
    </div>
  );
}