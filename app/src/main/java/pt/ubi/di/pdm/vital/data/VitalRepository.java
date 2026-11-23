package pt.ubi.di.pdm.vital.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;

import pt.ubi.di.pdm.vital.data.dao.ChatDao;
import pt.ubi.di.pdm.vital.data.dao.ComunidadeDao;
import pt.ubi.di.pdm.vital.data.dao.ConsultaDao;
import pt.ubi.di.pdm.vital.data.dao.MedicamentoDao;
import pt.ubi.di.pdm.vital.data.dao.MembroDao;
import pt.ubi.di.pdm.vital.data.dao.MetricaSaudeDao;
import pt.ubi.di.pdm.vital.data.dao.RegistoTomaDao;
import pt.ubi.di.pdm.vital.data.model.CategoriaDuvida;
import pt.ubi.di.pdm.vital.data.model.Consulta;
import pt.ubi.di.pdm.vital.data.model.Duvida;
import pt.ubi.di.pdm.vital.data.model.DuvidaResumo;
import pt.ubi.di.pdm.vital.data.model.EstadoConsulta;
import pt.ubi.di.pdm.vital.data.model.EventoHistorial;
import pt.ubi.di.pdm.vital.data.model.Horario;
import pt.ubi.di.pdm.vital.data.model.Medicamento;
import pt.ubi.di.pdm.vital.data.model.MedicamentoComHorarios;
import pt.ubi.di.pdm.vital.data.model.Membro;
import pt.ubi.di.pdm.vital.data.model.MensagemChat;
import pt.ubi.di.pdm.vital.data.model.MensagemComAutor;
import pt.ubi.di.pdm.vital.data.model.MetricaSaude;
import pt.ubi.di.pdm.vital.data.model.Nota;
import pt.ubi.di.pdm.vital.data.model.Resposta;
import pt.ubi.di.pdm.vital.data.model.ResumoDia;
import pt.ubi.di.pdm.vital.data.model.TipoMensagem;
import pt.ubi.di.pdm.vital.data.model.TipoMetrica;
import pt.ubi.di.pdm.vital.data.model.TomaDeHoje;
import pt.ubi.di.pdm.vital.util.DataUtil;

/**
 * Ponto único de acesso aos dados. A interface nunca fala diretamente com os DAOs:
 * as leituras devolvem LiveData (atualizam-se sozinhas quando a tabela muda) e as
 * escritas são enviadas para o executor, nunca para a thread principal.
 */
public class VitalRepository {

    private static final String TAG = "VitalRepository";

    /** Janela de tolerância: passado este tempo sem confirmação, a toma conta como falhada. */
    private static final long TOLERANCIA_MS = 2 * 60 * 60 * 1000L; // 2 horas

    private final AppDatabase bd;
    private final MembroDao membroDao;
    private final MedicamentoDao medicamentoDao;
    private final RegistoTomaDao tomaDao;
    private final MetricaSaudeDao metricaDao;
    private final ConsultaDao consultaDao;
    private final ChatDao chatDao;
    private final ComunidadeDao comunidadeDao;

    public VitalRepository(Context context) {
        bd = AppDatabase.getInstance(context);
        membroDao = bd.membroDao();
        medicamentoDao = bd.medicamentoDao();
        tomaDao = bd.registoTomaDao();
        metricaDao = bd.metricaSaudeDao();
        consultaDao = bd.consultaDao();
        chatDao = bd.chatDao();
        comunidadeDao = bd.comunidadeDao();
    }

    /** Resultado devolvido numa leitura pontual feita fora da thread principal. */
    public interface Resultado<T> {
        void aoObter(T valor);
    }

    // ==================================================================
    //  DASHBOARD
    // ==================================================================

    public LiveData<TomaDeHoje> observarProximaToma() {
        return tomaDao.observarProximaToma(System.currentTimeMillis());
    }

