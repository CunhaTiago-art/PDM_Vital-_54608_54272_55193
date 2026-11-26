package pt.ubi.di.pdm.vital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import pt.ubi.di.pdm.vital.ui.comunidade.ComunidadeViewModel;

/**
 * Comunidade: o centro da rede de apoio.
 *
 * Mostra de quem se está a cuidar, quem faz parte da rede, e os contadores
 * de cada secção — mensagens por ler e consultas marcadas — todos vindos
 * de contagens feitas pela base de dados.
 */
public class ComunidadeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comunidade);

        ComunidadeViewModel modelo = new ViewModelProvider(this).get(ComunidadeViewModel.class);

        TextView tvNomeIdoso = findViewById(R.id.tvNomeIdoso);
        TextView tvMensagensNovas = findViewById(R.id.tvMensagensNovas);
        TextView tvResumoConsultas = findViewById(R.id.tvResumoConsultas);
        TextView tvResumoSaude = findViewById(R.id.tvResumoSaude);

        modelo.getIdoso().observe(this, idoso ->
                tvNomeIdoso.setText(idoso == null ? "Sem idoso registado" : idoso.getNome()));

        modelo.getMensagensPorLer().observe(this, porLer -> {
            int n = porLer == null ? 0 : porLer;
            tvMensagensNovas.setText(n == 0
                    ? "Sem mensagens novas"
                    : n + (n == 1 ? " mensagem nova" : " mensagens novas"));
        });

        modelo.getConsultasFuturas().observe(this, futuras -> {
            int n = futuras == null ? 0 : futuras;
            tvResumoConsultas.setText(n == 0
                    ? "Nenhuma marcada"
                    : n + (n == 1 ? " próxima" : " próximas"));
        });

        modelo.getUltimoBatimento().observe(this, batimento ->
                tvResumoSaude.setText(batimento == null
                        ? "Sem registos"
                        : Math.round(batimento.getValor()) + " bpm · última medição"));

        findViewById(R.id.btnVoltarComunidade).setOnClickListener(v -> finish());

        abrir(R.id.cardHistorial, HistorialActivity.class);
        abrir(R.id.cardConsultas, ConsultasActivity.class);
        abrir(R.id.cardChat, ChatFamiliaActivity.class);
        abrir(R.id.cardDuvidas, DuvidasActivity.class);
        abrir(R.id.cardSaude, MetricasActivity.class);

        findViewById(R.id.btnConvidarMembro).setOnClickListener(v ->
                Toast.makeText(this,
                        "Partilhe o código de família para juntar alguém à conversa.",
                        Toast.LENGTH_LONG).show());
    }

    private void abrir(int idDoCartao, Class<?> destino) {
        View cartao = findViewById(idDoCartao);
        if (cartao != null) {
            cartao.setOnClickListener(v -> startActivity(new Intent(this, destino)));
        }
    }
}
