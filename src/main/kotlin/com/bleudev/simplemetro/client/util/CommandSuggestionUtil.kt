package com.bleudev.simplemetro.client.util

private fun checkCommand(input: String, vararg commands: String): Boolean {
    val input2 = input.split(" ")
    if (input2.size == commands.size) return false
    for ((inp, com) in input2.zip(commands)) {
        if (com != "%" && inp != com) {
            return false
        }
    }
    return true
}

// /m
fun shouldForceRenderTooltip(input: String): Boolean {
    var input2 = input.removePrefix("/m") // /m
    if (input == input2) return false
    input2 = input2.removePrefix("etro").removePrefix(" ") // /metro
    return checkCommand(input2, "station") ||
        checkCommand(input2, "station", "remove") ||
        checkCommand(input2, "station", "%", "place") ||
        checkCommand(input2, "station", "%", "place", "add", "corner") ||
        checkCommand(input2, "station", "%", "place", "add", "between", "%") ||
        checkCommand(input2, "line")
}
