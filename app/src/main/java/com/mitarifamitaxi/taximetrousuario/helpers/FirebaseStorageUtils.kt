package com.mitarifamitaxi.taximetrousuario.helpers

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream



object FirebaseStorageUtils {
    suspend fun uploadImage(folder: String, bitmap: Bitmap): String? {
        return try {
            val fileName = "${folder}/${System.currentTimeMillis()}.png"
            val storageRef = FirebaseStorage.getInstance().reference.child(fileName)
            val metadata = storageMetadata {
                contentType = "image/png"
            }
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            storageRef.putBytes(byteArray, metadata).await()
            storageRef.downloadUrl.await().toString()
        } catch (error: Exception) {
            Log.e("TripSummaryViewModel", "Error uploading image: ${error.message}")
            null
        }
    }

    suspend fun deleteImage(imageUrl: String) {
        try {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
            Log.d("FirebaseStorageUtils", "Imagen borrada de Storage correctamente.")
        } catch (e: Exception) {
            Log.e("FirebaseStorageUtils", "Error al borrar imagen de Storage: ${e.message}")
        }
    }
}




