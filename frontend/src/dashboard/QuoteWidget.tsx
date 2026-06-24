import quoteFrame from "../assets/storybook/panels/fantasy-quote-frame.png";

export default function QuoteWidget() {
  return (
    <article
      className="fantasy-widget fantasy-quote-widget"
      style={{ backgroundImage: `url(${quoteFrame})` }}
    >
      <h3>Today's Quote</h3>

      <blockquote>
        “You have to write the book that wants to be written.”
      </blockquote>

      <span>— Madeleine L'Engle</span>
    </article>
  );
}