package pt.ubi.di.pdm.vital.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import pt.ubi.di.pdm.vital.data.model.Duvida;
import pt.ubi.di.pdm.vital.data.model.DuvidaResumo;
import pt.ubi.di.pdm.vital.data.model.Nota;
import pt.ubi.di.pdm.vital.data.model.Resposta;

/** Dúvidas, respostas e notas partilhadas pela rede de apoio. */
@Dao
public interface ComunidadeDao {

    // ---------- Dúvidas ----------

    @Insert
    long inserirDuvida(Duvida duvida);

    @Update
    void atualizarDuvida(Duvida duvida);

    @Delete
    void apagarDuvida(Duvida duvida);

    /**
     * Lista do ecrã "Dúvidas e ajuda", já com o número de respostas de cada uma.
     * categoria = null devolve todas (chip "Todas");
     * pesquisa = "" ignora a caixa de pesquisa.
     */
    @Query("SELECT d.*, COUNT(r.id) AS numRespostas "
            + "FROM duvidas d "
            + "LEFT JOIN respostas r ON r.duvidaId = d.id "
            + "WHERE (:categoria IS NULL OR d.categoria = :categoria) "
            + "AND (:pesquisa = '' "
            + "     OR d.titulo LIKE '%' || :pesquisa || '%' "
            + "     OR IFNULL(d.descricao, '') LIKE '%' || :pesquisa || '%') "
            + "GROUP BY d.id "
            + "ORDER BY d.resolvida ASC, d.dataHora DESC")
    LiveData<List<DuvidaResumo>> observarDuvidas(String categoria, String pesquisa);

    @Query("SELECT * FROM duvidas WHERE id = :id")
    LiveData<Duvida> observarDuvida(long id);

    @Query("UPDATE duvidas SET resolvida = 1 WHERE id = :id")
    void marcarResolvida(long id);

    // ---------- Respostas ----------

    @Insert
    long inserirResposta(Resposta resposta);

    @Query("SELECT * FROM respostas WHERE duvidaId = :duvidaId ORDER BY dataHora ASC")
    LiveData<List<Resposta>> observarRespostas(long duvidaId);

    @Query("SELECT COUNT(*) FROM respostas WHERE duvidaId = :duvidaId")
    LiveData<Integer> contarRespostas(long duvidaId);

    // ---------- Notas ----------

    @Insert
    long inserirNota(Nota nota);

    @Delete
    void apagarNota(Nota nota);

    @Query("SELECT * FROM notas ORDER BY dataHora DESC")
    LiveData<List<Nota>> observarNotas();

    @Query("SELECT * FROM notas ORDER BY dataHora DESC LIMIT :limite")
    LiveData<List<Nota>> observarNotasRecentes(int limite);
}
