
## Instructions
Przygotowanie prostej aplikacji:
- załadowanie pliku csv, dowolnej długości (liczby w kolejnych wierszach)
- podział streamu na x mniejszych -> liczba z pliku % (operacja modulo) x
- dodajemy liczby w każdym streamie do siebie i wrzucamy na kolejkę (Kafka, RabbitMq)
- dodatkowo websocket gdzie możemy się zasubskrybować na dany dzielnik [0,1 ... x-1]

Wymagania:
- Zadanie udostępnione na Github
- swagger do ws (tapir)
- Docker compose
- Scala 2, fs2 + cats-effect
- dodatkowo tapir dla ws, http4s - na potrzeby ostatniego punktu (websocket)


## How to run
1. `sbt docker:publishLocal`
2. `docker-compose up`
3. App is running if you see message similar to this: 
```
   iceo-task-iceo-task-1  | [io-compute-1] INFO  o.h.e.s.EmberServerBuilderCompanionPlatform - Ember-Server service bound to address: 0.0.0.0:8080
```
4. Use some websocket client to connect i.e. wscat: `wscat -c ws://localhost:8080/count`, send some number to trigger processing
5. You can inspect rabbitmq at `http://localhost:15672` (login/pw: guest/guest), get messages for queue `testQueue`
6. If you want to modify the csv, either mount different path in docker-compose.yml file or alter/replace the data.csv file

## Limitations
1. Swagger is generated from the routes and available under `localhost:8080/docs`, but the problem is that Swagger doesn't support websockets, so you have to use some external client

## Confusing parts
The splitting part in instruction was unclear, I divided it in parts of size x
or you expected me to create chunk when element matches n % x == 0? (so use split(_ % x == 0))?

Anyway, we can talk about it and maybe modify it live.
It would be easier to understand with some example.
