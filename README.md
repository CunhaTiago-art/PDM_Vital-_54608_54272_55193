# Vital+

Miniprojeto de PDM — Desafio Societal 1: Autonomia, Saúde e Segurança na Palma da Mão.

Aplicação Android para gestão de medicação de pessoas idosas, com uma rede de
apoio de familiares, cuidadores e profissionais de saúde.

---

## Como pôr a correr

1. `Git → Update Project`
2. `File → Sync Project with Gradle Files`
3. Run

O `google-services.json` está no repositório, por isso o Firebase funciona sem
configuração adicional. Não é um segredo: vai dentro do APK de qualquer forma,
e a segurança está nas regras do Firestore (`firestore.rules`).

Na primeira execução a base de dados é criada e preenchida com dados de
demonstração (3 medicamentos, 4 membros, consultas, chat e dúvidas), com datas
calculadas a partir do dia em que se instala.

---

## Onde ficam os dados

| | Onde | Porquê |
|---|---|---|
| Medicação, tomas, métricas, consultas, notas | **Só no telemóvel** (Room/SQLite) | São dados de saúde, categoria especial no RGPD |
| Mensagens de chat, dúvidas, respostas | **Telemóvel + Firestore** | Só funcionam se chegarem a quem está longe |

A app é *offline-first*: escreve sempre primeiro no Room e sincroniza depois.
Sem rede nada falha — fica pendente e sobe no arranque seguinte.

---

## Como ligar um ecrã à base de dados

Nunca chamar o Room nem o Firebase diretamente a partir de uma Activity.
O caminho é sempre **Activity → ViewModel → VitalRepository → Room**.

O `ChatFamiliaActivity` é o exemplo mais completo. O padrão é este:

```java
// 1. ViewModel: pede ao repositório e guarda o LiveData
public class XViewModel extends AndroidViewModel {
    private final VitalRepository repositorio;
    private final LiveData<List<Coisa>> coisas;

    public XViewModel(@NonNull Application app) {
        super(app);
        repositorio = new VitalRepository(app);
        coisas = repositorio.observarCoisas();
    }

    public LiveData<List<Coisa>> getCoisas() { return coisas; }
    public void guardar(Coisa c) { repositorio.guardar(c); }
}

// 2. Activity: observa e deixa a lista atualizar-se sozinha
modelo = new ViewModelProvider(this).get(XViewModel.class);
modelo.getCoisas().observe(this, adaptador::submeter);
```

Não é preciso recarregar nada depois de gravar. O LiveData volta a correr a
consulta sozinho quando a tabela muda.

**As escritas nunca bloqueiam a interface:** o repositório envia-as para um
executor próprio. Por isso os métodos de escrita não devolvem nada.

---

## O que o repositório oferece

Métodos principais de `VitalRepository`, por ecrã.

### Dashboard
- `observarProximaToma()` — a próxima toma por confirmar
- `observarTomasDeHoje()` — lista "Hoje", já com nome e dosagem
- `marcarComoTomada(registoId)` — regista a confirmação e o atraso, se houver
- `estaNaHora(dataPrevista)` — se a toma já pode ser confirmada

### Caixa de Remédios
- `observarMedicamentos()` — prescrições com os horários de cada uma
- `observarMedicamento(id)` — uma só, para o formulário de edição
- `guardarMedicamento(med, horasEmMinutos)` — grava e gera logo as tomas
- `desativarMedicamento(id)` — suspende sem apagar o historial

### Historial
- `observarHistorial(filtro)` — tomas, consultas e notas numa só lista
  (`"TUDO"`, `"TOMA"`, `"CONSULTA"`, `"NOTA"`)
- `observarSemana(deslocamento)` — contagens por dia; 0 = esta semana, -1 = a passada
- `contarTomadasDaSemana()` / `contarTomasDaSemana()`
- `adicionarNota(autorId, texto)`

### Consultas
- `observarProximaConsultaDetalhada()` — já com o nome do acompanhante
- `observarConsultasFuturasDetalhadas()` / `observarConsultasPassadasDetalhadas()`
- `guardarConsulta(consulta)`

### Saúde
- `observarUltimaMetrica(tipo)` — batimento e oxigénio
- `observarTotalDeHoje(tipo)` — passos e água, somados pelo SQL
- `registarMetrica(tipo, valor, origem)`

### Comunidade, chat e dúvidas
- `observarRedeDeApoio()` / `observarIdoso()`
- `observarConversa()` / `enviarMensagem(...)` / `contarMensagensPorLer(meuId)`
- `observarDuvidas(categoria, pesquisa)` — `categoria` a null mostra todas
- `criarDuvida(...)` / `responder(...)` / `marcarDuvidaResolvida(id)`

### Emergência
- `observarContactosEmergencia()` — ordenados por `ordemEmergencia`
- `obterContactoPrincipal(resultado)` — leitura pontual, com callback

---

## Para quem vai fazer os alarmes

As tomas **já estão todas geradas** na tabela `registo_tomas`, com data prevista
e estado. Não é preciso calcular horários a partir das prescrições: o
`GeradorDeTomas` corre no arranque da app e cria as tomas dos próximos 14 dias,
respeitando os dias da semana de cada medicamento. Correr duas vezes não duplica.

O que interessa:

```java
// Próxima toma a agendar (síncrono, chamar fora da thread principal)
RegistoToma proxima = bd.registoTomaDao().obterProximaPendente(System.currentTimeMillis());

// Abrir o ecrã de alarme para uma toma concreta
Intent i = new Intent(contexto, AlertaAlarmeActivity.class);
i.putExtra(AlertaAlarmeActivity.EXTRA_REGISTO_ID, proxima.getId());

// Botão SOS: contacto principal, leitura pontual
repositorio.obterContactoPrincipal(membro -> { /* ligar */ });

// Lembretes de consultas (minutos de antecedência em lembreteMinutos)
List<Consulta> futuras = bd.consultaDao().listarFuturas(System.currentTimeMillis());
```

O `AlertaAlarmeActivity` já aceita o `EXTRA_REGISTO_ID` e já confirma ou adia a
toma na base de dados. Sem esse extra, usa a próxima toma pendente.

---

## Testes

```
app/src/test/        19 testes — lógica pura, correm sem emulador
app/src/androidTest/ 22 testes — base de dados em memória, precisam de emulador
```

Os testes não tocam na base de dados real nem dependem dos dados de
demonstração: cada um constrói o que precisa.

---

## Estrutura

```
data/
  model/        entidades e enums
  dao/          consultas (7 DAOs)
  remote/       sincronização com o Firestore
  AppDatabase   base de dados e migrações
  VitalRepository  ponto único de acesso
  GeradorDeTomas   prescrições -> tomas concretas
  SeedData         dados de demonstração
  SessionManager   quem está a usar o telemóvel
ui/<ecrã>/      ViewModel e Adapter de cada ecrã
util/           datas e dias da semana
```
