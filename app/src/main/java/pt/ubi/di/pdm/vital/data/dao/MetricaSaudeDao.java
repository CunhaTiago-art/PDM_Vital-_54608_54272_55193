package pt.ubi.di.pdm.vital.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import pt.ubi.di.pdm.vital.data.model.MetricaSaude;

/** Registo de métricas de saúde: batimento, oxigénio, passos e água. */
@Dao
public interface MetricaSaudeDao {

    @Insert
    long inserir(MetricaSaude metrica);

    @Insert
    void inserirVarias(List<MetricaSaude> metricas);

    @Delete
    void apagar(MetricaSaude metrica);

    @Query("SELECT * FROM metricas_saude ORDER BY dataHora DESC")
    LiveData<List<MetricaSaude>> observarTodas();

    @Query("SELECT * FROM metricas_saude WHERE tipo = :tipo ORDER BY dataHora DESC")
    LiveData<List<MetricaSaude>> observarPorTipo(String tipo);

    /** Último valor conhecido de uma métrica, para o cartão do dashboard. */
    @Query("SELECT * FROM metricas_saude WHERE tipo = :tipo ORDER BY dataHora DESC LIMIT 1")
    LiveData<MetricaSaude> observarUltima(String tipo);

    /** Série de valores para o gráfico de saúde, dentro de um intervalo. */
    @Query("SELECT * FROM metricas_saude "
            + "WHERE tipo = :tipo AND dataHora BETWEEN :inicio AND :fim "
            + "ORDER BY dataHora ASC")
    LiveData<List<MetricaSaude>> observarSerie(String tipo, long inicio, long fim);

    /** Média de uma métrica num intervalo (ex.: batimento médio da semana). */
    @Query("SELECT AVG(valor) FROM metricas_saude "
            + "WHERE tipo = :tipo AND dataHora BETWEEN :inicio AND :fim")
    LiveData<Double> observarMedia(String tipo, long inicio, long fim);

    /** Total acumulado de uma métrica num intervalo (ex.: água bebida hoje). */
    @Query("SELECT IFNULL(SUM(valor), 0) FROM metricas_saude "
            + "WHERE tipo = :tipo AND dataHora BETWEEN :inicio AND :fim")
    LiveData<Double> observarTotal(String tipo, long inicio, long fim);
}
