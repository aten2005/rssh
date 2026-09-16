package dev.aten.rssh.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Routes {
    const val HOME = "home"
    const val KEY = "key"
    const val HOST = "host/{id}"
    const val COMMAND = "command/{id}"
    fun host(id: Long) = "host/$id"
    fun command(id: Long) = "command/$id"
}

@Composable
fun RsshNavHost() {
    val nav = rememberNavController()
    val idArg = listOf(navArgument("id") { type = NavType.LongType })
    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenHost = { nav.navigate(Routes.host(it)) },
                onOpenCommand = { nav.navigate(Routes.command(it)) },
                onOpenKey = { nav.navigate(Routes.KEY) },
            )
        }
        composable(Routes.HOST, arguments = idArg) { entry ->
            HostEditScreen(hostId = entry.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() })
        }
        composable(Routes.COMMAND, arguments = idArg) { entry ->
            CommandEditScreen(commandId = entry.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() })
        }
        composable(Routes.KEY) {
            KeyScreen(onBack = { nav.popBackStack() })
        }
    }
}
