package com.autonomous.phone.script

data class Script(
    val id: String,
    val name: String,
    val description: String = "",
    val actions: List<Action>,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun execute() {
        // Execution will be handled by ScriptExecutor
    }
}
