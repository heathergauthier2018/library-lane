import dailyRitualFrame from "../assets/storybook/frames/Daily Ritual Frame.png";

export default function ReadingStreak() {
  return (
    <article
      className="middle-widget reading-streak-widget"
      style={{ backgroundImage: `url(${dailyRitualFrame})` }}
    >
      <h3>Reading Streak</h3>

      <div className="streak-horizontal-layout">
        <div className="streak-count-side">
          <strong className="streak-number">12</strong>

          <span className="streak-days">
            days in a row
          </span>
        </div>

        <div className="streak-divider" />

        <div className="streak-magic-side">
          <span className="sparkle sparkle-1">✦</span>
          <span className="sparkle sparkle-2">✦</span>
          <span className="sparkle sparkle-3">✦</span>
          <span className="sparkle sparkle-4">✦</span>
          <span className="sparkle sparkle-5">✦</span>
          <span className="sparkle sparkle-6">✦</span>
          <span className="sparkle sparkle-7">✦</span>
          <span className="sparkle sparkle-8">✦</span>
          <span className="sparkle sparkle-9">✦</span>
          <span className="sparkle sparkle-10">✦</span>

          <span className="magic-word keep">Keep</span>
          <span className="magic-word the">the</span>
          <span className="magic-word magic">magic</span>
          <span className="magic-word going">going</span>
        </div>
      </div>
    </article>
  );
}