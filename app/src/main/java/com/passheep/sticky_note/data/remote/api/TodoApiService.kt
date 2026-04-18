package com.passheep.sticky_note.data.remote.api

import com.passheep.sticky_note.data.network.ApiEnvelope
import com.passheep.sticky_note.data.remote.dto.CreateTodoRequest
import com.passheep.sticky_note.data.remote.dto.RemoteTodoDto
import com.passheep.sticky_note.data.remote.dto.UpdateTodoRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TodoApiService {

    @GET("/open/v1/todos")
    suspend fun getTodos(
        @Query("status") status: Int? = null,
        @Query("deviceId") deviceId: String? = null,
    ): ApiEnvelope<List<RemoteTodoDto>>

    @POST("/open/v1/todos")
    suspend fun createTodo(
        @Body request: CreateTodoRequest,
    ): ApiEnvelope<RemoteTodoDto>

    @PUT("/open/v1/todos/{id}")
    suspend fun updateTodo(
        @Path("id") id: Long,
        @Body request: UpdateTodoRequest,
    ): ApiEnvelope<RemoteTodoDto>

    @PUT("/open/v1/todos/{id}/complete")
    suspend fun toggleComplete(
        @Path("id") id: Long,
    ): ApiEnvelope<Unit>

    @DELETE("/open/v1/todos/{id}")
    suspend fun deleteTodo(
        @Path("id") id: Long,
    ): ApiEnvelope<Unit>
}

