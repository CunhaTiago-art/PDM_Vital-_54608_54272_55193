package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Pessoa da rede de apoio: o próprio idoso, familiares, cuidadores e profissionais de saúde.
 * Alimenta os ecrãs Comunidade (lista de membros), Chat da família (autor das mensagens)
 * e Emergência (contactos ordenados).
 */
@Entity(tableName = "membros", indices = {@Index("ordemEmergencia")})
public class Membro {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String nome;
    private PapelMembro papel;
    private String telefone;          // opcional, pode ser null
    private String fotoUri;           // opcional, pode ser null

    /** 0 = não é contacto de emergência; 1 = contacto principal; 2 = segundo contacto. */
    private int ordemEmergencia;

    /** Autorização de acesso ao historial clínico do idoso. */
    private boolean podeVerHistorial;

    /** Recebe avisos de consultas marcadas e de alarmes ignorados. */
    private boolean recebeAvisos;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public PapelMembro getPapel() { return papel; }
    public void setPapel(PapelMembro papel) { this.papel = papel; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getFotoUri() { return fotoUri; }
    public void setFotoUri(String fotoUri) { this.fotoUri = fotoUri; }

    public int getOrdemEmergencia() { return ordemEmergencia; }
    public void setOrdemEmergencia(int ordemEmergencia) { this.ordemEmergencia = ordemEmergencia; }

    public boolean isPodeVerHistorial() { return podeVerHistorial; }
    public void setPodeVerHistorial(boolean podeVerHistorial) { this.podeVerHistorial = podeVerHistorial; }

    public boolean isRecebeAvisos() { return recebeAvisos; }
    public void setRecebeAvisos(boolean recebeAvisos) { this.recebeAvisos = recebeAvisos; }
}
