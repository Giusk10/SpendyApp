import { AggregatedExpenseMetrics } from '../types/expense';
import { formatCurrency } from '../utils/expense';

interface Props {
  metrics: AggregatedExpenseMetrics;
}

export const CategoryBreakdown: React.FC<Props> = ({ metrics }) => {
  if (!metrics.categories.length) {
    return null;
  }

  return (
    <div className="card">
      <h3 className="section-title">Categorie più rilevanti</h3>
      <p className="section-subtitle">Dove stai spendendo di più questo periodo</p>
      <div className="chip-group">
        {metrics.categories.map((category) => (
          <div key={category.name} className="chip">
            <strong>{category.name}</strong>&nbsp;
            {formatCurrency(category.total)} · {category.transactions} movimenti
          </div>
        ))}
      </div>
    </div>
  );
};
