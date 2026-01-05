import { useState, useCallback } from 'react';
import { logger } from '@/utils/logger';

interface ApiState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

interface UseApiReturn<T> extends ApiState<T> {
  execute: (...args: unknown[]) => Promise<T | null>;
  reset: () => void;
}

export function useApi<T>(
  apiFunction: (...args: unknown[]) => Promise<T>,
  options?: {
    onSuccess?: (data: T) => void;
    onError?: (error: string) => void;
    retries?: number;
  }
): UseApiReturn<T> {
  const [state, setState] = useState<ApiState<T>>({
    data: null,
    loading: false,
    error: null,
  });

  const execute = useCallback(
    async (...args: unknown[]): Promise<T | null> => {
      setState((prev) => ({ ...prev, loading: true, error: null }));

      const maxRetries = options?.retries ?? 1;
      let lastError: string = '';

      for (let attempt = 0; attempt < maxRetries; attempt++) {
        try {
          const data = await apiFunction(...args);
          setState({ data, loading: false, error: null });
          options?.onSuccess?.(data);
          return data;
        } catch (err) {
          lastError = err instanceof Error ? err.message : 'Unknown error';
          logger.warn(`API attempt ${attempt + 1} failed`, { error: lastError });

          if (attempt < maxRetries - 1) {
            await new Promise((resolve) => setTimeout(resolve, 1000 * (attempt + 1)));
          }
        }
      }

      setState({ data: null, loading: false, error: lastError });
      options?.onError?.(lastError);
      return null;
    },
    [apiFunction, options]
  );

  const reset = useCallback(() => {
    setState({ data: null, loading: false, error: null });
  }, []);

  return { ...state, execute, reset };
}

export default useApi;
