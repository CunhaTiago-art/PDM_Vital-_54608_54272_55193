package pt.ubi.di.pdm.vital.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pt.ubi.di.pdm.vital.R;
import pt.ubi.di.pdm.vital.data.model.MensagemComAutor;
import pt.ubi.di.pdm.vital.util.DataUtil;

/**
 * Desenha a conversa. Usa dois layouts diferentes: as mensagens escritas neste
 * telemóvel vão para a direita (item_chat_enviado) e as dos outros para a
 * esquerda (item_chat_recebido), tal como no wireframe.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.BalaoViewHolder> {

    private static final int ENVIADA = 1;
    private static final int RECEBIDA = 2;

    private final List<MensagemComAutor> mensagens = new ArrayList<>();
    private long meuId;

    public void setMeuId(long meuId) {
        this.meuId = meuId;
        notifyDataSetChanged();
    }

    public void submeter(List<MensagemComAutor> novas) {
        mensagens.clear();
        if (novas != null) {
            mensagens.addAll(novas);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int posicao) {
        return mensagens.get(posicao).mensagem.getAutorId() == meuId ? ENVIADA : RECEBIDA;
    }

    @NonNull
    @Override
    public BalaoViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        int layout = tipo == ENVIADA
                ? R.layout.item_chat_enviado
                : R.layout.item_chat_recebido;
        View vista = LayoutInflater.from(pai.getContext()).inflate(layout, pai, false);
        return new BalaoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull BalaoViewHolder holder, int posicao) {
        MensagemComAutor item = mensagens.get(posicao);

        String autor = item.mensagem.getAutorId() == meuId
                ? "Eu"
                : (item.autorNome == null ? "Alguém" : item.autorNome);

        holder.autorHora.setText(autor + " · " + DataUtil.formatarHora(item.mensagem.getDataHora()));
        holder.texto.setText(item.mensagem.getTexto());
    }

    @Override
    public int getItemCount() {
        return mensagens.size();
    }

    static class BalaoViewHolder extends RecyclerView.ViewHolder {

        final TextView autorHora;
        final TextView texto;

        BalaoViewHolder(@NonNull View vista) {
            super(vista);
            autorHora = vista.findViewById(R.id.tvChatAutorHora);
            texto = vista.findViewById(R.id.tvChatMensagem);
        }
    }
}
