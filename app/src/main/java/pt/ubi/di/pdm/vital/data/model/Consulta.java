package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Consulta marcada no ecrã "Marcar consulta".
 * O acompanhante é um membro da rede de apoio; se esse membro for removido,
 * a consulta fica sem acompanhante em vez de desaparecer.
 */
@Entity(tableName = "consultas",
        indices = {@Index("dataHora"), @Index("acompanhanteId")},
        foreignKeys = @ForeignKey(
                entity = Membro.class,
                parentColumns = "id",
                childColumns = "acompanhanteId",
                onDelete = ForeignKey.SET_NULL))
public class Consulta {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String especialidade;     // "Cardiologia"
    private long dataHora;
    private String local;             // "Centro de saúde, hospital..."
    private String medico;            // opcional, pode ser null

    /** Membro que acompanha; null se ainda não estiver definido. */
    private Long acompanhanteId;

    /** Antecedência do lembrete, em minutos: 60 = 1 hora, 1440 = 1 dia, 10080 = 1 semana. */
    private int lembreteMinutos;

    private EstadoConsulta estado;
    private String notas;             // opcional, pode ser null

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }

    public long getDataHora() { return dataHora; }
    public void setDataHora(long dataHora) { this.dataHora = dataHora; }

    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }

    public String getMedico() { return medico; }
    public void setMedico(String medico) { this.medico = medico; }

    public Long getAcompanhanteId() { return acompanhanteId; }
    public void setAcompanhanteId(Long acompanhanteId) { this.acompanhanteId = acompanhanteId; }

    public int getLembreteMinutos() { return lembreteMinutos; }
    public void setLembreteMinutos(int lembreteMinutos) { this.lembreteMinutos = lembreteMinutos; }

    public EstadoConsulta getEstado() { return estado; }
    public void setEstado(EstadoConsulta estado) { this.estado = estado; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
