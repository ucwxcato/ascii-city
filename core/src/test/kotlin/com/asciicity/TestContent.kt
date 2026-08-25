package com.asciicity

import com.asciicity.content.ContentLoader
import com.asciicity.content.LoadedContent
import java.nio.file.Path

object TestContent {
    val loaded: LoadedContent by lazy {
        ContentLoader().load(Path.of("."), "city-alpha")
    }
}
