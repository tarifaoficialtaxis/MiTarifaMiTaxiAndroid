package com.mitarifamitaxi.taximetrousuario.helpers

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.mitarifamitaxi.taximetrousuario.models.Feature
import com.mitarifamitaxi.taximetrousuario.models.PlacePrediction
import com.mitarifamitaxi.taximetrousuario.models.Properties
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import com.mitarifamitaxi.taximetrousuario.resources.countries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

suspend fun getCityFromCoordinates(
    context: Context,
    latitude: Double,
    longitude: Double,
    callbackSuccess: (country: String?, countryCode: String?, countryCodeWhatsapp: String?, countryCurrency: String?) -> Unit,
    callbackError: (Exception) -> Unit
) {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]

                val country = countries.find { it.code == address.countryCode }

                callbackSuccess(
                    address.countryName,
                    address.countryCode,
                    country?.dial?.replace("+", ""),
                    country?.currency
                )

            } else {
                callbackError(IOException("Unexpected response"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callbackError(IOException("No results found"))
        }
    }
}

// OPEN CAGE API

/*fun getAddressFromCoordinates(
    latitude: Double,
    longitude: Double,
    callbackSuccess: (address: String) -> Unit,
    callbackError: (Exception) -> Unit
) {
    val url =
        "${K.OPEN_CAGE_API_URL}geocode/v1/json?q=$latitude,$longitude&key=${K.OPEN_CAGE_API_KEY}"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callbackError(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    callbackError(IOException("Unexpected response $response"))
                    return
                }

                val jsonResponse = JSONObject(it.body?.string() ?: "")
                val results = jsonResponse.optJSONArray("results")

                if (results != null && results.length() > 0) {
                    val address =
                        results.getJSONObject(0).getString("formatted")

                    callbackSuccess(address)

                } else {
                    callbackError(IOException("No results found"))
                }
            }
        }
    })
}*/

// NOMINATIM API
fun getAddressFromCoordinates(
    latitude: Double,
    longitude: Double,
    callbackSuccess: (address: String) -> Unit,
    callbackError: (Exception) -> Unit
) {
    val url =
        "${K.NOMINATIM_URL}reverse?lat=${latitude}&lon=${longitude}&format=json"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callbackError(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    callbackError(IOException("Unexpected response $response"))
                    return
                }

                val jsonResponse = JSONObject(it.body?.string() ?: "")

                if (jsonResponse.has("address")) {
                    val addressObject = jsonResponse.getJSONObject("address")
                    val road = addressObject.optString("road", "")
                    val city = addressObject.optString("city", "")

                    if (road.isNotBlank() || city.isNotBlank()) {
                        val finalAddress = listOf(road, city)
                            .filter { part -> part.isNotBlank() }
                            .joinToString(", ")
                        callbackSuccess(finalAddress)
                    } else {
                        callbackError(IOException("Road or City not found in address details"))
                    }
                } else {
                    callbackError(IOException("Address object not found in response"))
                }
            }
        }
    })
}


/*fun getAddressFromCoordinates(
    latitude: Double,
    longitude: Double,
    callbackSuccess: (address: String) -> Unit,
    callbackError: (Exception) -> Unit
) {
    val url =
        "${K.MAPS_API_URL}geocode/json?latlng=$latitude,$longitude&key=${K.GOOGLE_API_KEY}"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callbackError(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    callbackError(IOException("Unexpected response $response"))
                    return
                }

                val jsonResponse = JSONObject(it.body?.string() ?: "")
                val results = jsonResponse.optJSONArray("results")

                if (results != null && results.length() > 0) {
                    val address =
                        results.getJSONObject(0).getString("formatted_address")

                    callbackSuccess(address)

                } else {
                    callbackError(IOException("No results found"))
                }
            }
        }
    })
}*/

/*fun getPlacePredictions(
    input: String,
    latitude: Double,
    longitude: Double,
    country: String = "CO",
    radius: Int = 30000,
    callbackSuccess: (ArrayList<PlacePrediction>) -> Unit,
    callbackError: (Exception) -> Unit
) {
    val encodedInput = URLEncoder.encode(input, "UTF-8")

    val url =
        "${K.MAPS_API_URL}place/autocomplete/json?" +
                "input=$encodedInput" +
                "&location=$latitude,$longitude" +
                "&radius=$radius" +
                "&language=es" +
                "&strictbounds" +
                "&components=country:$country" +
                "&key=${K.GOOGLE_API_KEY}"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callbackError(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    callbackError(IOException("Unexpected response $response"))
                    return
                }

                val jsonResponse = JSONObject(it.body?.string() ?: "")
                val predictions = jsonResponse.optJSONArray("predictions")

                if (predictions != null && predictions.length() > 0) {
                    val predictionsList = ArrayList<PlacePrediction>()
                    for (i in 0 until predictions.length()) {
                        val prediction = predictions.getJSONObject(i)
                        predictionsList.add(
                            PlacePrediction(
                                placeId = prediction.optString("place_id"),
                                description = prediction.optString("description")
                            )
                        )
                    }
                    callbackSuccess(predictionsList)
                } else {
                    callbackSuccess(ArrayList())
                }
            }
        }
    })
}*/

