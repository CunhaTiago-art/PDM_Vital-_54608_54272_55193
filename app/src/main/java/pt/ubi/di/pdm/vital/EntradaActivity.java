package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

public class EntradaActivity extends AppCompatActivity {

    private String perfilSelecionado = "Idoso";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrada);

        MaterialButton btnIdoso = findViewById(R.id.btnPerfilIdoso);
        MaterialButton btnCuidador = findViewById(R.id.btnPerfilCuidador);
        EditText etNome = findViewById(R.id.etNomeUtilizador);
        MaterialButton btnEntrar = findViewById(R.id.btnEntrarApp);

        btnIdoso.setOnClickListener(v -> {
            perfilSelecionado = "Idoso";
            atualizarEstadoBotoes(btnIdoso, btnCuidador, "✓  Idoso", "Cuidador");
        });

        btnCuidador.setOnClickListener(v -> {
            perfilSelecionado = "Cuidador";
            atualizarEstadoBotoes(btnCuidador, btnIdoso, "✓  Cuidador", "Idoso");
        });

        btnEntrar.setOnClickListener(v -> {
            String nome = etNome.getText().toString().trim();
            if (nome.isEmpty()) {
                nome = perfilSelecionado;
            }

            SharedPreferences prefs = getSharedPreferences("VitalPlusPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putString("utilizador_nome", nome)
                    .putString("utilizador_perfil", perfilSelecionado)
                    .apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void atualizarEstadoBotoes(MaterialButton ativo, MaterialButton inativo,
                                       String textoAtivo, String textoInativo) {
        int verde = ContextCompat.getColor(this, R.color.vital_green);
        int navy = ContextCompat.getColor(this, R.color.vital_navy);
        int branco = ContextCompat.getColor(this, R.color.white);

        ativo.setBackgroundTintList(ColorStateList.valueOf(verde));
        ativo.setTextColor(branco);
        ativo.setStrokeWidth(0);
        ativo.setText(textoAtivo);

        inativo.setBackgroundTintList(ColorStateList.valueOf(branco));
        inativo.setTextColor(navy);
        inativo.setStrokeColor(ColorStateList.valueOf(navy));
        inativo.setStrokeWidth(4);
        inativo.setText(textoInativo);
    }
}