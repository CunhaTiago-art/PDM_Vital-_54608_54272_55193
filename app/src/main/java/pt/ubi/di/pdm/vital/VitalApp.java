package pt.ubi.di.pdm.vital;

import android.app.Application;

import pt.ubi.di.pdm.vital.data.SessionManager;
import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.remote.SincronizacaoFirebase;

/**
 * Classe de arranque da aplicação.
 *
 * Faz duas coisas: põe a base de dados local em dia (fecha consultas que já
 * passaram, marca as tomas que ficaram por confirmar e gera as tomas dos
 * próximos dias) e liga a sincronização do chat com a nuvem, se houver um
 * código de família definido.
 *
 * Sem código de família, ou sem rede, a app funciona na mesma: tudo o que
 * importa está no Room, no telemóvel.
 */
public class VitalApp extends Application {

    private VitalRepository repositorio;
    private SincronizacaoFirebase sincronizacao;
    private SessionManager sessao;

    @Override
    public void onCreate() {
        super.onCreate();

        sessao = new SessionManager(this);
        repositorio = new VitalRepository(this);
        repositorio.atualizarEstados();

        sincronizacao = new SincronizacaoFirebase(this);
        VitalRepository.setSincronizador(sincronizacao::enviarPendentes);
        sincronizacao.iniciar();
    }

    /** Repositório partilhado, para as activities não criarem um por cada ecrã. */
    public VitalRepository getRepositorio() {
        return repositorio;
    }

    public SincronizacaoFirebase getSincronizacao() {
        return sincronizacao;
    }

    public SessionManager getSessao() {
        return sessao;
    }
}
