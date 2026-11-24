package pt.ubi.di.pdm.vital.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Resposta de um membro da rede a uma dúvida. */
@Entity(tableName = "respostas",
        indices = {@Index("duvidaId"), @Index("autorId"),
                   @Index(value = "idRemoto", unique = true)},
        foreignKeys = {
                @ForeignKey(entity = Duvida.class,
                        parentColumns = "id",
                        childColumns = "duvidaId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Membro.class,
                        parentColumns = "id",
                        childColumns = "autorId",
                        onDelete = ForeignKey.CASCADE)})
public class Resposta {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long duvidaId;
    private long autorId;
    private String texto;
    private long dataHora;

    /** Identificador do documento no Firestore. Null enquanto só existir neste telemóvel. */
    private String idRemoto;

    /** Falso enquanto a resposta ainda não tiver subido para a nuvem. */
    @ColumnInfo(defaultValue = "0")
    private boolean sincronizado;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getDuvidaId() { return duvidaId; }
    public void setDuvidaId(long duvidaId) { this.duvidaId = duvidaId; }

    public long getAutorId() { return autorId; }
    public void setAutorId(long autorId) { this.autorId = autorId; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public long getDataHora() { return dataHora; }
    public void setDataHora(long dataHora) { this.dataHora = dataHora; }

    public String getIdRemoto() { return idRemoto; }
    public void setIdRemoto(String idRemoto) { this.idRemoto = idRemoto; }

    public boolean isSincronizado() { return sincronizado; }
    public void setSincronizado(boolean sincronizado) { this.sincronizado = sincronizado; }
}
