https://groovy-lang.org/documentation.html#gettingstarted

Started with installing groovy: https://groovy-lang.org/install.html

Then https://groovy-lang.org/differences.html
 - _holy shit, am I able to do this?_
 - _yes, I am, I've been here before_
 - closed it because I don't have ~~a lot~~ any java knowledge and shouldn't mud
   my groovy experience

Then https://groovy-lang.org/groovy-dev-kit.html

Then https://groovy-lang.org/metaprogramming.html
 - stopped shortly after `Categories` (extending classes you don't control with
   additional methods), I have to revisit this.

# Creating a http server

Started searching for examples of http server implementation, tried both examples
from https://gist.github.com/renatoathaydes/8ad276cedd515f8ff5fc. Second one worked
out of the box.

# Parse CLI arguments

Looking for solutions to parse cli arguments.

Looked into https://opensource.com/article/21/8/parsing-command-options-groovy,
but against what they were writing, `groovy.cli.picocli.CliBuilder` is not imported by default.

Found https://issues.apache.org/jira/browse/GROOVY-9432 which explains how to work
around it. It seems not even they know why the workaround is needed.

After failing at using the annotations of PicoCli, I fiddled with the requirements of `CliBuilder` and added required fields and usage.

Next up was converting a string `"8080"` to an integer.

At this point the server is echoing the ip address of the requestee.

# VSCode

While looking into parsing cli arguments and trying importing the picocli dependency, I switched to looking into groovy vscode integration. Found `marlon407.code-groovy` and `NicolasVuillamy.vscode-groovy-lint`.

Linter immideatly has a few things to warn me about. Why shouldn't def be used for declaration?

# Digging into the http server

- why is my server not printing anything. Seems that the print statement throws an error, but is caught somewhere.
  - `http.remoteAddress.holder` does not exist and an error is thrown. No idea where it is catched though.