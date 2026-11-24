package pt.ubi.di.pdm.vital.data.remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.ubi.di.pdm.vital.data.AppDatabase;
import pt.ubi.di.pdm.vital.data.SessionManager;
import pt.ubi.di.pdm.vital.data.VitalRepository;
import pt.ubi.di.pdm.vital.data.dao.ChatDao;
import pt.ubi.di.pdm.vital.data.dao.ComunidadeDao;
import pt.ubi.di.pdm.vital.data.dao.MembroDao;
import pt.ubi.di.pdm.vital.data.model.CategoriaDuvida;
import pt.ubi.di.pdm.vital.data.model.Duvida;
import pt.ubi.di.pdm.vital.data.model.MensagemChat;
import pt.ubi.di.pdm.vital.data.model.Membro;
import pt.ubi.di.pdm.vital.data.model.PapelMembro;
import pt.ubi.di.pdm.vital.data.model.Resposta;
import pt.ubi.di.pdm.vital.data.model.TipoMensagem;

/**
 * Sincroniza com a nuvem apenas o que é conversa: mensagens de chat, dúvidas e respostas.
 *
 * Os dados clínicos — medicação, tomas, métricas de saúde, consultas e notas —
 * nunca passam por aqui. Ficam no Room, no telemóvel.
 *
 * O desenho é offline-first: a app escreve sempre primeiro na base de dados local,
 * o ecrã atualiza-se logo a partir do Room, e esta classe trata de empurrar o que
 * está pendente para o Firestore e de trazer o que os outros escreveram. Se não
 * houver rede, nada falha — fica por sincronizar e sobe da próxima vez.
 *
 * Estrutura no Firestore:
 *   familias/{codigo}/mensagens/{id}
 *   familias/{codigo}/duvidas/{id}
 *   familias/{codigo}/respostas/{id}
 */
public class SincronizacaoFirebase {

    private static final String TAG = "SincronizacaoFirebase";

    private static final String COL_FAMILIAS = "familias";
    private static final String COL_MENSAGENS = "mensagens";
    private static final String COL_DUVIDAS = "duvidas";
    private static final String COL_RESPOSTAS = "respostas";

    private final AppDatabase bd;
    private final VitalRepository repositorio;
    private final SessionManager sessao;
    private final ChatDao chatDao;
    private final ComunidadeDao comunidadeDao;
    private final MembroDao membroDao;

    private final FirebaseFirestore nuvem;
    private final FirebaseAuth autenticacao;
    private final List<ListenerRegistration> escutas = new ArrayList<>();

    private boolean ativa;

    public SincronizacaoFirebase(Context context) {
        bd = AppDatabase.getInstance(context);
        repositorio = new VitalRepository(context);
        sessao = new SessionManager(context);
        chatDao = bd.chatDao();
        comunidadeDao = bd.comunidadeDao();
        membroDao = bd.membroDao();
        nuvem = FirebaseFirestore.getInstance();
        autenticacao = FirebaseAuth.getInstance();
    }

    // ==================================================================
    //  Arranque e paragem
    // ==================================================================

