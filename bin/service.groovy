#!/usr/bin/env groovy

// https://issues.apache.org/jira/browse/GROOVY-9432
@GrabConfig(systemClassLoader=true)
@Grab('info.picocli:picocli:4.2.0')

import com.sun.net.httpserver.HttpServer
import groovy.cli.picocli.CliBuilder


def cli = new CliBuilder(usage: 'service.groovy --port <port>')

cli.with {
   p longOpt: 'port', args: 1, argName: 'port', required: true, 'Listening port'
}

def options = cli.parse(args)

if (!options) {
   return
}

if (!options.port || !options.port.isNumber()) {
    println "Port must be numeric"
    System.exit(1)
    return
}

HttpServer.create(new InetSocketAddress(options.port as int), /*max backlog*/ 0).with {
    println "Server is listening on ${options.port}, hit Ctrl+C to exit."

    createContext('/') { http ->
        http.responseHeaders.add('Content-type', 'text/plain')
        http.sendResponseHeaders(200, 0)
        http.responseBody.withWriter { out ->
            out << "Hello ${http.remoteAddress.hostName}!\n"
        }

        println "Hit from Host: ${http.remoteAddress.hostName} from port: ${http.remoteAddress.port}"
    }
    start()
}
