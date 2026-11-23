package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Pergunta colocada no ecrã "Dúvidas e ajuda". */
@Entity(tableName = "duvidas",
        indices = {@Index("autorId"), @Index("categoria"), @Index("dataHora")},
        foreignKeys = @ForeignKey(
                entity = Membro.class,
                parentColumns = "id",
                childColumns = "autorId",
                onDelete = ForeignKey.CASCADE))
public class Duvida {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long autorId;
    private String titulo;            // "O alarme não tocou. O que faço?"
    private String descricao;         // opcional, pode ser null
    private CategoriaDuvida categoria;
    private long dataHora;
    private boolean resolvida;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAutorId() { return autorId; }
    public void setAutorId(long autorId) { this.autorId = autorId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public CategoriaDuvida getCategoria() { return categoria; }
    public void setCategoria(CategoriaDuvida categoria) { this.categoria = categoria; }

    public long getDataHora() { return dataHora; }
    public void setDataHora(long dataHora) { this.dataHora = dataHora; }

    public boolean isResolvida() { return resolvida; }
    public void setResolvida(boolean resolvida) { this.resolvida = resolvida; }
}
