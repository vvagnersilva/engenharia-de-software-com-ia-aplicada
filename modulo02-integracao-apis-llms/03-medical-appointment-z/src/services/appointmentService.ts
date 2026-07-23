import { getDatabase, seedProfessionals } from '../database/database.ts';

export const professionals = [
    { id: 1, name: 'Dr. Alicio da Silva', specialty: 'Cardiologia' },
    { id: 2, name: 'Dra. Ana Pereira',    specialty: 'Dermatologia' },
    { id: 3, name: 'Dra. Carol Gomes',    specialty: 'Neurologia' },
];

type Appointment = {
    id?: number;
    date: string;
    patientName: string;
    reason: string;
    professionalId: number;
};

export class AppointmentService {
    constructor() {
        seedProfessionals(professionals);
    }

    getAppointmentsForProfessional(professionalId: number, date: Date, patientName?: string): Appointment | undefined {
        const db = getDatabase();
        const dateStr = date.toISOString();

        if (patientName) {
            return db.prepare(
                'SELECT * FROM appointments WHERE professionalId = ? AND date = ? AND patientName = ?'
            ).get(professionalId, dateStr, patientName) as Appointment | undefined;
        }

        return db.prepare(
            'SELECT * FROM appointments WHERE professionalId = ? AND date = ?'
        ).get(professionalId, dateStr) as Appointment | undefined;
    }

    checkAvailability(professionalId: number, date: Date): boolean {
        const existing = this.getAppointmentsForProfessional(professionalId, date);
        return !existing;
    }

    bookAppointment(professionalId: number, date: Date, patientName: string, reason: string): Appointment {
        if (!this.checkAvailability(professionalId, date)) {
            throw new Error('Horário indisponível para este profissional');
        }

        const db = getDatabase();
        const dateStr = date.toISOString();

        const result = db.prepare(
            'INSERT INTO appointments (date, patientName, reason, professionalId) VALUES (?, ?, ?, ?)'
        ).run(dateStr, patientName, reason, professionalId);

        return {
            id: Number(result.lastInsertRowid),
            date: dateStr,
            patientName,
            reason,
            professionalId,
        };
    }

    cancelAppointment(professionalId: number, patientName: string, date: Date): void {
        const existing = this.getAppointmentsForProfessional(professionalId, date, patientName);
        if (!existing) {
            throw new Error('Agendamento não encontrado para cancelamento');
        }

        const db = getDatabase();
        db.prepare(
            'DELETE FROM appointments WHERE professionalId = ? AND patientName = ? AND date = ?'
        ).run(professionalId, patientName, date.toISOString());
    }
}
