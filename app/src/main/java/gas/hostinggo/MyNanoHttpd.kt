package gas.hostinggo

import fi.iki.elonen.NanoHTTPD
import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MyNanoHttpd(port: Int, private val rootDir: File) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession?): Response {
        if (session?.method == Method.POST && session.uri == "/register") {
            return handleRegister(session)
        }

        val uri = session?.uri?.removePrefix("/") ?: "login.html"
        val file = File(rootDir, uri)

        return if (file.exists()) {
            NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(uri), file.readText())
        } else {
            NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
        }
    }

    private fun getMimeType(uri: String): String {
        return when {
            uri.endsWith(".html") -> "text/html"
            uri.endsWith(".css") -> "text/css"
            uri.endsWith(".js") -> "application/javascript"
            else -> "text/plain"
        }
    }

    @Serializable
    data class User(val username: String, val password: String)

    private fun handleRegister(session: IHTTPSession): Response {
        return try {
            val postData = mutableMapOf<String, String>()
            session.parseBody(postData)

            val json = postData["postData"] ?: return NanoHTTPD.newFixedLengthResponse(
                Response.Status.BAD_REQUEST, "text/plain", "Invalid Data"
            )

            val user = Json.decodeFromString<User>(json) 
            val username = user.username.trim()
            val password = user.password.trim()

            if (username.isEmpty() || password.isEmpty()) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Username dan Password tidak boleh kosong!")
            }

            val userDir = File(rootDir, username)
            if (userDir.exists()) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.CONFLICT, "text/plain", "Username sudah digunakan!")
            }

            userDir.mkdirs() 

            val userFile = File(userDir, "$username.txt")
            userFile.outputStream().use { it.write("$username:$password".toByteArray()) } 

            listOf("beranda.html", "beranda.js", "beranda.css").forEach { fileName ->
                val sourceFile = File(rootDir, fileName)
                val destFile = File(userDir, fileName)
                if (sourceFile.exists()) {
                    sourceFile.copyTo(destFile, overwrite = true)
                }
            }

            val responseJson = Json.encodeToString(mapOf("status" to "OK", "message" to "Pendaftaran berhasil!"))
            NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", responseJson)

        } catch (e: Exception) {
            NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server Error: ${e.message}")
        }
    }
    
    private fun handleLogin(session: IHTTPSession): Response {
    try {
        val params = session.parameters
        val username = params["username"]?.firstOrNull()?.trim() ?: return errorResponse("Username tidak boleh kosong!")
        val password = params["password"]?.firstOrNull()?.trim() ?: return errorResponse("Password tidak boleh kosong!")

        val userDir = File(rootDir, username)
        val userFile = File(userDir, "$username.txt")

        if (!userFile.exists()) {
            return errorResponse("Username atau Password salah!")
        }

        val savedData = userFile.readText().split(":")
        if (savedData.size < 2 || savedData[0] != username || savedData[1] != password) {
            return errorResponse("Username atau Password salah!")
        }

        val responseJson = Json.encodeToString(mapOf("success" to true, "redirect" to "/$username/beranda.html"))
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", responseJson)

    } catch (e: Exception) {
        return errorResponse("Server Error: ${e.message}")
    }
}

private fun errorResponse(message: String): Response {
    val responseJson = Json.encodeToString(mapOf("success" to false, "message" to message))
    return NanoHTTPD.newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", responseJson)
}
}