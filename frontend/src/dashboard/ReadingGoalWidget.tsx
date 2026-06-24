import goalFrame from "../assets/storybook/panels/fantasy-goal-frame.png";

export default function ReadingGoalWidget() {
  return (
    <article
      className="fantasy-widget fantasy-goal-widget"
      style={{ backgroundImage: `url(${goalFrame})` }}
    >
      <h3>2026 Reading Goal</h3>

      <p className="goal-main-text">30 of 50 books completed</p>

      <div className="goal-progress-row">
        <div className="widget-progress">
          <div style={{ width: "60%" }} />
        </div>
        <span>60%</span>
      </div>

      <p className="goal-remaining">20 books to go</p>
    </article>
  );
}