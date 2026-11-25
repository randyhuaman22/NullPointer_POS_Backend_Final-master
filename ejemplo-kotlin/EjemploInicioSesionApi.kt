// Ejemplo ilustrativo para shared/commonMain de Kotlin Multiplatform con Ktor.
// No forma parte de una aplicación completa. Ajusta las dependencias a tu proyecto KMP.
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SolicitudInicioSesion(
    val codigoTrabajador: String,
    val clave: String
)

@Serializable
data class UsuarioSesion(
    val idUsuario: Long,
    val codigoTrabajador: String,
    val nombreCompleto: String,
    val rol: String
)

@Serializable
data class RespuestaInicioSesion(
    val token: String,
    val tipoToken: String,
    val expiraEnSegundos: Long,
    val usuario: UsuarioSesion
)

val clienteHttpPos = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

suspend fun iniciarSesionMozo(
    urlBase: String,
    codigoTrabajador: String,
    clave: String
): RespuestaInicioSesion =
    clienteHttpPos.post("$urlBase/api/v1/autenticacion/iniciar-sesion") {
        contentType(ContentType.Application.Json)
        setBody(SolicitudInicioSesion(codigoTrabajador, clave))
    }.body()

// PC/Postman: urlBase = "http://localhost:8080"
// Emulador Android: urlBase = "http://10.0.2.2:8080"
// Celular físico: urlBase = "http://IP_LOCAL_DE_LA_PC:8080"
// Después del inicio de sesión, envía Authorization: Bearer <respuesta.token>
