package pt.ubi.di.pdm.vital.ui.duvidas;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

import pt.ubi.di.pdm.vital.data.SessionManager;
import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.CategoriaDuvida;
import pt.ubi.di.pdm.vital.data.model.DuvidaResumo;

/**
 * Liga o ecrã "Dúvidas e ajuda" à base de dados.
 *
 * A categoria e o texto de pesquisa são estado observável: mudar qualquer um
 * deles faz a consulta correr de novo, com o filtro aplicado no SQL e não em Java.
 */
public class DuvidasViewModel extends AndroidViewModel {

    private final VitalRepository repositorio;
    private final SessionManager sessao;

    private final MutableLiveData<CategoriaDuvida> categoria = new MutableLiveData<>(null);
    private final MutableLiveData<String> pesquisa = new MutableLiveData<>("");
    private final MutableLiveData<Integer> gatilho = new MutableLiveData<>(0);

    private final LiveData<List<DuvidaResumo>> duvidas;

    public DuvidasViewModel(@NonNull Application aplicacao) {
        super(aplicacao);
        repositorio = new VitalRepository(aplicacao);
        sessao = repositorio.getSessao();

        // O gatilho junta as duas fontes: qualquer alteração passa por ele.
        duvidas = Transformations.switchMap(gatilho,
                ignorado -> repositorio.observarDuvidas(categoria.getValue(), pesquisa.getValue()));
    }

    public LiveData<List<DuvidaResumo>> getDuvidas() {
        return duvidas;
    }

    public CategoriaDuvida getCategoria() {
        return categoria.getValue();
    }

    public void filtrarPorCategoria(CategoriaDuvida nova) {
        categoria.setValue(nova);
        recarregar();
    }

    public void pesquisar(String texto) {
        pesquisa.setValue(texto == null ? "" : texto);
        recarregar();
    }

    private void recarregar() {
        Integer actual = gatilho.getValue();
        gatilho.setValue(actual == null ? 1 : actual + 1);
    }

    public void criarDuvida(String titulo, CategoriaDuvida categoriaNova) {
        long autor = sessao.getMembroId();
        if (autor != 0 && titulo != null && !titulo.trim().isEmpty()) {
            repositorio.criarDuvida(autor, titulo.trim(), null, categoriaNova);
        }
    }

    public void marcarResolvida(long duvidaId) {
        repositorio.marcarDuvidaResolvida(duvidaId);
    }

    public boolean temAutor() {
        return sessao.getMembroId() != 0;
    }
}
