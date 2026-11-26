package pt.ubi.di.pdm.vital.ui.consultas;

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
import pt.ubi.di.pdm.vital.data.model.Consulta;
import pt.ubi.di.pdm.vital.data.model.ConsultaComAcompanhante;
import pt.ubi.di.pdm.vital.data.model.EstadoConsulta;
import pt.ubi.di.pdm.vital.util.DataUtil;

/** Lista de consultas: as que estão para vir e as que já se realizaram. */
public class ConsultasAdapter extends RecyclerView.Adapter<ConsultasAdapter.CartaoViewHolder> {

    private final List<ConsultaComAcompanhante> consultas = new ArrayList<>();

    public void submeter(List<ConsultaComAcompanhante> novas) {
        consultas.clear();
        if (novas != null) {
            consultas.addAll(novas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartaoViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View vista = LayoutInflater.from(pai.getContext())
                .inflate(R.layout.item_consulta, pai, false);
        return new CartaoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CartaoViewHolder holder, int posicao) {
        ConsultaComAcompanhante item = consultas.get(posicao);
        Consulta consulta = item.consulta;

        boolean passada = consulta.getEstado() != EstadoConsulta.AGENDADA
                || consulta.getDataHora() < System.currentTimeMillis();

        holder.estado.setText(passada ? "PASSADA" : "AGENDADA");
        holder.estado.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                passada ? R.color.vital_gray : R.color.vital_green));

        holder.dataHora.setText(DataUtil.formatarDiaSemana(consulta.getDataHora())
                + " · " + DataUtil.formatarHora(consulta.getDataHora()));
        holder.tipo.setText(consulta.getEspecialidade());
        holder.detalhe.setText(descrever(item));
    }

    /** "Centro de Saúde · Acompanha: Ana" — só com o que estiver preenchido. */
    private String descrever(ConsultaComAcompanhante item) {
        StringBuilder sb = new StringBuilder();
        String local = item.consulta.getLocal();
        if (local != null && !local.trim().isEmpty()) {
            sb.append(local);
        }
        if (item.acompanhanteNome != null) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("Acompanha: ").append(item.acompanhanteNome);
        }
        return sb.length() == 0 ? "Sem detalhes" : sb.toString();
    }

    @Override
    public int getItemCount() {
        return consultas.size();
    }

    static class CartaoViewHolder extends RecyclerView.ViewHolder {

        final TextView estado;
        final TextView dataHora;
        final TextView tipo;
        final TextView detalhe;

        CartaoViewHolder(@NonNull View vista) {
            super(vista);
            estado = vista.findViewById(R.id.tvConsultaEstado);
            dataHora = vista.findViewById(R.id.tvConsultaDataHora);
            tipo = vista.findViewById(R.id.tvConsultaTipo);
            detalhe = vista.findViewById(R.id.tvConsultaDetalhe);
        }
    }
}
