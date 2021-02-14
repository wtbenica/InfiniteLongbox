package com.wtb.comiccollector

class Scratch {
}

open class Bob(var num: Int)

class And : Bob(8)

class But : Bob(7)

fun getBob(h: Boolean) : Bob {
    when (h) {
       true -> return And()
       false -> return But()
    }
}

