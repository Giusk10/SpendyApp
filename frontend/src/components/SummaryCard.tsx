interface SummaryCardProps {
  title: string;
  value: string;
  helper?: string;
  accent?: string;
}

export const SummaryCard: React.FC<SummaryCardProps> = ({ title, value, helper, accent }) => {
  return (
    <div className="card" style={{ borderTop: `4px solid ${accent ?? '#2563eb'}` }}>
      <p className="section-subtitle" style={{ marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
        {title}
      </p>
      <h3 style={{ fontSize: '2rem', margin: 0, color: '#0f172a' }}>{value}</h3>
      {helper && (
        <p style={{ marginTop: '0.5rem', color: '#475569', fontSize: '0.9rem' }}>
          {helper}
        </p>
      )}
    </div>
  );
};