// GEOCODE EARTH API
fun getPlacePredictions(
    input: String,
    latitude: Double,
    longitude: Double,
    country: String = "CO",
    radius: Int = 30000,
    callbackSuccess: (ArrayList<PlacePrediction>) -> Unit,
    callbackError: (Exception) -> Unit
) {
    val encodedInput = URLEncoder.encode(input, "UTF-8")
    val radiusInKm = radius / 1000

    val url =
        "${K.GEOCODE_EARTH_API_URL}autocomplete?" +
                "api_key=${K.GEOCODE_EARTH_API_KEY}" +
                "&text=$encodedInput" +
                "&focus.point.lat=$latitude" +
                "&focus.point.lon=$longitude" +
                "&boundary.country=$country" +
                "&boundary.circle.lat=$latitude" +
                "&boundary.circle.lon=$longitude" +
                "&boundary.circle.radius=$radiusInKm" +
                "&lang=es"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callbackError(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    callbackError(IOException("Unexpected response $response"))
                    return
                }

                val jsonResponse = JSONObject(it.body?.string() ?: "")
                val features = jsonResponse.optJSONArray("features")

                if (features != null && features.length() > 0) {
                    val predictionsList = ArrayList<PlacePrediction>()
                    for (i in 0 until features.length()) {
                        val feature = features.getJSONObject(i)
                        val properties = feature.optJSONObject("properties")
                        if (properties != null) {
                            predictionsList.add(
                                PlacePrediction(
                                    placeId = properties.optString("gid"),
                                    description = properties.optString("label")
                                )
                            )
                        }
                    }
                    callbackSuccess(predictionsList)
                } else {
                    callbackSuccess(ArrayList())
                }
            }
        }
    })
}


/*fun getPlaceDetails(
    placeId: String,
    callbackSuccess: (UserLocation) -> Unit,
    callbackError: (Exception) -> Unit
) {
    val url =
        "${K.MAPS_API_URL}place/details/json?place_id=$placeId&key=${K.GOOGLE_API_KEY}&language=es"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callbackError(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    callbackError(IOException("Unexpected response $response"))
                    return
                }

                val jsonResponse = JSONObject(it.body?.string() ?: "")

                val result = jsonResponse.optJSONObject("result")
                val geometry = result?.optJSONObject("geometry")
                val location = geometry?.optJSONObject("location")

                callbackSuccess(
                    UserLocation(
                        latitude = location?.optDouble("lat"),
                        longitude = location?.optDouble("lng")
                    )
                )

            }
        }
    })
}*/

fun getCoordinatesFromQuery(
    query: String,
    callbackSuccess: (UserLocation) -> Unit,
    callbackError: (Exception) -> Unit
) {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val url = "${K.NOMINATIM_URL}search?q=$encodedQuery&format=json"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callbackError(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    callbackError(IOException("Unexpected response $response"))
                    return
                }

                val responseBody = it.body?.string() ?: ""
                if (responseBody.isEmpty()) {
                    callbackError(IOException("Response body is empty."))
                    return
                }

                val jsonArray = JSONArray(responseBody)

                if (jsonArray.length() > 0) {
                    val firstResult = jsonArray.getJSONObject(0)
                    callbackSuccess(
                        UserLocation(
                            latitude = firstResult.optDouble("lat"),
                            longitude = firstResult.optDouble("lon")
                        )
                    )
                } else {
                    callbackError(Exception("No results found for query: $query"))
                }
            }
        }
    })
}

fun getShortAddress(inputString: String?): String {
    if (inputString.isNullOrEmpty()) return ""
    val parts = inputString.split(",")
    val addressStreet = parts[0]
    val addressComplement = parts.getOrNull(1)
    return addressStreet + if (addressComplement != null) ", $addressComplement" else ""
}

fun getStreetAddress(inputString: String?): String {
    if (inputString.isNullOrEmpty()) return ""
    return inputString.split(",")[0]
}

fun getComplementAddress(inputString: String?): String {
    if (inputString.isNullOrEmpty()) return ""
    val commaIndex = inputString.indexOf(',')
    return inputString.substring(commaIndex + 1).trim()
}

fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        poly.add(LatLng(lat / 1E5, lng / 1E5))
    }
    return poly
}

fun calculateDistance(startCoordinates: LatLng, endCoordinates: LatLng): Double {
    val toRad = { value: Double -> (value * Math.PI) / 180 }
    val r = 6371e3

    val lat1 = toRad(startCoordinates.latitude)
    val lat2 = toRad(endCoordinates.latitude)
    val deltaLat = toRad(endCoordinates.latitude - startCoordinates.latitude)
    val deltaLon = toRad(endCoordinates.longitude - startCoordinates.longitude)

    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) *
            sin(deltaLon / 2) * sin(deltaLon / 2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return r * c
}

fun calculateBearing(startPosition: LatLng, endPosition: LatLng): Float {
    val lat1 = Math.toRadians(startPosition.latitude)
    val lat2 = Math.toRadians(endPosition.latitude)
    val lon1 = Math.toRadians(startPosition.longitude)
    val lon2 = Math.toRadians(endPosition.longitude)

    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

    val bearing = Math.toDegrees(atan2(y, x))
    return ((bearing + 360) % 360).toFloat()
}


private fun isPointInPolygon(lon: Double, lat: Double, polygon: List<List<Double>>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i][0]
        val yi = polygon[i][1]
        val xj = polygon[j][0]
        val yj = polygon[j][1]
        val intersect = ((yi > lat) != (yj > lat)) &&
                (lon < (xj - xi) * (lat - yi) / (yj - yi) + xi)
        if (intersect) {
            inside = !inside
        }
        j = i
    }
    return inside
}

fun findRegionForCoordinates(
    lat: Double,
    lon: Double,
    features: List<Feature>
): Properties? {

    features.forEach { feature ->
        val geometry = feature.geometry ?: return@forEach
        val outerRing = geometry.coordinates?.firstOrNull() ?: return@forEach
        if (isPointInPolygon(lon, lat, outerRing)) {
            return feature.properties
        }
    }

    return null
}




