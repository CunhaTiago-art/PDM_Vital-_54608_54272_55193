package pt.ubi.di.pdm.vital;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import pt.ubi.di.pdm.vital.data.AppDatabase;
import pt.ubi.di.pdm.vital.data.GeradorDeTomas;
import pt.ubi.di.pdm.vital.data.dao.MedicamentoDao;
import pt.ubi.di.pdm.vital.data.dao.RegistoTomaDao;
import pt.ubi.di.pdm.vital.data.model.EstadoToma;
import pt.ubi.di.pdm.vital.data.model.Horario;
import pt.ubi.di.pdm.vital.data.model.Medicamento;
import pt.ubi.di.pdm.vital.data.model.RegistoToma;
import pt.ubi.di.pdm.vital.util.DataUtil;
import pt.ubi.di.pdm.vital.util.DiasSemana;

/**
 * Testes ao gerador de tomas.
 *
 * Esta é a única lógica verdadeiramente nossa na camada de dados — o resto são
 * consultas que o Room valida em tempo de compilação. O gerador transforma uma
 * prescrição (medicamento + horários + dias da semana) nas tomas concretas de
 * cada dia, e corre sempre que a app arranca.
 *
 * Duas coisas têm de ser verdade, e nenhuma é óbvia: correr duas vezes não pode
 * duplicar tomas, e os dias da semana têm de ser respeitados. Se a primeira
 * falhar, o idoso vê a mesma toma repetida; se a segunda falhar, é avisado para
 * tomar medicação em dias em que não devia.
 */
@RunWith(AndroidJUnit4.class)
public class GeradorDeTomasTest {

    private AppDatabase bd;
    private MedicamentoDao medicamentoDao;
    private RegistoTomaDao tomaDao;

    @Before
    public void criarBaseDeDados() {
        Context contexto = ApplicationProvider.getApplicationContext();
        bd = Room.inMemoryDatabaseBuilder(contexto, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        medicamentoDao = bd.medicamentoDao();
        tomaDao = bd.registoTomaDao();
    }

    @After
    public void fecharBaseDeDados() {
        bd.close();
    }

    @Test
    public void geraUmaTomaPorDiaPorHorario() {
        long id = inserirMedicamento(DiasSemana.TODOS);
        inserirHorario(id, 8 * 60);
        inserirHorario(id, 20 * 60);

        int criadas = GeradorDeTomas.gerar(bd, 7);

        // 7 dias x 2 horários
        assertEquals(14, criadas);
    }

    @Test
    public void correrDuasVezesNaoDuplica() {
        long id = inserirMedicamento(DiasSemana.TODOS);
        inserirHorario(id, 8 * 60);

        int primeira = GeradorDeTomas.gerar(bd, 7);
        int segunda = GeradorDeTomas.gerar(bd, 7);

        assertEquals(7, primeira);
        assertEquals("a segunda passagem não pode criar nada", 0, segunda);
    }

    @Test
    public void respeitaOsDiasDaSemana() {
        // Só às segundas-feiras: em 14 dias há exatamente duas.
        long id = inserirMedicamento(DiasSemana.SEGUNDA);
        inserirHorario(id, 8 * 60);

        GeradorDeTomas.gerar(bd, 14);

        List<RegistoToma> todas = todasAsTomas();
        assertEquals(2, todas.size());
        for (RegistoToma toma : todas) {
            DayOfWeek dia = DataUtil.paraDataHora(toma.getDataPrevista()).getDayOfWeek();
            assertEquals("gerou uma toma fora dos dias escolhidos",
                    DayOfWeek.MONDAY, dia);
        }
    }

    @Test
    public void naoGeraParaMedicamentoSuspenso() {
        long id = inserirMedicamento(DiasSemana.TODOS);
        inserirHorario(id, 8 * 60);
        medicamentoDao.desativar(id);

        assertEquals(0, GeradorDeTomas.gerar(bd, 7));
    }

    @Test
    public void asTomasNascemPendentes() {
        long id = inserirMedicamento(DiasSemana.TODOS);
        inserirHorario(id, 8 * 60);

        GeradorDeTomas.gerar(bd, 3);

        for (RegistoToma toma : todasAsTomas()) {
            assertEquals(EstadoToma.PENDENTE, toma.getEstado());
        }
    }

    @Test
    public void acrescentarUmHorarioGeraSoAsTomasEmFalta() {
        long id = inserirMedicamento(DiasSemana.TODOS);
        inserirHorario(id, 8 * 60);
        GeradorDeTomas.gerar(bd, 7);

        // O utilizador acrescenta uma segunda hora à mesma prescrição.
        inserirHorario(id, 20 * 60);
        int novas = GeradorDeTomas.gerar(bd, 7);

        assertEquals("só as da hora nova", 7, novas);
        assertEquals(14, todasAsTomas().size());
    }

    @Test
    public void naoGeraTomasParaOPassado() {
        long id = inserirMedicamento(DiasSemana.TODOS);
        inserirHorario(id, 8 * 60);

        GeradorDeTomas.gerar(bd, 7);

        long inicioDeHoje = DataUtil.inicioDeHoje();
        for (RegistoToma toma : todasAsTomas()) {
            assertTrue("gerou uma toma antes de hoje",
                    toma.getDataPrevista() >= inicioDeHoje);
        }
    }

    // ==================================================================

    private long inserirMedicamento(int diasSemana) {
        Medicamento med = new Medicamento();
        med.setNome("Metformina");
        med.setDosagem("500 mg");
        med.setQuantidadePorToma(1);
        med.setDiasSemana(diasSemana);
        med.setAtivo(true);
        med.setCriadoEm(System.currentTimeMillis());
        return medicamentoDao.inserir(med);
    }

    private void inserirHorario(long medicamentoId, int minutos) {
        Horario horario = new Horario();
        horario.setMedicamentoId(medicamentoId);
        horario.setMinutosDoDia(minutos);
        medicamentoDao.inserirHorario(horario);
    }

    /** Lê todas as tomas de um intervalo largo, sem depender de LiveData. */
    private List<RegistoToma> todasAsTomas() {
        LocalDate hoje = LocalDate.now();
        return tomaDao.listarEntre(
                DataUtil.paraMillis(hoje.minusDays(30).atStartOfDay()),
                DataUtil.paraMillis(hoje.plusDays(60).atStartOfDay()));
    }
}
