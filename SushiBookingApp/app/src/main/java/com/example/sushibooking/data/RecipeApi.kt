package com.example.sushibooking.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class RecipeResponse(val recipes: List<Recipe> = emptyList())

data class Recipe(
    val id: Int,
    val name: String,
    val cuisine: String? = null,
    val difficulty: String? = null
)

interface RecipeApi {
    @GET("recipes/search")
    suspend fun searchRecipes(@Query("q") query: String = "Japanese"): RecipeResponse

    @GET("recipes")
    suspend fun getAllRecipes(): RecipeResponse

    companion object {
        val instance: RecipeApi by lazy {
            Retrofit.Builder()
                .baseUrl("https://dummyjson.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RecipeApi::class.java)
        }
    }
}
