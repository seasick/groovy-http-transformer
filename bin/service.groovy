#!/usr/bin/env groovy

// https://issues.apache.org/jira/browse/GROOVY-9432
@GrabConfig(systemClassLoader=true)
@Grab('info.picocli:picocli:4.2.0')

import com.sun.net.httpserver.HttpServer
import groovy.cli.picocli.CliBuilder
import server.routes.Endpoints
import server.exceptions.*

// Define what arguments this script accepts.
def cli = new CliBuilder(usage: 'service.groovy --port <port>')
cli.with {
    p longOpt: 'port', args: 1, argName: 'port', required: true, 'Listening port'
    d longOpt: 'configpath', args: 1, argName: 'configpath', required: true,
        defaultValue: 'config', 'Path to configuration directory'
}

// Parse the arguments
def options = cli.parse(args)

// If there aren't any options at all, return
if (!options) {
    println "No options given."
    System.exit(1)
    return
}

// Check if the port is numeric
if (!options.port || !options.port.isNumber()) {
    println "Port must be numeric."
    System.exit(1)
    return
}

// Create a http server
HttpServer.create(new InetSocketAddress(options.port as int), /*max backlog*/ 0).with {
    println "Server is listening on ${options.port}, hit Ctrl+C to exit."

    // Handler for endpoint requests
    def endpoints = new Endpoints(options.configpath)

    // Closure for logging http "events"
    def log =  { http, message = null ->
        try {
            def method = http.getRequestMethod()
            def remoteAddress = http.remoteAddress.hostName

            if (message) {
                println "[${remoteAddress}] ${method} ${http.requestURI}: ${message}"
            } else {
                println "[${remoteAddress}] ${method} ${http.requestURI}"
            }
        } catch (Exception e) {
            println e.message
        }
    }

    // Register route for reading configurations
    createContext('/endpoints') { http ->
        try {
            if (http.getRequestMethod() == 'GET') {
                endpoints.getConfigs(http)
            } else {
                throw new MethodNotAllowedException()
            }
        } catch (MethodNotAllowedException e) {
            log(http, 'Method not allowed')
            http.sendResponseHeaders(405, 0)
            http.responseBody.close()
        } catch (Exception e) {
            log(http, e.message)
            http.sendResponseHeaders(500, 0)
            http.responseBody.close()
        }
    }

    // Register route for handling endpoints
    createContext('/endpoints/') { http ->
        try {
            endpoints.handle(http)
        } catch (MethodNotAllowedException e) {
            log(http, 'Method not allowed')
            http.sendResponseHeaders(405, 0)
            http.responseBody.close()
        } catch (Exception e) {
            log(http, e.message)
            http.sendResponseHeaders(500, 0)
            http.responseBody.close()
        }
    }

    // Catchall path
    createContext('/') { http ->
        http.responseHeaders.add('Content-type', 'text/plain')
        http.sendResponseHeaders(200, 0)
        http.responseBody.withWriter { out ->
            out << "Hello ${http.remoteAddress.hostName}!\n"
        }

        log(http)
    }

    // Start the http server
    start()
}