    public LiveData<List<TomaDeHoje>> observarTomasDeHoje() {
        return tomaDao.observarTomasEntre(DataUtil.inicioDeHoje(), DataUtil.fimDeHoje());
    }

    /** Botão "JÁ TOMEI". */
    public void marcarComoTomada(long registoId) {
        executar("marcar a toma", () ->
                tomaDao.marcarComoTomada(registoId, System.currentTimeMillis()));
    }

    /** Botão "ADIAR 10 MINUTOS" do ecrã de alarme. */
    public void adiarToma(long registoId, int minutos) {
        executar("adiar a toma", () ->
                tomaDao.adiar(registoId, System.currentTimeMillis() + minutos * 60_000L));
    }

    // ==================================================================
    //  CAIXA DE REMÉDIOS / NOVO MEDICAMENTO
    // ==================================================================

    public LiveData<List<MedicamentoComHorarios>> observarMedicamentos() {
        return medicamentoDao.observarComHorarios();
    }

    public LiveData<MedicamentoComHorarios> observarMedicamento(long id) {
        return medicamentoDao.observarComHorariosPorId(id);
    }

    public LiveData<Integer> contarMedicamentosAtivos() {
        return medicamentoDao.contarAtivos();
    }

    /**
     * Grava um medicamento novo ou alterado com os seus horários e cria logo
     * as tomas dos próximos dias. Corre tudo dentro de uma transação: ou grava
     * tudo, ou não grava nada.
     *
     * @param minutosDosHorarios horas de toma em minutos desde a meia-noite (480 = 08:00)
     */
    public void guardarMedicamento(Medicamento medicamento, List<Integer> minutosDosHorarios) {
        executar("guardar o medicamento", () -> {
            bd.runInTransaction(() -> {
                long id = medicamento.getId();
                if (id == 0) {
                    if (medicamento.getCriadoEm() == 0) {
                        medicamento.setCriadoEm(System.currentTimeMillis());
                    }
                    id = medicamentoDao.inserir(medicamento);
                    medicamento.setId(id);
                } else {
                    medicamentoDao.atualizar(medicamento);
                    medicamentoDao.apagarHorariosDe(id);
                }
                for (Integer minutos : minutosDosHorarios) {
                    Horario horario = new Horario();
                    horario.setMedicamentoId(id);
                    horario.setMinutosDoDia(minutos);
                    medicamentoDao.inserirHorario(horario);
                }
            });
            GeradorDeTomas.gerar(bd);
        });
    }

    public void desativarMedicamento(long id) {
        executar("suspender o medicamento", () -> medicamentoDao.desativar(id));
    }

    public void apagarMedicamento(Medicamento medicamento) {
        executar("apagar o medicamento", () -> medicamentoDao.apagar(medicamento));
    }

    // ==================================================================
    //  HISTORIAL
    // ==================================================================

    /** filtro: "TUDO", "TOMA", "CONSULTA" ou "NOTA". */
    public LiveData<List<EventoHistorial>> observarHistorial(String filtro) {
        return tomaDao.observarHistorial(filtro);
    }

    public LiveData<List<ResumoDia>> observarSemana() {
        return tomaDao.observarResumoPorDia(DataUtil.inicioDaSemana(), DataUtil.fimDaSemana());
    }

    public LiveData<Integer> contarTomadasDaSemana() {
        return tomaDao.contarTomadasEntre(DataUtil.inicioDaSemana(), DataUtil.fimDaSemana());
    }

    public LiveData<Integer> contarTomasDaSemana() {
        return tomaDao.contarTotalEntre(DataUtil.inicioDaSemana(), DataUtil.fimDaSemana());
    }

    public LiveData<List<Nota>> observarNotas() {
        return comunidadeDao.observarNotas();
    }

    public void adicionarNota(long autorId, String texto) {
        executar("adicionar a nota", () -> {
            Nota nota = new Nota();
            nota.setAutorId(autorId);
            nota.setTexto(texto);
            nota.setDataHora(System.currentTimeMillis());
            comunidadeDao.inserirNota(nota);
        });
    }

