package com.memebattle.pwc.domain.model

abstract class PwcDbModel<Key> {
    abstract fun getKey(): Key
}