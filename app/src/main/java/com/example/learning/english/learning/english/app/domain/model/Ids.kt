package com.example.learning.english.learning.english.app.domain.model

/** Identificadores de dominio. Se usan strings para que el seed JSON sea idempotente. */

@JvmInline
value class ExpressionId(val value: String)

@JvmInline
value class ExampleId(val value: String)

@JvmInline
value class PatternId(val value: String)

@JvmInline
value class TagId(val value: String)

@JvmInline
value class PackId(val value: String)

@JvmInline
value class SessionId(val value: String)

@JvmInline
value class ReviewId(val value: String)
