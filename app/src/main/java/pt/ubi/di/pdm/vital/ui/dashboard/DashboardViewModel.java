package pt.ubi.di.pdm.vital.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.TomaDeHoje;

/** Liga o dashboard à base de dados: próxima toma, tomas de hoje e confirmação. */
public class DashboardViewModel extends AndroidViewModel {

    private final VitalRepository repositorio;
    private final LiveData<TomaDeHoje> proximaToma;
    private final LiveData<List<TomaDeHoje>> tomasDeHoje;

    public DashboardViewModel(@NonNull Application aplicacao) {
        super(aplicacao);
        repositorio = new VitalRepository(aplicacao);
        proximaToma = repositorio.observarProximaToma();
        tomasDeHoje = repositorio.observarTomasDeHoje();
    }

    public LiveData<TomaDeHoje> getProximaToma() {
        return proximaToma;
    }

    public LiveData<List<TomaDeHoje>> getTomasDeHoje() {
        return tomasDeHoje;
    }

    /** Verdadeiro quando a toma já pode ser confirmada (não falta mais de meia hora). */
    public boolean podeConfirmar(TomaDeHoje toma) {
        return toma != null && VitalRepository.estaNaHora(toma.registo.getDataPrevista());
    }

    /** Botão "JÁ TOMEI": grava a confirmação com a hora a que foi mesmo tomada. */
    public void confirmar(long registoId) {
        repositorio.marcarComoTomada(registoId);
    }

    /** Botão "ADIAR 10 MINUTOS" do ecrã de alarme. */
    public void adiar(long registoId, int minutos) {
        repositorio.adiarToma(registoId, minutos);
    }
}
