package net.ubikapps.mediascreensaver.ui.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.ubikapps.mediascreensaver.R

@Composable
fun AlbumArt(albumArt: Bitmap?, modifier: Modifier){
    if (albumArt != null) {
        val matrix = ColorMatrix()
        matrix.setToSaturation(0f)

        Image(
            bitmap = albumArt.asImageBitmap(),
            contentDescription = stringResource(R.string.album_art),
            modifier = modifier,
            colorFilter = ColorFilter.colorMatrix(matrix)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_music_note),
            contentDescription = stringResource(R.string.album_art),
            modifier = modifier.background(Color.LightGray)
        )
    }
}