import { describe, it, before } from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from '../src/server.ts';
import { professionals } from '../src/services/appointmentService.ts';
import { getDatabase } from '../src/database/database.ts';

const app = createServer();

async function makeARequest(question: string) {
    return await app.inject({
        method: 'POST',
        url: '/chat',
        payload: {
            question,
        },
    });
}

// Garante que as consultas criadas por essas execuções anteriores não
// deixem o mesmo horário "ocupado" e quebrem uma nova rodada dos testes.
function clearTestAppointments() {
    const db = getDatabase();
    db.prepare('DELETE FROM appointments WHERE patientName = ? AND professionalId = ?')
        .run('Maria Santos', professionals.at(0)!.id);
    db.prepare('DELETE FROM appointments WHERE patientName = ? AND professionalId = ?')
        .run('Joao da Silva', professionals.at(1)!.id);
}

describe('Medical Appointment System - E2E Tests', async () => {

    before(() => {
        clearTestAppointments();
    });

    it('Schedule appointment - Success', async () => {
        const response = await makeARequest(
            `Olá, sou Maria Santos e quero agendar uma consulta com ${professionals.at(0)?.name} para amanhã às 16h para um check-up regular`
        )

        console.log('Schedule Success Response:', response.body);

        assert.equal(response.statusCode, 200);
        const body = JSON.parse(response.body);
        assert.equal(body.intent, 'schedule');
        assert.equal(body.actionSuccess, true);
    });


    it('Cancel appointment - Success', async () => {

         await makeARequest(
            `Sou Joao da Silva e quero agendar uma consulta com ${professionals.at(1)?.name} para hoje às 14h`
        )

        const response = await makeARequest(
            `Cancele minha consulta com ${professionals.at(1)?.name} que tenho hoje às 14h, me chamo Joao da Silva`
        );

        console.log('Cancel Success Response:', response.body);

        assert.equal(response.statusCode, 200);
        const body = JSON.parse(response.body);
        assert.equal(body.intent, 'cancel');
        assert.equal(body.actionSuccess, true);
    });
});
