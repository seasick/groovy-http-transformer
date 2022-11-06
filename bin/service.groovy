#!/usr/bin/env groovy

// https://issues.apache.org/jira/browse/GROOVY-9432
@GrabConfig(systemClassLoader=true)
@Grab('info.picocli:picocli:4.2.0')

import com.sun.net.httpserver.HttpServer
import groovy.cli.picocli.CliBuilder
import groovy.json.JsonOutput
import server.routes.Endpoints
import server.exceptions.*

// Define what arguments this script accepts.
def cli = new CliBuilder(usage: 'service.groovy --port <port>')
cli.with {
    p longOpt: 'port', args: 1, argName: 'port', required: true, 'Listening port'
    d longOpt: 'configpath', args: 1, argName: 'configpath', required: true,
        defaultValue: 'config', 'Path to configuration directory'
    t longOpt: 'threads', args: 1, argName: 'threads', defaultValue: '10',
        'Number of threads used for processing http requests'
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
    println "Server is listening on ${options.port}"
    println "  threads ${options.threads}"
    println "  config path '${options.configpath}'"
    println "\nHit Ctrl+C to exit."

    setExecutor(java.util.concurrent.Executors.newFixedThreadPool(options.threads as int));

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

    // Wrapper to help handle exceptions, use various http status codes and take
    // care of response encoding.
    def requestWrapper = { http, Closure closure ->
        try {
            def result = closure()

            http.responseHeaders.add('Content-Type', 'application/json')
            http.sendResponseHeaders(200, 0)

            http.responseBody.withWriter { out ->
                out << JsonOutput.toJson(result)
            }

            log(http)
        } catch (HttpStatusException e) {
            log(http, e.message)
            http.sendResponseHeaders(e.code, 0)
            http.responseBody.close()
        } catch (Exception e) {
            log(http, e.message)
            http.sendResponseHeaders(500, 0)
            http.responseBody.close()
        }
    }

    // Register route for reading configurations
    createContext('/endpoints') { http ->
        requestWrapper(http) {
            if (http.requestMethod == 'GET') {
                return endpoints.getConfigs()
            } else {
                throw new MethodNotAllowedException("${http.requestMethod} is not allowed")
            }
        }
    }

    // Register route for handling endpoints
    createContext('/endpoints/') { http ->
        requestWrapper(http) {
            return endpoints.handle(http)
        }
    }

    // Catchall path
    createContext('/') { http ->
        requestWrapper(http) {
            if (http.requestURI.path != '/') {
                throw new NotFoundException("${http.requestURI.path} not found")
            }
            return "Hello ${http.remoteAddress.hostName}!\n"
        }
    }

    // Start the http server
    start()
}
