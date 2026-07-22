import type { AppPage } from "../App";
import sidebarBg from "../assets/storybook/backgrounds/fantasy-sidebar.png";

const navItems: AppPage[] = [
  "My Library",
  "Books",
  "Collections",
  "Goals",
  "Insights",
  "Settings",
];

type FantasySidebarProps = {
  currentPage: AppPage;
  setCurrentPage: (page: AppPage) => void;
};

export default function FantasySidebar({
  currentPage,
  setCurrentPage,
}: FantasySidebarProps) {
  return (
    <aside
      className="fantasy-sidebar"
      style={{ backgroundImage: `url(${sidebarBg})` }}
    >
      <div className="sidebar-brand">
        <div className="sidebar-logo">📖</div>

        <div>
          <h1>Library Lane</h1>
          <p>Your reading journey</p>
        </div>
      </div>

      <nav className="sidebar-nav" aria-label="Library navigation">
        {navItems.map((item) => (
          <button
            key={item}
            type="button"
            className={currentPage === item ? "active" : ""}
            aria-current={currentPage === item ? "page" : undefined}
            onClick={() => setCurrentPage(item)}
          >
            {item}
          </button>
        ))}
      </nav>

      <div className="sidebar-profile">
        <div className="profile-avatar">H</div>

        <div>
          <strong>Heather G.</strong>
          <p>Book Lover</p>
        </div>
      </div>
    </aside>
  );
}