import { DatabaseSync } from 'node:sqlite';
import { join } from 'node:path';

const DB_PATH = join(process.cwd(), 'data', 'appointments.db');

let instance: DatabaseSync | null = null;

export function getDatabase(): DatabaseSync {
    if (instance) return instance;

    instance = new DatabaseSync(DB_PATH);
    instance.exec(`
        CREATE TABLE IF NOT EXISTS professionals (
            id        INTEGER PRIMARY KEY,
            name      TEXT NOT NULL,
            specialty TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS appointments (
            id             INTEGER PRIMARY KEY AUTOINCREMENT,
            date           TEXT    NOT NULL,
            patientName    TEXT    NOT NULL,
            reason         TEXT    NOT NULL,
            professionalId INTEGER NOT NULL,
            FOREIGN KEY (professionalId) REFERENCES professionals(id)
        );
    `);

    return instance;
}

export function seedProfessionals(professionals: { id: number; name: string; specialty: string }[]) {
    const db = getDatabase();
    const insert = db.prepare(
        'INSERT OR IGNORE INTO professionals (id, name, specialty) VALUES (?, ?, ?)'
    );
    for (const p of professionals) {
        insert.run(p.id, p.name, p.specialty);
    }
}
