package calculator

import java.util.Scanner
import java.util.stream.Collectors

fun main() {
    val scanner = Scanner(System.`in`)
    do {
        val line = scanner.nextLine()
        when (line) {
            "/help" -> println("The program calculates the sum and difference of numbers")
            "/exit" -> println("Bye!")
            else -> {
                if (line.isNotEmpty() && line.first() == '/') println("Unknown command")
                else try {
                    Scanner(line)
                        .tokens()
                        .collect(Collectors.toList())
                        .reduceWithSignOrNull { sum, sign, it -> sum + sign * it }
                        ?.let { println(it) }
                } catch (e: Exception) {
                    println("Invalid expression")
                }
            }
        }
    } while(line != "/exit")
}

fun Iterable<String>.reduceWithSignOrNull(operation: (acc: Int, sign: Int, Int) -> Int): Int? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) return null
    var accumulator: Int = iterator.next().parseToInt()
    while (iterator.hasNext()) {
        val sign = iterator.next().foldToSign()
        accumulator = operation(accumulator, sign, iterator.next().parseToInt())
    }
    return accumulator
}

fun String.parseToInt(): Int {
    return if (first() == '+') substring(1).toInt() else toInt()
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