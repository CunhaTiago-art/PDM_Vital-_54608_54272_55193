package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Embedded;

/** Dúvida já com o número de respostas ("✓ Resolvido · 3 respostas"). */
public class DuvidaResumo {

    @Embedded
    public Duvida duvida;

    public int numRespostas;
}
