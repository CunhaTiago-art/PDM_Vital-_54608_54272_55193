package pt.ubi.di.pdm.vital;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import pt.ubi.di.pdm.vital.data.model.Consulta;
import pt.ubi.di.pdm.vital.data.model.EstadoConsulta;
import pt.ubi.di.pdm.vital.data.model.Membro;
import pt.ubi.di.pdm.vital.data.model.PapelMembro;
import pt.ubi.di.pdm.vital.ui.consultas.ConsultasViewModel;
import pt.ubi.di.pdm.vital.util.DataUtil;

/** Formulário "Marcar consulta". Grava na base de dados e volta atrás. */
public class MarcarConsultaActivity extends AppCompatActivity {

    private static final int LEMBRETE_HORA = 60;
    private static final int LEMBRETE_DIA = 1440;
    private static final int LEMBRETE_SEMANA = 10080;

    private ConsultasViewModel modelo;

    private LocalDate dataEscolhida = LocalDate.now().plusDays(1);
    private LocalTime horaEscolhida = LocalTime.of(10, 0);

    private Long acompanhanteId;
    private int lembreteMinutos = LEMBRETE_DIA;

    private MaterialButton btnFilha;
    private MaterialButton btnNeto;
    private MaterialButton btnEnfermeiro;
    private MaterialButton btn1Hora;
    private MaterialButton btn1Dia;
    private MaterialButton btn1Semana;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marcar_consulta);

        modelo = new ViewModelProvider(this).get(ConsultasViewModel.class);

        EditText etEspecialidade = findViewById(R.id.etEspecialidade);
        EditText etData = findViewById(R.id.etDataConsulta);
        EditText etHora = findViewById(R.id.etHoraConsulta);
        EditText etLocal = findViewById(R.id.etLocalConsulta);

        btnFilha = findViewById(R.id.btnAcompFilha);
        btnNeto = findViewById(R.id.btnAcompNeto);
        btnEnfermeiro = findViewById(R.id.btnAcompEnfermeiro);
        btn1Hora = findViewById(R.id.btnLembrete1Hora);
        btn1Dia = findViewById(R.id.btnLembrete1Dia);
        btn1Semana = findViewById(R.id.btnLembrete1Semana);

        // ---------- Data e hora com seletores do sistema ----------
        // Os campos não aceitam escrita: evita datas inválidas e é mais fácil
        // para quem tem dificuldade em escrever no teclado.
        etData.setFocusable(false);
        etHora.setFocusable(false);
        mostrarData(etData);
        mostrarHora(etHora);

        etData.setOnClickListener(v -> new DatePickerDialog(this,
                (picker, ano, mes, dia) -> {
                    dataEscolhida = LocalDate.of(ano, mes + 1, dia);
                    mostrarData(etData);
                },
                dataEscolhida.getYear(),
                dataEscolhida.getMonthValue() - 1,
                dataEscolhida.getDayOfMonth()).show());

        etHora.setOnClickListener(v -> new TimePickerDialog(this,
                (picker, hora, minuto) -> {
                    horaEscolhida = LocalTime.of(hora, minuto);
                    mostrarHora(etHora);
                },
                horaEscolhida.getHour(), horaEscolhida.getMinute(), true).show());

        // ---------- Quem acompanha ----------
        // Os botões do wireframe são papéis, não pessoas. Aqui procura-se na
        // rede de apoio quem tem esse papel e guarda-se o id dessa pessoa.
        modelo.getRede().observe(this, rede -> {
            btnFilha.setOnClickListener(v -> escolherAcompanhante(rede, PapelMembro.FILHA, btnFilha));
            btnNeto.setOnClickListener(v -> escolherAcompanhante(rede, PapelMembro.NETO, btnNeto));
            btnEnfermeiro.setOnClickListener(v ->
                    escolherAcompanhante(rede, PapelMembro.ENFERMEIRO, btnEnfermeiro));
        });

        // ---------- Lembrete ----------
        btn1Hora.setOnClickListener(v -> escolherLembrete(LEMBRETE_HORA));
        btn1Dia.setOnClickListener(v -> escolherLembrete(LEMBRETE_DIA));
        btn1Semana.setOnClickListener(v -> escolherLembrete(LEMBRETE_SEMANA));
        pintarLembretes();

        findViewById(R.id.btnVoltarMarcarConsulta).setOnClickListener(v -> finish());

        findViewById(R.id.btnGuardarConsulta).setOnClickListener(v ->
                guardar(etEspecialidade, etLocal));
    }

    private void guardar(EditText etEspecialidade, EditText etLocal) {
        String especialidade = etEspecialidade.getText().toString().trim();
        if (especialidade.isEmpty()) {
            etEspecialidade.setError("Indique a especialidade ou o motivo");
            etEspecialidade.requestFocus();
            return;
        }

        LocalDateTime quando = LocalDateTime.of(dataEscolhida, horaEscolhida);
        if (quando.isBefore(LocalDateTime.now())) {
            Toast.makeText(this, "Essa data já passou.", Toast.LENGTH_LONG).show();
            return;
        }

        Consulta consulta = new Consulta();
        consulta.setEspecialidade(especialidade);
        consulta.setDataHora(DataUtil.paraMillis(quando));
        consulta.setLocal(etLocal.getText().toString().trim());
        consulta.setAcompanhanteId(acompanhanteId);
        consulta.setLembreteMinutos(lembreteMinutos);
        consulta.setEstado(EstadoConsulta.AGENDADA);

        modelo.guardar(consulta);
        Toast.makeText(this, "Consulta marcada.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void escolherAcompanhante(java.util.List<Membro> rede, PapelMembro papel,
                                      MaterialButton botao) {
        acompanhanteId = null;
        if (rede != null) {
            for (Membro membro : rede) {
                if (membro.getPapel() == papel) {
                    acompanhanteId = membro.getId();
                    break;
                }
            }
        }
        if (acompanhanteId == null) {
            Toast.makeText(this,
                    "Ainda não há ninguém com esse papel na rede de apoio.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        pintar(btnFilha, botao == btnFilha);
        pintar(btnNeto, botao == btnNeto);
        pintar(btnEnfermeiro, botao == btnEnfermeiro);
    }

    private void escolherLembrete(int minutos) {
        lembreteMinutos = minutos;
        pintarLembretes();
    }

    private void pintarLembretes() {
        pintar(btn1Hora, lembreteMinutos == LEMBRETE_HORA);
        pintar(btn1Dia, lembreteMinutos == LEMBRETE_DIA);
        pintar(btn1Semana, lembreteMinutos == LEMBRETE_SEMANA);
    }

    private void pintar(MaterialButton botao, boolean ativo) {
        int verde = ContextCompat.getColor(this, R.color.vital_green);
        int navy = ContextCompat.getColor(this, R.color.vital_navy);
        int branco = ContextCompat.getColor(this, R.color.white);

        botao.setBackgroundTintList(ColorStateList.valueOf(ativo ? verde : branco));
        botao.setTextColor(ativo ? branco : navy);
        botao.setStrokeColor(ColorStateList.valueOf(navy));
        botao.setStrokeWidth(ativo ? 0 : 3);
    }

    private void mostrarData(EditText campo) {
        campo.setText(dataEscolhida.format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private void mostrarHora(EditText campo) {
        campo.setText(horaEscolhida.format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
    }
}
