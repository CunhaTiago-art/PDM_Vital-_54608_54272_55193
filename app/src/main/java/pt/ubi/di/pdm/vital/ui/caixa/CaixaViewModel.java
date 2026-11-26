package pt.ubi.di.pdm.vital.ui.caixa;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.MedicamentoComHorarios;

/** Liga a Caixa de Remédios à base de dados. */
public class CaixaViewModel extends AndroidViewModel {

    private final VitalRepository repositorio;
    private final LiveData<List<MedicamentoComHorarios>> medicamentos;

    public CaixaViewModel(@NonNull Application aplicacao) {
        super(aplicacao);
        repositorio = new VitalRepository(aplicacao);
        medicamentos = repositorio.observarMedicamentos();
    }

    /**
     * Cada medicamento vem já com os seus horários, numa só consulta.
     * É o @Relation do Room a evitar uma query por cada linha da lista.
     */
    public LiveData<List<MedicamentoComHorarios>> getMedicamentos() {
        return medicamentos;
    }

    /** Suspende um medicamento sem apagar o historial de tomas já registado. */
    public void suspender(long medicamentoId) {
        repositorio.desativarMedicamento(medicamentoId);
    }
}
