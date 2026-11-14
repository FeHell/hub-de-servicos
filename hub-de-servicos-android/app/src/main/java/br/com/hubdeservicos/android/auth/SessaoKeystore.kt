package br.com.hubdeservicos.android.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessaoKeystore(context: Context, origem: String) : ArmazenamentoSessao {
    private val preferencias = context.getSharedPreferences("autenticacao", Context.MODE_PRIVATE)
    private val chaveToken = "atualizacao@$origem"

    override fun ler(): String? {
        val dados = preferencias.getString(chaveToken, null) ?: return null
        return try {
            val (vetor, conteudo) = dados.split('.', limit = 2)
            val chave = keystore().getKey(ALIAS, null) as? SecretKey ?: throw GeneralSecurityException()
            val cipher = Cipher.getInstance(CIFRA)
            cipher.init(Cipher.DECRYPT_MODE, chave, GCMParameterSpec(128, decodificar(vetor)))
            cipher.doFinal(decodificar(conteudo)).toString(Charsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            apagar()
            null
        } catch (_: IllegalArgumentException) {
            apagar()
            null
        }
    }

    override fun gravar(atualizacao: String) {
        try {
            val cipher = Cipher.getInstance(CIFRA)
            cipher.init(Cipher.ENCRYPT_MODE, obterChave())
            val conteudo = cipher.doFinal(atualizacao.toByteArray(Charsets.UTF_8))
            val dados = "${codificar(cipher.iv)}.${codificar(conteudo)}"
            if (!preferencias.edit().putString(chaveToken, dados).commit()) throw IOException()
        } catch (_: GeneralSecurityException) {
            throw IOException()
        }
    }

    override fun apagar() {
        if (!preferencias.edit().remove(chaveToken).commit()) throw IOException()
    }

    private fun obterChave(): SecretKey =
        keystore().getKey(ALIAS, null) as? SecretKey ?: KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, PROVEDOR
        ).run {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }

    private fun keystore() = KeyStore.getInstance(PROVEDOR).apply { load(null) }

    private fun codificar(dados: ByteArray) = Base64.encodeToString(dados, Base64.NO_WRAP)

    private fun decodificar(dados: String) = Base64.decode(dados, Base64.NO_WRAP)

    private companion object {
        const val PROVEDOR = "AndroidKeyStore"
        const val ALIAS = "hub.sessao"
        const val CIFRA = "AES/GCM/NoPadding"
    }
}
