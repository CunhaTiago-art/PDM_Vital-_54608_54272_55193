package pt.ubi.di.pdm.vital.data;

import android.util.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import pt.ubi.di.pdm.vital.data.dao.MedicamentoDao;
import pt.ubi.di.pdm.vital.data.dao.RegistoTomaDao;
import pt.ubi.di.pdm.vital.data.model.EstadoToma;
import pt.ubi.di.pdm.vital.data.model.Horario;
import pt.ubi.di.pdm.vital.data.model.MedicamentoComHorarios;
import pt.ubi.di.pdm.vital.data.model.RegistoToma;
import pt.ubi.di.pdm.vital.util.DataUtil;
import pt.ubi.di.pdm.vital.util.DiasSemana;

/**
 * Transforma as prescrições (medicamento + horários + dias da semana) nas tomas
 * concretas de cada dia, que é o que o dashboard mostra e o que o AlarmManager agenda.
 *
 * É chamado sempre que a app arranca e sempre que um medicamento é criado ou alterado.
 * Nunca cria duas vezes a mesma toma, por isso pode correr as vezes que forem precisas.
 */
public final class GeradorDeTomas {

    private static final String TAG = "GeradorDeTomas";

    /** Quantos dias de tomas são criados de cada vez. */
    public static final int DIAS_A_GERAR = 14;

    private GeradorDeTomas() {
        // classe utilitária, não se instancia
    }

    /** Gera as tomas em falta para os próximos dias. Tem de correr fora da thread principal. */
    public static int gerar(AppDatabase bd) {
        return gerar(bd, DIAS_A_GERAR);
    }

    public static int gerar(AppDatabase bd, int dias) {
        MedicamentoDao medicamentoDao = bd.medicamentoDao();
        RegistoTomaDao tomaDao = bd.registoTomaDao();

        List<MedicamentoComHorarios> prescricoes = medicamentoDao.listarComHorarios();
        List<RegistoToma> novas = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        for (MedicamentoComHorarios prescricao : prescricoes) {
            if (prescricao.medicamento == null || prescricao.horarios == null) {
                continue;
            }
            for (int i = 0; i < dias; i++) {
                LocalDate dia = hoje.plusDays(i);
                if (!DiasSemana.contem(prescricao.medicamento.getDiasSemana(), dia.getDayOfWeek())) {
                    continue;
                }
                for (Horario horario : prescricao.horarios) {
                    long dataPrevista = DataUtil.combinar(dia, horario.getMinutosDoDia());
                    if (tomaDao.contarExistentes(horario.getId(), dataPrevista) > 0) {
                        continue;
                    }
                    RegistoToma toma = new RegistoToma();
                    toma.setMedicamentoId(prescricao.medicamento.getId());
                    toma.setHorarioId(horario.getId());
                    toma.setDataPrevista(dataPrevista);
                    toma.setEstado(EstadoToma.PENDENTE);
                    novas.add(toma);
                }
            }
        }

        if (!novas.isEmpty()) {
            tomaDao.inserirVarios(novas);
            Log.d(TAG, "Foram geradas " + novas.size() + " tomas.");
        }
        return novas.size();
    }
}
