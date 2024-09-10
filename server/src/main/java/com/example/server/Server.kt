package com.example.server

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.files
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticRootFolder
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File

class Server (
    private val portNumber: Int = 10_000,
    private val hostingFolder : File,
    private val indexHtml :String
) {
    private val server = embeddedServer(Netty, port = portNumber){
        routing {
            // 返回一个包含 Web 套接字和视频播放器的简单 HTML
            get("/") {
                call.respondText(indexHtml, ContentType.parse("text/html"))
            }
            static {
                staticRootFolder = hostingFolder
                files(hostingFolder)
            }

        }
    }
    fun startServer(){
        server.start()
    }

    fun stopServer(){
        server.stop()
    }

}