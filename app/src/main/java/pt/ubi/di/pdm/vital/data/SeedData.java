package pt.ubi.di.pdm.vital.data;

import java.time.LocalDate;
import java.util.ArrayList;
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
import pt.ubi.di.pdm.vital.data.model.EstadoConsulta;
import pt.ubi.di.pdm.vital.data.model.EstadoToma;
import pt.ubi.di.pdm.vital.data.model.Horario;
import pt.ubi.di.pdm.vital.data.model.Medicamento;
import pt.ubi.di.pdm.vital.data.model.Membro;
import pt.ubi.di.pdm.vital.data.model.MensagemChat;
import pt.ubi.di.pdm.vital.data.model.MetricaSaude;
import pt.ubi.di.pdm.vital.data.model.Nota;
import pt.ubi.di.pdm.vital.data.model.PapelMembro;
import pt.ubi.di.pdm.vital.data.model.RegistoToma;
import pt.ubi.di.pdm.vital.data.model.Resposta;
import pt.ubi.di.pdm.vital.data.model.TipoMensagem;
import pt.ubi.di.pdm.vital.data.model.TipoMetrica;
import pt.ubi.di.pdm.vital.util.DataUtil;
import pt.ubi.di.pdm.vital.util.DiasSemana;

/**
 * Dados de demonstração inseridos na primeira vez que a app corre.
 * Reproduzem o conteúdo dos wireframes para a aplicação arrancar com
 * conteúdo real em vez de ecrãs vazios.
 *
 * As datas são calculadas a partir do dia de hoje, por isso a demonstração
 * continua a fazer sentido seja qual for o dia em que a app é instalada.
 */
public final class SeedData {

    private SeedData() {
        // classe utilitária, não se instancia
    }

