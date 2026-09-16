package dev.aten.rssh

import android.app.Application
import android.content.Context
import dev.aten.rssh.data.AppDatabase
import dev.aten.rssh.ssh.KeyManager
import dev.aten.rssh.ssh.SshRunner
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

class RsshApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Android ships a stripped-down BouncyCastle without Ed25519; sshj needs the full one.
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val db = AppDatabase.get(app)
    val keyManager = KeyManager(app)
    val runner = SshRunner(keyManager::loadKeyPair)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as RsshApp).container
