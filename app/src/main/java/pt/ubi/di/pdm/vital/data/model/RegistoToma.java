package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Uma toma agendada e o que aconteceu com ela.
 * É esta tabela que alimenta a lista "Hoje" do dashboard e todo o Historial.
 */
@Entity(tableName = "registo_tomas",
        indices = {@Index("medicamentoId"), @Index("dataPrevista")},
        foreignKeys = @ForeignKey(
                entity = Medicamento.class,
                parentColumns = "id",
                childColumns = "medicamentoId",
                onDelete = ForeignKey.CASCADE))
public class RegistoToma {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long medicamentoId;
    private long horarioId;

    /** Momento em que a toma estava marcada. */
    private long dataPrevista;

    /** Momento em que foi efetivamente confirmada; null enquanto não for tomada. */
    private Long dataReal;

    private EstadoToma estado;

    /** Nota opcional ("tomado mais tarde", "faltava o comprimido"). */
    private String observacao;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMedicamentoId() { return medicamentoId; }
    public void setMedicamentoId(long medicamentoId) { this.medicamentoId = medicamentoId; }

    public long getHorarioId() { return horarioId; }
    public void setHorarioId(long horarioId) { this.horarioId = horarioId; }

    public long getDataPrevista() { return dataPrevista; }
    public void setDataPrevista(long dataPrevista) { this.dataPrevista = dataPrevista; }

    public Long getDataReal() { return dataReal; }
    public void setDataReal(Long dataReal) { this.dataReal = dataReal; }

    public EstadoToma getEstado() { return estado; }
    public void setEstado(EstadoToma estado) { this.estado = estado; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
