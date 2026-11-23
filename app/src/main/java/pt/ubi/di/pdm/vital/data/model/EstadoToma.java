package pt.ubi.di.pdm.vital.data.model;

/**
 * Estado de uma toma agendada.
 * PENDENTE  - ainda não chegou a hora, ou chegou e ainda não houve resposta
 * TOMADA    - o idoso confirmou com o botão "JÁ TOMEI"
 * ADIADA    - o alarme foi adiado 10 minutos
 * FALHADA   - passou a janela de tolerância sem confirmação
 */
public enum EstadoToma {
    PENDENTE,
    TOMADA,
    ADIADA,
    FALHADA
}
