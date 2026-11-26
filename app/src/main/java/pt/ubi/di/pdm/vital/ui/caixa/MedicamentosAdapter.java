package pt.ubi.di.pdm.vital.ui.caixa;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pt.ubi.di.pdm.vital.R;
import pt.ubi.di.pdm.vital.data.model.Horario;
import pt.ubi.di.pdm.vital.data.model.Medicamento;
import pt.ubi.di.pdm.vital.data.model.MedicamentoComHorarios;
import pt.ubi.di.pdm.vital.util.DataUtil;
import pt.ubi.di.pdm.vital.util.DiasSemana;

/** Lista da Caixa de Remédios: cada cartão é uma prescrição com os seus horários. */
public class MedicamentosAdapter
        extends RecyclerView.Adapter<MedicamentosAdapter.CartaoViewHolder> {

    /** Avisa a activity de que o utilizador tocou num medicamento. */
    public interface AoTocar {
        void aoTocar(MedicamentoComHorarios item);
    }

    private final List<MedicamentoComHorarios> itens = new ArrayList<>();
    private final AoTocar escuta;

    public MedicamentosAdapter(AoTocar escuta) {
        this.escuta = escuta;
    }

    public void submeter(List<MedicamentoComHorarios> novos) {
        itens.clear();
        if (novos != null) {
            itens.addAll(novos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartaoViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View vista = LayoutInflater.from(pai.getContext())
                .inflate(R.layout.item_medicamento, pai, false);
        return new CartaoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CartaoViewHolder holder, int posicao) {
        MedicamentoComHorarios item = itens.get(posicao);
        Medicamento med = item.medicamento;

        String dosagem = med.getDosagem();
        holder.nome.setText(dosagem == null || dosagem.trim().isEmpty()
                ? med.getNome()
                : med.getNome() + " " + dosagem);

        int quantidade = med.getQuantidadePorToma();
        String dose = quantidade + (quantidade == 1 ? " comprimido" : " comprimidos");
        holder.dose.setText(dose + " · " + DiasSemana.descrever(med.getDiasSemana()));

        holder.horas.setText(descreverHorarios(item));

        holder.itemView.setOnClickListener(v -> {
            if (escuta != null) {
                escuta.aoTocar(item);
            }
        });
    }

    /** "08:00" ou "08:00 · 14:00 · 21:00". */
    private String descreverHorarios(MedicamentoComHorarios item) {
        if (item.horarios == null || item.horarios.isEmpty()) {
            return "Sem horário definido";
        }
        StringBuilder sb = new StringBuilder();
        for (Horario horario : item.horarios) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(DataUtil.formatarMinutos(horario.getMinutosDoDia()));
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class CartaoViewHolder extends RecyclerView.ViewHolder {

        final TextView nome;
        final TextView dose;
        final TextView horas;

        CartaoViewHolder(@NonNull View vista) {
            super(vista);
            nome = vista.findViewById(R.id.tvItemNomeMed);
            dose = vista.findViewById(R.id.tvItemDoseMed);
            horas = vista.findViewById(R.id.tvItemHoraMed);
        }
    }
}
