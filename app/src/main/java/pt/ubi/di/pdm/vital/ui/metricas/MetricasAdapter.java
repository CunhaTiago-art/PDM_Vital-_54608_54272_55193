package pt.ubi.di.pdm.vital.ui.metricas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import pt.ubi.di.pdm.vital.R;
import pt.ubi.di.pdm.vital.data.model.MetricaSaude;
import pt.ubi.di.pdm.vital.util.DataUtil;

/** Histórico de medições, da mais recente para trás. */
public class MetricasAdapter extends RecyclerView.Adapter<MetricasAdapter.LinhaViewHolder> {

    private final List<MetricaSaude> metricas = new ArrayList<>();

    public void submeter(List<MetricaSaude> novas) {
        metricas.clear();
        if (novas != null) {
            metricas.addAll(novas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LinhaViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View vista = LayoutInflater.from(pai.getContext())
                .inflate(R.layout.item_metrica, pai, false);
        return new LinhaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull LinhaViewHolder holder, int posicao) {
        MetricaSaude metrica = metricas.get(posicao);

        holder.quando.setText(descreverQuando(metrica.getDataHora()));
        holder.tipo.setText(metrica.getTipo().getEtiqueta());
        holder.valor.setText(formatar(metrica));
    }

    /** Sem casas decimais: nenhuma destas métricas as usa na prática. */
    private String formatar(MetricaSaude metrica) {
        return Math.round(metrica.getValor()) + " " + metrica.getTipo().getUnidade();
    }

    private String descreverQuando(long millis) {
        LocalDate dia = DataUtil.paraDataHora(millis).toLocalDate();
        LocalDate hoje = LocalDate.now();

        String etiqueta;
        if (dia.equals(hoje)) {
            etiqueta = "Hoje";
        } else if (dia.equals(hoje.minusDays(1))) {
            etiqueta = "Ontem";
        } else {
            etiqueta = DataUtil.formatarDataCurta(millis);
        }
        return etiqueta + "\n" + DataUtil.formatarHora(millis);
    }

    @Override
    public int getItemCount() {
        return metricas.size();
    }

    static class LinhaViewHolder extends RecyclerView.ViewHolder {

        final TextView quando;
        final TextView tipo;
        final TextView valor;

        LinhaViewHolder(@NonNull View vista) {
            super(vista);
            quando = vista.findViewById(R.id.tvMetricaQuando);
            tipo = vista.findViewById(R.id.tvMetricaTipo);
            valor = vista.findViewById(R.id.tvMetricaValor);
        }
    }
}
