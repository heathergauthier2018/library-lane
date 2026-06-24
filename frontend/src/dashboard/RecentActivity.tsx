import recentActivityFrame from "../assets/storybook/frames/Recent Activity Frame.png";
import updatedProgressIcon from "../assets/storybook/icons/Updated Progress.png";
import journalIcon from "../assets/storybook/icons/Journal Entry.png";
import savedQuoteIcon from "../assets/storybook/icons/Saved Quote.png";
import ratedBookIcon from "../assets/storybook/icons/Rated Book.png";

const activities = [
  {
    icon: updatedProgressIcon,
    text: "You updated your progress in The Midnight Library",
  },
  {
    icon: journalIcon,
    text: "New journal entry added",
  },
  {
    icon: savedQuoteIcon,
    text: "You saved a quote from The Four Winds",
  },
  {
    icon: ratedBookIcon,
    text: "You rated The Night Circus",
  },
];

export default function RecentActivity() {
  return (
    <article
      className="middle-widget recent-activity-widget"
      style={{ backgroundImage: `url(${recentActivityFrame})` }}
    >
      <p className="eyebrow">Recent Activity</p>
      <h3>The latest from your journey</h3>

      <div className="activity-list">
        {activities.map((activity) => (
          <div className="activity-row" key={activity.text}>
            <img src={activity.icon} alt="" />
            <span className="activity-text">
  {activity.text}
</span>
          </div>
        ))}
      </div>

      <button className="text-link-button">View all activity →</button>
    </article>
  );
}