    // ==================================================================
    //  MÉTRICAS DE SAÚDE
    // ==================================================================

    public LiveData<MetricaSaude> observarUltimaMetrica(TipoMetrica tipo) {
        return metricaDao.observarUltima(tipo.name());
    }

    public LiveData<List<MetricaSaude>> observarSerie(TipoMetrica tipo, long inicio, long fim) {
        return metricaDao.observarSerie(tipo.name(), inicio, fim);
    }

    public LiveData<Double> observarMediaDaSemana(TipoMetrica tipo) {
        return metricaDao.observarMedia(tipo.name(), DataUtil.inicioDaSemana(), DataUtil.fimDaSemana());
    }

    public LiveData<Double> observarTotalDeHoje(TipoMetrica tipo) {
        return metricaDao.observarTotal(tipo.name(), DataUtil.inicioDeHoje(), DataUtil.fimDeHoje());
    }

    public void registarMetrica(TipoMetrica tipo, double valor, String origem) {
        executar("registar a métrica", () -> {
            MetricaSaude metrica = new MetricaSaude();
            metrica.setTipo(tipo);
            metrica.setValor(valor);
            metrica.setOrigem(origem);
            metrica.setDataHora(System.currentTimeMillis());
            metricaDao.inserir(metrica);
        });
    }

    // ==================================================================
    //  CONSULTAS
    // ==================================================================

    public LiveData<Consulta> observarProximaConsulta() {
        return consultaDao.observarProxima(System.currentTimeMillis());
    }

    public LiveData<List<Consulta>> observarConsultasFuturas() {
        return consultaDao.observarFuturas(System.currentTimeMillis());
    }

    public LiveData<List<Consulta>> observarConsultasPassadas() {
        return consultaDao.observarPassadas(System.currentTimeMillis());
    }

    public LiveData<Integer> contarConsultasFuturas() {
        return consultaDao.contarFuturas(System.currentTimeMillis());
    }

    /** Botão "GUARDAR E AVISAR TODOS" do ecrã Marcar Consulta. */
    public void guardarConsulta(Consulta consulta) {
        executar("guardar a consulta", () -> {
            if (consulta.getEstado() == null) {
                consulta.setEstado(EstadoConsulta.AGENDADA);
            }
            if (consulta.getId() == 0) {
                consultaDao.inserir(consulta);
            } else {
                consultaDao.atualizar(consulta);
            }
        });
    }

    public void apagarConsulta(Consulta consulta) {
        executar("apagar a consulta", () -> consultaDao.apagar(consulta));
    }

    // ==================================================================
    //  REDE DE APOIO / EMERGÊNCIA
    // ==================================================================

    public LiveData<List<Membro>> observarRedeDeApoio() {
        return membroDao.observarRedeDeApoio();
    }

    public LiveData<Membro> observarIdoso() {
        return membroDao.observarIdoso();
    }

    public LiveData<List<Membro>> observarContactosEmergencia() {
        return membroDao.observarContactosEmergencia();
    }

    public LiveData<Membro> observarContactoPrincipal() {
        return membroDao.observarContactoPrincipal();
    }

    public LiveData<Integer> contarRede() {
        return membroDao.contarRede();
    }

    public void guardarMembro(Membro membro) {
        executar("guardar o membro", () -> {
            if (membro.getId() == 0) {
                membroDao.inserir(membro);
            } else {
                membroDao.atualizar(membro);
            }
        });
    }

    public void apagarMembro(Membro membro) {
        executar("apagar o membro", () -> membroDao.apagar(membro));
    }

    // ==================================================================
    //  CHAT DA FAMÍLIA
    // ==================================================================

    public LiveData<List<MensagemComAutor>> observarConversa() {
        return chatDao.observarConversa();
    }

    public LiveData<Integer> contarMensagensPorLer(long idDoProprio) {
        return chatDao.contarPorLer(idDoProprio);
    }

