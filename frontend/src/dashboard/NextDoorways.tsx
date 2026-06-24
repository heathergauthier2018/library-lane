import frame from "../assets/storybook/frames/next-doorways-frame.png";

import fantasyDoorway from "../assets/storybook/doorways/fantasy-doorway.png";
import literaryDoorway from "../assets/storybook/doorways/literary-doorway.png";
import scienceFictionDoorway from "../assets/storybook/doorways/science-fiction-doorway.png";
import romanceDoorway from "../assets/storybook/doorways/romance-doorway.png";
import addDoorway from "../assets/storybook/doorways/add-new-doorway.png";

const doorways = [
  { image: fantasyDoorway, title: "The Atlas Six" },
  { image: literaryDoorway, title: "Demon Copperhead" },
  { image: scienceFictionDoorway, title: "Project Hail Mary" },
  { image: romanceDoorway, title: "Book Lovers" },
];

type NextDoorwaysProps = {
  onAddBook: () => void;
};

export default function NextDoorways({ onAddBook }: NextDoorwaysProps) {
  return (
    <section
      className="next-doorways-widget"
      style={{ backgroundImage: `url(${frame})` }}
    >
      <div className="doorways-header">
        <p>Next Doorways</p>
        <span>Stories calling to you</span>
      </div>

      <div className="doorways-grid">
        {doorways.map((doorway) => (
          <div className="doorway-card" key={doorway.title}>
            <img src={doorway.image} alt={doorway.title} />
            <div className="doorway-book-title">{doorway.title}</div>
          </div>
        ))}

        <button
          type="button"
          className="doorway-card add-doorway"
          onClick={onAddBook}
        >
          <img src={addDoorway} alt="Add new story" />
          <div className="doorway-book-title">Add to TBR</div>
        </button>
      </div>
    </section>
  );
}