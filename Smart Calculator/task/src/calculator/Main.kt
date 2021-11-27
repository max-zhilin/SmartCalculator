package calculator

import java.util.Scanner
import java.util.stream.Collectors

fun main() {
    val calc = Calculator()
    val scanner = Scanner(System.`in`)
    do {
        val line = scanner.nextLine()
        if (line.isBlank()) continue
        else if (line.first() == '/') {
            when (line) {
                "/help" -> println("The program calculates the sum and difference of numbers")
                "/exit" -> println("Bye!")
                else -> println("Unknown command")
            }
        } else
            try {
                if (line.contains('=')) {
                    val assignment = line.split('=', limit = 2)
                    val (identifier, expression) = assignment
                    calc.assignExpression(identifier, expression)
                } else {
                    println(calc.evaluateExpression(line))
                }
            } catch (e: MyException) {
                println(e.message)
            }
    } while(line != "/exit")
}

class Calculator {

    private val map = mutableMapOf<String, Int>()

    fun evaluateExpression(expression: String): Int {
        require(expression.isNotBlank())    // it is not necessary, but let it be for clearance

        val iterator = Scanner(expression)
            .tokens()   // are strings with no whitespaces
            .collect(Collectors.toList())
            .iterator()

        try {
            var accumulator: Int = evalWithUnarySign(iterator.next())

            while (iterator.hasNext()) {
                val sign = iterator.next().foldToSign()
                accumulator += sign * evalWithUnarySign(iterator.next())
            }

            return accumulator

        } catch (e: UnknownVariable) {  // only this exception rises as is
            throw UnknownVariable()
        } catch (e: Exception) {        // all other ones convert to special exception
            throw InvalidExpression()
        }
    }

    private fun evalWithUnarySign(token: String): Int {
        return when (token.first()) {
            '+' -> eval(token.drop(1))
            '-' -> -eval(token.drop(1))
            else -> eval(token)
        }
    }

    private fun eval(token: String): Int {
        return if (token.matches(Regex("[a-zA-Z]+"))) {
            map[token] ?: throw UnknownVariable()
        } else token.toInt()
    }

    fun assignExpression(identifier: String, expression: String) {
        val identifierName = identifier.trim()
        if (!identifierName.matches(Regex("[a-zA-Z]+"))) throw InvalidIdentifier()
        try {
            val value = evaluateExpression(expression)
            map[identifierName] = value
        } catch (e: InvalidExpression) {
            throw InvalidAssignment()   // this exception is only from assignment
        }
    }
}

open class MyException(override val message: String): Exception(message)
class InvalidIdentifier : MyException("Invalid identifier")
class InvalidAssignment : MyException("Invalid assignment")
class UnknownVariable : MyException("Unknown variable")
class InvalidExpression : MyException("Invalid expression")

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