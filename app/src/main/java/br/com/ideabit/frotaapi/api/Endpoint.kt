package br.com.ideabit.frotaapi.api

import br.com.ideabit.frotaapi.model.UserModel
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface Endpoint {
    @POST("login")
    fun auth(@Body user : UserModel) : Call<JsonObject>

    @GET("/frota/saida")
    fun getCurrencies() : Call<JsonObject >
}