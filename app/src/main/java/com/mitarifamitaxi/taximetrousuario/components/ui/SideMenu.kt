package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.BuildConfig
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.models.ItemSideMenu
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import androidx.compose.foundation.layout.WindowInsets

@Composable
fun DrawerContent(
    userData: LocalUser,
    onProfileClicked: () -> Unit,
    onSectionClicked: (ItemSideMenu) -> Unit
) {

    val sideMenuItems = sideMenuItems()

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(0.dp),
        drawerContainerColor = colorResource(id = R.color.white),
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        modifier = Modifier
            .fillMaxWidth(0.87f)
    ) {
        Column(
            modifier = Modifier
                .background(colorResource(id = R.color.white))
                .fillMaxSize()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        colorResource(id = R.color.main),
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )
                    .padding(top = 30.dp)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.city_background),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))

                )

                Button(
                    onClick = { onProfileClicked() },
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RectangleShape,
                    modifier =
                        Modifier
                            .padding(horizontal = 29.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(65.dp)
                                .background(colorResource(id = R.color.blue1), shape = CircleShape)
                                .border(2.dp, colorResource(id = R.color.white), CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "content description",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(45.dp),
                                tint = colorResource(id = R.color.white),

                                )
                        }

                        Column {
                            Text(
                                text = userData.firstName + " " + userData.lastName,
                                color = colorResource(id = R.color.white),
                                fontSize = 20.sp,
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.Start)
                            )

                            Text(
                                text = userData.city ?: "-",
                                color = colorResource(id = R.color.white),
                                fontSize = 16.sp,
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier
                                    .align(Alignment.Start)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))


                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "content description",
                            tint = colorResource(id = R.color.white),
                            modifier = Modifier
                                .size(45.dp),
                        )

                    }
                }


            }


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 29.dp)
                    .padding(top = 10.dp)
            ) {
                sideMenuItems.forEach { item ->
                    Button(
                        onClick = { onSectionClicked(item) },
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SideMenuItemRow(item)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(
                        id = R.string.version_param,
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    ),
                    color = colorResource(id = R.color.blue1),
                    fontSize = 14.sp,
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(20.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }


        }
    }
}

@Composable
fun sideMenuItems(): List<ItemSideMenu> {
    return listOf(
        ItemSideMenu(
            id = "HOME",
            icon = Icons.Outlined.Home,
            iconColor = colorResource(id = R.color.gray1),
            name = stringResource(id = R.string.home)
        ),
        ItemSideMenu(
            id = "TAXIMETER",
            icon = Icons.Default.Speed,
            iconColor = colorResource(id = R.color.main),
            name = stringResource(id = R.string.taximeter)
        ),
        ItemSideMenu(
            id = "SOS",
            icon = Icons.Default.WarningAmber,
            iconColor = colorResource(id = R.color.red1),
            name = stringResource(id = R.string.sos)
        ),
        ItemSideMenu(
            id = "PQRS",
            icon = Icons.AutoMirrored.Outlined.Chat,
            iconColor = colorResource(id = R.color.blue2),
            name = stringResource(id = R.string.pqrs)
        ),
        ItemSideMenu(
            id = "MY_TRIPS",
            icon = Icons.Default.Speed,
            iconColor = colorResource(id = R.color.purple1),
            name = stringResource(id = R.string.my_trips)
        ),
    )
}

@Composable
fun SideMenuItemRow(item: ItemSideMenu) {

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = item.iconColor
            )
            Spacer(modifier = Modifier.width(15.dp))
            Text(
                text = item.name,
                color = colorResource(id = R.color.blue1),
                fontSize = 15.sp,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Bold,
            )


        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colorResource(id = R.color.gray3))
        )
    }


}