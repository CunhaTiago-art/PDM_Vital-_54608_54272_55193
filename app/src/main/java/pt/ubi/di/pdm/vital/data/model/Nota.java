package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Nota deixada por um membro da rede no historial
 * (ex.: "Avó, lembra-te de beber água", do ecrã Historial).
 */
@Entity(tableName = "notas",
        indices = {@Index("autorId"), @Index("dataHora")},
        foreignKeys = @ForeignKey(
                entity = Membro.class,
                parentColumns = "id",
                childColumns = "autorId",
                onDelete = ForeignKey.CASCADE))
public class Nota {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long autorId;
    private String texto;
    private long dataHora;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAutorId() { return autorId; }
    public void setAutorId(long autorId) { this.autorId = autorId; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public long getDataHora() { return dataHora; }
    public void setDataHora(long dataHora) { this.dataHora = dataHora; }
}
