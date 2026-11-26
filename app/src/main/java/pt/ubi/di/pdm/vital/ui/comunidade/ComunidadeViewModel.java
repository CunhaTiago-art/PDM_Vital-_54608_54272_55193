package pt.ubi.di.pdm.vital.ui.comunidade;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.Membro;

/** Contadores e identidade do ecrã Comunidade. */
public class ComunidadeViewModel extends AndroidViewModel {

    private final LiveData<Membro> idoso;
    private final LiveData<Integer> mensagensPorLer;
    private final LiveData<Integer> consultasFuturas;

    public ComunidadeViewModel(@NonNull Application aplicacao) {
        super(aplicacao);
        VitalRepository repositorio = new VitalRepository(aplicacao);
        idoso = repositorio.observarIdoso();
        // As próprias mensagens não contam como por ler.
        mensagensPorLer = repositorio.contarMensagensPorLer(repositorio.getSessao().getMembroId());
        consultasFuturas = repositorio.contarConsultasFuturas();
    }

    public LiveData<Membro> getIdoso() {
        return idoso;
    }

    public LiveData<Integer> getMensagensPorLer() {
        return mensagensPorLer;
    }

    public LiveData<Integer> getConsultasFuturas() {
        return consultasFuturas;
    }
}
