package pt.ubi.di.pdm.vital;

import android.app.Application;

import pt.ubi.di.pdm.vital.data.VitalRepository;

/**
 * Classe de arranque da aplicação.
 * Serve para pôr a base de dados em dia assim que a app abre: fecha consultas
 * que já passaram, marca as tomas que ficaram por confirmar e gera as tomas
 * dos próximos dias. Tudo isto corre fora da thread principal.
 */
public class VitalApp extends Application {

    private VitalRepository repositorio;

    @Override
    public void onCreate() {
        super.onCreate();
        repositorio = new VitalRepository(this);
        repositorio.atualizarEstados();
    }

    /** Repositório partilhado, para as activities não criarem um por cada ecrã. */
    public VitalRepository getRepositorio() {
        return repositorio;
    }
}
