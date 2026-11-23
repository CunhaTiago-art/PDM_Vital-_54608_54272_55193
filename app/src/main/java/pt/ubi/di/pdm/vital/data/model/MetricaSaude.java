package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Registo de uma métrica de saúde (batimento, oxigénio, passos, água). */
@Entity(tableName = "metricas_saude", indices = {@Index("tipo"), @Index("dataHora")})
public class MetricaSaude {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private TipoMetrica tipo;
    private double valor;
    private long dataHora;

    /** Quem ou o que registou o valor ("manual", "relógio"). */
    private String origem;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public TipoMetrica getTipo() { return tipo; }
    public void setTipo(TipoMetrica tipo) { this.tipo = tipo; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public long getDataHora() { return dataHora; }
    public void setDataHora(long dataHora) { this.dataHora = dataHora; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
}
