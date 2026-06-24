import type { AppPage } from "../App";

import dashboardBackground from "../assets/storybook/backgrounds/dashboard-background.png";
import fantasyHeader from "../assets/storybook/backgrounds/fantasy-header.png";

import FantasySidebar from "./FantasySidebar";
import FantasyStats from "./FantasyStats";
import CurrentReadingWidget from "./CurrentReadingWidget";
import QuoteWidget from "./QuoteWidget";
import ReadingGoalWidget from "./ReadingGoalWidget";
import ReadingPathPanel from "./ReadingPathPanel";
import ReadingStreak from "./ReadingStreak";
import CollectionOverview from "./CollectionOverview";
import RecentActivity from "./RecentActivity";
import NextDoorways from "./NextDoorways";

type FantasyDashboardProps = {
  currentPage: AppPage;
  setCurrentPage: (page: AppPage) => void;
  onAddBook: () => void;
};

export default function FantasyDashboard({
  currentPage,
  setCurrentPage,
  onAddBook,
}: FantasyDashboardProps) {
  return (
    <div
      className="fantasy-dashboard"
      style={{
        backgroundImage: `
          linear-gradient(
            to bottom,
            rgba(6, 8, 20, 0.05) 0%,
            rgba(6, 8, 20, 0.18) 45%,
            rgba(8, 10, 24, 0.82) 85%,
            #080a18 100%
          ),
          url(${dashboardBackground})
        `,
      }}
    >
      <FantasySidebar
        currentPage={currentPage}
        setCurrentPage={setCurrentPage}
      />

      <main
        className="fantasy-main"
        style={{
          backgroundImage: `url(${fantasyHeader})`,
        }}
      >
        <section className="hero-copy">
          <p>Personal Reading World</p>
          <h1>My Library</h1>
        </section>

        <section className="fantasy-top-actions">
          <div className="fantasy-search">
            <span>⌕</span>
            <input placeholder="Search books, authors, quotes..." />
          </div>

          <button
            className="fantasy-add-book-button"
            onClick={onAddBook}
          >
            + Add Book
          </button>
        </section>

        <section className="fantasy-dashboard-grid">
          <section className="fantasy-left-column">
            <section className="fantasy-overview-panel">
              <section className="welcome-panel">
                <div>
                  <h2>Welcome back, Heather</h2>
                  <p>
                    Another chapter awaits. Your library is ready for the next
                    adventure.
                  </p>
                </div>

                <span className="welcome-date">
                  Thursday
                  <br />
                  June 11, 2026
                </span>
              </section>

              <FantasyStats />
            </section>

            <section className="fantasy-middle-row">
              <ReadingPathPanel />
              <CollectionOverview />
              <RecentActivity />
            </section>
          </section>

          <aside className="fantasy-right-rail">
            <CurrentReadingWidget />
            <QuoteWidget />
            <ReadingStreak />
            <ReadingGoalWidget />
          </aside>
        </section>

        <section className="fantasy-doorways-section">
          <div className="doorways-section-header">
            <p>NEXT DOORWAYS</p>
            <span>Stories calling to you</span>
          </div>

          <NextDoorways onAddBook={onAddBook} />
        </section>
      </main>
    </div>
  );
}