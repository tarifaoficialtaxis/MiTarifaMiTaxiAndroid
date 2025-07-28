package com.mitarifamitaxi.taximetrousuario.helpers

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

object FirebaseStorageUtils {
    suspend fun uploadImage(folder: String, bitmap: Bitmap): String? {
        return try {
            // 1. Escalar (opcional)
            val target = bitmap.scaled(1024, 1024)

            // 2. Elegir formato y calidad
            val useWebp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            val format =
                if (useWebp) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.JPEG
            val quality = 80

            // 3. Preparar bytes
            val baos = ByteArrayOutputStream()
            target.compress(format, quality, baos)
            val data = baos.toByteArray()

            // 4. Metadata correcta
            val mime = if (format == Bitmap.CompressFormat.JPEG) "image/jpeg" else "image/webp"
            val metadata = storageMetadata { contentType = mime }

            // 5. Subir
            val fileName =
                "$folder/${System.currentTimeMillis()}.${if (format == Bitmap.CompressFormat.JPEG) "jpg" else "webp"}"
            val ref = FirebaseStorage.getInstance().reference.child(fileName)
            ref.putBytes(data, metadata).await()
            return ref.path
        } catch (e: Exception) {
            Log.e("FirebaseStorageUtils", "Error uploading image: ${e.message}")
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

    fun deleteFolder(folderPath: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child(folderPath)

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                listResult.items.forEach { item ->
                    item.delete()
                        .addOnSuccessListener {
                            Log.d("FirebaseStorageUtils", "Deleted file: ${item.path}")
                        }
                        .addOnFailureListener {
                            Log.e("FirebaseStorageUtils", "Failed to delete file: ${item.path}", it)
                        }
                }

                listResult.prefixes.forEach { subFolder ->
                    deleteFolder(subFolder.path)
                }
            }
            .addOnFailureListener {
                Log.e("FirebaseStorageUtils", "Failed to list folder: $folderPath", it)
            }
    }
}




