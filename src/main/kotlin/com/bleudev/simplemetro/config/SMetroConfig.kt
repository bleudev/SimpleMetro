package com.bleudev.simplemetro.config

import com.bleudev.simplemetro.obj.MetroLine
import com.bleudev.simplemetro.obj.MetroStation

data class SMetroConfig(val stations: List<MetroStation>, val lines: List<MetroLine>) {
    companion object {
        fun getInstance() {

        }
    }
}