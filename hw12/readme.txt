Домашнее задание

Доработка Akka Stream pipeline для Read Side

Цель:

В проекте https://github.com/vadopolski/cqs-akka доработать сторону чтения TypedCalculatorReadSide, чтобы разобраться с типами компонентов Akka Strems


Описание/Пошаговая инструкция выполнения домашнего задания:

В read side приложения с архитектурой CQRS (объект TypedCalculatorReadSide в TypedCalculatorReadAndWriteSide.scala) необходимо разделить чтение событий, бизнес логику и запись в целевой получатель и сделать их асинхронными, т.е.
вместо CalculatorRepository создать Sink c любой БД (например Postgres из docker-compose файла).
Для последнего задания пригодится документация - https://doc.akka.io/docs/alpakka/current/slick.html#using-a-slick-flow-or-sink Результат выполненного д.з. необходимо оформить либо на github gist либо PR к текущему репозиторию.


Критерии оценки:

"Принято" - задание выполнено полностью
"Возвращено на доработку" - задание не выполнено полностью