import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { apiClient, routes } from '../lib/api';
import styles from '../styles/AuthForm.module.css';

interface RegisterFormValues {
  username: string;
  name: string;
  surname: string;
  email: string;
  password: string;
  confirmPassword: string;
}

interface RegisterFormProps {
  onSwitchToLogin: () => void;
}

const RegisterForm = ({ onSwitchToLogin }: RegisterFormProps) => {
  const { register, handleSubmit, watch, formState, reset } = useForm<RegisterFormValues>({
    defaultValues: {
      username: '',
      name: '',
      surname: '',
      email: '',
      password: '',
      confirmPassword: ''
    }
  });
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    setMessage(null);

    if (values.password !== values.confirmPassword) {
      setError('Le password non coincidono.');
      return;
    }

    try {
      await apiClient.post(routes.auth.register, {
        username: values.username,
        name: values.name,
        surname: values.surname,
        email: values.email,
        password: values.password
      });
      setMessage('Registrazione completata! Ora puoi accedere.');
      reset();
      setTimeout(onSwitchToLogin, 1500);
    } catch (err: any) {
      const responseMessage = err?.response?.data ?? err?.message ?? 'Registrazione fallita';
      setError(typeof responseMessage === 'string' ? responseMessage : JSON.stringify(responseMessage));
    }
  });

  return (
    <form className={styles.form} onSubmit={onSubmit}>
      <div className={styles.inlineGroup}>
        <div className={styles.group}>
          <label className={styles.label} htmlFor="username">
            Username
          </label>
          <input
            id="username"
            type="text"
            placeholder="mario.rossi"
            {...register('username', { required: 'Inserisci un username' })}
            className={styles.input}
          />
          {formState.errors.username && <span className={styles.error}>{formState.errors.username.message}</span>}
        </div>

        <div className={styles.group}>
          <label className={styles.label} htmlFor="email">
            Email
          </label>
          <input
            id="email"
            type="email"
            placeholder="mario@rossi.it"
            {...register('email', {
              required: 'Inserisci un indirizzo email',
              pattern: { value: /\S+@\S+\.\S+/, message: 'Email non valida' }
            })}
            className={styles.input}
          />
          {formState.errors.email && <span className={styles.error}>{formState.errors.email.message}</span>}
        </div>
      </div>

      <div className={styles.inlineGroup}>
        <div className={styles.group}>
          <label className={styles.label} htmlFor="name">
            Nome
          </label>
          <input
            id="name"
            type="text"
            placeholder="Mario"
            {...register('name', { required: 'Inserisci il nome' })}
            className={styles.input}
          />
          {formState.errors.name && <span className={styles.error}>{formState.errors.name.message}</span>}
        </div>

        <div className={styles.group}>
          <label className={styles.label} htmlFor="surname">
            Cognome
          </label>
          <input
            id="surname"
            type="text"
            placeholder="Rossi"
            {...register('surname', { required: 'Inserisci il cognome' })}
            className={styles.input}
          />
          {formState.errors.surname && <span className={styles.error}>{formState.errors.surname.message}</span>}
        </div>
      </div>

      <label className={styles.label} htmlFor="password">
        Password
      </label>
      <input
        id="password"
        type="password"
        placeholder="••••••••"
        {...register('password', {
          required: 'Inserisci una password',
          minLength: { value: 6, message: 'Almeno 6 caratteri' }
        })}
        className={styles.input}
      />
      {formState.errors.password && <span className={styles.error}>{formState.errors.password.message}</span>}

      <label className={styles.label} htmlFor="confirmPassword">
        Conferma password
      </label>
      <input
        id="confirmPassword"
        type="password"
        placeholder="••••••••"
        {...register('confirmPassword', {
          validate: (value) => value === watch('password') || 'Le password devono coincidere'
        })}
        className={styles.input}
      />
      {formState.errors.confirmPassword && <span className={styles.error}>{formState.errors.confirmPassword.message}</span>}

      {error && <div className={styles.alert}>{error}</div>}
      {message && <div className={styles.success}>{message}</div>}

      <button type="submit" className={styles.submit} disabled={formState.isSubmitting}>
        {formState.isSubmitting ? 'Registrazione...' : 'Crea account'}
      </button>
    </form>
  );
};

export default RegisterForm;
