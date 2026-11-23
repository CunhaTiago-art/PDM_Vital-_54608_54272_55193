package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Embedded;

/**
 * Uma toma agendada já com os dados do medicamento associado.
 * Evita que a interface tenha de fazer uma segunda consulta por cada linha.
 * Alimenta a lista "Hoje" do dashboard e o ecrã de alarme.
 */
public class TomaDeHoje {

    @Embedded
    public RegistoToma registo;

    public String nome;
    public String dosagem;
    public int quantidadePorToma;
    public String instrucoes;
    public String fotoUri;

    /** Texto pronto a mostrar: "Metformina 500 mg". */
    public String getNomeCompleto() {
        return dosagem == null || dosagem.trim().isEmpty() ? nome : nome + " " + dosagem;
    }

    /** Texto pronto a mostrar: "1 comprimido · com o almoço". */
    public String getDescricao() {
        String base = quantidadePorToma + (quantidadePorToma == 1 ? " comprimido" : " comprimidos");
        return instrucoes == null || instrucoes.trim().isEmpty() ? base : base + " · " + instrucoes;
    }
}
