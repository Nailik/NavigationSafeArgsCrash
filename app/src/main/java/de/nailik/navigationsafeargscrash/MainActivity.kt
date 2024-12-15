package de.nailik.navigationsafeargscrash

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.nailik.navigationsafeargscrash.Routes.NestedParamRouteSealedInterface
import de.nailik.navigationsafeargscrash.SomeDataSealedInterface.SomeDataClassInner
import de.nailik.navigationsafeargscrash.ui.theme.NavigationSafeArgsCrashTheme
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NavigationSafeArgsCrashTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavGraph(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Home(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterVertically
        ),
    ) {
        var textFieldValue by remember { mutableStateOf(TextFieldValue("test %")) }
        TextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
            }
        )

        Button(onClick = {
            navController.navigate(
                StandardRoute(textFieldValue.text)
            )
        }) {
            Text(text = "StandardRoute")
        }

        Button(onClick = {
            navController.navigate(
                NestedParamRoute(SomeDataClass(textFieldValue.text))
            )
        }) {
            Text(text = "NestedParamRoute")
        }

        Button(onClick = {
            navController.navigate(
                NestedParamRouteSealedInterface(
                    SomeDataClassInner(
                        textFieldValue.text
                    )
                )
            )
        }
        ) {
            Text(text = "NestedParamRouteSealedInterface")
        }

    }
}

@Composable
fun RouteContent(
    navController: NavController,
    text: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterVertically
        ),
    ) {
        Text(text = text)


        Button(onClick = { navController.navigateUp() }) {
            Text(text = "Back")
        }
    }
}

@Composable
fun NavGraph(modifier: Modifier) {
    val navController = rememberNavController()
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Start,
    ) {
        composable<Start> {
            Home(
                navController = navController,
            )
        }
        composable<StandardRoute> { navBackStackEntry ->
            val route = navBackStackEntry.toRoute<StandardRoute>()
            RouteContent(
                navController = navController,
                text = route.text,
            )
        }
        composable<NestedParamRoute>(
            typeMap = mapOf(
                typeOf<SomeDataClass>() to serializableType<SomeDataClass>()
            )
        ) { navBackStackEntry ->
            val route = navBackStackEntry.toRoute<NestedParamRoute>()
            RouteContent(
                navController = navController,
                text = route.data.text,
            )
        }
        composable<NestedParamRouteSealedInterface>(
            typeMap = mapOf(
                typeOf<SomeDataSealedInterface>() to serializableType<SomeDataSealedInterface>()
            )
        ) { navBackStackEntry ->
            val route = navBackStackEntry.toRoute<NestedParamRouteSealedInterface>()
            RouteContent(
                navController = navController,
                text = (route.data as SomeDataClassInner).text,
            )
        }
    }
}

//java.lang.IllegalArgumentException: Destination with route StandardRoute cannot be found in navigation graph ComposeNavGraph(0x0) startDestination={Destination(0xf4524814) route=de.nailik.navigationsafeargscrash.Start}
//java.lang.IllegalArgumentException: Navigation destination that matches route de.nailik.navigationsafeargscrash.NestedParamRoute/{"text":"test %"} cannot be found in the navigation graph ComposeNavGraph(0x0) startDestination={Destination(0xf4524814) route=de.nailik.navigationsafeargscrash.Start}
//java.lang.IllegalArgumentException: Navigation destination that matches route de.nailik.navigationsafeargscrash.NestedParamRoute/%7B%22text%22%3A%22test%20%25%22%7D cannot be found in the navigation graph ComposeNavGraph(0x0) startDestination={Destination(0xf4524814) route=de.nailik.navigationsafeargscrash.Start}
//java.lang.IllegalArgumentException: Destination with route NestedParamRoute cannot be found in navigation graph ComposeNavGraph(0x0) startDestination={Destination(0xf4524814) route=de.nailik.navigationsafeargscrash.Start}

@Serializable
object Start

@Serializable
data class StandardRoute(
    val text: String,
)

@Parcelize
@Serializable
data class SomeDataClass(
    val text: String,
) : Parcelable

@Serializable
data class NestedParamRoute(
    val data: SomeDataClass,
)

interface Route

sealed interface Routes : Route {

    @Serializable
    data class NestedParamRouteSealedInterface(
        val data: SomeDataSealedInterface,
        val test: Boolean = true,
    ) : Routes

}

@Parcelize
@Serializable
sealed interface SomeDataSealedInterface : Parcelable {

    @Serializable
    data class SomeDataClassInner(val text: String) : SomeDataSealedInterface

}

inline fun <reified T : Parcelable> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {
    override fun get(
        bundle: Bundle,
        key: String,
    ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        bundle.getParcelable(key, T::class.java)!!
    } else {
        @Suppress("DEPRECATION")
        bundle.getParcelable(key)!!
    }

    override fun parseValue(value: String): T =
        json.decodeFromString(value)

    override fun serializeAsValue(value: T): String =
        Uri.encode(json.encodeToString(value))

    override fun put(
        bundle: Bundle,
        key: String,
        value: T,
    ) = bundle.putParcelable(key, value)

    override val name: String = T::class.java.simpleName
}