package com.example.data.api

import com.example.data.model.ChatSession
import com.example.data.model.CreateMemoryRequest
import com.example.data.model.HealthResponse
import com.example.data.model.MemoryItem
import com.example.data.model.PinChatRequest
import com.example.data.model.RenameChatRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NextAiApiService {

    @GET(".")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("history")
    suspend fun getHistory(): Response<List<ChatSession>>

    @POST("chat/rename/{chatId}")
    suspend fun renameChat(
        @Path("chatId") chatId: String,
        @Body body: RenameChatRequest
    ): Response<Unit>

    @POST("rename")
    suspend fun renameChatFallback(
        @Body body: Map<String, String>
    ): Response<Unit>

    @DELETE("chat/{chatId}")
    suspend fun deleteChat(
        @Path("chatId") chatId: String
    ): Response<Unit>

    @DELETE("history/{chatId}")
    suspend fun deleteHistoryChat(
        @Path("chatId") chatId: String
    ): Response<Unit>

    @POST("chat/pin/{chatId}")
    suspend fun pinChat(
        @Path("chatId") chatId: String,
        @Body body: PinChatRequest
    ): Response<Unit>

    @GET("memory")
    suspend fun getMemories(): Response<List<MemoryItem>>

    @POST("memory")
    suspend fun createMemory(
        @Body body: CreateMemoryRequest
    ): Response<MemoryItem>

    @DELETE("memory/{id}")
    suspend fun deleteMemory(
        @Path("id") id: String
    ): Response<Unit>
}
