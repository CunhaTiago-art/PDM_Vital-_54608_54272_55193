package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Uma hora de toma de um medicamento (um medicamento pode ter várias: 08:00, 14:00, 21:00).
 * Apagar o medicamento apaga os horários em cascata.
 */
@Entity(tableName = "horarios",
        indices = {@Index("medicamentoId")},
        foreignKeys = @ForeignKey(
                entity = Medicamento.class,
                parentColumns = "id",
                childColumns = "medicamentoId",
                onDelete = ForeignKey.CASCADE))
public class Horario {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long medicamentoId;

    /** Hora do dia em minutos desde a meia-noite: 08:00 = 480, 14:00 = 840. */
    private int minutosDoDia;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMedicamentoId() { return medicamentoId; }
    public void setMedicamentoId(long medicamentoId) { this.medicamentoId = medicamentoId; }

    public int getMinutosDoDia() { return minutosDoDia; }
    public void setMinutosDoDia(int minutosDoDia) { this.minutosDoDia = minutosDoDia; }
}
