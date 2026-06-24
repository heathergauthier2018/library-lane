import collectionFrame from "../assets/storybook/frames/Collection Overview Frame.png";

const collectionStats = [
  { label: "Completed", value: 22, color: "green" },
  { label: "Currently Reading", value: 2, color: "blue" },
  { label: "TBR", value: 15, color: "gold" },
  { label: "Wishlist", value: 9, color: "red" },
  { label: "DNF", value: 3, color: "gray" },
];

export default function CollectionOverview() {
  return (
    <article
      className="middle-widget collection-overview-widget"
      style={{ backgroundImage: `url(${collectionFrame})` }}
    >
      <p className="eyebrow">Collection Overview</p>
      <h3>Your collection at a glance</h3>

      <div className="collection-list">
        {collectionStats.map((item) => (
          <div className="collection-row" key={item.label}>
            <span className={`collection-dot ${item.color}`} />
            <span className="collection-label">
  {item.label}
</span>
            <strong>{item.value}</strong>
          </div>
        ))}
      </div>

      <button className="text-link-button">View full collection →</button>
    </article>
  );
}