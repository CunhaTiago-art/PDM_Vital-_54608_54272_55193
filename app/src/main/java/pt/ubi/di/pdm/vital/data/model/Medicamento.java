package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Prescrição guardada na Caixa de Remédios.
 * Os horários de cada dia estão na tabela associada {@link Horario}.
 */
@Entity(tableName = "medicamentos")
public class Medicamento {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String nome;              // "Metformina"
    private String dosagem;           // "500 mg"
    private int quantidadePorToma;    // número de comprimidos por toma
    private String instrucoes;        // "com o almoço" - opcional, pode ser null
    private String fotoUri;           // foto da caixa - opcional, pode ser null

    /** Dias da semana em que é tomado, em máscara de bits (ver DiasSemana). */
    private int diasSemana;

    /** Falso quando o medicamento é suspenso sem apagar o historial de tomas. */
    private boolean ativo;

    private long criadoEm;            // milissegundos desde 1970

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDosagem() { return dosagem; }
    public void setDosagem(String dosagem) { this.dosagem = dosagem; }

    public int getQuantidadePorToma() { return quantidadePorToma; }
    public void setQuantidadePorToma(int quantidadePorToma) { this.quantidadePorToma = quantidadePorToma; }

    public String getInstrucoes() { return instrucoes; }
    public void setInstrucoes(String instrucoes) { this.instrucoes = instrucoes; }

    public String getFotoUri() { return fotoUri; }
    public void setFotoUri(String fotoUri) { this.fotoUri = fotoUri; }

    public int getDiasSemana() { return diasSemana; }
    public void setDiasSemana(int diasSemana) { this.diasSemana = diasSemana; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public long getCriadoEm() { return criadoEm; }
    public void setCriadoEm(long criadoEm) { this.criadoEm = criadoEm; }
}
