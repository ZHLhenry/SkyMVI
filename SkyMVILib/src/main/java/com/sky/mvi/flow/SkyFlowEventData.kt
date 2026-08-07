package com.sky.mvi.flow

/**
 * @Class: SkyFlowEventData
 * @Author: Henry
 * @Date: 2025/3/5 11:11
 * @Description: SkyFlow事件数据模型，包含事件Key和Value
 */

data class SkyFlowEventData(
    var eventKey: String,
    var eventValue: String
)
