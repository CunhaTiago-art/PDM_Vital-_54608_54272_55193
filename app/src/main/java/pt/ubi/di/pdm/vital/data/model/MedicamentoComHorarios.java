package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * Um medicamento com todos os seus horários, numa só consulta.
 * Alimenta a lista do ecrã "Caixa de Remédios".
 */
public class MedicamentoComHorarios {

    @Embedded
    public Medicamento medicamento;

    @Relation(parentColumn = "id", entityColumn = "medicamentoId")
    public List<Horario> horarios;
}
