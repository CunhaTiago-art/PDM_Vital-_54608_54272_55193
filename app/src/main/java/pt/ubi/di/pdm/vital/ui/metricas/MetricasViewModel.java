package pt.ubi.di.pdm.vital.ui.metricas;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.MetricaSaude;
import pt.ubi.di.pdm.vital.data.model.TipoMetrica;

/**
 * Liga o ecrã de Saúde à base de dados.
 *
 * Nem todas as métricas se leem da mesma maneira: o batimento e o oxigénio
 * interessam no último valor medido, enquanto os passos e a água só fazem
 * sentido somados ao longo do dia. Por isso umas usam a última leitura e
 * outras um total — e a soma é feita pelo SQL, não em Java.
 */
public class MetricasViewModel extends AndroidViewModel {

    private final VitalRepository repositorio;

    private final LiveData<List<MetricaSaude>> todas;
    private final LiveData<MetricaSaude> ultimoBatimento;
    private final LiveData<MetricaSaude> ultimoOxigenio;
    private final LiveData<Double> passosDeHoje;
    private final LiveData<Double> aguaDeHoje;

    public MetricasViewModel(@NonNull Application aplicacao) {
        super(aplicacao);
        repositorio = new VitalRepository(aplicacao);

        todas = repositorio.observarMetricas();
        ultimoBatimento = repositorio.observarUltimaMetrica(TipoMetrica.BATIMENTO);
        ultimoOxigenio = repositorio.observarUltimaMetrica(TipoMetrica.OXIGENIO);
        passosDeHoje = repositorio.observarTotalDeHoje(TipoMetrica.PASSOS);
        aguaDeHoje = repositorio.observarTotalDeHoje(TipoMetrica.AGUA);
    }

    public LiveData<List<MetricaSaude>> getTodas() {
        return todas;
    }

    public LiveData<MetricaSaude> getUltimoBatimento() {
        return ultimoBatimento;
    }

    public LiveData<MetricaSaude> getUltimoOxigenio() {
        return ultimoOxigenio;
    }

    public LiveData<Double> getPassosDeHoje() {
        return passosDeHoje;
    }

    public LiveData<Double> getAguaDeHoje() {
        return aguaDeHoje;
    }

    public void registar(TipoMetrica tipo, double valor) {
        repositorio.registarMetrica(tipo, valor, "manual");
    }
}
