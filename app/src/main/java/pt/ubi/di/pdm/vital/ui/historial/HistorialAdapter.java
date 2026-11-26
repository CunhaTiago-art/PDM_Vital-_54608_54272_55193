package pt.ubi.di.pdm.vital.ui.historial;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import pt.ubi.di.pdm.vital.R;
import pt.ubi.di.pdm.vital.data.model.EventoHistorial;
import pt.ubi.di.pdm.vital.util.DataUtil;

/** Lista do Historial: tomas, consultas e notas, por ordem inversa de tempo. */
public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.LinhaViewHolder> {

    private final List<EventoHistorial> eventos = new ArrayList<>();

    public void submeter(List<EventoHistorial> novos) {
        eventos.clear();
        if (novos != null) {
            eventos.addAll(novos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LinhaViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View vista = LayoutInflater.from(pai.getContext())
                .inflate(R.layout.item_historial, pai, false);
        return new LinhaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull LinhaViewHolder holder, int posicao) {
        EventoHistorial evento = eventos.get(posicao);

        holder.dataHora.setText(descreverQuando(evento.dataHora));
        holder.titulo.setText(evento.titulo);

        String detalhe = evento.subtitulo == null ? "" : evento.subtitulo;
        int cor;

        if ("TOMA".equals(evento.tipo)) {
            if (detalhe.startsWith("Tomado")) {
                detalhe = "✓ " + detalhe;
                cor = R.color.vital_green;
            } else if (detalhe.startsWith("Não tomado")) {
                detalhe = "✗ " + detalhe;
                cor = R.color.vital_red;
            } else {
                cor = R.color.vital_gray;
            }
        } else if ("CONSULTA".equals(evento.tipo)) {
            detalhe = detalhe.isEmpty() ? "Consulta" : "Consulta · " + detalhe;
            cor = R.color.vital_navy;
        } else {
            cor = R.color.vital_gray;
        }

        holder.detalhe.setText(detalhe);
        holder.detalhe.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), cor));
    }

    /** "Hoje\n08:00", "Ontem\n21:00" ou "12 nov\n18:00". */
    private String descreverQuando(long millis) {
        LocalDate dia = DataUtil.paraDataHora(millis).toLocalDate();
        LocalDate hoje = LocalDate.now();

        String etiqueta;
        if (dia.equals(hoje)) {
            etiqueta = "Hoje";
        } else if (dia.equals(hoje.minusDays(1))) {
            etiqueta = "Ontem";
        } else if (dia.equals(hoje.plusDays(1))) {
            etiqueta = "Amanhã";
        } else {
            etiqueta = DataUtil.formatarDataCurta(millis);
        }
        return etiqueta + "\n" + DataUtil.formatarHora(millis);
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    static class LinhaViewHolder extends RecyclerView.ViewHolder {

        final TextView dataHora;
        final TextView titulo;
        final TextView detalhe;

        LinhaViewHolder(@NonNull View vista) {
            super(vista);
            dataHora = vista.findViewById(R.id.tvHistDataHora);
            titulo = vista.findViewById(R.id.tvHistTitulo);
            detalhe = vista.findViewById(R.id.tvHistDetalhe);
        }
    }
}
