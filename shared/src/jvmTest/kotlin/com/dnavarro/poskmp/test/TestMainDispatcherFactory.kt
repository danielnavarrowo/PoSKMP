package com.dnavarro.poskmp.test

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.internal.MainDispatcherFactory
import kotlin.coroutines.CoroutineContext

class TestMainDispatcher : MainCoroutineDispatcher() {
    override val immediate: MainCoroutineDispatcher get() = this
    override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
}

@OptIn(InternalCoroutinesApi::class)
class TestMainDispatcherFactory : MainDispatcherFactory {
    override val loadPriority: Int get() = Int.MAX_VALUE
    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher = TestMainDispatcher()
    override fun hintOnError(): String? = null
}
