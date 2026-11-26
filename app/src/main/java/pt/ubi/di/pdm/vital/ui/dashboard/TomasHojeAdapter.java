package pt.ubi.di.pdm.vital.ui.dashboard;

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
import pt.ubi.di.pdm.vital.data.model.EstadoToma;
import pt.ubi.di.pdm.vital.data.model.TomaDeHoje;
import pt.ubi.di.pdm.vital.util.DataUtil;

/**
 * Lista "Hoje" do dashboard: hora, medicamento e estado da toma.
 *
 * O estado "Em breve" não existe na base de dados — é calculado aqui, a partir
 * da hora prevista. Guardar na base de dados um estado que muda sozinho com o
 * passar do tempo obrigaria a reescrevê-lo a cada minuto.
 */
public class TomasHojeAdapter extends RecyclerView.Adapter<TomasHojeAdapter.LinhaViewHolder> {

    /** Dentro de quanto tempo uma toma pendente passa a mostrar "Em breve". */
    private static final long EM_BREVE_MS = 60 * 60 * 1000L; // 1 hora

    /** Avisa a activity de que o utilizador tocou numa linha da lista. */
    public interface AoTocar {
        void aoTocar(TomaDeHoje toma);
    }

    private final List<TomaDeHoje> tomas = new ArrayList<>();
    private final AoTocar escuta;

    public TomasHojeAdapter(AoTocar escuta) {
        this.escuta = escuta;
    }

    public void submeter(List<TomaDeHoje> novas) {
        tomas.clear();
        if (novas != null) {
            tomas.addAll(novas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LinhaViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View vista = LayoutInflater.from(pai.getContext())
                .inflate(R.layout.item_toma_hoje, pai, false);
        return new LinhaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull LinhaViewHolder holder, int posicao) {
        TomaDeHoje toma = tomas.get(posicao);

        holder.hora.setText(DataUtil.formatarHora(toma.registo.getDataPrevista()));
        holder.nome.setText(toma.getNomeCompleto());

        EstadoToma estado = toma.registo.getEstado();
        String texto;
        int cor;

        if (estado == EstadoToma.TOMADA) {
            texto = "✓ Tomado";
            cor = R.color.vital_green;
        } else if (estado == EstadoToma.FALHADA) {
            texto = "✗ Não tomado";
            cor = R.color.vital_red;
        } else {
            long faltam = toma.registo.getDataPrevista() - System.currentTimeMillis();
            texto = faltam <= EM_BREVE_MS ? "⏱ Em breve" : "⏱ Pendente";
            cor = R.color.vital_gray;
        }

        holder.estado.setText(texto);
        holder.estado.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), cor));

        // Uma toma já confirmada não se volta a confirmar; as outras sim,
        // para que uma toma esquecida possa ser registada mais tarde.
        boolean confirmavel = estado != EstadoToma.TOMADA;
        holder.itemView.setClickable(confirmavel);
        holder.itemView.setOnClickListener(confirmavel && escuta != null
                ? v -> escuta.aoTocar(toma)
                : null);
    }

    @Override
    public int getItemCount() {
        return tomas.size();
    }

    static class LinhaViewHolder extends RecyclerView.ViewHolder {

        final TextView hora;
        final TextView nome;
        final TextView estado;

        LinhaViewHolder(@NonNull View vista) {
            super(vista);
            hora = vista.findViewById(R.id.tvHojeHora);
            nome = vista.findViewById(R.id.tvHojeNome);
            estado = vista.findViewById(R.id.tvHojeEstado);
        }
    }
}
