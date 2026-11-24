package pt.ubi.di.pdm.vital.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import pt.ubi.di.pdm.vital.data.model.MensagemChat;
import pt.ubi.di.pdm.vital.data.model.MensagemComAutor;

/** Chat da família, guardado localmente para funcionar sem rede. */
@Dao
public interface ChatDao {

    @Insert
    long inserir(MensagemChat mensagem);

    @Insert
    void inserirVarias(List<MensagemChat> mensagens);

    /** Conversa completa, já com o nome e o papel de quem escreveu cada mensagem. */
    @Query("SELECT m.*, mem.nome AS autorNome, mem.papel AS autorPapel "
            + "FROM mensagens_chat m "
            + "INNER JOIN membros mem ON mem.id = m.autorId "
            + "ORDER BY m.dataHora ASC")
    LiveData<List<MensagemComAutor>> observarConversa();

    /** Badge "2 mensagens novas" no cartão Chat da Comunidade. */
    @Query("SELECT COUNT(*) FROM mensagens_chat WHERE lida = 0 AND autorId <> :idDoProprio")
    LiveData<Integer> contarPorLer(long idDoProprio);

    @Query("UPDATE mensagens_chat SET lida = 1 WHERE lida = 0")
    void marcarTudoComoLido();

    @Query("SELECT m.*, mem.nome AS autorNome, mem.papel AS autorPapel "
            + "FROM mensagens_chat m "
            + "INNER JOIN membros mem ON mem.id = m.autorId "
            + "ORDER BY m.dataHora DESC LIMIT 1")
    LiveData<MensagemComAutor> observarUltima();

    // ---------- Apoio à sincronização ----------

    @Update
    void atualizar(MensagemChat mensagem);

    /** Mensagens escritas neste telemóvel que ainda não subiram para a nuvem. */
    @Query("SELECT * FROM mensagens_chat WHERE sincronizado = 0 ORDER BY dataHora ASC")
    List<MensagemChat> listarPorSincronizar();

    /** Evita inserir duas vezes a mesma mensagem quando ela chega da nuvem. */
    @Query("SELECT * FROM mensagens_chat WHERE idRemoto = :idRemoto LIMIT 1")
    MensagemChat obterPorIdRemoto(String idRemoto);

    @Query("UPDATE mensagens_chat SET sincronizado = 1, idRemoto = :idRemoto WHERE id = :id")
    void marcarSincronizada(long id, String idRemoto);

    @Query("SELECT COUNT(*) FROM mensagens_chat WHERE sincronizado = 0")
    LiveData<Integer> contarPorSincronizar();
}
