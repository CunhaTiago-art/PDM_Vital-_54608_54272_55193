package pt.ubi.di.pdm.vital.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import pt.ubi.di.pdm.vital.data.SessionManager;
import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.model.MensagemComAutor;
import pt.ubi.di.pdm.vital.data.model.Membro;
import pt.ubi.di.pdm.vital.data.model.TipoMensagem;

/**
 * Liga o ecrã do chat à base de dados.
 *
 * O ecrã nunca fala com o Room nem com o Firebase: pede tudo aqui, e o que recebe
 * é LiveData, por isso a lista atualiza-se sozinha quando chega uma mensagem nova,
 * venha ela deste telemóvel ou da nuvem.
 */
public class ChatViewModel extends AndroidViewModel {

    private final VitalRepository repositorio;
    private final SessionManager sessao;
    private final LiveData<List<MensagemComAutor>> mensagens;
    private final LiveData<List<Membro>> rede;

    public ChatViewModel(@NonNull Application aplicacao) {
        super(aplicacao);
        repositorio = new VitalRepository(aplicacao);
        sessao = repositorio.getSessao();
        mensagens = repositorio.observarConversa();
        rede = repositorio.observarRedeDeApoio();
    }

    public LiveData<List<MensagemComAutor>> getMensagens() {
        return mensagens;
    }

    public LiveData<List<Membro>> getRede() {
        return rede;
    }

    /** Id do membro que está a usar o telemóvel: decide que balões ficam à direita. */
    public long getMeuId() {
        return sessao.getMembroId();
    }

    public boolean temFamilia() {
        return sessao.temFamilia();
    }

    public String getCodigoFamilia() {
        return sessao.getCodigoFamilia();
    }

    /** Grava a mensagem localmente; a sincronização trata de a enviar se houver rede. */
    public void enviar(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return;
        }
        long meuId = getMeuId();
        if (meuId == 0) {
            return; // ainda não se sabe quem está a usar o telemóvel
        }
        repositorio.enviarMensagem(meuId, texto.trim(), TipoMensagem.TEXTO, null);
    }

    public void marcarComoLido() {
        repositorio.marcarChatComoLido();
    }
}
