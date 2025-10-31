import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { apiClient, routes } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import styles from '../styles/AuthForm.module.css';

interface LoginFormValues {
  identifier: string;
  password: string;
}

const LoginForm = () => {
  const { register, handleSubmit, formState } = useForm<LoginFormValues>({
    defaultValues: { identifier: '', password: '' }
  });
  const { login } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      const payload = values.identifier.includes('@')
        ? { email: values.identifier, password: values.password }
        : { username: values.identifier, password: values.password };

      const response = await apiClient.post(routes.auth.login, payload);
      const token = response.data?.token ?? response.data;
      const username = payload.username ?? values.identifier;
      login(token, username);
    } catch (err: any) {
      const message = err?.response?.data ?? err?.message ?? 'Credenziali non valide';
      setError(typeof message === 'string' ? message : JSON.stringify(message));
    }
  });

  return (
    <form className={styles.form} onSubmit={onSubmit}>
      <label className={styles.label} htmlFor="identifier">
        Email o username
      </label>
      <input
        id="identifier"
        type="text"
        placeholder="mario.rossi o mario@rossi.it"
        {...register('identifier', { required: 'Inserisci email o username' })}
        className={styles.input}
      />
      {formState.errors.identifier && <span className={styles.error}>{formState.errors.identifier.message}</span>}

      <label className={styles.label} htmlFor="password">
        Password
      </label>
      <input
        id="password"
        type="password"
        placeholder="••••••••"
        {...register('password', { required: 'Inserisci la password' })}
        className={styles.input}
      />
      {formState.errors.password && <span className={styles.error}>{formState.errors.password.message}</span>}

      {error && <div className={styles.alert}>{error}</div>}

      <button type="submit" className={styles.submit} disabled={formState.isSubmitting}>
        {formState.isSubmitting ? 'Accesso...' : 'Accedi'}
      </button>
    </form>
  );
};

export default LoginForm;
