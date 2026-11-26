package pt.ubi.di.pdm.vital;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.List;

import pt.ubi.di.pdm.vital.data.AppDatabase;
import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.dao.ChatDao;
import pt.ubi.di.pdm.vital.data.dao.ComunidadeDao;
import pt.ubi.di.pdm.vital.data.dao.ConsultaDao;
import pt.ubi.di.pdm.vital.data.dao.MedicamentoDao;
import pt.ubi.di.pdm.vital.data.dao.MembroDao;
import pt.ubi.di.pdm.vital.data.dao.RegistoTomaDao;
import pt.ubi.di.pdm.vital.data.model.CategoriaDuvida;
import pt.ubi.di.pdm.vital.data.model.Consulta;
import pt.ubi.di.pdm.vital.data.model.Duvida;
import pt.ubi.di.pdm.vital.data.model.DuvidaResumo;
import pt.ubi.di.pdm.vital.data.model.EstadoConsulta;
import pt.ubi.di.pdm.vital.data.model.EstadoToma;
import pt.ubi.di.pdm.vital.data.model.EventoHistorial;
import pt.ubi.di.pdm.vital.data.model.Horario;
import pt.ubi.di.pdm.vital.data.model.Medicamento;
import pt.ubi.di.pdm.vital.data.model.MedicamentoComHorarios;
import pt.ubi.di.pdm.vital.data.model.Membro;
import pt.ubi.di.pdm.vital.data.model.MensagemChat;
import pt.ubi.di.pdm.vital.data.model.Nota;
import pt.ubi.di.pdm.vital.data.model.PapelMembro;
import pt.ubi.di.pdm.vital.data.model.RegistoToma;
import pt.ubi.di.pdm.vital.data.model.Resposta;
import pt.ubi.di.pdm.vital.data.model.ResumoDia;
import pt.ubi.di.pdm.vital.data.model.TipoMensagem;
import pt.ubi.di.pdm.vital.data.model.TomaDeHoje;
import pt.ubi.di.pdm.vital.util.DataUtil;
import pt.ubi.di.pdm.vital.util.DiasSemana;

/**
 * Testes às consultas da base de dados.
 *
 * Correm sobre uma base de dados em memória, criada vazia antes de cada teste
 * e deitada fora no fim. Não tocam na base de dados real do telemóvel e não
 * dependem dos dados de demonstração — cada teste constrói o que precisa.
 */
@RunWith(AndroidJUnit4.class)
public class BaseDeDadosTest {

    private AppDatabase bd;
    private MedicamentoDao medicamentoDao;
    private RegistoTomaDao tomaDao;
    private MembroDao membroDao;
    private ConsultaDao consultaDao;
    private ChatDao chatDao;
    private ComunidadeDao comunidadeDao;

