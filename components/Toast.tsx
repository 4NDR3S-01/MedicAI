import { Palette } from '@/constants/Colors';
import React, { createContext, useCallback, useContext, useRef, useState } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';

interface ToastData { id: number; message: string; type?: 'info' | 'success' | 'error'; }
interface ToastContextValue { show: (message: string, type?: ToastData['type']) => void; }

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastData[]>([]);
  const idRef = useRef(0);

  const removeToast = (id: number) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  };

  const show = useCallback((message: string, type: ToastData['type'] = 'info') => {
    const id = ++idRef.current;
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => removeToast(id), 3000);
  }, []);

  const value = React.useMemo(() => ({ show }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <View pointerEvents="none" style={styles.wrap}>
        {toasts.map(t => <ToastItem key={t.id} data={t} />)}
      </View>
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast debe usarse dentro de ToastProvider');
  return ctx;
};

const ToastItem: React.FC<{ data: ToastData }> = ({ data }) => {
  const opacity = useRef(new Animated.Value(0)).current;
  React.useEffect(() => {
    Animated.timing(opacity, { toValue: 1, duration: 180, useNativeDriver: true }).start();
  }, [opacity]);
  let bg = Palette.primary;
  if (data.type === 'success') bg = Palette.success;
  else if (data.type === 'error') bg = Palette.danger;
  return (
    <Animated.View style={[styles.toast, { backgroundColor: bg, opacity }]}> 
      <Text style={styles.toastText}>{data.message}</Text>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  wrap: { position: 'absolute', bottom: 40, left: 0, right: 0, alignItems: 'center', paddingHorizontal: 16 },
  toast: { paddingHorizontal: 16, paddingVertical: 10, borderRadius: 24, marginTop: 8, minWidth: '40%' },
  toastText: { color: '#fff', fontWeight: '600', textAlign: 'center' }
});
