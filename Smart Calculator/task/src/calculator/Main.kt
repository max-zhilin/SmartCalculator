package calculator

import java.math.BigInteger
import java.util.Scanner

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
                val tokensList = tokenize(line)
                if (tokensList.size >= 2 && tokensList[1].type == TokenType.DELIMITER && tokensList[1].value == "=") {
                    val identifier = tokensList.first().value
                    val expression = Parser(tokensList.subList(2, tokensList.size)).parseToRPN()
                    val value = calc.evaluateExpression(expression)
                    calc.assignVariable(identifier, value)
                } else {
                    val expression = Parser(tokensList).parseToRPN()
                    val value = calc.evaluateExpression(expression)
                    println(value)
                }
            } catch (e: MyException) {
                println(e.message)
            }
    } while(line != "/exit")
}

class Calculator {

    private val map = mutableMapOf<String, BigInteger>()

    fun evaluateExpression(expression: List<Element>): BigInteger {
        val iterator = expression.iterator()
        val stack = Stack<BigInteger>()

        try {
            while (iterator.hasNext()) {
                when (val element = iterator.next()) {
                    is Element.Number -> stack.push(element.value)
                    is Element.Identifier -> stack.push(eval(element.name))
                    is Element.Negate -> stack.push( - stack.pop())
                    is Element.Minus -> {
                        val second = stack.pop() // second is first
                        stack.push(stack.pop() - second)
                    }
                    is Element.Plus -> stack.push(stack.pop() + stack.pop())
                    is Element.Times -> stack.push(stack.pop() * stack.pop())
                    is Element.Div -> {
                        val second = stack.pop() // second is first
                        stack.push(stack.pop() / second)
                    }
                    is Element.Power -> {
                        val second = stack.pop() // second is first
                        stack.push(pow(stack.pop(), second))
                    }
                    is Element.Parenthesis -> throw InvalidExpression() // it will never strike, for exhaustive when only
                }
            }

            val result = stack.pop()

            if (stack.isEmpty())
                return result
            else throw InvalidExpression()

        } catch (e: UnknownVariable) {  // only this exception rises as is
            throw UnknownVariable()
        } catch (e: Exception) {        // all other ones convert to special exception
            throw InvalidExpression()
        }
    }

    private fun eval(identifier: String): BigInteger {
        return if (identifier.matches(Regex("[a-zA-Z]+"))) {
            map[identifier] ?: throw UnknownVariable()
        } else identifier.toBigInteger()
    }

    fun assignVariable(identifier: String, value: BigInteger) {
        if (!identifier.matches(Regex("[a-zA-Z]+"))) throw InvalidIdentifier()
        map[identifier] = value
    }
}

open class MyException(override val message: String): Exception(message)
class InvalidIdentifier : MyException("Invalid identifier")
class UnknownVariable : MyException("Unknown variable")
class InvalidExpression : MyException("Invalid expression")

class Stack<T> {
    private val list = mutableListOf<T>()

    fun push(element: T) {
        list.add(element)
    }

    fun pop(): T = list.removeAt(list.lastIndex)

    fun isEmpty() = list.isEmpty()
 
    fun onTop(): T = list[list.lastIndex]
}

enum class TokenType {
    IDENTIFIER, NUMBER, DELIMITER
}
class Token(val type: TokenType, val value: String)

