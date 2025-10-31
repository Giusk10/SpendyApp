import { useForm } from 'react-hook-form';
import styles from '../styles/DateRangeFilters.module.css';

interface DateRangeFormValues {
  startDate: string;
  endDate: string;
  month: string;
  year: string;
}

interface DateRangeFiltersProps {
  onRange: (startDate: string, endDate: string) => void;
  onMonth: (month: string, year: string) => void;
  onReset: () => void;
}

const DateRangeFilters = ({ onRange, onMonth, onReset }: DateRangeFiltersProps) => {
  const {
    register,
    handleSubmit,
    formState: { isSubmitting },
    reset
  } = useForm<DateRangeFormValues>({
    defaultValues: {
      startDate: '',
      endDate: '',
      month: '',
      year: String(new Date().getFullYear())
    }
  });

  const submitRange = handleSubmit((values) => {
    if (!values.startDate || !values.endDate) {
      return;
    }
    onRange(values.startDate, values.endDate);
  });

  const submitMonth = handleSubmit((values) => {
    if (!values.month || !values.year) {
      return;
    }
    onMonth(values.month.padStart(2, '0'), values.year);
  });

  const handleReset = () => {
    reset();
    onReset();
  };

  return (
    <div className={styles.container}>
      <form className={styles.rangeForm} onSubmit={submitRange}>
        <div>
          <label className={styles.label} htmlFor="startDate">
            Data iniziale
          </label>
          <input id="startDate" type="date" className={styles.input} {...register('startDate')} />
        </div>
        <div>
          <label className={styles.label} htmlFor="endDate">
            Data finale
          </label>
          <input id="endDate" type="date" className={styles.input} {...register('endDate')} />
        </div>
        <button type="submit" className={styles.button} disabled={isSubmitting}>
          Filtra intervallo
        </button>
      </form>

      <form className={styles.monthForm} onSubmit={submitMonth}>
        <div>
          <label className={styles.label} htmlFor="month">
            Mese
          </label>
          <select id="month" className={styles.input} {...register('month')}>
            <option value="">Seleziona</option>
            {Array.from({ length: 12 }).map((_, index) => {
              const value = String(index + 1).padStart(2, '0');
              return (
                <option key={value} value={value}>
                  {value}
                </option>
              );
            })}
          </select>
        </div>
        <div>
          <label className={styles.label} htmlFor="year">
            Anno
          </label>
          <input id="year" type="number" className={styles.input} {...register('year')} />
        </div>
        <button type="submit" className={styles.button} disabled={isSubmitting}>
          Filtra mese
        </button>
      </form>

      <button type="button" className={styles.reset} onClick={handleReset} disabled={isSubmitting}>
        Reset filtri
      </button>
    </div>
  );
};

export default DateRangeFilters;
