package pt.ubi.di.pdm.vital.ui.duvidas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pt.ubi.di.pdm.vital.R;
import pt.ubi.di.pdm.vital.data.model.DuvidaResumo;

/** Lista do ecrã "Dúvidas e ajuda". */
public class DuvidasAdapter extends RecyclerView.Adapter<DuvidasAdapter.LinhaViewHolder> {

    public interface AoTocar {
        void aoTocar(DuvidaResumo item);
    }

    private final List<DuvidaResumo> duvidas = new ArrayList<>();
    private final AoTocar escuta;

    public DuvidasAdapter(AoTocar escuta) {
        this.escuta = escuta;
    }

    public void submeter(List<DuvidaResumo> novas) {
        duvidas.clear();
        if (novas != null) {
            duvidas.addAll(novas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LinhaViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View vista = LayoutInflater.from(pai.getContext())
                .inflate(R.layout.item_duvida, pai, false);
        return new LinhaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull LinhaViewHolder holder, int posicao) {
        DuvidaResumo item = duvidas.get(posicao);

        holder.titulo.setText(item.duvida.getTitulo());

        String respostas = item.numRespostas
                + (item.numRespostas == 1 ? " resposta" : " respostas");

        if (item.duvida.isResolvida()) {
            holder.estado.setText("✓ Resolvido · " + respostas);
            holder.estado.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.vital_green));
        } else {
            holder.estado.setText("Por resolver · " + respostas);
            holder.estado.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.vital_gray));
        }

        holder.itemView.setOnClickListener(v -> {
            if (escuta != null) {
                escuta.aoTocar(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return duvidas.size();
    }

    static class LinhaViewHolder extends RecyclerView.ViewHolder {

        final TextView titulo;
        final TextView estado;

        LinhaViewHolder(@NonNull View vista) {
            super(vista);
            titulo = vista.findViewById(R.id.tvPerguntaTitulo);
            estado = vista.findViewById(R.id.tvPerguntaEstado);
        }
    }
}
