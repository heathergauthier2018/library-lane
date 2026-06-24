import booksIcon from "../assets/storybook/icons/books-read-icon.png";
import readingIcon from "../assets/storybook/icons/currently-reading-icon.png";
import completedIcon from "../assets/storybook/icons/completed-icon.png";
import favoritesIcon from "../assets/storybook/icons/favorites-icon.png";
import quotesIcon from "../assets/storybook/icons/quotes-icon.png";

const stats = [
  { label: "Books Read", value: "24", sub: "This year", icon: booksIcon },
  { label: "Currently Reading", value: "2", sub: "In progress", icon: readingIcon },
  { label: "Completed", value: "22", sub: "All time", icon: completedIcon },
  { label: "Favorites", value: "18", sub: "Most loved", icon: favoritesIcon },
  { label: "Quotes Saved", value: "143", sub: "Collected lines", icon: quotesIcon },
];

export default function FantasyStats() {
  return (
    <section className="fantasy-stats-row">
      {stats.map((stat) => (
        <article className="fantasy-stat-card" key={stat.label}>
          <img src={stat.icon} alt="" />
          <p>{stat.label}</p>
          <strong>{stat.value}</strong>
          <span>{stat.sub}</span>
        </article>
      ))}
    </section>
  );
}