fun tokenize(s: String) : List<Token> {
    val tokens = mutableListOf<Token>()

    val scanner = Scanner(s)
    scanner.useDelimiter("")    // read one char
    var eof = false // End of file
    var c = scanner.next().first()
    while (!eof) {

        when (c) {
            ' ' -> {
                if (scanner.hasNext()) c = scanner.next().first()
                else eof = true
            }
            in 'a'..'z', in 'A'..'Z' -> {
                var buffer = c.toString()
                do {
                    if (scanner.hasNext()) c = scanner.next().first()
                    else eof = true

                    val isLetter = !eof && c.isLetter()
                    if (isLetter)
                        buffer += c
                    else {
                        tokens.add(Token(TokenType.IDENTIFIER, buffer))
                    }
                } while (isLetter)
            }
            in '0'..'9' -> {
                var buffer = c.toString()
                do {
                    if (scanner.hasNext()) c = scanner.next().first()
                    else eof = true

                    val isDigit = !eof && c.isDigit()
                    if (isDigit)
                        buffer += c
                    else {
                        tokens.add(Token(TokenType.NUMBER, buffer))
                    }
                } while (isDigit)
                if (c.isLetter()) throw InvalidExpression()
            }
            '+','-' -> {
                var sign = c.toString()
                do {
                    if (scanner.hasNext()) c = scanner.next().first()
                    else eof = true

                    val isSign = !eof && (c == '+' || c == '-')
                    if (isSign) {
                        if (c == '-')
                            sign = if (sign == "-") "+" else "-"
                    }
                    else {
                        tokens.add(Token(TokenType.DELIMITER, sign))
                    }
                } while (isSign)
            }
            '=','*','/','(',')','^' -> {
                tokens.add(Token(TokenType.DELIMITER, c.toString()))
                if (scanner.hasNext()) c = scanner.next().first()
                else eof = true
            }
        }
    }

    return tokens
}

sealed class Element(val priority: Int) {
    class Identifier(val name: String) : Element(0)
    class Number(val value: BigInteger) : Element(0)
    object Parenthesis : Element(0)
    object Plus : Element(1)
    object Minus : Element(1)
    object Times : Element(2)
    object Div : Element(2)
    object Power : Element(3)
    object Negate : Element(4)
}

class Parser(tokens: List<Token>) {
    val iterator = tokens.iterator()
    private val stack = Stack<Element>()
    private val list = mutableListOf<Element>()
    fun parseToRPN(): List<Element> {

        var unaryAllowed = true // at start of after '('
        while (iterator.hasNext()) {
            val token = iterator.next()
            when(token.type) {
                TokenType.NUMBER -> list.add(Element.Number(token.value.toBigInteger()))
                TokenType.IDENTIFIER -> list.add(Element.Identifier(name = token.value))
                TokenType.DELIMITER -> {
                    when (token.value) {
                        "-" -> {
                            if (unaryAllowed)
                                stack.push(Element.Negate)
                            else {
                                popByPriority(1)
                                stack.push(Element.Minus)
                            }
                        }
                        "+" -> {
                            popByPriority(1)
                            stack.push(Element.Plus)
                        }
                        "*" -> {
                            popByPriority(2)
                            stack.push(Element.Times)
                        }
                        "/" -> {
                            popByPriority(2)
                            stack.push(Element.Div)
                        }
                        "^" -> {
                            popByPriority(3)
                            stack.push(Element.Power)
                        }
                        "(" -> {
                            stack.push(Element.Parenthesis)
                        }
                        ")" -> {
                            popByParenthesis()
                        }
                        else -> throw InvalidExpression()
                    }
                }
            }
            unaryAllowed = token.type == TokenType.DELIMITER && token.value == "("
        }
        while (!stack.isEmpty()) {
            if (stack.onTop() != Element.Parenthesis)
                list.add(stack.pop())
            else throw InvalidExpression()
        }

        return list
    }

    private fun popByPriority(priority: Int) {
        while (!stack.isEmpty() && stack.onTop() != Element.Parenthesis && stack.onTop().priority >= priority)
            list.add(stack.pop())
    }
    private fun popByParenthesis() {
        while (!stack.isEmpty() && stack.onTop() != Element.Parenthesis)
            list.add(stack.pop())

        if (!stack.isEmpty() && stack.onTop() == Element.Parenthesis)
            stack.pop()
        else throw InvalidExpression()
    }
}

fun pow(n: BigInteger, exp: BigInteger): BigInteger {
    return n.pow(exp.toInt())
}