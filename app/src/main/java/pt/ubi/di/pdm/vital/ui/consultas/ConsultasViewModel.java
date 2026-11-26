package pt.ubi.di.pdm.vital.ui.consultas;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.Consulta;
import pt.ubi.di.pdm.vital.data.model.ConsultaComAcompanhante;
import pt.ubi.di.pdm.vital.data.model.Membro;

/** Liga os ecrãs de Consultas e Marcar Consulta à base de dados. */
public class ConsultasViewModel extends AndroidViewModel {

    private final VitalRepository repositorio;
    private final LiveData<ConsultaComAcompanhante> proxima;
    private final LiveData<List<ConsultaComAcompanhante>> futuras;
    private final LiveData<List<ConsultaComAcompanhante>> passadas;
    private final LiveData<List<Membro>> rede;

    public ConsultasViewModel(@NonNull Application aplicacao) {
        super(aplicacao);
        repositorio = new VitalRepository(aplicacao);
        proxima = repositorio.observarProximaConsultaDetalhada();
        futuras = repositorio.observarConsultasFuturasDetalhadas();
        passadas = repositorio.observarConsultasPassadasDetalhadas();
        rede = repositorio.observarRedeDeApoio();
    }

    public LiveData<ConsultaComAcompanhante> getProxima() {
        return proxima;
    }

    public LiveData<List<ConsultaComAcompanhante>> getFuturas() {
        return futuras;
    }

    public LiveData<List<ConsultaComAcompanhante>> getPassadas() {
        return passadas;
    }

    /** Quem pode acompanhar: a rede de apoio, sem o próprio idoso. */
    public LiveData<List<Membro>> getRede() {
        return rede;
    }

    public void guardar(Consulta consulta) {
        repositorio.guardarConsulta(consulta);
    }
}
