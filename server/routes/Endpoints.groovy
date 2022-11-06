package server.routes

import static groovy.io.FileType.FILES
import com.sun.net.httpserver.HttpExchange
import groovy.json.JsonSlurper
import server.exceptions.NotFoundException


class Endpoints {

    groovy.text.SimpleTemplateEngine templateEngine = new groovy.text.SimpleTemplateEngine()
    JsonSlurper jsonSlurper = new JsonSlurper()
    String configpath

    Endpoints(String configpath) {
        this.configpath = configpath
    }

    Map decodeQuery(String query) {
        // get all query params as list
        String[] queryParams = query?.split('&') // safe operator for urls without query params

        // transform the params list to a Map spliting
        // each query param
        Map mapParams = queryParams.collectEntries { param ->
            param.split('=').collect { value ->
                URLDecoder.decode(value)
            }
        }

        return mapParams
    }

    String template(String template, Map params) {
        return templateEngine.createTemplate(template).make(params.collectEntries { key, value ->
            return [key, URLEncoder.encode(value)]
        }).toString()
    }

    def getConfigs() {
        File dir = new File(this.configpath + '/endpoints')

        def configs = []

        dir.eachFile(FILES) { file ->
            configs << jsonSlurper.parse(file)
        }

        return configs
    }

    Map handle(HttpExchange http) {
        String endpoint = http.requestURI.path.replace('/endpoints/', '')
        String configfilepath = this.configpath + '/endpoints/' + endpoint + '.json'
        File configfile = new File(configfilepath) // TODO Prevent directory traversal

        // First check if there is a configuration for the given requestUri
        if (!configfile.exists()) {
            throw new NotFoundException('Config does not exist')
        }

        // Load the configuration
        Map config = jsonSlurper.parse(configfile)
        Map query = decodeQuery(http.requestURI.query)
        Map input = [:]

        // Parse the input from the incoming request according to config
        config.input.each { inputParam ->

            if (inputParam.from) {
                if (inputParam.from.type == 'query') {
                    input[inputParam.name] = query[inputParam.from.name]
                }
            }
        }

        // Execute the call to the receiver
        Map receiver = config.receiver
        String receiverPath = template(receiver.path, input)
        String receiverUrl = "${receiver.protocol}://${receiver.host}:${receiver.port}/${receiverPath}"

        def getUrl = new URL(receiverUrl)
        URLConnection get = getUrl.openConnection()


        if (get.responseCode == 200) {
            // Transform application/json to Groovy data structures
            if (get.contentType == 'application/json') {
                return jsonSlurper.parseText(get.inputStream.text)
            }

            return [
                result: get.inputStream.text
            ]
        } else {
            throw new Exception("ResponseCode was not 200 (${get.responseCode})")
        }
    }
}
