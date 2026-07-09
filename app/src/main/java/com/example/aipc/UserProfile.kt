package com.example.aipc

data class UserProfile(
    var userName: String = "玩家",           // 用户名字（优先级高于设置中的名字）
    var gender: String = "保密",             // 男/女/保密
    var language: String = "Chinese",        // Chinese / English
    var mode: String = "纯净",               // 纯净、微黄、黄、血腥、亲情
    var storyType: String = "真实",          // 真实 / 为所欲为
    var favorability: Int = 0,               // 好感度 (0-100)
    var loveValue: Int = 0,                  // 爱意值 (0-100)
    var knownInfo: String = "",              // 已知信息 (文本)
    var keyMemories: String = ""             // 关键记忆点 (文本)
)