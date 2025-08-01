package com.mitarifamitaxi.taximetrousuario.components.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mitarifamitaxi.taximetrousuario.R

@Composable
fun ProfilePictureBox(
    imageUri: Uri? = null,
    imagePath: String? = null,
    editable: Boolean = true,
    boxSize: Int = 90,
    iconSize: Int = 55,
    onClickEdit: () -> Unit = {}
) {

    Box(
        modifier = Modifier.size(90.dp)
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.blue1))
                    .size(boxSize.dp),
                contentScale = ContentScale.Crop,
                onError = { }
            )
        } else if (imagePath != null && imagePath.isNotEmpty()) {
            FirebaseImage(
                storagePath = imagePath,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.blue1))
                    .size(boxSize.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.blue1))
                    .size(boxSize.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Icon",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize.dp)
                )
            }
        }

        if (editable) {
            IconButton(
                onClick = { onClickEdit() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, colorResource(id = R.color.blue1), CircleShape)
                    .size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Icon",
                    tint = colorResource(id = R.color.blue1),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun DefaultPreview() {
    ProfilePictureBox(
        imageUri = null,
        onClickEdit = {}
    )
}