# groovy-http-transformer

This project is supposed to help me learn and understand _Groovy_.

## Learning goals

- deal with incoming http requests
- read and write to/from a database
- read and write to/from file system

## What should it do?

It should be a HTTP service which transforms messages between different services which would otherwise not be able to communicate.

This service a distinct endpoint for each configuration (which are stored on fs) to receive messages, transform then and send them to a configured recipient.

