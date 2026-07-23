import { AppointmentService } from '../services/appointmentService.ts';

const svc = new AppointmentService();

const today = new Date();
today.setUTCHours(11, 0, 0, 0);

const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
tomorrow.setUTCHours(14, 0, 0, 0);

const seeds = [
    { professionalId: 1, date: today,    patientName: 'Joao da Silva', reason: 'check-up regular' },
    { professionalId: 2, date: tomorrow, patientName: 'Luana Costa',   reason: 'Erupção cutânea' },
    { professionalId: 3, date: tomorrow, patientName: 'Carlos Mendes', reason: 'Dor de cabeça persistente' },
];

for (const s of seeds) {
    try {
        const appt = svc.bookAppointment(s.professionalId, s.date, s.patientName, s.reason);
        console.log(`✅ Agendado: ${s.patientName} com profissional #${s.professionalId} — id ${appt.id}`);
    } catch (e) {
        console.log(`⚠️  Já existe: ${s.patientName} — ${(e as Error).message}`);
    }
}

console.log('\nSeed concluído.');
