package pt.ubi.di.pdm.vital;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pt.ubi.di.pdm.vital.data.model.Membro;
import pt.ubi.di.pdm.vital.ui.chat.ChatAdapter;
import pt.ubi.di.pdm.vital.ui.chat.ChatViewModel;

/**
 * Chat da família.
 *
 * Este ecrã é o exemplo completo do caminho dos dados na aplicação:
 * Activity -> ViewModel -> VitalRepository -> Room, e o Room devolve LiveData
 * que atualiza a lista sozinha. A sincronização com a nuvem acontece por trás,
 * sem este ecrã saber que ela existe.
 */
public class ChatFamiliaActivity extends AppCompatActivity {

    private ChatViewModel modelo;
    private ChatAdapter adaptador;
    private RecyclerView lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_familia);

        modelo = new ViewModelProvider(this).get(ChatViewModel.class);

        EditText etMensagem = findViewById(R.id.etMensagemChat);
        TextView tvMembros = findViewById(R.id.tvMembrosChat);
        lista = findViewById(R.id.rvChatMensagens);

        // ---------- Lista de mensagens ----------
        adaptador = new ChatAdapter();
        adaptador.setMeuId(modelo.getMeuId());
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        modelo.getMensagens().observe(this, mensagens -> {
            // O membro local pode ser criado numa thread de fundo logo a seguir
            // ao ecrã de Entrada, por isso o id é relido aqui e não só no arranque.
            adaptador.setMeuId(modelo.getMeuId());
            adaptador.submeter(mensagens);
            if (mensagens != null && !mensagens.isEmpty()) {
                // post() evita mexer na lista enquanto ela está a calcular o layout
                lista.post(() -> lista.scrollToPosition(adaptador.getItemCount() - 1));
            }
        });

        // ---------- Subtítulo com quem está na conversa ----------
        modelo.getRede().observe(this, membros -> tvMembros.setText(descreverRede(membros)));

        // ---------- Botões ----------
        findViewById(R.id.btnVoltarChat).setOnClickListener(v -> finish());

        findViewById(R.id.btnEnviarChat).setOnClickListener(v -> {
            String texto = etMensagem.getText().toString().trim();
            if (texto.isEmpty()) {
                return;
            }
            if (modelo.getMeuId() == 0) {
                Toast.makeText(this,
                        "Volte ao ecrã inicial e indique o seu nome.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            modelo.enviar(texto);
            etMensagem.setText("");
        });

        findViewById(R.id.btnVozChat).setOnClickListener(v ->
                Toast.makeText(this, "Gravação de voz (a integrar).", Toast.LENGTH_SHORT).show()
        );

        // Ao abrir o chat, as mensagens deixam de estar por ler
        modelo.marcarComoLido();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adaptador.setMeuId(modelo.getMeuId());
    }

    /** Subtítulo do cabeçalho: "Filha, Neto, Enfermeiro" ou o estado da ligação. */
    private String descreverRede(List<Membro> membros) {
        if (!modelo.temFamilia()) {
            return "Só neste telemóvel · sem código de família";
        }
        List<String> nomes = new ArrayList<>();
        if (membros != null) {
            for (Membro membro : membros) {
                nomes.add(membro.getNome());
            }
        }
        if (nomes.isEmpty()) {
            return "Família " + modelo.getCodigoFamilia();
        }
        return String.join(", ", nomes) + " · " + modelo.getCodigoFamilia();
    }
}
