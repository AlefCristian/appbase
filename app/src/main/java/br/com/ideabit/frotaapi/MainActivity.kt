package br.com.ideabit.frotaapi

import TimeVisualTransformation
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.ideabit.frotaapi.api.Endpoint
import br.com.ideabit.frotaapi.model.SaidaModel
import br.com.ideabit.frotaapi.model.UserModel
import br.com.ideabit.frotaapi.ui.theme.FrotaApiTheme
import br.com.ideabit.frotaapi.util.NetworkUtils
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
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
import br.com.ideabit.frotaapi.util.UserPreferences
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
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
                TelaPrincipal(saidaSync)
            }
        }
    }
    private fun getEndPoint() : Endpoint {
        val retrofitClient = NetworkUtils.getRetrofitInstance("http://192.168.15.8:8080/api/")
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
                throw Exception("Token nulo na resposta")
            }
        } else {
            val errorBody = response.errorBody()?.string()
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
fun TelaPrincipal(saidaSync: MainActivity.SaidaSync) {
    val context = LocalContext.current
    val saidaPrefs = remember { SaidaPreferences(context.applicationContext) }

    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    var showLoginDialog by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val usuario = AppPreferences.userPrefs.userFlow.first()
        val senha = AppPreferences.userPrefs.passwordFlow.first()
        showLoginDialog = usuario.isEmpty() || senha.isEmpty()
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

    if (showLoginDialog) {
        LoginDialog(onDismiss = { showLoginDialog = false })
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val incompleta = saidas.find { !it.completa }
                if (incompleta != null) {
                    println("Editar saída de ${incompleta.nome_motorista}")
                    saidaSelecionada = incompleta
                } else {
                    println("Criar nova saída")
                    saidaSelecionada = null
                }
                showDialog = true
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
                                Text("Retorno: ${saida.horario_retorno ?: "Não voltou"}")
                                Text("Km retorno: ${saida.km_retorno ?: "-"}")
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
fun extractNumberFromBitmap(bitmap: Bitmap, onResult: (String) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            // Procura o primeiro número encontrado
            val text = visionText.text
            val number = Regex("\\d+").find(text)?.value ?: ""
            onResult(number)
        }
        .addOnFailureListener {
            onResult("")
        }
}
@Composable
fun ModalCadastrarSaida(
    saidaExistente: SaidaDTO? = null,
    onDismiss: () -> Unit,
    onConfirm: (SaidaDTO) -> Unit
) {
    var nome by remember { mutableStateOf(saidaExistente?.nome_motorista ?: "") }
    var horarioSaida by remember { mutableStateOf(saidaExistente?.horario_saida ?: "") }
    var odometroSaida by remember { mutableStateOf(saidaExistente?.km_saida?.toString() ?: "") }
    var horarioRetorno by remember { mutableStateOf(saidaExistente?.horario_retorno ?: "") }
    var odometroRetorno by remember { mutableStateOf(saidaExistente?.km_retorno?.toString() ?: "") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            extractNumberFromBitmap(it) { numero ->
                if (numero.isNotBlank()) {
                    odometroSaida = numero
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val completa = nome.isNotBlank() && horarioSaida.isNotBlank() && (odometroSaida.toIntOrNull() ?: 0) > 0
                val saida = SaidaDTO(
                    id = saidaExistente?.id, // mantém o id null
                    nome_motorista = nome,
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FrotaApiTheme {
        TelaPrincipal(object : MainActivity.SaidaSync{
            override suspend fun start(saidas: List<SaidaDTO>, saidaPrefs: SaidaPreferences) {
                TODO("Not yet implemented")
            }
        })
    }
}

