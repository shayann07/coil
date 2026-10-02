package coil3.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter.Input
import coil3.compose.AsyncImagePainter.State
import coil3.compose.internal.transformOf
import coil3.request.ImageRequest
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AsyncImagePainterStateTest : RobolectricTest() {

    @Test
    fun updatingTransformReappliesToCurrentState() = runTest {
        val imageLoader = ImageLoader.Builder(context)
            .coroutineContext(coroutineContext)
            .build()
        val request = ImageRequest.Builder(context)
            .data("unsupported_data_that_fails")
            .build()
        val input = Input(imageLoader, request, AsyncImageModelEqualityDelegate.Default)

        val initialErrorPainter = ColorPainter(Color.Red)
        val updatedErrorPainter = ColorPainter(Color.Blue)

        val painter = AsyncImagePainter(input)
        painter.scope = this
        painter.transform = transformOf(placeholder = null, error = initialErrorPainter, fallback = null)

        painter.onRemembered()
        advanceUntilIdle()

        val state1 = painter.state.value
        assertIs<State.Error>(state1)
        assertSame(initialErrorPainter, state1.painter)

        painter.transform = transformOf(placeholder = null, error = updatedErrorPainter, fallback = null)

        val state2 = painter.state.value
        assertIs<State.Error>(state2)
        assertSame(updatedErrorPainter, state2.painter)
    }

    @Test
    fun updatingTransformReappliesPlaceholderWhileLoading() = runTest {
        val imageLoader = ImageLoader.Builder(context)
            .coroutineContext(coroutineContext)
            .build()
        val request = ImageRequest.Builder(context)
            .data("unsupported_data_that_fails")
            .build()
        val input = Input(imageLoader, request, AsyncImageModelEqualityDelegate.Default)

        val initialPlaceholder = ColorPainter(Color.Yellow)
        val updatedPlaceholder = ColorPainter(Color.Green)

        val painter = AsyncImagePainter(input)
        painter.scope = this
        painter.transform = transformOf(placeholder = initialPlaceholder, error = null, fallback = null)

        painter.onRemembered()

        val state1 = painter.state.value
        assertIs<State.Loading>(state1)
        assertSame(initialPlaceholder, state1.painter)

        painter.transform = transformOf(placeholder = updatedPlaceholder, error = null, fallback = null)

        val state2 = painter.state.value
        assertIs<State.Loading>(state2)
        assertSame(updatedPlaceholder, state2.painter)

        advanceUntilIdle()
    }

    @Test
    fun changingInputResetsRawState() = runTest {
        val imageLoader = ImageLoader.Builder(context)
            .coroutineContext(coroutineContext)
            .build()
        val request1 = ImageRequest.Builder(context)
            .data("unsupported_data_that_fails_1")
            .build()
        val input1 = Input(imageLoader, request1, AsyncImageModelEqualityDelegate.Default)

        val errorPainter1 = ColorPainter(Color.Red)
        val errorPainter2 = ColorPainter(Color.Blue)

        val painter = AsyncImagePainter(input1)
        painter.scope = this
        painter.transform = transformOf(placeholder = null, error = errorPainter1, fallback = null)

        painter.onRemembered()
        advanceUntilIdle()

        assertIs<State.Error>(painter.state.value)

        val request2 = ImageRequest.Builder(context)
            .data("unsupported_data_that_fails_2")
            .build()
        val input2 = Input(imageLoader, request2, AsyncImageModelEqualityDelegate.Default)
        painter._input = input2

        painter.transform = transformOf(placeholder = null, error = errorPainter2, fallback = null)
        assertIs<State.Loading>(painter.state.value)

        advanceUntilIdle()
        val state = painter.state.value
        assertIs<State.Error>(state)
        assertSame(errorPainter2, state.painter)
    }
}
