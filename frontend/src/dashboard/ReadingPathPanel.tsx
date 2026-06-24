import adventureMap from "../assets/storybook/maps/Adventure Map Background.png";
import adventureMapFrame from "../assets/storybook/frames/Adventure Map Frame.png";

const sideStats = [
  { label: "Favorite Genre", value: "Fantasy" },
  { label: "Most Read Format", value: "Physical" },
  { label: "Most Active Month", value: "March" },
  { label: "Average Book Length", value: "387 pages" },
  { label: "Highest Rated Genre", value: "Historical Fiction" },
  { label: "New Authors", value: "7" },
];

export default function ReadingPathPanel() {
  return (
    <article
      className="middle-widget reading-path-panel"
      style={{ backgroundImage: `url(${adventureMapFrame})` }}
    >
      <div className="reading-path-card">
        <div className="middle-widget-header">
          <p>Reading Path</p>
          <h3>Adventure Map</h3>
          <span>Your reading journey this year</span>
        </div>

        <div className="adventure-map-content">
          <div className="adventure-map-visual">
            <img
              className="adventure-map-img"
              src={adventureMap}
              alt="Adventure reading map"
            />
          </div>

          <div className="adventure-side-stats">
            {sideStats.map((stat) => (
              <div className="adventure-stat" key={stat.label}>
                <span>{stat.label}</span>
                <strong>{stat.value}</strong>
              </div>
            ))}
          </div>
        </div>
      </div>
    </article>
  );
}