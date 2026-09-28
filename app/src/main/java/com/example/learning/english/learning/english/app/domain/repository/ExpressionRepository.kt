package com.example.learning.english.learning.english.app.domain.repository

import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.PackId
import kotlinx.coroutines.flow.Flow

interface ExpressionRepository {
    fun observeAll(): Flow<List<Expression>>

    fun observeByPack(packId: PackId): Flow<List<Expression>>

    fun observeById(expressionId: ExpressionId): Flow<Expression?>

    suspend fun getById(expressionId: ExpressionId): Expression?

    suspend fun getAll(): List<Expression>

    suspend fun upsertAll(expressions: List<Expression>)
}
