Домашнее задание

монадический парсер csv файлов с использованием scala3

Цель:

Надо написать парсер csv файла (слова в строках разделены ,; итд в зависимости от настроек локализации ОС). Этот парсер обсуждался в занятии "Библиотека коллекций"
Но теперь надо испольовать новый синтаксис scala3 где это возможно,
например
val result = str.split('\n').map(parser.parse)
разделитель строки передавайте как контекст, а не как константу. Тоесть парсер надо завернуть в класс с given параметром
как то:
class ParserWithGivenParam(using splitter: String):
...


Описание/Пошаговая инструкция выполнения домашнего задания:

• посмотреть в занятии "Библиотека коллекций" и вспомнить что такое монадический парсер

• обернуть его (он уже был написан в ходе того урока) в класс с given параметром

• использовать по максимуму конструкции scala3 при переписывании этого парсера

• https://habr.com/ru/post/326002/

• https://docs.scala-lang.org/scala3/book/introduction.html


Критерии оценки:

• структурированность кода

• наличие конструкций scala3

• монадический парсер действительно должен работать

