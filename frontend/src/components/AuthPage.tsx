import { useState } from 'react';
import clsx from 'clsx';
import LoginForm from './LoginForm';
import RegisterForm from './RegisterForm';
import styles from '../styles/AuthPage.module.css';

const AuthPage = () => {
  const [tab, setTab] = useState<'login' | 'register'>('login');

  return (
    <div className={styles.wrapper}>
      <div className={styles.card}>
        <h1 className={styles.title}>SpendyApp</h1>
        <p className={styles.subtitle}>Gestisci le tue spese con dashboard intelligenti e importazione automatica.</p>

        <div className={styles.tabs}>
          <button
            type="button"
            className={clsx(styles.tabButton, tab === 'login' && styles.active)}
            onClick={() => setTab('login')}
          >
            Accedi
          </button>
          <button
            type="button"
            className={clsx(styles.tabButton, tab === 'register' && styles.active)}
            onClick={() => setTab('register')}
          >
            Registrati
          </button>
        </div>

        <div className={styles.content}>
          {tab === 'login' ? <LoginForm /> : <RegisterForm onSwitchToLogin={() => setTab('login')} />}
        </div>
      </div>
    </div>
  );
};

export default AuthPage;
