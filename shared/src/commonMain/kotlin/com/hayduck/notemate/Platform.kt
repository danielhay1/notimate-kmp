package com.hayduck.notemate

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform