package calculator

import java.util.Scanner
import java.util.stream.Collectors

fun main() {
    val scanner = Scanner(System.`in`)
    do {
        val line = scanner.nextLine()
        //println("'$line'")
        when (line) {
            "/help" -> println("The program calculates the sum and difference of numbers")
            "/exit" -> println("Bye!")
            else -> Scanner(line)
                    .tokens()
                    //.map(String::toInt)
                    .collect(Collectors.toList())
                    .reduceWithSignOrNull { sum, sign, it -> sum + sign * it }
                    ?.let { println(it) }
        }
    } while(line != "/exit")
}

fun Iterable<String>.reduceWithSignOrNull(operation: (acc: Int, sign: Int, Int) -> Int): Int? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) return null
    var accumulator: Int = iterator.next().toInt()
    while (iterator.hasNext()) {
        val sign = iterator.next().foldToSign()
        accumulator = operation(accumulator, sign, iterator.next().toInt())
    }
    return accumulator
}

fun String.foldToSign(): Int {
    var sign = 1
    for (c in this) {
        sign *= when(c) {
            '+' -> 1
            '-' -> -1
            else -> throw IllegalArgumentException()
        }
    }
    return sign
}