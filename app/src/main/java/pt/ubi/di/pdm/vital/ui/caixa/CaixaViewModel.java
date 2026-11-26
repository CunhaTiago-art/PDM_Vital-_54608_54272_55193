package pt.ubi.di.pdm.vital.ui.caixa;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.Medicamento;
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

    /** Um medicamento com os seus horários, para preencher o formulário de edição. */
    public LiveData<MedicamentoComHorarios> observarUm(long id) {
        return repositorio.observarMedicamento(id);
    }

    /**
     * Grava o medicamento e os horários numa só transação, e gera logo
     * as tomas dos próximos dias a partir da nova prescrição.
     */
    public void guardar(Medicamento medicamento, List<Integer> minutosDosHorarios) {
        repositorio.guardarMedicamento(medicamento, minutosDosHorarios);
    }
}