    public static void popular(AppDatabase bd) {
        MembroDao membroDao = bd.membroDao();
        MedicamentoDao medicamentoDao = bd.medicamentoDao();
        RegistoTomaDao tomaDao = bd.registoTomaDao();
        ConsultaDao consultaDao = bd.consultaDao();
        ChatDao chatDao = bd.chatDao();
        ComunidadeDao comunidadeDao = bd.comunidadeDao();
        MetricaSaudeDao metricaDao = bd.metricaSaudeDao();

        // ---------- Rede de apoio ----------
        long idIdoso = membroDao.inserir(criarMembro("Maria Silva", PapelMembro.IDOSO, null, 0, true, false));
        long idFilha = membroDao.inserir(criarMembro("Ana", PapelMembro.FILHA, "912 345 678", 1, true, true));
        long idNeto = membroDao.inserir(criarMembro("Tiago", PapelMembro.NETO, "913 222 111", 2, true, true));
        long idEnfermeiro = membroDao.inserir(criarMembro("Enf. Carlos", PapelMembro.ENFERMEIRO, "914 555 333", 0, true, true));

        // ---------- Medicamentos e horários ----------
        long idRamipril = medicamentoDao.inserir(
                criarMedicamento("Ramipril", "5 mg", 1, "em jejum"));
        long hRamipril = inserirHorario(medicamentoDao, idRamipril, 8 * 60);          // 08:00

        long idMetformina = medicamentoDao.inserir(
                criarMedicamento("Metformina", "500 mg", 1, "com o almoço"));
        long hMetformina = inserirHorario(medicamentoDao, idMetformina, 14 * 60);     // 14:00

        long idAtorvastatina = medicamentoDao.inserir(
                criarMedicamento("Atorvastatina", "20 mg", 1, "ao deitar"));
        long hAtorvastatina = inserirHorario(medicamentoDao, idAtorvastatina, 21 * 60); // 21:00

        // ---------- Historial de tomas desta semana ----------
        criarHistorialDaSemana(tomaDao,
                new long[]{idRamipril, idMetformina, idAtorvastatina},
                new long[]{hRamipril, hMetformina, hAtorvastatina},
                new int[]{8 * 60, 14 * 60, 21 * 60});

        // ---------- Tomas dos próximos dias ----------
        GeradorDeTomas.gerar(bd);

        // ---------- Consultas ----------
        Consulta cardiologia = new Consulta();
        cardiologia.setEspecialidade("Cardiologia");
        cardiologia.setDataHora(DataUtil.combinar(LocalDate.now().plusDays(4), 10 * 60 + 30));
        cardiologia.setLocal("Centro de Saúde da Covilhã");
        cardiologia.setMedico("Dr. Nuno Pereira");
        cardiologia.setAcompanhanteId(idFilha);
        cardiologia.setLembreteMinutos(1440); // 1 dia antes
        cardiologia.setEstado(EstadoConsulta.AGENDADA);
        consultaDao.inserir(cardiologia);

        Consulta analises = new Consulta();
        analises.setEspecialidade("Análises ao sangue");
        analises.setDataHora(DataUtil.combinar(LocalDate.now().plusDays(15), 15 * 60));
        analises.setLocal("Laboratório Central");
        analises.setLembreteMinutos(10080); // 1 semana antes
        analises.setEstado(EstadoConsulta.AGENDADA);
        consultaDao.inserir(analises);

        Consulta medicinaGeral = new Consulta();
        medicinaGeral.setEspecialidade("Medicina Geral");
        medicinaGeral.setDataHora(DataUtil.combinar(LocalDate.now().minusDays(11), 9 * 60));
        medicinaGeral.setLocal("Centro de Saúde da Covilhã");
        medicinaGeral.setAcompanhanteId(idFilha);
        medicinaGeral.setLembreteMinutos(60);
        medicinaGeral.setEstado(EstadoConsulta.REALIZADA);
        medicinaGeral.setNotas("Receita renovada. Manter a medicação atual.");
        consultaDao.inserir(medicinaGeral);

        // ---------- Chat da família ----------
        long hoje = DataUtil.inicioDeHoje();
        chatDao.inserir(criarMensagem(idFilha, "Já tomou o Ramipril?", hoje + hora(9, 12), true));
        chatDao.inserir(criarMensagem(idIdoso, "Sim, já tomei.", hoje + hora(9, 14), true));
        chatDao.inserir(criarMensagem(idEnfermeiro, "Consulta marcada para dia 27, às 10:30.", hoje + hora(9, 30), false));
        chatDao.inserir(criarMensagem(idNeto, "Eu levo a avó. Até lá!", hoje + hora(9, 31), false));

        // ---------- Dúvidas e respostas ----------
        long idDuvidaAlarme = comunidadeDao.inserirDuvida(criarDuvida(idFilha,
                "O alarme não tocou. O que faço?",
                "Ontem à noite o telemóvel não tocou à hora do comprimido.",
                CategoriaDuvida.ALARMES, diasAtras(3), true));
        comunidadeDao.inserirResposta(criarResposta(idDuvidaAlarme, idEnfermeiro,
                "Confirme se o som do telemóvel não está no silêncio.", diasAtras(3) + hora(1, 0)));
        comunidadeDao.inserirResposta(criarResposta(idDuvidaAlarme, idNeto,
                "Também é preciso deixar a app correr em segundo plano.", diasAtras(3) + hora(2, 0)));
        comunidadeDao.inserirResposta(criarResposta(idDuvidaAlarme, idFilha,
                "Obrigada, já ficou resolvido.", diasAtras(2)));

        long idDuvidaHorario = comunidadeDao.inserirDuvida(criarDuvida(idNeto,
                "Como mudo o horário de um medicamento?",
                null, CategoriaDuvida.USAR_APP, diasAtras(6), true));
        comunidadeDao.inserirResposta(criarResposta(idDuvidaHorario, idFilha,
                "Abre a Caixa de Remédios e toca no medicamento.", diasAtras(6) + hora(3, 0)));
        comunidadeDao.inserirResposta(criarResposta(idDuvidaHorario, idEnfermeiro,
                "Depois é só acrescentar ou apagar a hora e guardar.", diasAtras(5)));

        long idDuvidaDose = comunidadeDao.inserirDuvida(criarDuvida(idIdoso,
                "Esqueci-me de uma toma. Posso tomar duas?",
                "Não tomei o comprimido do almoço.",
                CategoriaDuvida.MEDICACAO, diasAtras(1), false));
        comunidadeDao.inserirResposta(criarResposta(idDuvidaDose, idEnfermeiro,
                "Nunca duplique a dose. Fale primeiro com o médico ou o farmacêutico.",
                diasAtras(1) + hora(1, 30)));

        // ---------- Notas no historial ----------
        Nota nota = new Nota();
        nota.setAutorId(idNeto);
        nota.setTexto("Avó, lembra-te de beber água ao longo do dia.");
        nota.setDataHora(diasAtras(2) + hora(18, 0));
        comunidadeDao.inserirNota(nota);

        // ---------- Métricas de saúde ----------
        List<MetricaSaude> metricas = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            long dia = diasAtras(i);
            metricas.add(criarMetrica(TipoMetrica.BATIMENTO, 68 + (i % 4) * 3, dia + hora(9, 0)));
            metricas.add(criarMetrica(TipoMetrica.OXIGENIO, 96 + (i % 3), dia + hora(9, 5)));
            metricas.add(criarMetrica(TipoMetrica.PASSOS, 2200 + (i % 5) * 450, dia + hora(20, 0)));
            metricas.add(criarMetrica(TipoMetrica.AGUA, 250, dia + hora(10, 0)));
            metricas.add(criarMetrica(TipoMetrica.AGUA, 250, dia + hora(16, 0)));
        }
        metricaDao.inserirVarias(metricas);
    }

    // ==================================================================
    //  Auxiliares
    // ==================================================================

    /**
     * Cria o historial de tomas desde segunda-feira até hoje.
     * Deixa duas tomas por confirmar para o ecrã de Historial mostrar
     * também o estado "Não tomado", tal como no wireframe.
     */
    private static void criarHistorialDaSemana(RegistoTomaDao tomaDao,
                                               long[] medicamentos,
                                               long[] horarios,
                                               int[] minutos) {
        List<RegistoToma> registos = new ArrayList<>();
        long agora = System.currentTimeMillis();
        LocalDate segunda = DataUtil.paraDataHora(DataUtil.inicioDaSemana()).toLocalDate();
        LocalDate hoje = LocalDate.now();

        int contador = 0;
        for (LocalDate dia = segunda; !dia.isAfter(hoje); dia = dia.plusDays(1)) {
            for (int i = 0; i < medicamentos.length; i++) {
                long prevista = DataUtil.combinar(dia, minutos[i]);

                RegistoToma registo = new RegistoToma();
                registo.setMedicamentoId(medicamentos[i]);
                registo.setHorarioId(horarios[i]);
                registo.setDataPrevista(prevista);

                if (prevista > agora) {
                    registo.setEstado(EstadoToma.PENDENTE);
                } else if (contador == 5 || contador == 11) {
                    // duas tomas falhadas para o historial não ficar perfeito demais
                    registo.setEstado(EstadoToma.FALHADA);
                    registo.setObservacao("Alarme ignorado");
                } else {
                    registo.setEstado(EstadoToma.TOMADA);
                    registo.setDataReal(prevista + 4 * 60_000L);
                }
                registos.add(registo);
                contador++;
            }
        }
        tomaDao.inserirVarios(registos);
    }

    private static Membro criarMembro(String nome, PapelMembro papel, String telefone,
                                      int ordemEmergencia, boolean podeVerHistorial,
                                      boolean recebeAvisos) {
        Membro membro = new Membro();
        membro.setNome(nome);
        membro.setPapel(papel);
        membro.setTelefone(telefone);
        membro.setOrdemEmergencia(ordemEmergencia);
        membro.setPodeVerHistorial(podeVerHistorial);
        membro.setRecebeAvisos(recebeAvisos);
        return membro;
    }

    private static Medicamento criarMedicamento(String nome, String dosagem,
                                                int quantidade, String instrucoes) {
        Medicamento medicamento = new Medicamento();
        medicamento.setNome(nome);
        medicamento.setDosagem(dosagem);
        medicamento.setQuantidadePorToma(quantidade);
        medicamento.setInstrucoes(instrucoes);
        medicamento.setDiasSemana(DiasSemana.TODOS);
        medicamento.setAtivo(true);
        medicamento.setCriadoEm(System.currentTimeMillis());
        return medicamento;
    }

    private static long inserirHorario(MedicamentoDao dao, long medicamentoId, int minutosDoDia) {
        Horario horario = new Horario();
        horario.setMedicamentoId(medicamentoId);
        horario.setMinutosDoDia(minutosDoDia);
        return dao.inserirHorario(horario);
    }

    private static MensagemChat criarMensagem(long autorId, String texto, long dataHora, boolean lida) {
        MensagemChat mensagem = new MensagemChat();
        mensagem.setAutorId(autorId);
        mensagem.setTexto(texto);
        mensagem.setDataHora(dataHora);
        mensagem.setTipo(TipoMensagem.TEXTO);
        mensagem.setLida(lida);
        return mensagem;
    }

    private static Duvida criarDuvida(long autorId, String titulo, String descricao,
                                      CategoriaDuvida categoria, long dataHora, boolean resolvida) {
        Duvida duvida = new Duvida();
        duvida.setAutorId(autorId);
        duvida.setTitulo(titulo);
        duvida.setDescricao(descricao);
        duvida.setCategoria(categoria);
        duvida.setDataHora(dataHora);
        duvida.setResolvida(resolvida);
        return duvida;
    }

    private static Resposta criarResposta(long duvidaId, long autorId, String texto, long dataHora) {
        Resposta resposta = new Resposta();
        resposta.setDuvidaId(duvidaId);
        resposta.setAutorId(autorId);
        resposta.setTexto(texto);
        resposta.setDataHora(dataHora);
        return resposta;
    }

    private static MetricaSaude criarMetrica(TipoMetrica tipo, double valor, long dataHora) {
        MetricaSaude metrica = new MetricaSaude();
        metrica.setTipo(tipo);
        metrica.setValor(valor);
        metrica.setDataHora(dataHora);
        metrica.setOrigem("demonstração");
        return metrica;
    }

    /** Primeiro milissegundo do dia que ficou a N dias de distância. */
    private static long diasAtras(int dias) {
        return DataUtil.paraMillis(LocalDate.now().minusDays(dias).atStartOfDay());
    }

    /** Converte uma hora do dia em milissegundos, para somar ao início de um dia. */
    private static long hora(int horas, int minutos) {
        return (horas * 60L + minutos) * 60_000L;
    }
}
