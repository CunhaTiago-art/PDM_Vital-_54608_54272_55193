package pt.ubi.di.pdm.vital.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.UUID;

import pt.ubi.di.pdm.vital.data.dao.ChatDao;
import pt.ubi.di.pdm.vital.data.dao.ComunidadeDao;
import pt.ubi.di.pdm.vital.data.dao.ConsultaDao;
import pt.ubi.di.pdm.vital.data.dao.MedicamentoDao;
import pt.ubi.di.pdm.vital.data.dao.MembroDao;
import pt.ubi.di.pdm.vital.data.dao.MetricaSaudeDao;
import pt.ubi.di.pdm.vital.data.dao.RegistoTomaDao;
import pt.ubi.di.pdm.vital.data.model.CategoriaDuvida;
import pt.ubi.di.pdm.vital.data.model.Consulta;
import pt.ubi.di.pdm.vital.data.model.ConsultaComAcompanhante;
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
import pt.ubi.di.pdm.vital.data.model.RegistoToma;
import pt.ubi.di.pdm.vital.data.model.PapelMembro;
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
    public static final long TOLERANCIA_MS = 2 * 60 * 60 * 1000L; // 2 horas

    /**
     * Margem, para um lado e para o outro, dentro da qual uma toma conta como
     * feita a horas. Antes desta janela não se pode confirmar; depois dela
     * pode-se, mas fica registado o atraso.
     */
    public static final long MARGEM_MS = 30 * 60 * 1000L; // 30 minutos

    private final AppDatabase bd;
    private final MembroDao membroDao;
    private final MedicamentoDao medicamentoDao;
    private final RegistoTomaDao tomaDao;
    private final MetricaSaudeDao metricaDao;
    private final ConsultaDao consultaDao;
    private final ChatDao chatDao;
    private final ComunidadeDao comunidadeDao;
    private final SessionManager sessao;

    public VitalRepository(Context context) {
        bd = AppDatabase.getInstance(context);
        sessao = new SessionManager(context);
        membroDao = bd.membroDao();
        medicamentoDao = bd.medicamentoDao();
        tomaDao = bd.registoTomaDao();
        metricaDao = bd.metricaSaudeDao();
        consultaDao = bd.consultaDao();
        chatDao = bd.chatDao();
        comunidadeDao = bd.comunidadeDao();
    }

    public SessionManager getSessao() {
        return sessao;
    }

    /**
     * Quem souber falar com a nuvem regista-se aqui.
     *
     * A camada Room não conhece o Firebase: limita-se a avisar que há coisas novas
     * por enviar. Assim a base de dados funciona na mesma sem rede, e trocar o
     * Firestore por outra tecnologia não obriga a mexer em nada do que está aqui.
     */
    public interface Sincronizador {
        void enviarPendentes();
    }

    private static volatile Sincronizador sincronizador;

    public static void setSincronizador(Sincronizador s) {
        sincronizador = s;
    }

    /** Avisa a camada de sincronização de que há conteúdo novo por enviar. */
    private void notificarSincronizador() {
        Sincronizador s = sincronizador;
        if (s != null) {
            try {
                s.enviarPendentes();
            } catch (Exception e) {
                Log.e(TAG, "Falha ao avisar a sincronização", e);
            }
        }
    }

    /** Resultado devolvido numa leitura pontual feita fora da thread principal. */
    public interface Resultado<T> {
        void aoObter(T valor);
    }

    // ==================================================================
    //  DASHBOARD
    // ==================================================================

    public LiveData<TomaDeHoje> observarProximaToma() {
        return tomaDao.observarProximaToma(System.currentTimeMillis() - TOLERANCIA_MS);
    }

    /**
     * Diz se uma toma já pode ser confirmada.
     *
     * Não se confirma medicação com horas de antecedência: ficaria registado que
     * a pessoa tomou comprimidos que ainda não tomou, e quem olhasse para o
     * historial era induzido em erro. Aceita-se meia hora antes da hora marcada.
     */
    public static boolean estaNaHora(long dataPrevista) {
        return System.currentTimeMillis() >= dataPrevista - MARGEM_MS;
    }

    /**
     * Nota a guardar quando a confirmação chega fora de horas.
     * Devolve null quando a toma foi feita dentro da margem — nesse caso não há
     * nada a assinalar e a coluna fica vazia.
     */
    public static String descreverAtraso(long dataPrevista, long dataReal) {
        long atraso = dataReal - dataPrevista;
        if (atraso <= MARGEM_MS) {
            return null;
        }
        long minutos = atraso / 60_000L;
        if (minutos < 60) {
            return "com " + minutos + " min de atraso";
        }
        long horas = minutos / 60;
        long resto = minutos % 60;
        if (resto == 0) {
            return "com " + horas + (horas == 1 ? " hora de atraso" : " horas de atraso");
        }
        return "com " + horas + "h" + String.format(java.util.Locale.ROOT, "%02d", resto)
                + " de atraso";
    }

    public LiveData<List<TomaDeHoje>> observarTomasDeHoje() {
        return tomaDao.observarTomasEntre(DataUtil.inicioDeHoje(), DataUtil.fimDeHoje());
    }

    /**
     * Botão "JÁ TOMEI".
     *
     * Lê a hora prevista para calcular o atraso antes de gravar. A confirmação
     * é sempre aceite depois da hora: uma toma tardia continua a ser uma toma,
     * e bloqueá-la só faria com que a informação se perdesse.
     */
    public void marcarComoTomada(long registoId) {
        executar("marcar a toma", () -> {
            RegistoToma registo = tomaDao.obterPorId(registoId);
            if (registo == null) {
                return;
            }
            long agora = System.currentTimeMillis();
            tomaDao.marcarComoTomada(registoId, agora,
                    descreverAtraso(registo.getDataPrevista(), agora));
        });
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
        return observarSemana(0);
    }

    public LiveData<Integer> contarTomadasDaSemana() {
        return contarTomadasDaSemana(0);
    }

    public LiveData<Integer> contarTomasDaSemana() {
        return contarTomasDaSemana(0);
    }

    /** deslocamento 0 = esta semana, -1 = a passada, e assim sucessivamente. */
    public LiveData<List<ResumoDia>> observarSemana(int deslocamento) {
        return tomaDao.observarResumoPorDia(
                DataUtil.inicioDaSemana(deslocamento), DataUtil.fimDaSemana(deslocamento));
    }

    public LiveData<Integer> contarTomadasDaSemana(int deslocamento) {
        return tomaDao.contarTomadasEntre(
                DataUtil.inicioDaSemana(deslocamento), DataUtil.fimDaSemana(deslocamento));
    }

    public LiveData<Integer> contarTomasDaSemana(int deslocamento) {
        return tomaDao.contarTotalEntre(
                DataUtil.inicioDaSemana(deslocamento), DataUtil.fimDaSemana(deslocamento));
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

    public LiveData<List<MetricaSaude>> observarMetricas() {
        return metricaDao.observarTodas();
    }

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

    public LiveData<ConsultaComAcompanhante> observarProximaConsultaDetalhada() {
        return consultaDao.observarProximaDetalhada(System.currentTimeMillis());
    }

    public LiveData<List<ConsultaComAcompanhante>> observarConsultasFuturasDetalhadas() {
        return consultaDao.observarFuturasDetalhadas(System.currentTimeMillis());
    }

    public LiveData<List<ConsultaComAcompanhante>> observarConsultasPassadasDetalhadas() {
        return consultaDao.observarPassadasDetalhadas(System.currentTimeMillis());
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
            // O identificador global é gerado aqui, e não pedido ao servidor.
            // Assim a linha local já o tem antes de a mensagem subir, e quando o
            // Firestore nos devolve o nosso próprio documento sabemos reconhecê-lo.
            mensagem.setIdRemoto(UUID.randomUUID().toString());
            chatDao.inserir(mensagem);
            notificarSincronizador();
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
            duvida.setIdRemoto(UUID.randomUUID().toString());
            comunidadeDao.inserirDuvida(duvida);
            notificarSincronizador();
        });
    }

    public void responder(long duvidaId, long autorId, String texto) {
        executar("responder à dúvida", () -> {
            Resposta resposta = new Resposta();
            resposta.setDuvidaId(duvidaId);
            resposta.setAutorId(autorId);
            resposta.setTexto(texto);
            resposta.setDataHora(System.currentTimeMillis());
            resposta.setIdRemoto(UUID.randomUUID().toString());
            comunidadeDao.inserirResposta(resposta);
            notificarSincronizador();
        });
    }

    public void marcarDuvidaResolvida(long duvidaId) {
        executar("marcar a dúvida como resolvida", () -> {
            comunidadeDao.marcarResolvida(duvidaId);
            notificarSincronizador();
        });
    }

    // ==================================================================
    //  IDENTIDADE E SINCRONIZAÇÃO
    // ==================================================================

    /**
     * Devolve o id local do membro com esta identidade global, criando-o se ainda
     * não existir neste telemóvel.
     *
     * É a peça central da sincronização: quando chega uma mensagem da nuvem escrita
     * por alguém que este aparelho nunca viu, é preciso haver uma linha na tabela
     * `membros` para a chave estrangeira da mensagem poder apontar para algum lado.
     *
     * Tem de ser chamado fora da thread principal.
     */
    public long garantirMembro(String uid, String nome, PapelMembro papel) {
        Membro existente = membroDao.obterPorUid(uid);
        if (existente != null) {
            if (nome != null && !nome.equals(existente.getNome())) {
                existente.setNome(nome);
                membroDao.atualizar(existente);
            }
            return existente.getId();
        }

        // Pode já existir localmente sem uid: o caso da Ana que foi criada à mão
        // no telemóvel do idoso e só mais tarde instalou a app no telemóvel dela.
        Membro porNome = nome == null ? null : membroDao.obterPorNome(nome);
        if (porNome != null && porNome.getUidRemoto() == null) {
            membroDao.definirUid(porNome.getId(), uid);
            return porNome.getId();
        }

        Membro novo = new Membro();
        novo.setNome(nome == null ? "Alguém da família" : nome);
        novo.setPapel(papel == null ? PapelMembro.CUIDADOR : papel);
        novo.setUidRemoto(uid);
        novo.setPodeVerHistorial(false);
        novo.setRecebeAvisos(true);
        return membroDao.inserir(novo);
    }

    /**
     * Liga o perfil escolhido no ecrã de Entrada a uma linha da tabela `membros`
     * e guarda o id na sessão, para a app saber quem está a usar o telemóvel.
     */
    public void prepararSessao(Resultado<Long> resultado) {
        executar("preparar a sessão", () -> {
            long id = sessao.getMembroId();
            if (id == 0) {
                id = garantirMembroLocal();
                if (id != 0) {
                    sessao.setMembroId(id);
                }
            }
            if (resultado != null) {
                resultado.aoObter(id);
            }
        });
    }

    /**
     * Garante que existe um membro para quem está a usar o telemóvel, com ou sem nuvem.
     *
     * A identidade local nunca pode depender do Firebase: sem rede, sem Google Play
     * Services ou sem código de família, a pessoa tem de conseguir escrever na mesma.
     * Quando mais tarde houver ligação, o uid é colado a esta mesma linha pelo nome.
     *
     * Tem de ser chamado fora da thread principal.
     */
    public long garantirMembroLocal() {
        String uid = sessao.getUidRemoto();
        if (uid != null) {
            return garantirMembro(uid, sessao.getNome(), sessao.getPapel());
        }

        String nome = sessao.getNome();
        if (nome == null || nome.trim().isEmpty()) {
            nome = sessao.getPerfil();
        }

        Membro existente = membroDao.obterPorNome(nome);
        if (existente != null) {
            return existente.getId();
        }

        // O perfil "Idoso" deve apanhar o idoso que já está na base de dados,
        // mesmo que o nome escrito não seja exatamente igual ao que lá está.
        if (sessao.isIdoso()) {
            Membro idoso = membroDao.obterIdoso();
            if (idoso != null) {
                return idoso.getId();
            }
        }

        Membro novo = new Membro();
        novo.setNome(nome);
        novo.setPapel(sessao.getPapel());
        novo.setPodeVerHistorial(true);
        novo.setRecebeAvisos(true);
        return membroDao.inserir(novo);
    }

    /** Conteúdo escrito neste telemóvel que ainda está à espera de subir para a nuvem. */
    public LiveData<Integer> contarPorSincronizar() {
        return chatDao.contarPorSincronizar();
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
