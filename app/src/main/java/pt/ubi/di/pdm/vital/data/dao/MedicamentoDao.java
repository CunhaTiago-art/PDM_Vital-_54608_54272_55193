package pt.ubi.di.pdm.vital.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import pt.ubi.di.pdm.vital.data.model.Horario;
import pt.ubi.di.pdm.vital.data.model.Medicamento;
import pt.ubi.di.pdm.vital.data.model.MedicamentoComHorarios;

/** Acesso às prescrições e aos respetivos horários (ecrãs Caixa de Remédios e Novo Medicamento). */
@Dao
public interface MedicamentoDao {

    // ---------- Medicamentos ----------

    @Insert
    long inserir(Medicamento medicamento);

    @Update
    void atualizar(Medicamento medicamento);

    @Delete
    void apagar(Medicamento medicamento);

    @Query("SELECT * FROM medicamentos WHERE ativo = 1 ORDER BY nome COLLATE NOCASE ASC")
    LiveData<List<Medicamento>> observarAtivos();

    /** Versão síncrona, para o agendamento de alarmes fora da thread principal. */
    @Query("SELECT * FROM medicamentos WHERE ativo = 1 ORDER BY nome COLLATE NOCASE ASC")
    List<Medicamento> listarAtivos();

    @Query("SELECT * FROM medicamentos WHERE id = :id")
    Medicamento obterPorId(long id);

    @Query("SELECT COUNT(*) FROM medicamentos WHERE ativo = 1")
    LiveData<Integer> contarAtivos();

    /** Suspende um medicamento sem apagar o historial de tomas já registado. */
    @Query("UPDATE medicamentos SET ativo = 0 WHERE id = :id")
    void desativar(long id);

    // ---------- Medicamento + horários numa só consulta ----------

    @Transaction
    @Query("SELECT * FROM medicamentos WHERE ativo = 1 ORDER BY nome COLLATE NOCASE ASC")
    LiveData<List<MedicamentoComHorarios>> observarComHorarios();

    @Transaction
    @Query("SELECT * FROM medicamentos WHERE id = :id")
    LiveData<MedicamentoComHorarios> observarComHorariosPorId(long id);

    @Transaction
    @Query("SELECT * FROM medicamentos WHERE ativo = 1")
    List<MedicamentoComHorarios> listarComHorarios();

    // ---------- Horários ----------

    @Insert
    long inserirHorario(Horario horario);

    @Delete
    void apagarHorario(Horario horario);

    @Query("SELECT * FROM horarios WHERE medicamentoId = :medicamentoId ORDER BY minutosDoDia ASC")
    LiveData<List<Horario>> observarHorarios(long medicamentoId);

    @Query("SELECT * FROM horarios WHERE medicamentoId = :medicamentoId ORDER BY minutosDoDia ASC")
    List<Horario> listarHorarios(long medicamentoId);

    @Query("DELETE FROM horarios WHERE medicamentoId = :medicamentoId")
    void apagarHorariosDe(long medicamentoId);
}
