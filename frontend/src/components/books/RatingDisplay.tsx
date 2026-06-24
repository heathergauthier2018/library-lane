type RatingDisplayProps = {
  value?: string;
  icon: string;
  className?: string;
};

export default function RatingDisplay({
  value,
  icon,
  className = "",
}: RatingDisplayProps) {
  const numericValue = Number(value || "0");

  if (!numericValue || Number.isNaN(numericValue)) {
    return <span className="reading-experience-not-rated">Not rated</span>;
  }

  const fullIcons = Math.floor(numericValue);
  const hasHalfIcon = numericValue % 1 >= 0.5;

  return (
    <span className={`shared-rating-icons ${className}`}>
      {Array.from({ length: fullIcons }).map((_, index) => (
        <img
          key={`full-${index}`}
          src={icon}
          alt=""
          className="shared-rating-icon"
        />
      ))}

      {hasHalfIcon && (
        <span className="shared-half-rating-wrap">
          <img
            src={icon}
            alt=""
            className="shared-rating-icon shared-half-rating-icon"
          />
        </span>
      )}
    </span>
  );
}