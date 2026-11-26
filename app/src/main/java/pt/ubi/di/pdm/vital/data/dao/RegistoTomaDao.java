package pt.ubi.di.pdm.vital.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import pt.ubi.di.pdm.vital.data.model.EventoHistorial;
import pt.ubi.di.pdm.vital.data.model.RegistoToma;
import pt.ubi.di.pdm.vital.data.model.ResumoDia;
import pt.ubi.di.pdm.vital.data.model.TomaDeHoje;

/** Acesso às tomas agendadas: dashboard, ecrã de alarme e historial. */
@Dao
public interface RegistoTomaDao {

    @Insert
    long inserir(RegistoToma registo);

    @Insert
    void inserirVarios(List<RegistoToma> registos);

    @Update
    void atualizar(RegistoToma registo);

    // ---------- Dashboard ----------

    /**
     * Lista "Hoje" do dashboard: cada toma do dia já com o nome e a dosagem do medicamento,
     * para o adapter não ter de fazer uma consulta por linha.
     */
    @Query("SELECT r.*, m.nome AS nome, m.dosagem AS dosagem, "
            + "m.quantidadePorToma AS quantidadePorToma, m.instrucoes AS instrucoes, "
            + "m.fotoUri AS fotoUri "
            + "FROM registo_tomas r "
            + "INNER JOIN medicamentos m ON m.id = r.medicamentoId "
            + "WHERE r.dataPrevista BETWEEN :inicio AND :fim "
            + "ORDER BY r.dataPrevista ASC")
    LiveData<List<TomaDeHoje>> observarTomasEntre(long inicio, long fim);

    /**
     * Cartão "PRÓXIMA TOMA": a primeira toma por confirmar.
     *
     * O limite é `desde`, e não o instante atual, para que uma toma cuja hora
     * acabou de passar continue a aparecer e possa ser confirmada. Sem isto,
     * chegar cinco minutos atrasado fazia a toma desaparecer do ecrã.
     */
    @Query("SELECT r.*, m.nome AS nome, m.dosagem AS dosagem, "
            + "m.quantidadePorToma AS quantidadePorToma, m.instrucoes AS instrucoes, "
            + "m.fotoUri AS fotoUri "
            + "FROM registo_tomas r "
            + "INNER JOIN medicamentos m ON m.id = r.medicamentoId "
            + "WHERE r.estado IN ('PENDENTE', 'ADIADA') AND r.dataPrevista >= :desde "
            + "ORDER BY r.dataPrevista ASC LIMIT 1")
    LiveData<TomaDeHoje> observarProximaToma(long desde);

    /** Versão síncrona da próxima toma, para o AlarmManager agendar o alarme seguinte. */
    @Query("SELECT * FROM registo_tomas "
            + "WHERE estado IN ('PENDENTE', 'ADIADA') AND dataPrevista >= :agora "
            + "ORDER BY dataPrevista ASC LIMIT 1")
    RegistoToma obterProximaPendente(long agora);

    @Query("SELECT * FROM registo_tomas WHERE id = :id")
    RegistoToma obterPorId(long id);

    // ---------- Ações do idoso ----------

    /**
     * Botão "JÁ TOMEI".
     *
     * Guarda a hora a que foi mesmo confirmada e, quando é fora de horas,
     * uma nota com o atraso. Uma toma tardia continua a ser uma toma — o que
     * não pode acontecer é ficar registada como se tivesse sido a horas.
     */
    @Query("UPDATE registo_tomas SET estado = 'TOMADA', dataReal = :agora, "
            + "observacao = :observacao WHERE id = :id")
    void marcarComoTomada(long id, long agora, String observacao);

    /** Botão "ADIAR 10 MINUTOS" do ecrã de alarme. */
    @Query("UPDATE registo_tomas SET estado = 'ADIADA', dataPrevista = :novaHora WHERE id = :id")
    void adiar(long id, long novaHora);

    /** Marca como falhadas as tomas pendentes cuja janela de tolerância já passou. */
    @Query("UPDATE registo_tomas SET estado = 'FALHADA' "
            + "WHERE estado IN ('PENDENTE', 'ADIADA') AND dataPrevista < :limite")
    int marcarFalhadasAntesDe(long limite);

    // ---------- Historial ----------

    @Query("SELECT COUNT(*) FROM registo_tomas WHERE dataPrevista BETWEEN :inicio AND :fim")
    LiveData<Integer> contarTotalEntre(long inicio, long fim);

    @Query("SELECT COUNT(*) FROM registo_tomas "
            + "WHERE estado = 'TOMADA' AND dataPrevista BETWEEN :inicio AND :fim")
    LiveData<Integer> contarTomadasEntre(long inicio, long fim);

    /**
     * Círculos dos dias da semana no Historial: quantas tomas tinha cada dia
     * e quantas foram efetivamente confirmadas.
     */
    @Query("SELECT strftime('%Y-%m-%d', dataPrevista / 1000, 'unixepoch', 'localtime') AS dia, "
            + "COUNT(*) AS total, "
            + "SUM(CASE WHEN estado = 'TOMADA' THEN 1 ELSE 0 END) AS tomadas "
            + "FROM registo_tomas "
            + "WHERE dataPrevista BETWEEN :inicio AND :fim "
            + "GROUP BY dia ORDER BY dia ASC")
    LiveData<List<ResumoDia>> observarResumoPorDia(long inicio, long fim);

    /**
     * Lista do Historial com os separadores Tudo / Tomas / Consultas.
     * Junta três tabelas numa só lista ordenada no tempo.
     * O filtro aceita "TUDO", "TOMA", "CONSULTA" ou "NOTA".
     */
    @Query("SELECT 'TOMA' AS tipo, "
            + "       m.nome || ' ' || IFNULL(m.dosagem, '') AS titulo, "
            + "       CASE r.estado "
            + "            WHEN 'TOMADA'  THEN CASE WHEN IFNULL(r.observacao, '') = '' "
            + "                                     THEN 'Tomado' "
            + "                                     ELSE 'Tomado · ' || r.observacao END "
            + "            WHEN 'FALHADA' THEN 'Não tomado' "
            + "            WHEN 'ADIADA'  THEN 'Adiado' "
            + "            ELSE 'Pendente' END AS subtitulo, "
            + "       r.dataPrevista AS dataHora "
            + "FROM registo_tomas r "
            + "INNER JOIN medicamentos m ON m.id = r.medicamentoId "
            + "WHERE :filtro IN ('TUDO', 'TOMA') "
            + "UNION ALL "
            + "SELECT 'CONSULTA', c.especialidade, IFNULL(c.local, ''), c.dataHora "
            + "FROM consultas c "
            + "WHERE :filtro IN ('TUDO', 'CONSULTA') "
            + "UNION ALL "
            + "SELECT 'NOTA', 'Nota de ' || mem.nome, n.texto, n.dataHora "
            + "FROM notas n "
            + "INNER JOIN membros mem ON mem.id = n.autorId "
            + "WHERE :filtro IN ('TUDO', 'NOTA') "
            + "ORDER BY dataHora DESC")
    LiveData<List<EventoHistorial>> observarHistorial(String filtro);

    // ---------- Apoio à geração de tomas ----------

    /** Evita criar duas vezes a mesma toma ao regenerar o calendário do dia. */
    @Query("SELECT COUNT(*) FROM registo_tomas "
            + "WHERE horarioId = :horarioId AND dataPrevista = :dataPrevista")
    int contarExistentes(long horarioId, long dataPrevista);

    @Query("SELECT MAX(dataPrevista) FROM registo_tomas")
    Long obterUltimaDataGerada();
}
