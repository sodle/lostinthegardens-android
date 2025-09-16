package com.sjodle.lostinthegardens.park_data

import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.sjodle.lostinthegardens.park_data.categories.CategoryFile
import com.sjodle.lostinthegardens.park_data.markers.Park
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.path
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class ParkData(
    val park: Park,
    val categories: CategoryFile,
)

sealed class ParkLoadingState {
    object Loading : ParkLoadingState()

    //    object Error : ParkLoadingState()
    class Success(val parkData: ParkData) : ParkLoadingState()
    class StaleData(val parkData: ParkData) : ParkLoadingState()
}

fun loadFallbackData(
    context: Context,
    @RawRes shapesFallback: Int,
    @RawRes categoriesFallback: Int,
): ParkLoadingState {
    val shapefile = context.resources.openRawResource(shapesFallback)
    val shapeJson = String(shapefile.readBytes())
    shapefile.close()

    val categoryFile = context.resources.openRawResource(categoriesFallback)
    val categoryJson = String(categoryFile.readBytes())
    categoryFile.close()

    return ParkLoadingState.StaleData(
        ParkData(
            Park.fromShapefile(shapeJson),
            CategoryFile.fromJson(categoryJson),
        )
    )
}

@Composable
fun loadParkData(
    context: Context,
    baseUrl: String = "https://lostinthegardens.com",
    parkId: String,
    @RawRes shapesFallback: Int,
    @RawRes categoriesFallback: Int,
    attemptId: UUID,
): State<ParkLoadingState> {
    return produceState<ParkLoadingState>(
        initialValue = ParkLoadingState.Loading,
        baseUrl,
        parkId,
        attemptId
    ) {
        launch {
            try {
                Log.d("loadParkData", "Starting")
                val client = HttpClient(CIO)
                val indexResponse = client.get(baseUrl) {
                    url {
                        path("/api")
                    }
                }.bodyAsText()
                val indexJson = JSONObject(indexResponse)
                    .getJSONObject("Android")
                    .getJSONObject(parkId)
                Log.d("loadParkData", "index: $indexJson")

                val shapefileUrl = indexJson.getJSONObject("shapeFile").getString("url")
                val shapefileResponse = client.get(baseUrl) {
                    url {
                        path(shapefileUrl)
                    }
                }.bodyAsText()

                val categoryFileUrl = indexJson.getJSONObject("categoryFile").getString("url")
                val categoryFileResponse = client.get(baseUrl) {
                    url {
                        path(categoryFileUrl)
                    }
                }.bodyAsText()

                value = ParkLoadingState.Success(
                    ParkData(
                        Park.fromShapefile(shapefileResponse),
                        CategoryFile.fromJson(categoryFileResponse),
                    )
                )
            } catch (e: Exception) {
                Log.e("loadParkData", "Couldn't load data from network: $e")
                value = loadFallbackData(context, shapesFallback, categoriesFallback)
            }
        }
    }
}