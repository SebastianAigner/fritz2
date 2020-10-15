package dev.fritz2

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import io.ktor.util.*
import org.slf4j.event.Level
import java.util.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

typealias Json = Map<String, Any>

object CRUDRepo {
    private const val idKey = "_id"
    private val store: MutableMap<String, Json> = mutableMapOf()

    fun create(json: Json): Json {
        val id = UUID.randomUUID().toString()
        val new = json.toMutableMap()
        new[idKey] = id
        store[id] = new
        return new
    }

    fun read() =
        store.values.toList()

    fun read(id: String) = store[id]

    fun update(id: String, json: Json): Json {
        val updated = json.toMutableMap()
        updated[idKey] = id
        store[id] = updated
        return json
    }

    fun delete(id: String) {
        store.remove(id)
    }

    fun clear() {
        store.clear()
    }

    override fun toString(): String {
        return store.toString()
    }
}

@KtorExperimentalAPI
fun Application.main() {

    install(CallLogging) {
        level = Level.INFO
    }

    install(ContentNegotiation) {
        jackson()
    }

    install(Authentication) {
        basic("auth") {
            realm = "Authenticated"
            validate { if (it.name == "test" && it.password == "password") UserIdPrincipal(it.name) else null }
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        method(HttpMethod.Head)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.Accept)
        header(HttpHeaders.CacheControl)
        header("test")
        anyHost()
        host("localhost")
        allowXHttpMethodOverride()
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }

    routing {

        get("/") {
            call.respondText("Test Server is running...", contentType = ContentType.Text.Plain)
        }

        // RESTFul API
        route("/rest") {
            get {
                val body = CRUDRepo.read()
                log.info("GET: $body")
                call.respond(body)
            }
            get("{id}") {
                val id = call.parameters["id"] ?: throw MissingRequestParameterException("id")
                val json = CRUDRepo.read(id) ?: throw NotFoundException("item with id=$id not found")
                log.info("GET: id=$id; json=$json")
                call.respond(json)
            }
            post {
                val body = call.receive<Json>()
                log.info("POST: $body")
                call.respond(CRUDRepo.create(body))
            }
            put("{id}") {
                val id = call.parameters["id"] ?: throw MissingRequestParameterException("id")
                val body = call.receive<Json>()
                log.info("PUT: $id; $body")
                call.respond(CRUDRepo.update(id, body))
            }
            delete("{id}") {
                val id = call.parameters["id"] ?: throw MissingRequestParameterException("id")
                CRUDRepo.delete(id)
                call.response.status(HttpStatusCode.OK)
            }
            get("/clear") {
                CRUDRepo.clear()
                call.response.status(HttpStatusCode.OK)
            }
        }

        // Remote Basics
        route("test") {
            get("/get") {
                call.respondText("GET")
            }
            delete("/delete") {
                call.respondText("DELETE")
            }
            patch("/patch") {
                call.respondText("PATCH")
            }
            post("/post") {
                call.respondText("POST")
            }
            put("/put") {
                call.respondText("PUT")
            }
            head("/head") {
                call.respondText("HEAD")
            }
            get("/status/{code}") {
                val code = call.parameters["code"] ?: throw MissingRequestParameterException("code")
                call.respond(HttpStatusCode.fromValue(code.toInt()))
            }
            authenticate("auth") {
                get("/basicAuth") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    call.respondText("Hello ${principal.name}")
                }
            }
            get("/headers") {
                call.respond(call.request.headers.toMap())
            }
        }
    }
}