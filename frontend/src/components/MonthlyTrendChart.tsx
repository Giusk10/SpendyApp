import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler } from 'chart.js';
import { Line } from 'react-chartjs-2';
import { monthLabels, monthValues } from '../utils/expense';
import styles from '../styles/MonthlyTrendChart.module.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler);

interface MonthlyTrendChartProps {
  data: Record<string, number>;
  isLoading: boolean;
  error: string | null;
  onYearChange: (year: string) => void;
  selectedYear: string;
}

const MonthlyTrendChart = ({ data, isLoading, error, onYearChange, selectedYear }: MonthlyTrendChartProps) => {
  const labels = monthLabels(data);
  const values = monthValues(data);

  const chartData = {
    labels,
    datasets: [
      {
        label: `Andamento ${selectedYear}`,
        data: values,
        fill: true,
        borderColor: '#38bdf8',
        backgroundColor: 'rgba(56, 189, 248, 0.2)',
        tension: 0.4,
        pointRadius: 4,
        pointBackgroundColor: '#0ea5e9'
      }
    ]
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (context: any) => `${context.parsed.y.toFixed(2)} €`
        }
      }
    },
    scales: {
      y: {
        grid: { color: 'rgba(148, 163, 184, 0.15)' },
        ticks: {
          callback: (value: number | string) => `${value} €`
        }
      },
      x: {
        grid: { color: 'rgba(148, 163, 184, 0.1)' }
      }
    }
  } as const;

  return (
    <div className={styles.card}>
      <div className={styles.header}>
        <h2>Andamento mensile</h2>
        <input
          type="number"
          value={selectedYear}
          onChange={(event) => onYearChange(event.target.value)}
          className={styles.yearInput}
        />
      </div>
      {isLoading ? (
        <p className={styles.placeholder}>Caricamento dati...</p>
      ) : error ? (
        <p className={styles.error}>{error}</p>
      ) : !values.length ? (
        <p className={styles.placeholder}>Nessun dato disponibile per l'anno selezionato.</p>
      ) : (
        <div className={styles.chartWrapper}>
          <Line data={chartData} options={options} />
        </div>
      )}
    </div>
  );
};

export default MonthlyTrendChart;
