type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface LogEntry {
  timestamp: string;
  level: LogLevel;
  message: string;
  data?: unknown;
}

const isDev = import.meta.env.DEV;

const formatTimestamp = (): string => {
  return new Date().toISOString();
};

const log = (level: LogLevel, message: string, data?: unknown): void => {
  const entry: LogEntry = {
    timestamp: formatTimestamp(),
    level,
    message,
    data,
  };

  if (!isDev && level === 'debug') return;

  const styles: Record<LogLevel, string> = {
    debug: 'color: #6b7280',
    info: 'color: #3b82f6',
    warn: 'color: #f59e0b',
    error: 'color: #ef4444',
  };

  console.log(
    `%c[${entry.level.toUpperCase()}] ${entry.timestamp}`,
    styles[level],
    message,
    data ?? ''
  );
};

export const logger = {
  debug: (message: string, data?: unknown) => log('debug', message, data),
  info: (message: string, data?: unknown) => log('info', message, data),
  warn: (message: string, data?: unknown) => log('warn', message, data),
  error: (message: string, data?: unknown) => log('error', message, data),
};

export default logger;
