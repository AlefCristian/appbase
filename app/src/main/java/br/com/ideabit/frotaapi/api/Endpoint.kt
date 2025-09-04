package br.com.ideabit.frotaapi.api

import br.com.ideabit.frotaapi.model.UserModel
import br.com.ideabit.frotaapi.util.SaidaDTO
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface Endpoint {
    @POST("login")
    suspend fun auth(@Body user : UserModel) : Response<JsonObject>

    @POST("frota/saida")
    suspend fun setSaida(
        @Header("Authorization") token: String,
        @Body saida : SaidaDTO
    ) : Response<JsonObject >
}