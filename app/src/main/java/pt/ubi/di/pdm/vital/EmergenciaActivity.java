package pt.ubi.di.pdm.vital;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import pt.ubi.di.pdm.vital.data.model.Membro;
import pt.ubi.di.pdm.vital.ui.emergencia.EmergenciaViewModel;

/**
 * Emergência.
 *
 * Os contactos vêm da tabela de membros, ordenados pela coluna ordemEmergencia:
 * 1 é o contacto principal, 2 o segundo. Não estão escritos no código nem no
 * layout, por isso mudar o contacto principal é alterar um número na base de dados.
 *
 * A chamada é feita com ACTION_DIAL, que abre o marcador com o número já escrito
 * mas deixa a decisão final à pessoa. ACTION_CALL ligaria sozinho e exigiria uma
 * permissão perigosa — numa app para idosos, um toque acidental não deve iniciar
 * uma chamada.
 */
public class EmergenciaActivity extends AppCompatActivity {

    private String numeroPrincipal;
    private String numeroSegundo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergencia);

        EmergenciaViewModel modelo = new ViewModelProvider(this).get(EmergenciaViewModel.class);

        TextView tvNomePrincipal = findViewById(R.id.tvNomeContactoPrincipal);
        TextView tvNumeroPrincipal = findViewById(R.id.tvNumeroContactoPrincipal);
        TextView tvNomeSegundo = findViewById(R.id.tvNomeSegundoContacto);
        TextView tvNumeroSegundo = findViewById(R.id.tvNumeroSegundoContacto);
        View btnLigarPrincipal = findViewById(R.id.btnLigarPrincipal);
        View btnLigarSecundario = findViewById(R.id.btnLigarSecundario);

        modelo.getContactos().observe(this, contactos -> {
            Membro principal = contacto(contactos, 0);
            Membro segundo = contacto(contactos, 1);

            if (principal != null) {
                tvNomePrincipal.setText(principal.getNome());
                tvNumeroPrincipal.setText(texto(principal.getTelefone()));
                numeroPrincipal = principal.getTelefone();
                btnLigarPrincipal.setVisibility(View.VISIBLE);
            } else {
                tvNomePrincipal.setText("Sem contacto principal");
                tvNumeroPrincipal.setText("—");
                numeroPrincipal = null;
                btnLigarPrincipal.setVisibility(View.GONE);
            }

            if (segundo != null) {
                tvNomeSegundo.setText(segundo.getNome());
                tvNumeroSegundo.setText(texto(segundo.getTelefone()));
                numeroSegundo = segundo.getTelefone();
                btnLigarSecundario.setVisibility(View.VISIBLE);
            } else {
                tvNomeSegundo.setText("Sem segundo contacto");
                tvNumeroSegundo.setText("—");
                numeroSegundo = null;
                btnLigarSecundario.setVisibility(View.GONE);
            }
        });

        btnLigarPrincipal.setOnClickListener(v -> ligar(numeroPrincipal));
        btnLigarSecundario.setOnClickListener(v -> ligar(numeroSegundo));
        findViewById(R.id.btnLigar112).setOnClickListener(v -> ligar("112"));
        findViewById(R.id.btnVoltarEmergencia).setOnClickListener(v -> finish());
    }

    private Membro contacto(List<Membro> contactos, int indice) {
        return contactos != null && contactos.size() > indice ? contactos.get(indice) : null;
    }

    private String texto(String telefone) {
        return telefone == null || telefone.trim().isEmpty() ? "Sem número" : telefone;
    }

    private void ligar(String numero) {
        if (numero == null || numero.trim().isEmpty()) {
            Toast.makeText(this, "Este contacto não tem número.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent marcador = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + numero.replaceAll("\\s+", "")));
        try {
            startActivity(marcador);
        } catch (ActivityNotFoundException e) {
            // Mais fiável do que resolveActivity(): tenta mesmo abrir o marcador
            // e só desiste se o sistema confirmar que não há nenhum.
            Toast.makeText(this,
                    "Este aparelho não tem aplicação de telefone.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
