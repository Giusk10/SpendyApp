import { ChangeEvent, useRef, useState } from 'react';
import * as XLSX from 'xlsx';
import { apiClient, routes } from '../lib/api';
import styles from '../styles/FileUploadCard.module.css';

interface FileUploadCardProps {
  onUploadSuccess: () => void;
}

const acceptedExtensions = ['.csv', '.xlsx', '.xls'];

const FileUploadCard = ({ onUploadSuccess }: FileUploadCardProps) => {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [fileName, setFileName] = useState<string>('Nessun file selezionato');
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      setFileName('Nessun file selezionato');
      return;
    }

    setFileName(file.name);
    setError(null);
    setSuccess(null);

    if (!acceptedExtensions.some((ext) => file.name.toLowerCase().endsWith(ext))) {
      setError('Formato non supportato. Carica un file CSV o Excel.');
      event.target.value = '';
      return;
    }

    try {
      setIsUploading(true);
      const formData = new FormData();

      if (file.name.toLowerCase().endsWith('.csv')) {
        formData.append('file', file);
      } else {
        const arrayBuffer = await file.arrayBuffer();
        const workbook = XLSX.read(arrayBuffer, { type: 'array' });
        const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
        const csv = XLSX.utils.sheet_to_csv(firstSheet);
        const csvBlob = new Blob([csv], { type: 'text/csv' });
        formData.append('file', csvBlob, `${file.name.replace(/\.[^.]+$/, '')}.csv`);
      }

      const response = await apiClient.post(routes.expense.import, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      const message = typeof response.data === 'string' ? response.data : 'File importato con successo';
      setSuccess(message);
      onUploadSuccess();
      if (inputRef.current) {
        inputRef.current.value = '';
      }
    } catch (err: any) {
      const message = err?.response?.data ?? err?.message ?? 'Importazione fallita';
      setError(typeof message === 'string' ? message : JSON.stringify(message));
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className={styles.card}>
      <h2>Importa estratto conto</h2>
      <p className={styles.description}>
        Carica un file CSV o Excel (formato N26/Revolut) per importare automaticamente movimenti e categorie.
      </p>
      <label className={styles.uploadButton}>
        <input
          ref={inputRef}
          type="file"
          accept={acceptedExtensions.join(',')}
          onChange={handleFileChange}
          hidden
        />
        <span>{isUploading ? 'Caricamento in corso...' : 'Seleziona file'}</span>
      </label>
      <p className={styles.fileName}>{fileName}</p>
      {error && <div className={styles.error}>{error}</div>}
      {success && <div className={styles.success}>{success}</div>}
    </div>
  );
};

export default FileUploadCard;
