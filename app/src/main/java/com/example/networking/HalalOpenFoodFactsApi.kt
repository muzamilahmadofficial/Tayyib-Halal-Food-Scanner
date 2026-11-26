package com.example.networking

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface HalalOpenFoodFactsApi {

    @GET("proxy/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = DEFAULT_FIELDS
    ): Response<ProductLookupResponse>

    @GET("proxy/v2/search")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1,
        @Query("is_halal") isHalal: Int? = null,
        @Query("halal_status") halalStatus: String? = null,
        @Query("countries_tags_en") country: String? = null,
        @Query("categories_tags_en") category: String? = null,
        @Query("fields") fields: String = DEFAULT_FIELDS
    ): Response<ProductSearchResponse>

    companion object {
        const val BASE_URL = "https://halalopenfoodfacts.org/"
        const val DEFAULT_FIELDS =
            "code,product_name,product_name_en,product_name_fr,brands," +
                "image_url,image_front_url,image_front_small_url," +
                "ingredients_text,ingredients_text_en,ingredients_text_fr," +
                "ingredients_tags,additives_tags,allergens_tags,labels_tags," +
                "categories_tags,countries_tags,ingredients_analysis_tags," +
                "halal_status,halal,is_halal,product_url,url,last_modified_t,nutriscore_grade"
    }
}

/**
 * Automatic live backup API using official Open Food Facts (https://world.openfoodfacts.org/).
 * Used transparently whenever Halal Open Food Facts is temporarily unreachable, returns an error,
 * or does not yet list a barcode. All retrieved products are independently analyzed by Tayyib's
 * local HalalClassificationEngine.
 */
interface OpenFoodFactsBackupApi {

    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = BACKUP_FIELDS
    ): Response<ProductLookupResponse>

    @GET("cgi/search.pl?search_simple=1&action=process&json=1")
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1,
        @Query("countries_tags_en") country: String? = null,
        @Query("categories_tags_en") category: String? = null,
        @Query("labels_tags_en") labelsTag: String? = null,
        @Query("fields") fields: String = BACKUP_FIELDS
    ): Response<ProductSearchResponse>

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
        const val BACKUP_FIELDS =
            "code,product_name,product_name_en,product_name_fr,brands," +
                "image_url,image_front_url,image_front_small_url," +
                "ingredients_text,ingredients_text_en,ingredients_text_fr," +
                "ingredients_tags,additives_tags,allergens_tags,labels_tags," +
                "categories_tags,countries_tags,ingredients_analysis_tags," +
                "url,last_modified_t,nutriscore_grade"
    }
}

object NetworkModule {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun buildClient(): OkHttpClient {
        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "TayyibHalalFoodScanner/1.0.0 (Android; Independent App; Muzamilofficially@gmail.com)"
                )
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(logging)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val sharedClient: OkHttpClient by lazy { buildClient() }

    fun createApi(): HalalOpenFoodFactsApi =
        Retrofit.Builder()
            .baseUrl(HalalOpenFoodFactsApi.BASE_URL)
            .client(sharedClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HalalOpenFoodFactsApi::class.java)

    fun createBackupApi(): OpenFoodFactsBackupApi =
        Retrofit.Builder()
            .baseUrl(OpenFoodFactsBackupApi.BASE_URL)
            .client(sharedClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenFoodFactsBackupApi::class.java)
}
