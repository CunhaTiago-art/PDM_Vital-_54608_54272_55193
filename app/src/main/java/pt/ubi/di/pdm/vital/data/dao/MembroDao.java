package pt.ubi.di.pdm.vital.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import pt.ubi.di.pdm.vital.data.model.Membro;

/** Acesso à rede de apoio (ecrãs Comunidade e Emergência). */
@Dao
public interface MembroDao {

    @Insert
    long inserir(Membro membro);

    @Update
    void atualizar(Membro membro);

    @Delete
    void apagar(Membro membro);

    @Query("SELECT * FROM membros ORDER BY ordemEmergencia DESC, nome COLLATE NOCASE ASC")
    LiveData<List<Membro>> observarTodos();

    @Query("SELECT * FROM membros WHERE papel <> 'IDOSO' ORDER BY nome COLLATE NOCASE ASC")
    LiveData<List<Membro>> observarRedeDeApoio();

    @Query("SELECT * FROM membros WHERE papel = 'IDOSO' LIMIT 1")
    LiveData<Membro> observarIdoso();

    /** Versão síncrona, para preparar a sessão fora da thread principal. */
    @Query("SELECT * FROM membros WHERE papel = 'IDOSO' LIMIT 1")
    Membro obterIdoso();

    @Query("SELECT * FROM membros WHERE id = :id")
    Membro obterPorId(long id);

    /** Contactos do ecrã Emergência, por ordem: principal primeiro. */
    @Query("SELECT * FROM membros WHERE ordemEmergencia > 0 ORDER BY ordemEmergencia ASC")
    LiveData<List<Membro>> observarContactosEmergencia();

    @Query("SELECT * FROM membros WHERE ordemEmergencia > 0 ORDER BY ordemEmergencia ASC LIMIT 1")
    LiveData<Membro> observarContactoPrincipal();

    /** Versão síncrona, para o botão SOS poder ligar sem esperar pelo LiveData. */
    @Query("SELECT * FROM membros WHERE ordemEmergencia > 0 ORDER BY ordemEmergencia ASC LIMIT 1")
    Membro obterContactoPrincipal();

    /** Quem recebe avisos quando uma consulta é marcada ou um alarme é ignorado. */
    @Query("SELECT * FROM membros WHERE recebeAvisos = 1 AND papel <> 'IDOSO'")
    List<Membro> listarQuemRecebeAvisos();

    @Query("SELECT COUNT(*) FROM membros WHERE papel <> 'IDOSO'")
    LiveData<Integer> contarRede();

    // ---------- Apoio à sincronização ----------

    /** Encontra o membro pela identidade global, para reconhecer autores vindos da nuvem. */
    @Query("SELECT * FROM membros WHERE uidRemoto = :uid LIMIT 1")
    Membro obterPorUid(String uid);

    @Query("SELECT * FROM membros WHERE nome = :nome LIMIT 1")
    Membro obterPorNome(String nome);

    @Query("UPDATE membros SET uidRemoto = :uid WHERE id = :id")
    void definirUid(long id, String uid);
}
