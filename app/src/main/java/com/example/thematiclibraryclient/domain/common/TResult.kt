package com.example.thematiclibraryclient.domain.common

sealed class TResult<out T, out V> (
    open val data: T? = null,
    open val exception: V? = null
) {
    data class Success<out T, out V>(override val data: T) : TResult<T, V>()
    data class Error<out T,out V>(override val exception: V) : TResult<T,V>()
}