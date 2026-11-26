package pt.ubi.di.pdm.vital;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import pt.ubi.di.pdm.vital.data.model.CategoriaDuvida;
import pt.ubi.di.pdm.vital.data.model.DuvidaResumo;
import pt.ubi.di.pdm.vital.ui.duvidas.DuvidasAdapter;
import pt.ubi.di.pdm.vital.ui.duvidas.DuvidasViewModel;

/**
 * Dúvidas e ajuda.
 *
 * A pesquisa e os chips de categoria não filtram em Java: são passados à
 * consulta SQL, que devolve já só o que interessa e com o número de respostas
 * de cada dúvida calculado pela própria base de dados.
 */
public class DuvidasActivity extends AppCompatActivity {

    private DuvidasViewModel modelo;
    private MaterialButton btnTodas;
    private MaterialButton btnAlarmes;
    private MaterialButton btnMedicacao;
    private MaterialButton btnUsarApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duvidas);

        modelo = new ViewModelProvider(this).get(DuvidasViewModel.class);

        EditText etPesquisa = findViewById(R.id.etPesquisarDuvida);
        RecyclerView lista = findViewById(R.id.rvDuvidas);

        btnTodas = findViewById(R.id.btnCatTodas);
        btnAlarmes = findViewById(R.id.btnCatAlarmes);
        btnMedicacao = findViewById(R.id.btnCatMedicacao);
        btnUsarApp = findViewById(R.id.btnCatUsarApp);

        DuvidasAdapter adaptador = new DuvidasAdapter(this::abrirDuvida);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);
        modelo.getDuvidas().observe(this, adaptador::submeter);

        // ---------- Pesquisa ----------
        etPesquisa.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int i, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int c, int a) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                modelo.pesquisar(s.toString());
            }
        });

        // ---------- Chips de categoria ----------
        btnTodas.setOnClickListener(v -> escolher(null));
        btnAlarmes.setOnClickListener(v -> escolher(CategoriaDuvida.ALARMES));
        btnMedicacao.setOnClickListener(v -> escolher(CategoriaDuvida.MEDICACAO));
        btnUsarApp.setOnClickListener(v -> escolher(CategoriaDuvida.USAR_APP));
        pintarCategorias();

        findViewById(R.id.btnVoltarDuvidas).setOnClickListener(v -> finish());
        findViewById(R.id.btnFazerPergunta).setOnClickListener(v -> perguntar());
    }

    private void escolher(CategoriaDuvida categoria) {
        modelo.filtrarPorCategoria(categoria);
        pintarCategorias();
    }

    private void pintarCategorias() {
        CategoriaDuvida actual = modelo.getCategoria();
        pintar(btnTodas, actual == null);
        pintar(btnAlarmes, actual == CategoriaDuvida.ALARMES);
        pintar(btnMedicacao, actual == CategoriaDuvida.MEDICACAO);
        pintar(btnUsarApp, actual == CategoriaDuvida.USAR_APP);
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

    /** Tocar numa dúvida por resolver permite dá-la como resolvida. */
    private void abrirDuvida(DuvidaResumo item) {
        if (item.duvida.isResolvida()) {
            Toast.makeText(this, "Esta dúvida já está resolvida.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(item.duvida.getTitulo())
                .setMessage("Marcar esta dúvida como resolvida?")
                .setNegativeButton("Agora não", null)
                .setPositiveButton("Sim, está resolvida",
                        (d, w) -> modelo.marcarResolvida(item.duvida.getId()))
                .show();
    }

    /** Botão "+ Fazer uma pergunta". */
    private void perguntar() {
        if (!modelo.temAutor()) {
            Toast.makeText(this,
                    "Volte ao ecrã inicial e indique o seu nome.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        EditText campo = new EditText(this);
        campo.setHint("Ex.: o alarme não tocou, o que faço?");
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        campo.setTextSize(18f);

        int margem = (int) (24 * getResources().getDisplayMetrics().density);
        campo.setPadding(margem, margem, margem, margem);

        // A categoria da pergunta nova é a que estiver a ser vista no momento.
        CategoriaDuvida categoria = modelo.getCategoria() == null
                ? CategoriaDuvida.OUTRO : modelo.getCategoria();

        new AlertDialog.Builder(this)
                .setTitle("Fazer uma pergunta")
                .setMessage("Será colocada em: " + categoria.getEtiqueta())
                .setView(campo)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Perguntar", (d, w) ->
                        modelo.criarDuvida(campo.getText().toString(), categoria))
                .show();
    }
}
