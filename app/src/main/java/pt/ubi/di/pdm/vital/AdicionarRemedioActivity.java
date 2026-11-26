package pt.ubi.di.pdm.vital;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.ubi.di.pdm.vital.data.model.Horario;
import pt.ubi.di.pdm.vital.data.model.Medicamento;
import pt.ubi.di.pdm.vital.data.model.MedicamentoComHorarios;
import pt.ubi.di.pdm.vital.ui.caixa.CaixaViewModel;
import pt.ubi.di.pdm.vital.util.DataUtil;
import pt.ubi.di.pdm.vital.util.DiasSemana;

/**
 * Formulário de medicamento, usado para criar e para editar.
 *
 * Os horários são uma lista dinâmica de chips: cada um é um minuto do dia,
 * e tocar num remove-o. Os dias da semana saem dos sete interruptores e são
 * convertidos na máscara de bits que a base de dados guarda.
 */
public class AdicionarRemedioActivity extends AppCompatActivity {

    /** Chave do Intent com o id do medicamento a editar; ausente significa novo. */
    public static final String EXTRA_MEDICAMENTO_ID = "medicamentoId";

    private CaixaViewModel modelo;

    private long medicamentoId;
    private int quantidade = 1;
    private final List<Integer> minutos = new ArrayList<>();

    private LinearLayout chips;
    private TextView tvQuantidade;
    private EditText etNome;
    private ToggleButton[] dias;
    private boolean jaCarregado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adicionar_remedio);

        modelo = new ViewModelProvider(this).get(CaixaViewModel.class);
        medicamentoId = getIntent().getLongExtra(EXTRA_MEDICAMENTO_ID, 0L);

        etNome = findViewById(R.id.etNomeMed);
        tvQuantidade = findViewById(R.id.tvQuantidadeDose);
        chips = findViewById(R.id.layoutHorariosChips);

        dias = new ToggleButton[]{
                findViewById(R.id.tbSeg), findViewById(R.id.tbTer),
                findViewById(R.id.tbQua), findViewById(R.id.tbQui),
                findViewById(R.id.tbSex), findViewById(R.id.tbSab),
                findViewById(R.id.tbDom)};

        // O chip do layout é só um exemplo de desenho; os reais são criados aqui.
        chips.removeAllViews();
        mostrarQuantidade();

        findViewById(R.id.btnMenosDose).setOnClickListener(v -> {
            if (quantidade > 1) {
                quantidade--;
                mostrarQuantidade();
            }
        });

        findViewById(R.id.btnMaisDose).setOnClickListener(v -> {
            quantidade++;
            mostrarQuantidade();
        });

        findViewById(R.id.btnAdicionarHora).setOnClickListener(v -> escolherHora());
        findViewById(R.id.btnVoltarNovo).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardarMed).setOnClickListener(v -> guardar());

        if (medicamentoId != 0) {
            carregarExistente();
        } else {
            // Um medicamento novo começa com uma hora sugerida, para o ecrã
            // não ficar vazio e sem indicação do que fazer.
            minutos.add(8 * 60);
            desenharChips();
        }
    }

    /** Edição: preenche o formulário com o que está na base de dados, uma só vez. */
    private void carregarExistente() {
        modelo.observarUm(medicamentoId).observe(this, item -> {
            if (item == null || item.medicamento == null || jaCarregado) {
                return;
            }
            jaCarregado = true;

            Medicamento med = item.medicamento;
            String dosagem = med.getDosagem();
            etNome.setText(dosagem == null || dosagem.trim().isEmpty()
                    ? med.getNome() : med.getNome() + " " + dosagem);

            quantidade = Math.max(1, med.getQuantidadePorToma());
            mostrarQuantidade();

            minutos.clear();
            if (item.horarios != null) {
                for (Horario horario : item.horarios) {
                    minutos.add(horario.getMinutosDoDia());
                }
            }
            desenharChips();

            for (int i = 0; i < dias.length; i++) {
                dias[i].setChecked((med.getDiasSemana() & (1 << i)) != 0);
            }
        });
    }

    private void mostrarQuantidade() {
        tvQuantidade.setText(quantidade + (quantidade == 1 ? " comprimido" : " comprimidos"));
    }

    private void escolherHora() {
        new TimePickerDialog(this, (picker, hora, minuto) -> {
            int valor = hora * 60 + minuto;
            if (minutos.contains(valor)) {
                Toast.makeText(this, "Essa hora já está na lista.", Toast.LENGTH_SHORT).show();
                return;
            }
            minutos.add(valor);
            desenharChips();
        }, 8, 0, true).show();
    }

    /** Recria os chips a partir da lista, sempre ordenados por hora. */
    private void desenharChips() {
        Collections.sort(minutos);
        chips.removeAllViews();

        for (Integer valor : new ArrayList<>(minutos)) {
            MaterialButton chip = new MaterialButton(this);
            chip.setText(DataUtil.formatarMinutos(valor) + "  ×");
            chip.setTextSize(20f);
            chip.setAllCaps(false);
            chip.setMinHeight((int) (60 * getResources().getDisplayMetrics().density));
            chip.setInsetTop(0);
            chip.setInsetBottom(0);
            chip.setCornerRadius((int) (12 * getResources().getDisplayMetrics().density));
            chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.vital_green)));
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                minutos.remove(valor);
                desenharChips();
            });

            chips.addView(chip);
        }
    }

    private void guardar() {
        String nome = etNome.getText().toString().trim();
        if (nome.isEmpty()) {
            etNome.setError("Indique o nome do medicamento");
            etNome.requestFocus();
            return;
        }
        if (minutos.isEmpty()) {
            Toast.makeText(this, "Acrescente pelo menos uma hora.", Toast.LENGTH_LONG).show();
            return;
        }

        int mascara = 0;
        for (int i = 0; i < dias.length; i++) {
            if (dias[i].isChecked()) {
                mascara |= (1 << i);
            }
        }
        if (mascara == 0) {
            Toast.makeText(this, "Escolha pelo menos um dia da semana.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Medicamento med = new Medicamento();
        med.setId(medicamentoId);
        separarNomeEDosagem(med, nome);
        med.setQuantidadePorToma(quantidade);
        med.setDiasSemana(mascara);
        med.setAtivo(true);

        modelo.guardar(med, new ArrayList<>(minutos));
        Toast.makeText(this, "Medicamento guardado.", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * O campo é único ("Metformina 500 mg") mas a base de dados guarda o nome
     * e a dosagem em colunas separadas, para poder procurar por nome sem
     * apanhar a dosagem pelo meio. A última palavra com um número passa a
     * dosagem; o resto é o nome.
     */
    private void separarNomeEDosagem(Medicamento med, String texto) {
        int corte = -1;
        for (int i = 0; i < texto.length(); i++) {
            if (Character.isDigit(texto.charAt(i))) {
                corte = i;
                break;
            }
        }
        if (corte > 0) {
            med.setNome(texto.substring(0, corte).trim());
            med.setDosagem(texto.substring(corte).trim());
        } else {
            med.setNome(texto);
            med.setDosagem(null);
        }
    }

    /** Converte um dia do calendário no índice usado pelos sete interruptores. */
    @SuppressWarnings("unused")
    private int indiceDe(DayOfWeek dia) {
        return DiasSemana.mascaraDe(dia);
    }
}