    @Before
    public void criarBaseDeDados() {
        Context contexto = ApplicationProvider.getApplicationContext();
        bd = Room.inMemoryDatabaseBuilder(contexto, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        medicamentoDao = bd.medicamentoDao();
        tomaDao = bd.registoTomaDao();
        membroDao = bd.membroDao();
        consultaDao = bd.consultaDao();
        chatDao = bd.chatDao();
        comunidadeDao = bd.comunidadeDao();
    }

    @After
    public void fecharBaseDeDados() {
        bd.close();
    }

    // ==================================================================
    //  Medicamentos e horários
    // ==================================================================

    @Test
    public void medicamentoVemComOsSeusHorarios() {
        long id = inserirMedicamento("Metformina", "500 mg");
        inserirHorario(id, 8 * 60);
        inserirHorario(id, 14 * 60);

        List<MedicamentoComHorarios> lista = medicamentoDao.listarComHorarios();

        assertEquals(1, lista.size());
        assertEquals("Metformina", lista.get(0).medicamento.getNome());
        assertEquals(2, lista.get(0).horarios.size());
    }

    @Test
    public void apagarMedicamentoApagaOsHorariosEmCascata() {
        long id = inserirMedicamento("Ramipril", "5 mg");
        inserirHorario(id, 8 * 60);
        assertEquals(1, medicamentoDao.listarHorarios(id).size());

        medicamentoDao.apagar(medicamentoDao.obterPorId(id));

        // Sem a cascata ficavam horários órfãos a apontar para nada.
        assertEquals(0, medicamentoDao.listarHorarios(id).size());
    }

    @Test
    public void desativarEscondeDaListaSemApagarOHistorial() {
        long id = inserirMedicamento("Atorvastatina", "20 mg");
        long horario = inserirHorario(id, 21 * 60);
        inserirToma(id, horario, agoraMenos(1), EstadoToma.TOMADA);

        medicamentoDao.desativar(id);

        assertEquals(0, medicamentoDao.listarAtivos().size());
        assertNotNull("o registo de toma tem de sobreviver", medicamentoDao.obterPorId(id));
    }

    // ==================================================================
    //  Tomas
    // ==================================================================

    @Test
    public void tomasDoDiaTrazemOsDadosDoMedicamento() throws Exception {
        long id = inserirMedicamento("Metformina", "500 mg");
        long horario = inserirHorario(id, 14 * 60);
        inserirToma(id, horario, DataUtil.combinar(LocalDate.now(), 14 * 60),
                EstadoToma.PENDENTE);

        List<TomaDeHoje> tomas = EsperarLiveData.valorDe(
                tomaDao.observarTomasEntre(DataUtil.inicioDeHoje(), DataUtil.fimDeHoje()));

        assertEquals(1, tomas.size());
        // A junção evita uma consulta por cada linha da lista no ecrã.
        assertEquals("Metformina 500 mg", tomas.get(0).getNomeCompleto());
    }

    @Test
    public void marcarComoTomadaGuardaAHoraRealEOAtraso() {
        long id = inserirMedicamento("Ramipril", "5 mg");
        long horario = inserirHorario(id, 8 * 60);
        long prevista = agoraMenos(3);
        long registo = inserirToma(id, horario, prevista, EstadoToma.PENDENTE);

        long agora = System.currentTimeMillis();
        tomaDao.marcarComoTomada(registo, agora, VitalRepository.descreverAtraso(prevista, agora));

        RegistoToma guardado = tomaDao.obterPorId(registo);
        assertEquals(EstadoToma.TOMADA, guardado.getEstado());
        assertEquals(Long.valueOf(agora), guardado.getDataReal());
        assertTrue("três horas de atraso tinham de ficar registadas",
                guardado.getObservacao().contains("atraso"));
    }

    @Test
    public void tomaDentroDaMargemNaoFicaMarcadaComoAtrasada() {
        long prevista = System.currentTimeMillis();
        assertNull(VitalRepository.descreverAtraso(prevista, prevista + 60_000L));
    }

    @Test
    public void tomasPendentesForaDaToleranciaPassamAFalhadas() {
        long id = inserirMedicamento("Ramipril", "5 mg");
        long horario = inserirHorario(id, 8 * 60);
        long antiga = inserirToma(id, horario, agoraMenos(5), EstadoToma.PENDENTE);
        long recente = inserirToma(id, horario, agoraMenos(1), EstadoToma.PENDENTE);

        tomaDao.marcarFalhadasAntesDe(agoraMenos(2));

        assertEquals(EstadoToma.FALHADA, tomaDao.obterPorId(antiga).getEstado());
        assertEquals("a recente ainda está dentro da tolerância",
                EstadoToma.PENDENTE, tomaDao.obterPorId(recente).getEstado());
    }

    @Test
    public void resumoDoDiaContaTotalETomadas() throws Exception {
        long id = inserirMedicamento("Metformina", "500 mg");
        long horario = inserirHorario(id, 8 * 60);
        LocalDate hoje = LocalDate.now();

        inserirToma(id, horario, DataUtil.combinar(hoje, 8 * 60), EstadoToma.TOMADA);
        inserirToma(id, horario, DataUtil.combinar(hoje, 14 * 60), EstadoToma.TOMADA);
        inserirToma(id, horario, DataUtil.combinar(hoje, 21 * 60), EstadoToma.FALHADA);

        List<ResumoDia> resumo = EsperarLiveData.valorDe(
                tomaDao.observarResumoPorDia(DataUtil.inicioDeHoje(), DataUtil.fimDeHoje()));

        assertEquals(1, resumo.size());
        assertEquals(3, resumo.get(0).total);
        assertEquals(2, resumo.get(0).tomadas);
        assertTrue("o dia não está completo", !resumo.get(0).isCompleto());
    }

    // ==================================================================
    //  Historial: a consulta com UNION sobre três tabelas
    // ==================================================================

    @Test
    public void historialJuntaTomasConsultasENotas() throws Exception {
        long medico = inserirMedicamento("Ramipril", "5 mg");
        long horario = inserirHorario(medico, 8 * 60);
        inserirToma(medico, horario, agoraMenos(2), EstadoToma.TOMADA);

        long membro = inserirMembro("Tiago", PapelMembro.NETO, 0);
        inserirConsulta("Cardiologia", agoraMenos(24));
        inserirNota(membro, "Beber mais água");

        List<EventoHistorial> tudo = EsperarLiveData.valorDe(
                tomaDao.observarHistorial("TUDO"));

        assertEquals(3, tudo.size());
        assertTrue(contem(tudo, "TOMA"));
        assertTrue(contem(tudo, "CONSULTA"));
        assertTrue(contem(tudo, "NOTA"));
    }

    @Test
    public void historialFiltradoMostraSoUmTipo() throws Exception {
        long medico = inserirMedicamento("Ramipril", "5 mg");
        long horario = inserirHorario(medico, 8 * 60);
        inserirToma(medico, horario, agoraMenos(2), EstadoToma.TOMADA);
        inserirConsulta("Cardiologia", agoraMenos(24));

        List<EventoHistorial> soConsultas = EsperarLiveData.valorDe(
                tomaDao.observarHistorial("CONSULTA"));

        assertEquals(1, soConsultas.size());
        assertEquals("CONSULTA", soConsultas.get(0).tipo);
    }

    @Test
    public void historialVemOrdenadoDoMaisRecenteParaOMaisAntigo() throws Exception {
        long medico = inserirMedicamento("Ramipril", "5 mg");
        long horario = inserirHorario(medico, 8 * 60);
        inserirToma(medico, horario, agoraMenos(10), EstadoToma.TOMADA);
        inserirToma(medico, horario, agoraMenos(2), EstadoToma.TOMADA);

        List<EventoHistorial> tudo = EsperarLiveData.valorDe(
                tomaDao.observarHistorial("TUDO"));

        assertTrue(tudo.get(0).dataHora > tudo.get(1).dataHora);
    }

    // ==================================================================
    //  Rede de apoio, chat e dúvidas
    // ==================================================================

    @Test
    public void contactoPrincipalEODeOrdemMaisBaixa() {
        inserirMembro("Tiago", PapelMembro.NETO, 2);
        inserirMembro("Ana", PapelMembro.FILHA, 1);
        inserirMembro("Carlos", PapelMembro.ENFERMEIRO, 0);

        Membro principal = membroDao.obterContactoPrincipal();

        assertNotNull(principal);
        assertEquals("Ana", principal.getNome());
    }

    @Test
    public void asPropriasMensagensNaoContamComoPorLer() throws Exception {
        long eu = inserirMembro("Maria", PapelMembro.IDOSO, 0);
        long filha = inserirMembro("Ana", PapelMembro.FILHA, 1);

        inserirMensagem(eu, "Já tomei", false);
        inserirMensagem(filha, "Tomou o comprimido?", false);

        Integer porLer = EsperarLiveData.valorDe(chatDao.contarPorLer(eu));

        assertEquals(Integer.valueOf(1), porLer);
    }

    @Test
    public void duvidasTrazemONumeroDeRespostas() throws Exception {
        long autor = inserirMembro("Ana", PapelMembro.FILHA, 1);
        long duvida = inserirDuvida(autor, "O alarme não tocou", CategoriaDuvida.ALARMES);
        inserirResposta(duvida, autor, "Verifique o som");
        inserirResposta(duvida, autor, "E o modo não incomodar");

        List<DuvidaResumo> lista = EsperarLiveData.valorDe(
                comunidadeDao.observarDuvidas(null, ""));

        assertEquals(1, lista.size());
        // A contagem é feita pelo SQL, não percorrendo a lista em Java.
        assertEquals(2, lista.get(0).numRespostas);
    }

    @Test
    public void duvidasFiltramPorCategoriaEPorTexto() throws Exception {
        long autor = inserirMembro("Ana", PapelMembro.FILHA, 1);
        inserirDuvida(autor, "O alarme não tocou", CategoriaDuvida.ALARMES);
        inserirDuvida(autor, "Posso tomar duas?", CategoriaDuvida.MEDICACAO);

        List<DuvidaResumo> porCategoria = EsperarLiveData.valorDe(
                comunidadeDao.observarDuvidas(CategoriaDuvida.ALARMES.name(), ""));
        assertEquals(1, porCategoria.size());

        List<DuvidaResumo> porTexto = EsperarLiveData.valorDe(
                comunidadeDao.observarDuvidas(null, "duas"));
        assertEquals(1, porTexto.size());
        assertEquals("Posso tomar duas?", porTexto.get(0).duvida.getTitulo());
    }

    // ==================================================================
    //  Auxiliares
    // ==================================================================

    private long inserirMedicamento(String nome, String dosagem) {
        Medicamento med = new Medicamento();
        med.setNome(nome);
        med.setDosagem(dosagem);
        med.setQuantidadePorToma(1);
        med.setDiasSemana(DiasSemana.TODOS);
        med.setAtivo(true);
        med.setCriadoEm(System.currentTimeMillis());
        return medicamentoDao.inserir(med);
    }

    private long inserirHorario(long medicamentoId, int minutos) {
        Horario horario = new Horario();
        horario.setMedicamentoId(medicamentoId);
        horario.setMinutosDoDia(minutos);
        return medicamentoDao.inserirHorario(horario);
    }

    private long inserirToma(long medicamentoId, long horarioId, long prevista,
                             EstadoToma estado) {
        RegistoToma toma = new RegistoToma();
        toma.setMedicamentoId(medicamentoId);
        toma.setHorarioId(horarioId);
        toma.setDataPrevista(prevista);
        toma.setEstado(estado);
        return tomaDao.inserir(toma);
    }

    private long inserirMembro(String nome, PapelMembro papel, int ordem) {
        Membro membro = new Membro();
        membro.setNome(nome);
        membro.setPapel(papel);
        membro.setOrdemEmergencia(ordem);
        return membroDao.inserir(membro);
    }

    private void inserirConsulta(String especialidade, long quando) {
        Consulta consulta = new Consulta();
        consulta.setEspecialidade(especialidade);
        consulta.setDataHora(quando);
        consulta.setEstado(EstadoConsulta.AGENDADA);
        consultaDao.inserir(consulta);
    }

    private void inserirNota(long autorId, String texto) {
        Nota nota = new Nota();
        nota.setAutorId(autorId);
        nota.setTexto(texto);
        nota.setDataHora(System.currentTimeMillis());
        comunidadeDao.inserirNota(nota);
    }

    private void inserirMensagem(long autorId, String texto, boolean lida) {
        MensagemChat mensagem = new MensagemChat();
        mensagem.setAutorId(autorId);
        mensagem.setTexto(texto);
        mensagem.setDataHora(System.currentTimeMillis());
        mensagem.setTipo(TipoMensagem.TEXTO);
        mensagem.setLida(lida);
        chatDao.inserir(mensagem);
    }

    private long inserirDuvida(long autorId, String titulo, CategoriaDuvida categoria) {
        Duvida duvida = new Duvida();
        duvida.setAutorId(autorId);
        duvida.setTitulo(titulo);
        duvida.setCategoria(categoria);
        duvida.setDataHora(System.currentTimeMillis());
        duvida.setResolvida(false);
        return comunidadeDao.inserirDuvida(duvida);
    }

    private void inserirResposta(long duvidaId, long autorId, String texto) {
        Resposta resposta = new Resposta();
        resposta.setDuvidaId(duvidaId);
        resposta.setAutorId(autorId);
        resposta.setTexto(texto);
        resposta.setDataHora(System.currentTimeMillis());
        comunidadeDao.inserirResposta(resposta);
    }

    private long agoraMenos(int horas) {
        return System.currentTimeMillis() - horas * 60L * 60L * 1000L;
    }

    private boolean contem(List<EventoHistorial> eventos, String tipo) {
        for (EventoHistorial evento : eventos) {
            if (tipo.equals(evento.tipo)) {
                return true;
            }
        }
        return false;
    }
}
