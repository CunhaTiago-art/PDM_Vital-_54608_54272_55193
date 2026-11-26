package pt.ubi.di.pdm.vital.ui.historial;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

import pt.ubi.di.pdm.vital.data.SessionManager;
import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.EventoHistorial;
import pt.ubi.di.pdm.vital.data.model.ResumoDia;

/**
 * Liga o Historial à base de dados.
 *
 * A lista junta tomas, consultas e notas numa só sequência ordenada no tempo —
 * é uma consulta com UNION ALL sobre três tabelas. Mudar de separador troca
 * apenas o filtro, e o switchMap volta a correr a consulta sozinho.
 */
public class HistorialViewModel extends AndroidViewModel {

    public static final String TUDO = "TUDO";
    public static final String TOMAS = "TOMA";
    public static final String CONSULTAS = "CONSULTA";

    private final VitalRepository repositorio;
    private final SessionManager sessao;

    private final MutableLiveData<String> filtro = new MutableLiveData<>(TUDO);
    private final LiveData<List<EventoHistorial>> eventos;

    /** 0 = esta semana, -1 = a passada. Nunca passa de 0: não há futuro para rever. */
    private final MutableLiveData<Integer> deslocamento = new MutableLiveData<>(0);

    private final LiveData<List<ResumoDia>> semana;
    private final LiveData<Integer> tomadas;
    private final LiveData<Integer> total;

    public HistorialViewModel(@NonNull Application aplicacao) {
        super(aplicacao);
        repositorio = new VitalRepository(aplicacao);
        sessao = repositorio.getSessao();

        eventos = Transformations.switchMap(filtro, repositorio::observarHistorial);
        // Mudar de semana troca o intervalo de datas e o switchMap volta a consultar.
        semana = Transformations.switchMap(deslocamento, repositorio::observarSemana);
        tomadas = Transformations.switchMap(deslocamento, repositorio::contarTomadasDaSemana);
        total = Transformations.switchMap(deslocamento, repositorio::contarTomasDaSemana);
    }

    public LiveData<List<EventoHistorial>> getEventos() {
        return eventos;
    }

    public LiveData<List<ResumoDia>> getSemana() {
        return semana;
    }

    public LiveData<Integer> getTomadas() {
        return tomadas;
    }

    public LiveData<Integer> getTotal() {
        return total;
    }

    public String getFiltroAtual() {
        String atual = filtro.getValue();
        return atual == null ? TUDO : atual;
    }

    public void filtrar(String novo) {
        if (!novo.equals(filtro.getValue())) {
            filtro.setValue(novo);
        }
    }

    public int getDeslocamento() {
        Integer valor = deslocamento.getValue();
        return valor == null ? 0 : valor;
    }

    public void semanaAnterior() {
        deslocamento.setValue(getDeslocamento() - 1);
    }

    /** Só avança até à semana atual: rever tomas por acontecer não faz sentido. */
    public void semanaSeguinte() {
        if (getDeslocamento() < 0) {
            deslocamento.setValue(getDeslocamento() + 1);
        }
    }

    public boolean podeAvancar() {
        return getDeslocamento() < 0;
    }

    public void adicionarNota(String texto) {
        long autor = sessao.getMembroId();
        if (autor != 0 && texto != null && !texto.trim().isEmpty()) {
            repositorio.adicionarNota(autor, texto.trim());
        }
    }

    public boolean temAutor() {
        return sessao.getMembroId() != 0;
    }
}