    /**
     * Entra na conversa da família. Sem código de família não há nada a sincronizar,
     * e a app continua a funcionar só com a base de dados local.
     */
    public void iniciar() {
        if (ativa) {
            return;
        }
        if (!sessao.temFamilia()) {
            Log.d(TAG, "Sem código de família: a app fica só em modo local.");
            return;
        }

        FirebaseUser utilizador = autenticacao.getCurrentUser();
        if (utilizador != null) {
            arrancar(utilizador.getUid());
        } else {
            autenticacao.signInAnonymously()
                    .addOnSuccessListener(resultado -> {
                        if (resultado.getUser() != null) {
                            arrancar(resultado.getUser().getUid());
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Falha na autenticação anónima", e));
        }
    }

    /** Larga as escutas. Chamar quando a app vai para segundo plano, se for preciso. */
    public void parar() {
        for (ListenerRegistration escuta : escutas) {
            escuta.remove();
        }
        escutas.clear();
        ativa = false;
    }

    /** Define o código de família e arranca a sincronização de seguida. */
    public void entrarNaFamilia(String codigo) {
        parar();
        sessao.setCodigoFamilia(codigo);
        iniciar();
    }

    private void arrancar(String uid) {
        ativa = true;
        sessao.setUidRemoto(uid);

        AppDatabase.EXECUTOR.execute(() -> {
            try {
                // Cola o uid ao membro local que já existe, em vez de criar outro.
                long meuId = sessao.getMembroId();
                if (meuId == 0) {
                    meuId = repositorio.garantirMembroLocal();
                    sessao.setMembroId(meuId);
                }
                Membro eu = membroDao.obterPorId(meuId);
                if (eu != null && eu.getUidRemoto() == null) {
                    membroDao.definirUid(meuId, uid);
                }
            } catch (Exception e) {
                Log.e(TAG, "Falha a ligar a identidade local à da nuvem", e);
            }
        });

        escutarMensagens();
        escutarDuvidas();
        escutarRespostas();
        enviarPendentes();
    }

    private CollectionReference colecao(String nome) {
        return nuvem.collection(COL_FAMILIAS)
                .document(sessao.getCodigoFamilia())
                .collection(nome);
    }

    // ==================================================================
    //  Descarregar: o que os outros escreveram
    // ==================================================================

    private void escutarMensagens() {
        escutas.add(colecao(COL_MENSAGENS)
                .orderBy("dataHora", Query.Direction.ASCENDING)
                .addSnapshotListener((instantaneo, erro) -> {
                    if (erro != null || instantaneo == null) {
                        Log.e(TAG, "Falha a escutar as mensagens", erro);
                        return;
                    }
                    for (DocumentChange alteracao : instantaneo.getDocumentChanges()) {
                        if (alteracao.getType() == DocumentChange.Type.ADDED) {
                            guardarMensagem(alteracao.getDocument());
                        }
                    }
                }));
    }

    private void guardarMensagem(DocumentSnapshot doc) {
        AppDatabase.EXECUTOR.execute(() -> {
            try {
                // Já cá está? Acontece sempre com as mensagens que nós próprios enviámos:
                // o Firestore devolve-nos o documento que acabámos de criar.
                if (chatDao.obterPorIdRemoto(doc.getId()) != null) {
                    return;
                }
                String uidAutor = doc.getString("autorUid");
                if (uidAutor == null) {
                    return;
                }
                long autorId = repositorio.garantirMembro(uidAutor,
                        doc.getString("autorNome"), lerPapel(doc.getString("autorPapel")));

                MensagemChat mensagem = new MensagemChat();
                mensagem.setAutorId(autorId);
                mensagem.setTexto(doc.getString("texto"));
                mensagem.setDataHora(lerData(doc, "dataHora"));
                mensagem.setTipo("VOZ".equals(doc.getString("tipo"))
                        ? TipoMensagem.VOZ : TipoMensagem.TEXTO);
                mensagem.setLida(uidAutor.equals(sessao.getUidRemoto()));
                mensagem.setIdRemoto(doc.getId());
                mensagem.setSincronizado(true);
                chatDao.inserir(mensagem);
            } catch (Exception e) {
                Log.e(TAG, "Falha ao guardar a mensagem recebida", e);
            }
        });
    }

    private void escutarDuvidas() {
        escutas.add(colecao(COL_DUVIDAS)
                .orderBy("dataHora", Query.Direction.ASCENDING)
                .addSnapshotListener((instantaneo, erro) -> {
                    if (erro != null || instantaneo == null) {
                        Log.e(TAG, "Falha a escutar as dúvidas", erro);
                        return;
                    }
                    for (DocumentChange alteracao : instantaneo.getDocumentChanges()) {
                        if (alteracao.getType() != DocumentChange.Type.REMOVED) {
                            guardarDuvida(alteracao.getDocument());
                        }
                    }
                }));
    }

    private void guardarDuvida(DocumentSnapshot doc) {
        AppDatabase.EXECUTOR.execute(() -> {
            try {
                Duvida existente = comunidadeDao.obterDuvidaPorIdRemoto(doc.getId());
                boolean resolvida = Boolean.TRUE.equals(doc.getBoolean("resolvida"));
                if (existente != null) {
                    // A única coisa que muda numa dúvida depois de criada
                    if (existente.isResolvida() != resolvida) {
                        existente.setResolvida(resolvida);
                        comunidadeDao.atualizarDuvida(existente);
                    }
                    return;
                }
                String uidAutor = doc.getString("autorUid");
                if (uidAutor == null) {
                    return;
                }
                long autorId = repositorio.garantirMembro(uidAutor,
                        doc.getString("autorNome"), lerPapel(doc.getString("autorPapel")));

                Duvida duvida = new Duvida();
                duvida.setAutorId(autorId);
                duvida.setTitulo(doc.getString("titulo"));
                duvida.setDescricao(doc.getString("descricao"));
                duvida.setCategoria(lerCategoria(doc.getString("categoria")));
                duvida.setDataHora(lerData(doc, "dataHora"));
                duvida.setResolvida(resolvida);
                duvida.setIdRemoto(doc.getId());
                duvida.setSincronizado(true);
                comunidadeDao.inserirDuvida(duvida);
            } catch (Exception e) {
                Log.e(TAG, "Falha ao guardar a dúvida recebida", e);
            }
        });
    }

    private void escutarRespostas() {
        escutas.add(colecao(COL_RESPOSTAS)
                .orderBy("dataHora", Query.Direction.ASCENDING)
                .addSnapshotListener((instantaneo, erro) -> {
                    if (erro != null || instantaneo == null) {
                        Log.e(TAG, "Falha a escutar as respostas", erro);
                        return;
                    }
                    for (DocumentChange alteracao : instantaneo.getDocumentChanges()) {
                        if (alteracao.getType() == DocumentChange.Type.ADDED) {
                            guardarResposta(alteracao.getDocument());
                        }
                    }
                }));
    }

    private void guardarResposta(DocumentSnapshot doc) {
        AppDatabase.EXECUTOR.execute(() -> {
            try {
                if (comunidadeDao.obterRespostaPorIdRemoto(doc.getId()) != null) {
                    return;
                }
                String uidAutor = doc.getString("autorUid");
                String duvidaRemota = doc.getString("duvidaRemotaId");
                if (uidAutor == null || duvidaRemota == null) {
                    return;
                }
                // A resposta pode chegar antes da dúvida a que pertence.
                // Nesse caso deixamos passar: volta a ser entregue no próximo arranque.
                Duvida duvida = comunidadeDao.obterDuvidaPorIdRemoto(duvidaRemota);
                if (duvida == null) {
                    return;
                }
                long autorId = repositorio.garantirMembro(uidAutor,
                        doc.getString("autorNome"), lerPapel(doc.getString("autorPapel")));

                Resposta resposta = new Resposta();
                resposta.setDuvidaId(duvida.getId());
                resposta.setAutorId(autorId);
                resposta.setTexto(doc.getString("texto"));
                resposta.setDataHora(lerData(doc, "dataHora"));
                resposta.setIdRemoto(doc.getId());
                resposta.setSincronizado(true);
                comunidadeDao.inserirResposta(resposta);
            } catch (Exception e) {
                Log.e(TAG, "Falha ao guardar a resposta recebida", e);
            }
        });
    }

    // ==================================================================
    //  Carregar: o que foi escrito neste telemóvel
    // ==================================================================

    /** Envia para a nuvem tudo o que ficou por sincronizar, por exemplo por falta de rede. */
    public void enviarPendentes() {
        if (!ativa) {
            return;
        }
        AppDatabase.EXECUTOR.execute(() -> {
            try {
                for (MensagemChat mensagem : chatDao.listarPorSincronizar()) {
                    enviarMensagem(mensagem);
                }
                for (Duvida duvida : comunidadeDao.listarDuvidasPorSincronizar()) {
                    enviarDuvida(duvida);
                }
                for (Resposta resposta : comunidadeDao.listarRespostasPorSincronizar()) {
                    enviarResposta(resposta);
                }
            } catch (Exception e) {
                Log.e(TAG, "Falha ao enviar o que estava pendente", e);
            }
        });
    }

    private void enviarMensagem(MensagemChat mensagem) {
        String docId = mensagem.getIdRemoto();
        if (docId == null) {
            return; // sem identificador global não há envio seguro
        }
        Map<String, Object> dados = baseDoAutor(mensagem.getAutorId());
        dados.put("texto", mensagem.getTexto());
        dados.put("dataHora", mensagem.getDataHora());
        dados.put("tipo", mensagem.getTipo() == null ? "TEXTO" : mensagem.getTipo().name());

        // document(id).set() em vez de add(): o identificador é nosso, por isso
        // reenviar a mesma mensagem escreve por cima em vez de criar outra.
        colecao(COL_MENSAGENS).document(docId).set(dados)
                .addOnSuccessListener(ignorado -> comExecutor("marcar a mensagem como enviada",
                        () -> chatDao.marcarSincronizada(mensagem.getId(), docId)))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Falha ao enviar a mensagem " + mensagem.getId(), e));
    }

    private void enviarDuvida(Duvida duvida) {
        String docId = duvida.getIdRemoto();
        if (docId == null) {
            return;
        }
        Map<String, Object> dados = baseDoAutor(duvida.getAutorId());
        dados.put("titulo", duvida.getTitulo());
        dados.put("descricao", duvida.getDescricao());
        dados.put("categoria", duvida.getCategoria() == null
                ? CategoriaDuvida.OUTRO.name() : duvida.getCategoria().name());
        dados.put("dataHora", duvida.getDataHora());
        dados.put("resolvida", duvida.isResolvida());

        colecao(COL_DUVIDAS).document(docId).set(dados)
                .addOnSuccessListener(ignorado -> comExecutor("marcar a dúvida como enviada",
                        () -> comunidadeDao.marcarDuvidaSincronizada(duvida.getId(), docId)))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Falha ao enviar a dúvida " + duvida.getId(), e));
    }

    private void enviarResposta(Resposta resposta) {
        String docId = resposta.getIdRemoto();
        if (docId == null) {
            return;
        }
        // Uma resposta só pode subir depois de a dúvida a que pertence ter identificador.
        String duvidaRemota = idRemotoDaDuvida(resposta.getDuvidaId());
        if (duvidaRemota == null) {
            return; // a dúvida ainda não subiu; tenta-se outra vez no próximo arranque
        }
        Map<String, Object> dados = baseDoAutor(resposta.getAutorId());
        dados.put("duvidaRemotaId", duvidaRemota);
        dados.put("texto", resposta.getTexto());
        dados.put("dataHora", resposta.getDataHora());

        colecao(COL_RESPOSTAS).document(docId).set(dados)
                .addOnSuccessListener(ignorado -> comExecutor("marcar a resposta como enviada",
                        () -> comunidadeDao.marcarRespostaSincronizada(resposta.getId(), docId)))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Falha ao enviar a resposta " + resposta.getId(), e));
    }

    // ==================================================================
    //  Auxiliares
    // ==================================================================

    /** Campos de autor comuns a tudo o que sobe: quem escreveu, como se chama e que papel tem. */
    private Map<String, Object> baseDoAutor(long autorId) {
        Map<String, Object> dados = new HashMap<>();
        Membro autor = membroDao.obterPorId(autorId);
        String uid = autor != null && autor.getUidRemoto() != null
                ? autor.getUidRemoto() : sessao.getUidRemoto();
        dados.put("autorUid", uid);
        dados.put("autorNome", autor != null ? autor.getNome() : sessao.getNome());
        dados.put("autorPapel", autor != null && autor.getPapel() != null
                ? autor.getPapel().name() : PapelMembro.CUIDADOR.name());
        return dados;
    }

    private String idRemotoDaDuvida(long duvidaIdLocal) {
        Duvida duvida = comunidadeDao.obterDuvidaPorId(duvidaIdLocal);
        return duvida == null ? null : duvida.getIdRemoto();
    }

    /**
     * Corre uma escrita no executor sem deixar a app fechar se falhar.
     * Os callbacks do Firestore chegam na thread principal; sem isto, uma exceção
     * dentro do executor derrubava a aplicação inteira.
     */
    private static void comExecutor(String operacao, Runnable tarefa) {
        AppDatabase.EXECUTOR.execute(() -> {
            try {
                tarefa.run();
            } catch (Exception e) {
                Log.e(TAG, "Falha ao " + operacao, e);
            }
        });
    }

    private static long lerData(@NonNull DocumentSnapshot doc, String campo) {
        Long valor = doc.getLong(campo);
        return valor == null ? System.currentTimeMillis() : valor;
    }

    private static PapelMembro lerPapel(String valor) {
        if (valor == null) {
            return PapelMembro.CUIDADOR;
        }
        try {
            return PapelMembro.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return PapelMembro.CUIDADOR;
        }
    }

    private static CategoriaDuvida lerCategoria(String valor) {
        if (valor == null) {
            return CategoriaDuvida.OUTRO;
        }
        try {
            return CategoriaDuvida.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return CategoriaDuvida.OUTRO;
        }
    }
}
