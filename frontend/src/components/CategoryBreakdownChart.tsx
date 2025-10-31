import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { Doughnut } from 'react-chartjs-2';
import styles from '../styles/CategoryBreakdownChart.module.css';
import { formatCurrency } from '../utils/expense';

ChartJS.register(ArcElement, Tooltip, Legend);

interface CategoryBreakdownChartProps {
  categories: Record<string, number>;
  total: number;
}

const palette = ['#38bdf8', '#6366f1', '#f97316', '#22c55e', '#e879f9', '#fbbf24', '#f87171'];

const CategoryBreakdownChart = ({ categories, total }: CategoryBreakdownChartProps) => {
  const labels = Object.keys(categories);
  const values = labels.map((label) => categories[label]);

  const data = {
    labels,
    datasets: [
      {
        label: 'Spesa per categoria',
        data: values,
        backgroundColor: labels.map((_, index) => palette[index % palette.length]),
        borderWidth: 0
      }
    ]
  };

  return (
    <div className={styles.card}>
      <h2>Distribuzione per categoria</h2>
      {!values.length ? (
        <p className={styles.placeholder}>Carica dati per vedere il dettaglio delle categorie.</p>
      ) : (
        <div className={styles.chartWrapper}>
          <Doughnut
            data={data}
            options={{
              cutout: '60%',
              plugins: {
                legend: {
                  display: true,
                  position: 'right',
                  labels: {
                    color: '#e2e8f0'
                  }
                },
                tooltip: {
                  callbacks: {
                    label: (context) => {
                      const label = context.label ?? '';
                      const value = context.parsed ?? 0;
                      const percentage = total ? ((value / total) * 100).toFixed(1) : '0';
                      return `${label}: ${formatCurrency(value)} (${percentage}%)`;
                    }
                  }
                }
              }
            }}
          />
        </div>
      )}
    </div>
  );
};

export default CategoryBreakdownChart;
