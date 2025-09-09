package br.com.ideabit.frotaapi.util

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
private val Context.saidaDataStore by preferencesDataStore(name = "saida_prefs")
data class SaidaDTO(
    var id: Long? = null,
    val nome_motorista: String,
    val data_saida: String? = null,
    val horario_saida: String,
    val km_saida: Int,
    val horario_retorno: String? = null,
    val km_retorno: Int? = null,
    var sincronizada: Boolean = false,
    var completa: Boolean = true
)
class SaidaPreferences(private val context: Context) {

    companion object {
        val SAIDAS_KEY = stringPreferencesKey("saidas_key")
    }

    private val gson = Gson()

    // Flow das saídas
    val saidasFlow: Flow<List<SaidaDTO>> = context.saidaDataStore.data
        .map { prefs ->
            val json = prefs[SAIDAS_KEY] ?: "[]"
            val type = object : TypeToken<List<SaidaDTO>>() {}.type
            gson.fromJson<List<SaidaDTO>>(json, type) ?: emptyList()
        }

    // Salvar todas as saídas (substitui o conteúdo)
    suspend fun saveSaidas(saidas: List<SaidaDTO>) {
        context.saidaDataStore.edit { prefs ->
            val json = gson.toJson(saidas)
            prefs[SAIDAS_KEY] = json
        }
    }

    // Adicionar uma saída nova
    suspend fun addSaida(saida: SaidaDTO) {
        val current = saidasFlow.first().toMutableList()
        saida.id = System.currentTimeMillis()
        current.add(saida)
        saveSaidas(current)
    }

    // Atualizar uma saída existente (procura pelo id)
    suspend fun updateSaida(updated: SaidaDTO) {
        val current = saidasFlow.first().toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            current[index] = updated
            saveSaidas(current)
        } // se não encontrar, não faz nada (ou você pode optar por adicionar)
    }

    // Remover uma saída por id
    suspend fun removeSaida(id: Long?) {
        val current = saidasFlow.first().toMutableList()
        val changed = current.removeAll { it.id == id }
        if (changed) saveSaidas(current)
    }

    suspend fun removeSincronizados() {
        val current = saidasFlow.first().toMutableList()
        val restantes = current.filter { !it.sincronizada }
        saveSaidas(restantes)
    }


    // Limpar todas as saídas
    suspend fun clear() {
        context.saidaDataStore.edit { it.clear() }
    }
}
