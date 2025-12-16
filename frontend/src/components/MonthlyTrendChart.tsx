import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts';
import { formatCurrency } from '../utils/expense';

interface Props {
  data: Array<{ month: string; value: number }>;
}

export const MonthlyTrendChart: React.FC<Props> = ({ data }) => {
  return (
    <div className="card" style={{ minHeight: '320px' }}>
      <h3 className="section-title">Andamento mensile</h3>
      <p className="section-subtitle">Importi assoluti delle uscite registrate per mese</p>
      <ResponsiveContainer width="100%" height={240}>
        <AreaChart data={data} margin={{ top: 20, right: 20, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#2563eb" stopOpacity={0.8} />
              <stop offset="100%" stopColor="#2563eb" stopOpacity={0.1} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis dataKey="month" stroke="#475569" />
          <YAxis tickFormatter={(value) => formatCurrency(value).replace('€', '').trim()} stroke="#475569" />
          <Tooltip
            formatter={(value: number) => formatCurrency(value)}
            contentStyle={{ borderRadius: '0.75rem', border: '1px solid rgba(148,163,184,0.3)' }}
          />
          <Area
            type="monotone"
            dataKey="value"
            stroke="#1d4ed8"
            fill="url(#colorExpense)"
            strokeWidth={2.5}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};
