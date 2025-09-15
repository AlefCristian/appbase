package br.com.ideabit.frotaapi

import br.com.ideabit.frotaapi.util.TimeVisualTransformation
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.ideabit.frotaapi.api.Endpoint
import br.com.ideabit.frotaapi.model.UserModel
import br.com.ideabit.frotaapi.ui.theme.FrotaApiTheme
import br.com.ideabit.frotaapi.util.NetworkUtils
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import br.com.ideabit.frotaapi.util.AppPreferences
import br.com.ideabit.frotaapi.util.SaidaDTO
import br.com.ideabit.frotaapi.util.SaidaPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var showLoginDialog = mutableStateOf(false)
    interface SaidaSync {
        suspend fun start(saidas: List<SaidaDTO>, saidaPrefs: SaidaPreferences)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val saidaSync = object : SaidaSync {
            override suspend fun start(saidas: List<SaidaDTO>, saidaPrefs : SaidaPreferences) {
                syncSaidas(saidas, saidaPrefs)
            }
        }
        // Inicializa o singleton
        AppPreferences.init(this)

        setContent {
            FrotaApiTheme {
                TelaPrincipal(saidaSync, showLoginDialog)
            }
        }
    }
    private fun getEndPoint() : Endpoint {
        val retrofitClient = NetworkUtils.getRetrofitInstance("http://172.16.1.75:8080/api/")
        val endpoint = retrofitClient.create(Endpoint::class.java)
        return endpoint
    }
    private suspend fun getLogin(): String {
        val usuario = AppPreferences.userPrefs.userFlow.first()
        val senha = AppPreferences.userPrefs.passwordFlow.first()

        val response = getEndPoint().auth(
            UserModel(usuario, "example@email.com", senha)
        )

        if (response.isSuccessful) {
            val json = response.body()
            val token = json?.get("token")?.asString
            if (token != null) {
                println("Token retornado do getLogin: $token")
                return token
            } else {
                Toast.makeText(this.applicationContext, "Erro ao realizar login", Toast.LENGTH_SHORT).show()
                throw Exception("Token nulo na resposta")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            println("Erro de Login")
            if(response.code() == 401) {
                runOnUiThread {
                    showLoginDialog.value = true
                }
            }
            throw Exception("Erro de login: ${response.code()} - $errorBody")
        }
    }
    suspend fun syncSaidas(saidas: List<SaidaDTO>, saidaPrefs : SaidaPreferences) {
        var rawBody : String? = "nada"
        try {
            val token = getLogin()
            for (saida in saidas) {
                if (saida.completa) {
                    val res = getEndPoint().setSaida("Bearer $token", saida)
                    rawBody = res.body()?.toString()
                    println(rawBody)
                    if (res.isSuccessful) {
                        saida.sincronizada = true
                        println("${saida.id} cadastrado com sucesso")
                    } else {
                        println("Erro ao cadastrar saida id ${saida.id}: ${res.code()} - ${res.errorBody()?.string()}")
                    }
                }
                delay(1000)
            }

            saidaPrefs.saveSaidas(saidas.filter { !it.sincronizada })

        } catch (e: Exception) {
            println("Falha na sincronização: ${e.message}")
            println(rawBody)
        }
        saidaPrefs.removeSincronizados()
    }
}
@SuppressLint("ContextCastToActivity")
@Composable
fun TelaPrincipal(saidaSync: MainActivity.SaidaSync,  showLoginDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val saidaPrefs = remember { SaidaPreferences(context.applicationContext) }

//    var showLoginDialog by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var showSaidaDialog by remember { mutableStateOf(false) }
    var showRetornoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val usuario = AppPreferences.userPrefs.userFlow.first()
        val senha = AppPreferences.userPrefs.passwordFlow.first()
        showLoginDialog.value = usuario.isEmpty() || senha.isEmpty()
    }

    val saidas by saidaPrefs.saidasFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var saidaSelecionada by remember { mutableStateOf<SaidaDTO?>(null) }

    if (showDialog) {
        ModalCadastrarSaida(
            saidaExistente = saidaSelecionada,
            onDismiss = { showDialog = false },
            onConfirm = { saida ->
                scope.launch {
                    if (saida.id != null) {
                        println("Atualizando saida")
                        saidaPrefs.updateSaida(saida) // edição
                    } else {
                        println("Cadastrando saida")
                        saidaPrefs.addSaida(saida) // novo
                    }
                }
                showDialog = false
                saidaSelecionada = null
            }
        )
    }

    if (showSaidaDialog) {
        ModalSaida(
            saidaExistente = saidaSelecionada,
            onDismiss = { showSaidaDialog = false },
            onConfirm = { saida ->
                scope.launch {
                    if (saida.id != null) {
                        println("Atualizando saida")
                        //saidaPrefs.updateSaida(saida) // edição
                    } else {
                        println("Cadastrando saida")
                        saidaPrefs.addSaida(saida) // novo
                    }
                }
                showSaidaDialog = false
                saidaSelecionada = null
            }
        )
    }

    if (showRetornoDialog) {
        ModalRetorno (
            saidaExistente = saidaSelecionada,
            onDismiss = { showRetornoDialog = false },
            onConfirm = { saida ->
                scope.launch {
                    if (saida.id != null) {
                        println("Atualizando saida")
                        saidaPrefs.updateSaida(saida) // edição
                    } else {
                        println("Cadastrando saida")
                        //saidaPrefs.addSaida(saida) // novo
                    }
                }
                showRetornoDialog = false
                saidaSelecionada = null
            }
        )
    }

    if (showLoginDialog.value) {
        LoginDialog(onDismiss = { showLoginDialog.value = false })
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val incompleta = saidas.find { !it.completa }
                if (incompleta != null) {
                    println("Editar saída de ${incompleta.nome_motorista}")
                    saidaSelecionada = incompleta
                    showRetornoDialog = true
                } else {
                    println("Criar nova saída")
                    saidaSelecionada = null
                    showSaidaDialog = true
                }
                //showDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar ou Editar Saída")
            }
        },
        bottomBar = {
            Button(
                onClick = {
                    val pendentes = saidas.filter { !it.sincronizada }
                    Toast.makeText(context,
                        if (pendentes.isNotEmpty())
                            "Sincronizando ${pendentes.size} saídas..."
                        else
                            "Nenhuma saída pendente",
                        Toast.LENGTH_SHORT
                    ).show()

                    CoroutineScope(Dispatchers.IO).launch {
                        saidaSync.start(pendentes, saidaPrefs)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Sincronizar")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (saidas.isEmpty()) {
                item {
                    Text(
                        text = "Não há dados a serem sincronizados.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(saidas) { saida ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        // Listra azul à esquerda
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight()
                                .padding(end = 8.dp)
                                .background(color = androidx.compose.ui.graphics.Color(0xFF2196F3)) // azul
                        )

                        // Card com informações
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Motorista: ${saida.nome_motorista}")
                                Text("Horário saída: ${saida.horario_saida}")
                                Text("Km saída: ${saida.km_saida}")
                                Text("Retorno: ${saida.horario_retorno}")
                                Text("Km retorno: ${saida.km_retorno}")
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun LoginDialog(
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    // Coletar os valores salvos do DataStore
    val savedUser by AppPreferences.userPrefs.userFlow.collectAsState(initial = "")
    val savedPass by AppPreferences.userPrefs.passwordFlow.collectAsState(initial = "")

    // Estados para edição
    var usuario by remember { mutableStateOf(savedUser) }
    var senha by remember { mutableStateOf(savedPass) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        AppPreferences.userPrefs.saveCredentials(usuario, senha)
                        // Debug
                        val testeUser = AppPreferences.userPrefs.userFlow.first()
                        println("Salvo: $testeUser")
                    }
                    onDismiss()
                }
            ) {
                Text("Login")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Login") },
        text = {
            Column {
                OutlinedTextField(
                    value = usuario,
                    onValueChange = { usuario = it },
                    label = { Text("Usuário") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        }
    )
}
@Composable
fun ModalCadastrarSaida(
    saidaExistente: SaidaDTO? = null,
    onDismiss: () -> Unit,
    onConfirm: (SaidaDTO) -> Unit
) {
    var nome by remember { mutableStateOf(saidaExistente?.nome_motorista ?: "") }
    var destino by remember { mutableStateOf(saidaExistente?.destino ?: "") }
    var horarioSaida by remember { mutableStateOf(saidaExistente?.horario_saida ?: "") }
    var odometroSaida by remember { mutableStateOf(saidaExistente?.km_saida?.toString() ?: "") }
    var horarioRetorno by remember { mutableStateOf(saidaExistente?.horario_retorno ?: "") }
    var odometroRetorno by remember { mutableStateOf(saidaExistente?.km_retorno?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val completa = nome.isNotBlank() && horarioSaida.isNotBlank() && (odometroSaida.toIntOrNull() ?: 0) > 0
                val saida = SaidaDTO(
                    id = saidaExistente?.id, // mantém o id null
                    nome_motorista = nome,
                    destino = destino,
                    horario_saida = horarioSaida,
                    km_saida = odometroSaida.toIntOrNull() ?: 0,
                    horario_retorno = horarioRetorno.ifBlank { null },
                    km_retorno = odometroRetorno.toIntOrNull(),
                    sincronizada = saidaExistente?.sincronizada ?: false,
                    completa = completa
                )
                onConfirm(saida)
                onDismiss()
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text(if (saidaExistente != null) "Editar Saída" else "Nova Saída") },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Motorista") }
                )
                OutlinedTextField(
                    value = horarioSaida,
                    onValueChange = {
                        // permite apenas dígitos
                        horarioSaida = it.filter { char -> char.isDigit() }.take(4)
                    },
                    label = { Text("Horário de Saída") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    visualTransformation = TimeVisualTransformation()
                )
                OutlinedTextField(
                    value = odometroSaida,
                    onValueChange = { odometroSaida = it },
                    label = { Text("Odômetro Saída") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = horarioRetorno,
                    onValueChange = {
                        // permite apenas dígitos
                        horarioRetorno = it.filter { char -> char.isDigit() }.take(4)
                    },
                    label = { Text("Horário de Retorno") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    visualTransformation = TimeVisualTransformation()
                )
                OutlinedTextField(
                    value = odometroRetorno,
                    onValueChange = { odometroRetorno = it },
                    label = { Text("Odômetro Retorno") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )

//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Botão para tirar foto do odômetro
//                Button(onClick = { launcher.launch() }) {
//                    Text("Tirar foto do odômetro")
//                }
            }
        }
    )
}

@Composable
fun ModalSaida(
    saidaExistente: SaidaDTO? = null,
    onDismiss: () -> Unit,
    onConfirm: (SaidaDTO) -> Unit
) {
    var nome by remember { mutableStateOf(saidaExistente?.nome_motorista ?: "") }
    var destino by remember { mutableStateOf(saidaExistente?.destino ?: "") }

    // data e hora armazenados SEM máscara: só números
    var data by remember { mutableStateOf("") }
    var horarioSaida by remember { mutableStateOf("") }
    var odometroSaida by remember { mutableStateOf(saidaExistente?.km_saida?.toString() ?: "") }

    val formatterDay = remember { SimpleDateFormat("ddMMyyyy", Locale.getDefault()) }
    val formatterHours = remember { SimpleDateFormat("HHmm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        if (saidaExistente == null) {
            val now = Date()
            data = formatterDay.format(now)      // exemplo: "12092025"
            horarioSaida = formatterHours.format(now)  // exemplo: "1445"
        } else {
            // já existente, preencher com valor limpo (sem máscara)
            data = saidaExistente.data_saida?.filter { it.isDigit() } ?: ""
            horarioSaida = saidaExistente.horario_saida?.filter { it.isDigit() } ?: ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                // Salvar no formato com máscara, montando a string final com os caracteres
                val dataFormatada = if (data.length == 8)
                    "${data.substring(0, 2)}-${data.substring(2, 4)}-${data.substring(4, 8)}"
                else data

                val horaFormatada = if (horarioSaida.length == 4)
                    "${horarioSaida.substring(0, 2)}:${horarioSaida.substring(2, 4)}"
                else horarioSaida

                val saida = SaidaDTO(
                    id = saidaExistente?.id,
                    nome_motorista = nome,
                    destino = destino,
                    data_saida = dataFormatada,
                    horario_saida = horaFormatada,
                    km_saida = odometroSaida.toIntOrNull() ?: 0,
                    sincronizada = false,
                    completa = false
                )
                onConfirm(saida)
                onDismiss()
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Nova Saída") },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Motorista") }
                )

                OutlinedTextField(
                    value = destino,
                    onValueChange = { destino = it },
                    label = { Text("Destino da viagem") }
                )

                OutlinedTextField(
                    value = data,
                    onValueChange = { new ->
                        data = new.filter { it.isDigit() }.take(8)
                    },
                    label = { Text("Data") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    visualTransformation = VisualTransformationUtil.dataMask()
                )

                OutlinedTextField(
                    value = horarioSaida,
                    onValueChange = { new ->
                        horarioSaida = new.filter { it.isDigit() }.take(4)
                    },
                    label = { Text("Horário de Saída") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    visualTransformation = VisualTransformationUtil.horaMask()
                )

                OutlinedTextField(
                    value = odometroSaida,
                    onValueChange = { odometroSaida = it },
                    label = { Text("Odômetro Saída") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }
        }
    )
}

@Composable
fun ModalRetorno(
    saidaExistente: SaidaDTO? = null,
    onDismiss: () -> Unit,
    onConfirm: (SaidaDTO) -> Unit
) {
    var horarioRetorno by remember { mutableStateOf("") }
    var odometroRetorno by remember { mutableStateOf(saidaExistente?.km_retorno?.toString() ?: "") }

    // Formatador para hora atual
    val formatterHours = remember { SimpleDateFormat("HHmm", Locale.getDefault()) }

    // Preenche hora automaticamente se for nulo
    LaunchedEffect(Unit) {
        if (saidaExistente?.horario_retorno.isNullOrEmpty()) {
            val now = Date()
            horarioRetorno = formatterHours.format(now) // Ex: "1445"
        } else {
            // Remove ":" se vier formatado
            horarioRetorno = saidaExistente?.horario_retorno?.filter { it.isDigit() } ?: ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val horaFormatada = if (horarioRetorno.length == 4)
                    "${horarioRetorno.substring(0, 2)}:${horarioRetorno.substring(2)}"
                else horarioRetorno

                val saida = saidaExistente?.copy(
                    horario_retorno = horaFormatada,
                    km_retorno = odometroRetorno.toIntOrNull(),
                    sincronizada = false,
                    completa = true
                )

                if (saida != null) {
                    onConfirm(saida)
                }
                onDismiss()
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Marcar Retorno") },
        text = {
            Column {
                OutlinedTextField(
                    value = horarioRetorno,
                    onValueChange = { input ->
                        // Aceita apenas dígitos, limita a 4
                        horarioRetorno = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text("Horário de Retorno") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    visualTransformation = VisualTransformationUtil.horaMask()
                )
                OutlinedTextField(
                    value = odometroRetorno,
                    onValueChange = { odometroRetorno = it },
                    label = { Text("Odômetro Retorno") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FrotaApiTheme {
        TelaPrincipal(object : MainActivity.SaidaSync{
            override suspend fun start(saidas: List<SaidaDTO>, saidaPrefs: SaidaPreferences) {
                TODO("Not yet implemented")
            }
        }, showLoginDialog = mutableStateOf(false))
    }
}

