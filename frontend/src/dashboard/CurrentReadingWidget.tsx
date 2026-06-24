const currentBook = {
  title: "The Midnight Library",
  author: "Matt Haig",
  coverUrl: "https://placehold.co/92x138/1f3b57/f8e7bd?text=Book%20Cover",
  currentPage: 76,
  totalPages: 304,
};

export default function CurrentReadingWidget() {
  const progress = Math.round(
    (currentBook.currentPage / currentBook.totalPages) * 100
  );

  return (
    <article className="fantasy-widget fantasy-current-widget">
      <h3>Currently Reading</h3>

      <div className="current-reading-layout">
        <img src={currentBook.coverUrl} alt={`${currentBook.title} cover`} />

        <div className="current-reading-details">
          <h4>{currentBook.title}</h4>
          <p className="current-author">by {currentBook.author}</p>

          <div className="current-progress-bar">
            <div style={{ width: `${progress}%` }} />
          </div>

          <div className="current-reading-meta">
            <span>
              {currentBook.currentPage} of {currentBook.totalPages} pages
            </span>
            <span>{progress}%</span>
          </div>

          <button>Continue Reading →</button>
        </div>
      </div>
    </article>
  );
}