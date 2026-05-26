package com.autonomous.phone.script

object ScriptManager {
    val scripts = mutableListOf<Script>()
    
    init {
        loadDefaultScripts()
    }
    
    fun addScript(script: Script) {
        scripts.add(script)
    }
    
    fun removeScript(id: String) {
        scripts.removeAll { it.id == id }
    }
    
    fun getScript(id: String): Script? {
        return scripts.firstOrNull { it.id == id }
    }
    
    fun getAllScripts(): List<Script> {
        return scripts.toList()
    }
    
    private fun loadDefaultScripts() {
        scripts.add(
            Script(
                id = "auto_douyin",
                name = "自动刷抖音",
                description = "自动浏览抖音，每3秒下滑一次",
                actions = buildList {
                    repeat(100) {
                        add(ScrollDownAction())
                        add(DelayAction(3000))
                    }
                }
            )
        )
        
        scripts.add(
            Script(
                id = "open_settings",
                name = "打开设置",
                description = "演示序列：返回首页 -> 下拉快捷设置",
                actions = listOf(
                    PressHomeAction(),
                    DelayAction(1000),
                    PressQuickSettingsAction()
                )
            )
        )
        
        scripts.add(
            Script(
                id = "demo_sequence",
                name = "演示序列",
                description = "简单的演示操作序列",
                actions = listOf(
                    PressHomeAction(),
                    DelayAction(1500),
                    ScrollDownAction(),
                    DelayAction(1000),
                    ScrollUpAction(),
                    DelayAction(1000),
                    PressBackAction()
                )
            )
        )
    }
}
