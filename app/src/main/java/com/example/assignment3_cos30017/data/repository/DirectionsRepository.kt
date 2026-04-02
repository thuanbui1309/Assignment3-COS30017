package com.example.assignment3_cos30017.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.assignment3_cos30017.util.PolylineDecoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class DirectionsRepository(
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {

    private data class Key(
        val oLat: Int,
        val oLng: Int,
        val dLat: Int,
        val dLng: Int
    )

    private val cache = ConcurrentHashMap<Key, List<LatLng>>()

    suspend fun getRoutePoints(
        context: Context,
        origin: LatLng,
        destination: LatLng
    ): List<LatLng>? = withContext(Dispatchers.IO) {
        val apiKey = getMapsApiKey(context) ?: return@withContext null

        val key = Key(
            oLat = (origin.latitude * 1e4).toInt(),
            oLng = (origin.longitude * 1e4).toInt(),
            dLat = (destination.latitude * 1e4).toInt(),
            dLng = (destination.longitude * 1e4).toInt()
        )

        cache[key]?.let { return@withContext it }

        val url = buildString {
            append("https://maps.googleapis.com/maps/api/directions/json")
            append("?origin=${origin.latitude},${origin.longitude}")
            append("&destination=${destination.latitude},${destination.longitude}")
            append("&mode=driving")
            append("&key=$apiKey")
        }

        val req = Request.Builder().url(url).get().build()
        val body = try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Directions HTTP ${resp.code}")
                    return@withContext null
                }
                resp.body?.string()
            }
        } catch (_: IOException) {
            null
        } ?: return@withContext null

        val json = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext null
        val status = json.optString("status")
        if (status != "OK") {
            val errorMessage = json.optString("error_message").takeIf { it.isNotBlank() }
            Log.w(TAG, "Directions status=$status" + (errorMessage?.let { " error=$it" } ?: ""))
            return@withContext null
        }

        val routes = json.optJSONArray("routes") ?: return@withContext null
        val firstRoute = routes.optJSONObject(0) ?: return@withContext null
        val overview = firstRoute.optJSONObject("overview_polyline") ?: return@withContext null
        val points = overview.optString("points")
        val decoded = PolylineDecoder.decode(points)
        if (decoded.isEmpty()) return@withContext null

        cache[key] = decoded
        decoded
    }

    private fun getMapsApiKey(context: Context): String? {
        return try {
            val ai = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            ai.metaData?.getString("com.google.android.geo.API_KEY")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "DirectionsRepository"
    }
}

