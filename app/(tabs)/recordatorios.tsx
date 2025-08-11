import { EmptyState } from '@/components/EmptyState';
import { Colors, Palette } from '@/constants/Colors';
import { useReminderStore } from '@/store/reminderStore';
import React, { useState } from 'react';
import { FlatList, Modal, StyleSheet, Text, TextInput, TouchableOpacity, useColorScheme, View } from 'react-native';

export default function RecordatoriosScreen() {
  const { medications, reminders, addMedication, addReminder, toggleReminder, updateMedication, updateReminder, deleteMedication, deleteReminder } = useReminderStore();
  const [openMedModal, setOpenMedModal] = useState(false);
  const [openRemModal, setOpenRemModal] = useState(false);
  const [medName, setMedName] = useState('');
  const [dose, setDose] = useState('');
  const [unit, setUnit] = useState('mg');
  const [remMedId, setRemMedId] = useState<string | undefined>();
  const [time, setTime] = useState('08:00');
  const [editingMedId, setEditingMedId] = useState<string | null>(null);
  const [editingReminderId, setEditingReminderId] = useState<string | null>(null);
  const [editTime, setEditTime] = useState('');
  const [errors, setErrors] = useState<{ med?: string; dose?: string; time?: string }>({});
  const scheme = useColorScheme();

  const handleAddMedication = () => {
    const newErrors: typeof errors = {};
    if (!medName.trim()) newErrors.med = 'Nombre requerido';
    if (!dose || isNaN(Number(dose))) newErrors.dose = 'Dosis inválida';
    setErrors(newErrors);
    if (Object.keys(newErrors).length) return;
    const med = addMedication({ name: medName.trim(), dose: Number(dose), unit, userId: 'local', instructions: '' });
    setRemMedId(med.id);
    setMedName('');
    setDose('');
    setErrors({});
    setOpenMedModal(false);
  };

  const handleAddReminder = () => {
    const newErrors: typeof errors = {};
    if (!remMedId) newErrors.med = 'Selecciona medicamento';
    if (!/^\d{2}:\d{2}$/.test(time)) newErrors.time = 'Formato HH:MM';
    setErrors(newErrors);
    if (Object.keys(newErrors).length) return;
    addReminder({ userId: 'local', medicationId: remMedId!, scheduleType: 'FIXED_TIMES', times: [time], timezone: 'UTC', enabled: true });
    setOpenRemModal(false);
  };

  return (
    <View style={styles.container}>
      {/* Medicamentos */}
      <View style={styles.header}> 
        <Text style={[styles.title, { color: Colors[scheme || 'light'].tint }]}>Medicamentos</Text>
        <TouchableOpacity onPress={() => { setEditingMedId(null); setOpenMedModal(true); }} style={styles.addBtn}><Text style={styles.addBtnText}>+ Med</Text></TouchableOpacity>
      </View>
      <FlatList
        data={medications}
        keyExtractor={i => i.id}
        renderItem={({ item }) => (
          <View style={styles.cardRow}> 
            <Text style={styles.cardTitle}>{item.name} {item.dose}{item.unit}</Text>
            <View style={styles.rowActions}> 
              <TouchableOpacity onPress={() => { setEditingMedId(item.id); setMedName(item.name); setDose(String(item.dose)); setUnit(item.unit); setOpenMedModal(true); }}><Text style={styles.actionLink}>Editar</Text></TouchableOpacity>
              <TouchableOpacity onPress={() => deleteMedication(item.id)}><Text style={[styles.actionLink, { color: Palette.danger }]}>Borrar</Text></TouchableOpacity>
            </View>
          </View>
        )}
        ListEmptyComponent={<EmptyState title="Sin medicamentos" message="Agrega tu primer medicamento" action={<TouchableOpacity onPress={() => setOpenMedModal(true)} style={styles.cta}><Text style={styles.ctaText}>Agregar</Text></TouchableOpacity>} />}
      />
      {/* Recordatorios */}
      <View style={styles.header}> 
        <Text style={[styles.title, { color: Colors[scheme || 'light'].tint }]}>Recordatorios</Text>
        <TouchableOpacity onPress={() => { setEditingReminderId(null); setOpenRemModal(true); if (medications[0]) setRemMedId(medications[0].id); }} style={styles.addBtn}><Text style={styles.addBtnText}>+ Rec</Text></TouchableOpacity>
      </View>
      <FlatList
        data={reminders}
        keyExtractor={i => i.id}
        renderItem={({ item }) => (
          <View style={styles.cardRow}> 
            <Text style={styles.cardTitle}>{medications.find(m => m.id === item.medicationId)?.name || 'Med'} - {(item.times || []).join(', ')}</Text>
            <View style={styles.rowActions}> 
              <TouchableOpacity onPress={() => { setEditingReminderId(item.id); setRemMedId(item.medicationId); setEditTime(item.times?.[0] || '08:00'); setOpenRemModal(true); }}><Text style={styles.actionLink}>Editar</Text></TouchableOpacity>
              <TouchableOpacity onPress={() => deleteReminder(item.id)}><Text style={[styles.actionLink, { color: Palette.danger }]}>Borrar</Text></TouchableOpacity>
              <TouchableOpacity onPress={() => toggleReminder(item.id, !item.enabled)}><Text style={[styles.actionLink, { color: item.enabled ? Palette.success : Palette.danger }]}>{item.enabled ? 'ON' : 'OFF'}</Text></TouchableOpacity>
            </View>
          </View>
        )}
        ListEmptyComponent={<EmptyState title="Sin recordatorios" message="Crea un recordatorio para que no olvides tu medicación" action={<TouchableOpacity onPress={() => { setOpenRemModal(true); if (medications[0]) setRemMedId(medications[0].id); }} style={styles.cta}><Text style={styles.ctaText}>Crear</Text></TouchableOpacity>} />}
      />
      {/* Modal medicamento */}
      <Modal visible={openMedModal} transparent animationType="slide">
        <View style={styles.modalBackdrop}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Nuevo medicamento</Text>
            <TextInput placeholder="Nombre" value={medName} onChangeText={setMedName} style={[styles.input, errors.med && styles.inputError]} />
            {errors.med && <Text style={styles.errorText}>{errors.med}</Text>}
            <TextInput placeholder="Dosis" value={dose} onChangeText={setDose} keyboardType="numeric" style={[styles.input, errors.dose && styles.inputError]} />
            {errors.dose && <Text style={styles.errorText}>{errors.dose}</Text>}
            <TextInput placeholder="Unidad" value={unit} onChangeText={setUnit} style={styles.input} />
            <View style={styles.modalActions}>
              <TouchableOpacity onPress={() => setOpenMedModal(false)}><Text>Cancelar</Text></TouchableOpacity>
              <TouchableOpacity onPress={editingMedId ? () => { updateMedication(editingMedId, { name: medName.trim(), dose: Number(dose), unit }); setOpenMedModal(false); } : handleAddMedication}><Text style={{ color: Palette.primary }}>{editingMedId ? 'Actualizar' : 'Guardar'}</Text></TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
      {/* Modal recordatorio */}
      <Modal visible={openRemModal} transparent animationType="slide">
        <View style={styles.modalBackdrop}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Nuevo recordatorio</Text>
            <Text style={{ marginBottom: 4 }}>Medicamento: {medications.find(m => m.id === remMedId)?.name || '-'}</Text>
            <TextInput placeholder="Hora (HH:MM)" value={editingReminderId ? editTime : time} onChangeText={val => editingReminderId ? setEditTime(val) : setTime(val)} style={[styles.input, errors.time && styles.inputError]} />
            {errors.time && <Text style={styles.errorText}>{errors.time}</Text>}
            <View style={styles.modalActions}>
              <TouchableOpacity onPress={() => setOpenRemModal(false)}><Text>Cancelar</Text></TouchableOpacity>
              <TouchableOpacity onPress={editingReminderId ? () => { if (!remMedId) return; updateReminder(editingReminderId, { times: [editTime] }); setOpenRemModal(false); } : handleAddReminder}><Text style={{ color: Palette.primary }}>{editingReminderId ? 'Actualizar' : 'Guardar'}</Text></TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 12 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 },
  title: { fontSize: 20, fontWeight: '600' },
  addBtn: { backgroundColor: Palette.primary, paddingHorizontal: 12, paddingVertical: 6, borderRadius: 8 },
  addBtnText: { color: '#fff', fontWeight: '600' },
  card: { padding: 12, backgroundColor: '#f1f7f8', borderRadius: 12, marginVertical: 6 },
  cardRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 12, backgroundColor: '#f1f7f8', borderRadius: 12, marginVertical: 6 },
  cardTitle: { fontSize: 16, fontWeight: '500' },
  cta: { backgroundColor: Palette.primary, paddingHorizontal: 18, paddingVertical: 10, borderRadius: 24 },
  ctaText: { color: '#fff', fontWeight: '600' },
  empty: { textAlign: 'center', marginVertical: 12, color: '#666' },
  modalBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', padding: 24 },
  modalContent: { backgroundColor: '#fff', borderRadius: 16, padding: 20 },
  modalTitle: { fontSize: 18, fontWeight: '600', marginBottom: 12 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 10, marginBottom: 10 },
  inputError: { borderColor: Palette.danger },
  errorText: { color: Palette.danger, fontSize: 12, marginBottom: 6 },
  modalActions: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 },
  rowActions: { flexDirection: 'row', gap: 12 },
  actionLink: { color: Palette.primary, fontWeight: '600', marginLeft: 12 }
});