package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Embedded;

/**
 * Consulta já com o nome de quem acompanha, resolvido pela base de dados.
 * Evita que a lista faça uma consulta à tabela de membros por cada linha.
 */
public class ConsultaComAcompanhante {

    @Embedded
    public Consulta consulta;

    /** Null quando ainda não foi definido quem acompanha. */
    public String acompanhanteNome;
}
