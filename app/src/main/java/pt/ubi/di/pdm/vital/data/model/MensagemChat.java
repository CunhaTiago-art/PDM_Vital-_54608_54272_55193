package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Mensagem do Chat da família, guardada localmente para funcionar offline. */
@Entity(tableName = "mensagens_chat",
        indices = {@Index("autorId"), @Index("dataHora")},
        foreignKeys = @ForeignKey(
                entity = Membro.class,
                parentColumns = "id",
                childColumns = "autorId",
                onDelete = ForeignKey.CASCADE))
public class MensagemChat {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long autorId;
    private String texto;
    private long dataHora;
    private TipoMensagem tipo;

    /** Falso enquanto o idoso ainda não abriu o chat (badge "2 mensagens novas"). */
    private boolean lida;

    /** Caminho do ficheiro de áudio quando o tipo é VOZ; null nas mensagens de texto. */
    private String anexoUri;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAutorId() { return autorId; }
    public void setAutorId(long autorId) { this.autorId = autorId; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public long getDataHora() { return dataHora; }
    public void setDataHora(long dataHora) { this.dataHora = dataHora; }

    public TipoMensagem getTipo() { return tipo; }
    public void setTipo(TipoMensagem tipo) { this.tipo = tipo; }

    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }

    public String getAnexoUri() { return anexoUri; }
    public void setAnexoUri(String anexoUri) { this.anexoUri = anexoUri; }
}
