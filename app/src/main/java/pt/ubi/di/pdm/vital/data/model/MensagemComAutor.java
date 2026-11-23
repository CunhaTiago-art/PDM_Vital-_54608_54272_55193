package pt.ubi.di.pdm.vital.data.model;

import androidx.room.Embedded;

/**
 * Mensagem de chat já com o nome e o papel de quem a escreveu,
 * para o adapter distinguir item_chat_enviado de item_chat_recebido.
 */
public class MensagemComAutor {

    @Embedded
    public MensagemChat mensagem;

    public String autorNome;
    public PapelMembro autorPapel;
}