    public void enviarMensagem(long autorId, String texto, TipoMensagem tipo, String anexoUri) {
        executar("enviar a mensagem", () -> {
            MensagemChat mensagem = new MensagemChat();
            mensagem.setAutorId(autorId);
            mensagem.setTexto(texto);
            mensagem.setTipo(tipo == null ? TipoMensagem.TEXTO : tipo);
            mensagem.setAnexoUri(anexoUri);
            mensagem.setDataHora(System.currentTimeMillis());
            mensagem.setLida(true); // as próprias mensagens já estão lidas
            chatDao.inserir(mensagem);
        });
    }

    public void marcarChatComoLido() {
        executar("marcar o chat como lido", chatDao::marcarTudoComoLido);
    }

    // ==================================================================
    //  DÚVIDAS E AJUDA
    // ==================================================================

    /** categoria = null mostra todas as dúvidas (chip "Todas"). */
    public LiveData<List<DuvidaResumo>> observarDuvidas(CategoriaDuvida categoria, String pesquisa) {
        String nome = categoria == null ? null : categoria.name();
        String texto = pesquisa == null ? "" : pesquisa.trim();
        return comunidadeDao.observarDuvidas(nome, texto);
    }

    public LiveData<List<Resposta>> observarRespostas(long duvidaId) {
        return comunidadeDao.observarRespostas(duvidaId);
    }

    /** Botão "+ Fazer uma pergunta". */
    public void criarDuvida(long autorId, String titulo, String descricao, CategoriaDuvida categoria) {
        executar("criar a dúvida", () -> {
            Duvida duvida = new Duvida();
            duvida.setAutorId(autorId);
            duvida.setTitulo(titulo);
            duvida.setDescricao(descricao);
            duvida.setCategoria(categoria == null ? CategoriaDuvida.OUTRO : categoria);
            duvida.setDataHora(System.currentTimeMillis());
            duvida.setResolvida(false);
            comunidadeDao.inserirDuvida(duvida);
        });
    }

    public void responder(long duvidaId, long autorId, String texto) {
        executar("responder à dúvida", () -> {
            Resposta resposta = new Resposta();
            resposta.setDuvidaId(duvidaId);
            resposta.setAutorId(autorId);
            resposta.setTexto(texto);
            resposta.setDataHora(System.currentTimeMillis());
            comunidadeDao.inserirResposta(resposta);
        });
    }

    public void marcarDuvidaResolvida(long duvidaId) {
        executar("marcar a dúvida como resolvida", () -> comunidadeDao.marcarResolvida(duvidaId));
    }

    // ==================================================================
    //  MANUTENÇÃO (chamar no arranque da app)
    // ==================================================================

    /**
     * Põe a base de dados em dia: fecha consultas que já passaram, marca como falhadas
     * as tomas que ficaram por confirmar e gera as tomas dos próximos dias.
     */
    public void atualizarEstados() {
        executar("atualizar os estados", () -> {
            long agora = System.currentTimeMillis();
            consultaDao.fecharConsultasPassadas(agora);
            tomaDao.marcarFalhadasAntesDe(agora - TOLERANCIA_MS);
            GeradorDeTomas.gerar(bd);
        });
    }

    /** Leitura pontual do contacto principal, para o botão SOS poder ligar logo. */
    public void obterContactoPrincipal(Resultado<Membro> resultado) {
        executar("obter o contacto principal", () ->
                resultado.aoObter(membroDao.obterContactoPrincipal()));
    }

    // ==================================================================

    /** Corre a escrita no executor, sem deixar a app fechar se alguma coisa falhar. */
    private void executar(String operacao, Runnable tarefa) {
        AppDatabase.EXECUTOR.execute(() -> {
            try {
                tarefa.run();
            } catch (Exception e) {
                Log.e(TAG, "Falha ao " + operacao, e);
            }
        });
    }
}
