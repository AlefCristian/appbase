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
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp


class MainActivity : ComponentActivity() {
    companion object {
        var token:String? = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrotaApiTheme {
                val saidas = remember { mutableStateListOf<SaidaModel>() }

                saidaScreen(
                    saidas = saidas,
                    onAddOrEditClick = {
                        val incompleta = saidas.find { !it.completa }
                        if (incompleta != null) {
                            // editarSaida(incompleta)
                        } else {
                            // criarNovaSaida()
                        }
                    },
                    onSyncClick = {
                        val pendentes = saidas.filter { !it.sincronizada }
                        // sincronizarSaidas(pendentes)
                    }
                )
            }
        }

    }

    @Override
    override fun onResume() {
        super.onResume()
        getLogin()
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
fun saidaScreen(
    saidas: List<SaidaModel>,
    onAddOrEditClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddOrEditClick() }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar ou Editar Saída")
            }
        },
        bottomBar = {
            Button(
                onClick = { onSyncClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Sincronizar")
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(saidas) { saida ->
                if (!saida.sincronizada) {
                    Text("Saída: ${saida.nome} - ${saida.horaSaida}")
                    // Você pode usar Card, Divider etc. para deixar mais bonito
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FrotaApiTheme {
        Greeting("Android")
    }
}

