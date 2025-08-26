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
import br.com.ideabit.frotaapi.model.UserModel
import br.com.ideabit.frotaapi.ui.theme.FrotaApiTheme
import br.com.ideabit.frotaapi.util.NetworkUtils
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrotaApiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        userLogin()
    }

    private fun userLogin() {
        val retrofitClient = NetworkUtils.getRetrofitInstance("http://10.0.2.2:8080/api/")
        val endpoint = retrofitClient.create(Endpoint::class.java)

        endpoint.auth(UserModel("alef.santos", "example@email.com", "123456"))
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if(response.isSuccessful) {
                        val json = response.body()
                        val token = json?.get("token")?.asString
                        println(token)

                    } else {
                        println("Erro: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    println("Erro de rede: ${t.message}")
                }
            })
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FrotaApiTheme {
        Greeting("Android")
    }
}

