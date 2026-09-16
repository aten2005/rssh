package dev.aten.rssh

import android.app.Application
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

class RsshApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Android ships a stripped-down BouncyCastle without Ed25519; sshj needs the full one.
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
    }
}
