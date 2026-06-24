import { useState } from "react";
import "./App.css";

import FantasyDashboard from "./dashboard/FantasyDashboard";
import BooksPage from "./pages/BooksPage";

export type AppPage =
  | "My Library"
  | "Books"
  | "Authors"
  | "Quotes"
  | "Reading Experiences"
  | "Journal"
  | "TBR"
  | "Wishlist"
  | "Collections"
  | "Goals"
  | "Insights"
  | "Settings";

function App() {
  const [currentPage, setCurrentPage] = useState<AppPage>("My Library");
  const [openAddBookOnBooksPage, setOpenAddBookOnBooksPage] = useState(false);

  function handleGlobalAddBook() {
    setOpenAddBookOnBooksPage(true);
    setCurrentPage("Books");
  }

  if (currentPage === "Books") {
    return (
      <BooksPage
        currentPage={currentPage}
        setCurrentPage={setCurrentPage}
        openAddBookOnLoad={openAddBookOnBooksPage}
        onAddBookOpened={() => setOpenAddBookOnBooksPage(false)}
      />
    );
  }

  return (
    <FantasyDashboard
      currentPage={currentPage}
      setCurrentPage={setCurrentPage}
      onAddBook={handleGlobalAddBook}
    />
  );
}

export default App;