package pt.ubi.di.pdm.vital.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import pt.ubi.di.pdm.vital.data.model.Consulta;

/** Acesso às consultas (ecrãs Consultas e Marcar Consulta). */
@Dao
public interface ConsultaDao {

    @Insert
    long inserir(Consulta consulta);

    @Update
    void atualizar(Consulta consulta);

    @Delete
    void apagar(Consulta consulta);

    @Query("SELECT * FROM consultas WHERE id = :id")
    LiveData<Consulta> observarPorId(long id);

    @Query("SELECT * FROM consultas WHERE id = :id")
    Consulta obterPorId(long id);

    /** Cartão "PRÓXIMA CONSULTA". */
    @Query("SELECT * FROM consultas "
            + "WHERE estado = 'AGENDADA' AND dataHora >= :agora "
            + "ORDER BY dataHora ASC LIMIT 1")
    LiveData<Consulta> observarProxima(long agora);

    /** Bloco "Mais tarde". */
    @Query("SELECT * FROM consultas "
            + "WHERE estado = 'AGENDADA' AND dataHora >= :agora "
            + "ORDER BY dataHora ASC")
    LiveData<List<Consulta>> observarFuturas(long agora);

    /** Bloco "Passada". */
    @Query("SELECT * FROM consultas "
            + "WHERE dataHora < :agora OR estado <> 'AGENDADA' "
            + "ORDER BY dataHora DESC")
    LiveData<List<Consulta>> observarPassadas(long agora);

    @Query("SELECT COUNT(*) FROM consultas WHERE estado = 'AGENDADA' AND dataHora >= :agora")
    LiveData<Integer> contarFuturas(long agora);

    /** Consultas cujo lembrete ainda tem de ser agendado no AlarmManager. */
    @Query("SELECT * FROM consultas WHERE estado = 'AGENDADA' AND dataHora >= :agora")
    List<Consulta> listarFuturas(long agora);

    @Query("UPDATE consultas SET estado = 'REALIZADA' "
            + "WHERE estado = 'AGENDADA' AND dataHora < :limite")
    int fecharConsultasPassadas(long limite);
}
