package br.com.ideabit.frotaapi.model

data class SaidaModel(
    val nome_motorista: String,
    val horario_saida: String,
    val km_saida: Int,
    val horario_retorno: String? = null,
    val km_retorno: Int? = null,
    val sincronizada: Boolean = false,
    val completa: Boolean = false
)
