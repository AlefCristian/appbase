package br.com.ideabit.frotaapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import br.com.ideabit.frotaapi.util.UserPreferences

class MainActivity : ComponentActivity() {
    companion object {
        var token: String? = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FrotaApiTheme {
                TelaPrincipal()
            }
        }
    }

    private fun getEndPoint() : Endpoint {
        val retrofitClient = NetworkUtils.getRetrofitInstance("http://10.0.2.2:8080/api/")
        val endpoint = retrofitClient.create(Endpoint::class.java)
        return endpoint
    }

    private fun getLogin() {
        getEndPoint().auth(UserModel("alef.santos", "example@email.com", "123456"))
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if(response.isSuccessful) {
                        val json = response.body()
                        val token = json?.get("token")?.asString
                        println(token)
                        MainActivity.token = token
                        setSaida()
                    } else {
                        println("Erro: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    println("Erro de rede: ${t.message}")
                }
            })
    }

    private fun setSaida() {
        getEndPoint().setSaida("Bearer ${MainActivity.token}" ?: "", SaidaModel("Alef Santos", "10:15", 5000, "12:15", 5001))
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if(response.isSuccessful) {
                        println("Deu tudo certo")
                    } else {
                        println("Erro : ${response.code()}")
                        println(response.message())
                        println("Token: ${MainActivity.token}")
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    println("Erro  de rede: ${t.message}")
                }
            })
    }
}

@Composable
fun TelaPrincipal() {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }

    LaunchedEffect(Unit) {
        val user = userPrefs.getUser()
        val pass = userPrefs.getPassword()

        if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
            println("Login salvo: $user / $pass")
            // Você pode já fazer login automático aqui, se quiser
            TODO("Implementar o login ao iniciar aplicação")
        }
    }

    // Lista simulada de saídas (poderia vir de Room, ViewModel, etc.)
    val saidas = remember {
        mutableStateListOf(
            SaidaModel("Alef Santos", "10:15", 5000, "12:15", 5001, sincronizada = false, completa = true),
            SaidaModel("João Silva", "14:00", 5002, "16:00", 5003, sincronizada = false, completa = false),
        )
    }

    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        ModalCadastrarSaida(
            onDismiss = { showDialog = false },
            onConfirm = { novaSaida ->
                saidas.add(novaSaida)
                showDialog = false
            }
        )
    }

    var showLoginDialog by remember { mutableStateOf(false) }
    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onLogin = { usuario, senha ->
                println("Usuário: $usuario, Senha: $senha")
                // Aqui você pode chamar a função de login da sua API
                // ex: viewModel.login(usuario, senha)
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val incompleta = saidas.find { !it.completa }
                if (incompleta != null) {
                    // Lógica para editar a saída incompleta
                    println("Editar saída de ${incompleta.nome_motorista}")
                    showDialog = true
                } else {
                    // Lógica para criar nova saída
                    println("Criar nova saída")
                    showDialog = true
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar ou Editar Saída")
            }
        },
        bottomBar = {
            Button(
                onClick = {
                    val pendentes = saidas.filter { !it.sincronizada }
                    if (pendentes.isNotEmpty()) {
                        println("Sincronizando ${pendentes.size} saídas...")
                    } else {
                        println("Nenhuma saída pendente")
                    }
                    showLoginDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Sincronizar")
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            items(saidas) { saida ->
                if (!saida.sincronizada) {
                    Text(
                        text = "Saída: ${saida.nome_motorista} - ${saida.horario_saida}",
                        modifier = Modifier.padding(16.dp)
                    )
                    Divider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FrotaApiTheme {
        TelaPrincipal()
    }
}

@Composable
fun ModalCadastrarSaida(
    onDismiss: () -> Unit,
    onConfirm: (SaidaModel) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var horarioSaida by remember { mutableStateOf("") }
    var odometroSaida by remember { mutableStateOf("") }
    var horarioRetorno by remember { mutableStateOf("") }
    var odometroRetorno by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val saida = SaidaModel(
                    nome_motorista = nome,
                    horario_saida = horarioSaida,
                    km_saida = odometroSaida.toIntOrNull() ?: 0,
                    horario_retorno = horarioRetorno,
                    km_retorno = odometroRetorno.toIntOrNull() ?: 0,
                    sincronizada = false,
                    completa = true
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
                    value = horarioSaida,
                    onValueChange = { horarioSaida = it },
                    label = { Text("Horário de Saída") }
                )
                OutlinedTextField(
                    value = odometroSaida,
                    onValueChange = { odometroSaida = it },
                    label = { Text("Odômetro Saída") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = horarioRetorno,
                    onValueChange = { horarioRetorno = it },
                    label = { Text("Horário de Retorno") }
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

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onLogin(usuario, senha)
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
            }
        }
    )
}

